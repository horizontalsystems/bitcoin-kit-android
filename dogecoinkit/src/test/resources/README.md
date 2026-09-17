# Block fixtures

`dogecoin_block*.bin` are serialized Dogecoin mainnet blocks taken from
[libdohj](https://github.com/dogecoin/libdohj), `core/src/test/resources/org/bitcoinj/core/`,
which is licensed under the Apache License 2.0 (Copyright Google Inc., Andreas Schildbach,
J. Ross Nicoll).

They cover the three cases the AuxPoW parser has to get right:

| fixture | height | notes |
|---|---|---|
| `dogecoin_block1.bin` | 1 | before merge mining, no AuxPoW |
| `dogecoin_block250000.bin` | 250,000 | before merge mining, several transactions |
| `dogecoin_block371337.bin` | 371,337 | the first merge-mined block |
| `dogecoin_block748634.bin` | 748,634 | merge-mined |
| `dogecoin_block894863.bin` | 894,863 | merge-mined |

These blocks are from 2014-2015, so their AuxPoW parent coinbase transactions predate Litecoin's
SegWit activation. That is still representative of the chain today: sampling mainnet at heights
5,800,000 and 6,377,800 (September 2026) shows the parent coinbase is serialized without a witness
in both cases — `size` equals `vsize` and there is no `0001` marker. `AuxPoW.skip` reads it with
`TransactionSerializer`, which handles either encoding, so the distinction does not change the
parse either way.
