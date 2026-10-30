// mkcheckpoint writes a header checkpoint bundle from a synced mwebd data
// directory. The bundle seeds the header stores of a fresh data directory so
// the first sync starts at the bundle height instead of genesis.
//
// Stop the daemon that uses the data directory before running this: the
// database is locked while it runs.
//
//	mkcheckpoint -chain mainnet -datadir ~/.mwebd -out checkpoint-mainnet.bin
package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"time"

	"github.com/ltcmweb/ltcd/chaincfg"
	"github.com/ltcmweb/ltcd/wire"
	"github.com/ltcmweb/neutrino/headerfs"
	"github.com/ltcmweb/neutrino/mwebdb"
	"github.com/ltcsuite/ltcwallet/walletdb"
	_ "github.com/ltcsuite/ltcwallet/walletdb/bdb"
)

var (
	chain   = flag.String("chain", "mainnet", "mainnet or testnet")
	dataDir = flag.String("datadir", ".", "synced mwebd data directory")
	out     = flag.String("out", "", "output file")
	height  = flag.Uint("height", 0, "checkpoint height (default: tip minus lag)")
	lag     = flag.Uint("lag", 100, "blocks below the tip when -height is 0")
	headers = flag.Uint("headers", 2017, "number of headers to bundle (at least one retarget window plus one)")
	noMweb  = flag.Bool("no-mweb-leaves", false, "omit the MWEB leaves-at-height map")
)

func main() {
	flag.Parse()
	if *out == "" {
		log.Fatal("-out is required")
	}

	var params *chaincfg.Params
	switch *chain {
	case "mainnet":
		params = &chaincfg.MainNetParams
	case "testnet":
		params = &chaincfg.TestNet4Params
	default:
		log.Fatalf("unknown chain %q", *chain)
	}

	db, err := walletdb.Open("bdb", filepath.Join(*dataDir, "neutrino.db"),
		true, 10*time.Second)
	if err != nil {
		log.Fatalf("open database: %v", err)
	}
	defer db.Close()

	blockHeaders, err := headerfs.NewBlockHeaderStore(*dataDir, db, params)
	if err != nil {
		log.Fatalf("open block headers: %v", err)
	}
	filterHeaders, err := headerfs.NewFilterHeaderStore(*dataDir, db,
		headerfs.RegularFilter, params, nil)
	if err != nil {
		log.Fatalf("open filter headers: %v", err)
	}

	_, tipHeight, err := blockHeaders.ChainTip()
	if err != nil {
		log.Fatalf("block header tip: %v", err)
	}
	_, filterTip, err := filterHeaders.ChainTip()
	if err != nil {
		log.Fatalf("filter header tip: %v", err)
	}

	h := uint32(*height)
	if h == 0 {
		h = tipHeight - uint32(*lag)
	}
	if h > filterTip {
		log.Fatalf("filter headers synced to %d only, want %d", filterTip, h)
	}
	n := uint32(*headers)
	if n > h {
		log.Fatalf("cannot bundle %d headers ending at height %d", n, h)
	}

	cp := &headerfs.Checkpoint{
		Net:        params.Net,
		Height:     h,
		Headers:    make([]wire.BlockHeader, 0, n),
		MwebLeaves: make(map[uint32]uint64),
	}
	for i := h - n + 1; i <= h; i++ {
		header, err := blockHeaders.FetchHeaderByHeight(i)
		if err != nil {
			log.Fatalf("block header %d: %v", i, err)
		}
		cp.Headers = append(cp.Headers, *header)
	}

	filterHeader, err := filterHeaders.FetchHeaderByHeight(h)
	if err != nil {
		log.Fatalf("filter header %d: %v", h, err)
	}
	cp.FilterHeader = *filterHeader

	if !*noMweb {
		coins, err := mwebdb.NewCoinStore(db)
		if err != nil {
			log.Fatalf("open mweb coins: %v", err)
		}
		leaves, err := coins.GetLeavesAtHeight()
		if err != nil {
			log.Fatalf("mweb leaves: %v", err)
		}
		for height, count := range leaves {
			if height <= h {
				cp.MwebLeaves[height] = count
			}
		}
	}

	if err := cp.Validate(params.Net); err != nil {
		log.Fatalf("checkpoint: %v", err)
	}

	f, err := os.Create(*out)
	if err != nil {
		log.Fatalf("create %s: %v", *out, err)
	}
	if err := headerfs.WriteCheckpoint(f, cp); err != nil {
		log.Fatalf("write %s: %v", *out, err)
	}
	if err := f.Close(); err != nil {
		log.Fatalf("close %s: %v", *out, err)
	}

	info, _ := os.Stat(*out)
	fmt.Printf("%s: %s height %d, headers %d..%d, block hash %s, "+
		"%d mweb leaf entries, %d bytes\n", *out, *chain, cp.Height,
		cp.StartHeight(), cp.Height,
		cp.Headers[len(cp.Headers)-1].BlockHash(), len(cp.MwebLeaves),
		info.Size())
}
