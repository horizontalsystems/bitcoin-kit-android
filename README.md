# BitcoinKit

Bitcoin and Bitcoin Cash(ABC) SPV wallet toolkit implemented in Kotlin. This is a full implementation of SPV node including wallet creation/restore, syncronzation with network, send/receive transactions, and more.

## Features

- Full SPV implementation for fast mobile performance
- Send/Receive Legacy transactions (*P2PKH*, *P2PK*, *P2SH*)
- Send/Receive Segwit transactions (*P2WPKH*)
- Send/Receive Segwit transactions compatible with legacy wallets (*P2WPKH-SH*)
- Fee calculation depending on wether sender pays the fee or receiver
- Encoding/Decoding of base58, bech32, cashaddr addresses
- [BIP32](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki) hierarchical deterministic wallets implementation.
- [BIP39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki) mnemonic code for generating deterministic keys.
- [BIP44](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki) multi-account hierarchy for deterministic wallets.
- [BIP21](https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki) URI schemes, which include payment address, amount, label and other params
- Real-time fee rates

## Usage

### Initialization

`BitcoinKit` requires you to provide mnemonic phrase when it is initialized:

```kotlin
val words = listOf("skill", ..., "century")
```

#### Bitcoin

```kotlin
val context: Context = App.instance
val walletId = 'MyWalletId'
val bitcoinKit = BitcoinKit(context, walletId, words, BitcoinKit.NetworkType.MainNet)
```

#### Bitcoin Cash
```kotlin
val bitCashKit = BitcoinCashKit(context, walletId, words, BitcoinCashKit.NetworkType.MainNet)
```

Both networks can be configured to work in `MainNet` or `TestNet`

##### Additional parameters:

- `peerSize`: Can be configured for number of peers to be connected (*default: 10*)
- `confirmationsThreshold`: Minimum number of confirmations required for an unspent output in incoming transaction to be spent (*default: 6*)

### Starting and Stopping

`BitcoinKit` requires to be started with `start` command, but does not need to be stopped. It will be in synced state as long as it is possible:

```kotlin
bitcoinKit.start()
```

#### Clearing data from device

`BitcoinKit` uses internal database for storing data fetched from blockchain. The `clear` command can be used to stop and clean all stored data:

```kotlin
bitcoinKit.clear()
```

### Getting wallet data

`BitcoinKit` holds all kinds of data obtained from and needed for working with blockchain network

#### Current Balance

Balance is provided in `Satoshi`:

```kotlin
bitcoinKit.balance
// 2937096768
```

#### Receive Address

Get an address which you can receive coins to. Receive address is changed each time after you actually get a transaction in which you receive coins to that address
```kotlin
bitcoinKit.receiveAddress()
// mfu3iYeZuh18uuN4CrGYz34HSMynZBgZyg
```

#### Transactions

You can get all your transactions as follows:
```kotlin
bitcoinKit.transactions

TransactionInfo(
    transactionHash=baede6420b4b2869cba87d768a7b4e2eef2a9899149f24c5c3d3ff66b8ad1405,
    from=[TransactionAddress(address=my1ysPMHdeQD6kwnimipbvFrr5NRHDvNgz, mine=false)],
    to=[
        TransactionAddress(address=moyrWfrks5EsHLb2hqUr8nUnC9q9DYXMtK, mine=true),
        TransactionAddress(address=2NDk1BuuKnbVqDAMVgGr6xurnctfaQVxFoN, mine=false)
    ],
    amount=32500000,
    blockHeight=1316822,
    timestamp=1527595501
  ),
  TransactionInfo(
    transactionHash=60ff9d204c17e0e71a1fd2d3f60b1bada5573857691d30d7518311057cc75720, 
    from=[
        TransactionAddress(address=mtmnfGuW4e5RbC64r3vx2qFhS3pobf57Ty, mine=false)
    ], 
    to=[
        TransactionAddress(address=mt59yxihUxy117RPQuEVQ7QSRuHJnRT4fG, mine=true), 
        TransactionAddress(address=2NAn7FBKvMFWteiHKSDDBudUP4vPwiBfVkr, mine=false)
    ], 
    amount=16250000, 
    blockHeight=1316823, 
    timestamp=1527595771
)
...
```

