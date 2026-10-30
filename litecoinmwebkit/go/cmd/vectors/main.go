// vectors prints the reference values used by the kit's Kotlin unit tests:
// MWEB scan/spend keys derived from a BIP39 seed, the addresses they yield,
// the address codec round trip and MWEB fee estimates. Run it after changing
// the pinned mwebd or ltcd version and compare with
// src/test/kotlin/io/horizontalsystems/litecoinmwebkit/*Test.kt.
package main

import (
	"encoding/hex"
	"fmt"

	"crypto/sha512"
	"github.com/ltcmweb/ltcd/chaincfg"
	"github.com/ltcmweb/ltcd/chaincfg/chainhash"
	"github.com/ltcmweb/ltcd/ltcutil"
	"github.com/ltcmweb/ltcd/ltcutil/hdkeychain"
	"github.com/ltcmweb/ltcd/ltcutil/mweb"
	"github.com/ltcmweb/ltcd/ltcutil/mweb/mw"
	"github.com/ltcmweb/ltcd/txscript"
	"github.com/ltcmweb/ltcd/wire"
	"github.com/ltcmweb/mwebd/sign"
	"golang.org/x/crypto/pbkdf2"
)

func seed(mnemonic, passphrase string) []byte {
	return pbkdf2.Key([]byte(mnemonic), []byte("mnemonic"+passphrase), 2048, 64, sha512.New)
}

func main() {
	mnemonic := "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
	s := seed(mnemonic, "")
	fmt.Printf("mnemonic: %s\nseed: %x\n", mnemonic, s)

	master, err := hdkeychain.NewMaster(s, &chaincfg.MainNetParams)
	if err != nil {
		panic(err)
	}
	purpose, _ := master.Derive(hdkeychain.HardenedKeyStart + 1000)
	scanKey, _ := purpose.Derive(hdkeychain.HardenedKeyStart + 0)
	spendKey, _ := purpose.Derive(hdkeychain.HardenedKeyStart + 1)
	scanPriv, _ := scanKey.ECPrivKey()
	spendPriv, _ := spendKey.ECPrivKey()
	scan := scanPriv.Key.Bytes()
	spend := spendPriv.Key.Bytes()
	spendPub := spendPriv.PubKey().SerializeCompressed()
	fmt.Printf("scan secret  (m/1000'/0'): %x\n", scan[:])
	fmt.Printf("spend secret (m/1000'/1'): %x\n", spend[:])
	fmt.Printf("spend pubkey: %x\n", spendPub)

	for _, params := range []*chaincfg.Params{&chaincfg.MainNetParams, &chaincfg.TestNet4Params} {
		resp := sign.Addresses(&sign.AddressesRequest{
			Scan: scan[:], SpendPub: spendPub, From: 0, To: 3,
		}, params)
		for i, a := range resp.Address {
			addr, err := ltcutil.DecodeAddress(a, params)
			if err != nil {
				panic(err)
			}
			fmt.Printf("%s address %d: %s\n  scan+spend pubkeys: %x\n", params.Name, i, a,
				addr.(*ltcutil.AddressMweb).ScriptAddress())
		}
	}

	// Fee estimates (litoshi): kernel with stealth excess (3 weight) plus
	// 18 weight per MWEB output, 100 litoshi per weight; peg-outs add
	// ceil(len(script)/42) weight plus their serialized size at the fee rate.
	kc := &mweb.Keychain{Scan: (*mw.SecretKey)(scan[:]), Spend: (*mw.SecretKey)(spend[:])}
	mwebScript := append([]byte{}, ltcutil.NewAddressMweb(kc.Address(1), &chaincfg.MainNetParams).ScriptAddress()...)
	p2wpkh, _ := txscript.NewScriptBuilder().AddOp(txscript.OP_0).AddData(make([]byte, 20)).Script()
	p2pkh, _ := txscript.NewScriptBuilder().AddOp(txscript.OP_DUP).AddOp(txscript.OP_HASH160).
		AddData(make([]byte, 20)).AddOp(txscript.OP_EQUALVERIFY).AddOp(txscript.OP_CHECKSIG).Script()
	cases := []struct {
		name    string
		outputs []*wire.TxOut
		feeRate int64
	}{
		{"one mweb output", []*wire.TxOut{wire.NewTxOut(1000, mwebScript)}, 10000},
		{"two mweb outputs", []*wire.TxOut{wire.NewTxOut(1000, mwebScript), wire.NewTxOut(2000, mwebScript)}, 10000},
		{"p2wpkh pegout + mweb change @10000", []*wire.TxOut{wire.NewTxOut(5000, p2wpkh), wire.NewTxOut(1000, mwebScript)}, 10000},
		{"p2wpkh pegout @2500", []*wire.TxOut{wire.NewTxOut(5000, p2wpkh)}, 2500},
		{"p2pkh pegout @1000", []*wire.TxOut{wire.NewTxOut(5000, p2pkh)}, 1000},
	}
	for _, c := range cases {
		fee := mweb.EstimateFee(c.outputs, ltcutil.Amount(c.feeRate), false)
		fmt.Printf("fee %s, feeRatePerKb=%d: %d\n", c.name, c.feeRate, fee)
	}
	fmt.Printf("p2wpkh script: %x (%d bytes, serialized %d)\n", p2wpkh, len(p2wpkh), wire.NewTxOut(0, p2wpkh).SerializeSize())
	fmt.Printf("pegin script for zero kernel hash: %x\n", mweb.NewPegin(1, &chainhash.Hash{}).PkScript)
	_ = hex.EncodeToString
}
