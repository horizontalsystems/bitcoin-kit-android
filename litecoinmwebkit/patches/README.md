# Patches to the MWEB daemon and its light client

`go/build.sh prepare` copies the two pinned upstream modules out of the Go
module cache into `go/build/` and applies these patches in file-name order.
`go/go.mod` replaces the modules with those copies, so every build of
`libmwebd.so` and of the host tools uses the patched code.

| Module | Pinned version | Patches |
|---|---|---|
| `github.com/ltcmweb/neutrino` (MWEB light client) | v0.17.4 | `neutrino/0001`, `neutrino/0002` |
| `github.com/ltcmweb/mwebd` (daemon, gRPC API) | v0.1.19 | `mwebd/0001` |

All three are candidates for upstream pull requests; nothing in them is
specific to this kit.

## neutrino/0001-header-checkpoint-start.patch

Starts the header chain from a trusted recent snapshot instead of genesis.

Without it a fresh data directory downloads every Litecoin block header
(3.2 million, 256 MB) plus every compact filter header (100 MB) and builds a
300 MB bbolt index before it can fetch the small MWEB UTXO set: 7 minutes on a
laptop, 30 minutes on a phone, 400 MB on the wire. With a checkpoint the
first sync downloads only the headers above the checkpoint height and the MWEB
data, about a minute.

- New `headerfs.Checkpoint`: the 2017 block headers ending at height H (one
  retarget window, so difficulty and median-time checks on header H+1 have
  their ancestors), the regular filter header at H, and optionally the MWEB
  leaves-at-height map for heights up to H. `ReadCheckpoint` /
  `WriteCheckpoint` serialize it; `go/cmd/mkcheckpoint` writes one from a
  synced data directory.
- `headerfs.NewBlockHeaderStoreFromCheckpoint` and
  `NewFilterHeaderStoreFromCheckpoint` seed an empty store: the genesis header
  is written at offset 0 as before, the bundled headers are written at their
  real flat-file offsets (`height * 80`, the file is sparse in between), the
  hash to height index gets the bundled hashes, and the chain tip is H. The
  original constructors call the new ones with a nil checkpoint and are
  unchanged in behaviour.
- The stores remember the seeded start height in the index and return
  `ErrHeaderNotFound` for heights between genesis and it, instead of reading
  zeros from the sparse file. Three readers learn to cope: the block locator
  jumps from the start height to genesis, the compact filter checkpoint
  sanity check skips heights the store does not have, and the MWEB header
  fetch skips those heights too (the bundled leaves map already covers
  them).
- `neutrino.Config.HeaderCheckpoint` carries the checkpoint into
  `NewChainService`, which also seeds the MWEB coin database's
  leaves-at-height map from it when that map is empty.

The checkpoint is trusted the same way bitcoinkit's bundled checkpoints are:
a wrong one makes the client follow a wrong chain, so bundles are generated
from a node that is at the real tip and refreshed at kit release time.

## neutrino/0002-leafset-from-one-peer.patch

Requests the `mwebheader` and `mwebleafset` of a new block from one peer,
through the work manager that retries on another peer when the first does not
answer, instead of broadcasting the request to every connected peer and
keeping the first verified answer. The leafset is one bit per MWEB output ever
created (48 KB today), so with eight peers the old behaviour cost about
400 KB per block; now it is about 55 KB.

The same patch stops the handler from asking for the leafset of a stale
tip. Litecoin Core serves `mwebleafset` and `getmwebutxos` only for blocks
within 10 of its tip (`MAX_MWEB_LEAFSET_DEPTH`) and disconnects a peer that
asks for anything older. `BlockHeadersSynced` is true whenever no sync peer
is selected, which is the case right after start-up and whenever the sync
peer just left, so the unpatched handler requests the leafset of whatever
tip is on disk; after a checkpoint start, or a restart after an hour away,
that tip is always too deep. With few peers (testnet has two) every
connection was dropped before any header arrived and the sync never
progressed. Now the handler waits for new headers while the tip header is
older than 10 block intervals, and pauses 5 seconds instead of 1 after a
failed fetch.

## mwebd/0001-blocks-only-and-header-checkpoint.patch

Adds two fields to `mwebd.ServerArgs` and forwards them to `neutrino.Config`:

- `BlocksOnly` sets the light client's existing blocks-only mode
  (`DisableRelayTx` in the version message), which stops peers from
  relaying unconfirmed transaction announcements, about 1.3 KB/s of idle
  traffic the daemon never uses: it learns about its own unconfirmed MWEB
  outputs through `Broadcast`.
- `HeaderCheckpoint` passes a checkpoint from patch 0001.

## Updating

1. Bump the version in `go/go.mod` (`require` line) and run
   `go/build.sh prepare`. `patch --forward` reports rejected hunks.
2. Fix the sources under `go/build/<module>` until `go build ./...` in `go/`
   passes, then regenerate the patch from the pristine module:

       cd go && M=$(go env GOMODCACHE)/github.com/ltcmweb/neutrino@vX.Y.Z
       diff -uN --label a/FILE --label b/FILE $M/FILE build/neutrino/FILE > ../patches/neutrino/NNNN-name.patch

   (one `diff -uN` per changed file, concatenated into the patch).
3. Run `go/build.sh` to rebuild `external-libs/*/libmwebd.so` and commit the
   patches, `go.mod`, `go.sum` and the libraries together.