### Sending new transaction

In order to create new transaction, call `send(address: String, value: Int, senderPay: Boolean = true)` method on `BitcoinKit`
 - `senderPay`: parameter defines who pays the fee

```kotlin
bitcoinKit.send("mrjQyzbX9SiJxRC2mQhT4LvxFEmt9KEeRY", 1000000)
```

This function first validates a given address and amount, creates new transaction, then sends it over the peers network. If there's any error with given address/amount or network, it raises an exception.

#### Validating transaction before send

One can validate address and fee by using following methods:

```
bitcoinKit.validate(address = "mrjQyzbX9SiJxRC2mQhT4LvxFEmt9KEeRY")
bitcoinKit.fee(value = 1000000, address = "mrjQyzbX9SiJxRC2mQhT4LvxFEmt9KEeRY", senderPay: true)
```
- `senderPay`: parameter defines who pays the fee


### Parsing BIP21 URI

You can use `parsePaymentAddress` method to parse a BIP21 URI:

```kotlin
bitcoinKit.parsePaymentAddress("bitcoin:175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W?amount=50&label=Luke-Jr&message=Donation%20for%20project%20xyz")

BitcoinPaymentData(
  address=175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W, 
  version=null, 
  amount=50.0,
  label=Luke-Jr,
  message=Donation for project xyz,
  parameters=null
)
```

### Subscribing to BitcoinKit data

`BitcoinKit` provides with data like transactions, blocks, balance, kitState in real-time. `BitcoinKit.Listener` interface must be implemented and set to `BitcoinKit` instance to receive that data.

```kotlin
class MainViewModel : ViewModel(), BitcoinKit.Listener {

    private var bitcoinKit: BitcoinKit
    ...
    
    init {
        bitcoinKit = BitcoinKit(words, BitcoinKit.NetworkType.TestNet)
        bitcoinKit.listener = this
    }
    
    override fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        // do something with transactions
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        // do something with transactions
    }

    override fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) {
        // do something with balance
    }

    override fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo) {
        // do something with blockInfo
    }

    override fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: KitState) {
        // These states can be used to implement progress bar, etc
        when (state) {
            is KitState.Synced -> {

            }
            is KitState.Syncing -> {
                // val progress: Double = state.progress
            }
            is KitState.NotSynced -> {
                
            }
        }
    }
}
```

Listener events are run in a dedicated background thread. It can be switched to main thread by setting the ```listenerExecutor``` property to ```MainThreadExecutor()```

```kotlin

bitcoinKit.listenerExecutor = MainThreadExecutor()

``` 

## Prerequisites
* JDK >= 1.8
* Android 6 (minSdkVersion 23) or greater

## Installation
Add the JitPack to module build.gradle
```
repositories {
    maven { url 'https://jitpack.io' }
}
```
Add the following dependency to your build.gradle file:
```
dependencies {
    implementation 'com.github.horizontalsystems:bitcoin-kit-android:master-SNAPSHOT'
}
```

## Example App

All features of the library are used in example project. It can be referred as a starting point for usage of the library.
* [Example App](https://github.com/horizontalsystems/bitcoin-kit-android/tree/master/app)

## Dependencies
* [HDWalletKit](https://github.com/horizontalsystems/hd-wallet-kit-android) - HD Wallet related features, mnemonic phrase 

## Contributing

[Contributing](CONTRIBUTING.md)

## License

The `BitcoinKit` is open source and available under the terms of the [MIT License](https://github.com/horizontalsystems/bitcoin-kit-android/blob/master/LICENSE)
