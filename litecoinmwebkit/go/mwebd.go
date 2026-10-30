// Package main builds the MWEB daemon as a C archive for the Android kit.
//
// It exposes a handful of C functions over cgo. The daemon itself
// (github.com/ltcmweb/mwebd) is reached from Kotlin over gRPC on the
// loopback port that mweb_start returns.
package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"errors"
	"fmt"
	"strings"
	"sync"
	"unsafe"

	"github.com/ltcmweb/ltcd/chaincfg"
	"github.com/ltcmweb/mwebd"
	"github.com/ltcmweb/mwebd/sign"
	"github.com/ltcmweb/neutrino/headerfs"
)

var (
	mtx    sync.Mutex
	server *mwebd.Server
	port   int
)

// setError stores a malloc'd copy of the error message in *errOut. The
// caller releases it with mweb_free.
func setError(errOut **C.char, err error) {
	if errOut != nil {
		*errOut = C.CString(err.Error())
	}
}

// mweb_start starts the daemon on a loopback gRPC port and returns the port.
// chain is "mainnet", "testnet" or "regtest". proxyURL is a SOCKS proxy URL
// such as "socks5://127.0.0.1:9050", or empty for direct connections.
// checkpointPath points to a header checkpoint bundle written by
// mkcheckpoint, or is empty to sync from genesis. A non-zero blocksOnly asks
// peers not to relay unconfirmed transactions. On failure it returns -1 and
// sets *errOut. Calling it while the daemon runs returns the current port.
//
//export mweb_start
func mweb_start(dataDir, chain, proxyURL, checkpointPath *C.char,
	blocksOnly C.int, errOut **C.char) C.int {

	mtx.Lock()
	defer mtx.Unlock()

	if server != nil {
		return C.int(port)
	}

	args := &mwebd.ServerArgs{
		Chain:      C.GoString(chain),
		DataDir:    C.GoString(dataDir),
		ProxyAddr:  C.GoString(proxyURL),
		BlocksOnly: blocksOnly != 0,
	}

	if path := C.GoString(checkpointPath); path != "" {
		cp, err := headerfs.ReadCheckpointFile(path)
		if err != nil {
			setError(errOut, fmt.Errorf("header checkpoint: %w", err))
			return -1
		}
		args.HeaderCheckpoint = cp
	}

	s, err := mwebd.NewServer2(args)
	if err != nil {
		setError(errOut, err)
		return -1
	}

	p, err := s.Start(0)
	if err != nil {
		s.Stop()
		setError(errOut, err)
		return -1
	}

	server, port = s, p
	return C.int(p)
}

// mweb_stop stops the daemon. The chain service and database are closed
// asynchronously once the gRPC server has drained; a following mweb_start
// waits on the database lock until that has happened.
//
//export mweb_stop
func mweb_stop() {
	mtx.Lock()
	defer mtx.Unlock()

	if server != nil {
		server.Stop()
		server, port = nil, 0
	}
}

// mweb_addresses derives the MWEB addresses with indices [from, to) for the
// account given by its 32-byte scan secret and 33-byte spend public key,
// both hex encoded, and returns them newline separated. The result is
// malloc'd and released with mweb_free. On failure it returns NULL and sets
// *errOut.
//
//export mweb_addresses
func mweb_addresses(scanSecretHex, spendPubKeyHex, chain *C.char,
	from, to C.uint, errOut **C.char) *C.char {

	scan, err := decodeHex(C.GoString(scanSecretHex), 32)
	if err != nil {
		setError(errOut, fmt.Errorf("scan secret: %w", err))
		return nil
	}
	spendPub, err := decodeHex(C.GoString(spendPubKeyHex), 33)
	if err != nil {
		setError(errOut, fmt.Errorf("spend pubkey: %w", err))
		return nil
	}
	if to < from {
		setError(errOut, errors.New("invalid index range"))
		return nil
	}

	params, err := chainParams(C.GoString(chain))
	if err != nil {
		setError(errOut, err)
		return nil
	}

	resp := sign.Addresses(&sign.AddressesRequest{
		Scan:     scan,
		SpendPub: spendPub,
		From:     uint32(from),
		To:       uint32(to),
	}, params)

	return C.CString(strings.Join(resp.Address, "\n"))
}

// mweb_free releases a string returned by this library.
//
//export mweb_free
func mweb_free(p *C.char) {
	C.free(unsafe.Pointer(p))
}

func chainParams(chain string) (*chaincfg.Params, error) {
	switch chain {
	case "mainnet":
		return &chaincfg.MainNetParams, nil
	case "testnet":
		return &chaincfg.TestNet4Params, nil
	case "regtest":
		return &chaincfg.RegressionNetParams, nil
	}
	return nil, fmt.Errorf("unknown chain %q", chain)
}

func decodeHex(s string, size int) ([]byte, error) {
	if len(s) != size*2 {
		return nil, fmt.Errorf("want %d hex characters, got %d", size*2,
			len(s))
	}
	out := make([]byte, size)
	for i := 0; i < size; i++ {
		var b byte
		for _, c := range s[2*i : 2*i+2] {
			b <<= 4
			switch {
			case c >= '0' && c <= '9':
				b |= byte(c - '0')
			case c >= 'a' && c <= 'f':
				b |= byte(c-'a') + 10
			case c >= 'A' && c <= 'F':
				b |= byte(c-'A') + 10
			default:
				return nil, fmt.Errorf("invalid hex character %q", c)
			}
		}
		out[i] = b
	}
	return out, nil
}

func main() {}
