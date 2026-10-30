# litecoinmwebkit

Litecoin MWEB (MimbleWimble Extension Blocks, LIP-0002/3/4) wallet kit for
Android. It embeds [`mwebd`](https://github.com/ltcmweb/mwebd), the Go MWEB
daemon that Cake Wallet, Stack Wallet, Litewallet and Electrum-LTC use, as a
shared library and talks to it over gRPC on the loopback interface. The
daemon runs the MWEB light client (block headers, a few MWEB headers and the
MWEB UTXO set from Litecoin peers with the `NODE_MWEB_LIGHT_CLIENT` service
bit) and all MWEB cryptography; the Kotlin side owns keys, address indices,
persisted history and the peg-in orchestration with `litecoinkit`.

## Layout

| Path | Contents |
|---|---|
| `go/` | Go module: the cgo exports (`mwebd.go`), `build.sh`, host tools `cmd/mwebd` (dev runner) and `cmd/mkcheckpoint` (checkpoint bundle generator) |
| `patches/` | Patches to the pinned upstream modules, applied by `go/build.sh` ([details](patches/README.md)) |
| `external-libs/<abi>/libmwebd.so` | Daemon built per ABI by `go/build.sh`, packaged as a JNI library |
| `external-libs/include/mwebd.h` | C interface of the daemon library |
| `src/main/cpp/` | JNI shim (`libmwebkit.so`) between Kotlin and `libmwebd.so` |
| `src/main/assets/mweb/checkpoint-*.bin` | Header checkpoint bundles per network |
| `src/main/kotlin/` | Kotlin API |

## Rebuilding the native library

Prerequisites on the build machine:

- Go. `go/go.mod` pins the toolchain (`toolchain go1.27.3`); with
  `GOTOOLCHAIN=auto` (the default) the Go command downloads it when the
  installed version differs.
- Android NDK `27.0.12077973` under `$ANDROID_HOME/ndk/` (the version Gradle
  uses for the JNI shim, see `ndkVersion` in `build.gradle`), or
  `ANDROID_NDK_HOME` pointing at it.
- `patch`.

Then:

```bash
cd litecoinmwebkit/go
./build.sh            # prepare patched sources, build arm64-v8a, armeabi-v7a, x86_64
```

`build.sh prepare` downloads the pinned `github.com/ltcmweb/neutrino` and
`github.com/ltcmweb/mwebd` into the module cache, copies them to `go/build/`
(git-ignored) and applies `patches/`. The build is
`go build -buildmode=c-shared` per ABI with the NDK clang for API 23,
`-trimpath`, `-buildvcs=false`, stripped (`-s -w`), without a Go build id or
an ELF build id, and with 16 KB ELF segment alignment
(`-Wl,-z,max-page-size=16384`) so the library loads on devices with 16 KB
pages. Only the hand-written `external-libs/include/mwebd.h` is kept as the
header; the one `go build` generates uses Go-specific typedefs and is
deleted.

Commit the resulting `external-libs/*/libmwebd.so` together with the change
that required them (`go/`, `patches/`).

To run the patched daemon on the development machine:

```bash
cd litecoinmwebkit/go
./build.sh host
./build/mwebd-host -c mainnet -d /tmp/mweb -l 127.0.0.1:12345 \
    -checkpoint ../src/main/assets/mweb/checkpoint-mainnet.bin -blocksonly
grpcurl -plaintext -import-path build/mwebd/proto -proto mwebd.proto 127.0.0.1:12345 Rpc.Status
```

## Header checkpoint bundles

A fresh data directory would otherwise download the whole Litecoin header
chain (3.2 million headers, about 400 MB on the wire and 700 MB on disk)
before the comparatively tiny MWEB data. The bundles in
`src/main/assets/mweb/` seed the header stores at a recent height, so the
first sync downloads only what came after the bundle: about a minute on a
laptop. Refresh them at release time from a synced data directory (stop the
daemon using it first; the database is locked while it runs):

```bash
cd litecoinmwebkit/go
go run ./cmd/mkcheckpoint -chain mainnet -datadir /tmp/mweb -out ../src/main/assets/mweb/checkpoint-mainnet.bin
go run ./cmd/mkcheckpoint -chain testnet -datadir /tmp/mweb-testnet -out ../src/main/assets/mweb/checkpoint-testnet.bin
```

`mkcheckpoint` takes the tip minus 100 blocks by default and bundles the 2017
headers ending there, the compact filter header at that height and the MWEB
leaves-at-height map (320 KB for mainnet). Check the printed block hash
against a block explorer before committing: a wrong bundle makes every
client follow a wrong chain. Data directories that already hold headers
ignore the bundle.
