// mwebd runs the patched daemon as a plain process for development, the same
// way the upstream github.com/ltcmweb/mwebd/cmd/mwebd does, with the two
// extra options the kit uses.
//
//	mwebd -c mainnet -d /tmp/mweb -l 127.0.0.1:12345 -checkpoint checkpoint-mainnet.bin -blocksonly
package main

import (
	"flag"
	"log"

	"github.com/ltcmweb/mwebd"
	"github.com/ltcmweb/neutrino/headerfs"
)

var (
	chain      = flag.String("c", "mainnet", "Chain")
	dataDir    = flag.String("d", ".", "Data directory")
	peer       = flag.String("p", "", "Connect to peer")
	bindAddr   = flag.String("l", "127.0.0.1:12345", "Bind address")
	proxy      = flag.String("proxy", "", `Proxy address (e.g. "socks5://127.0.0.1:9050")`)
	checkpoint = flag.String("checkpoint", "", "Header checkpoint bundle (see cmd/mkcheckpoint)")
	blocksOnly = flag.Bool("blocksonly", false, "Do not receive unconfirmed transaction relay")
)

func main() {
	flag.Parse()

	args := &mwebd.ServerArgs{
		Chain: *chain, DataDir: *dataDir,
		PeerAddr: *peer, ProxyAddr: *proxy,
		BlocksOnly: *blocksOnly,
	}
	if *checkpoint != "" {
		cp, err := headerfs.ReadCheckpointFile(*checkpoint)
		if err != nil {
			log.Fatalln("Unable to read checkpoint:", err)
		}
		args.HeaderCheckpoint = cp
	}

	server, err := mwebd.NewServer2(args)
	if err != nil {
		log.Fatalln("Unable to start server:", err)
	}
	if _, err = server.StartAddr(*bindAddr); err != nil {
		log.Fatalln("Failed to listen:", err)
	}
}
