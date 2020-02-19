## Implementing support for a new coin for external developers

Support for coin is implemented as a separate module that depends on the module `bitcoincore`. This repository contains modules for supporting coins like `Bitcoin`, `BitcoinCash` and `Dash`. Support for a new coin should be implemented in the owners repository.

### Structure of module

The module depends on the `bitcoincore`. This dependency can be added via JitPack repository. 

In the main `build.gradle` add the JitPack repository:

```
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add the following dependency to module `build.gradle` file:

```
dependencies {
    implementation 'com.github.horizontalsystems.bitcoin-kit-android:bitcoincore:${version}'
}
```

It implements `AbstractKit` and `Network` interfaces (abstract classes). 

Customizing can be done in 2 places:

1. Via `BitcoinCoreBuilder` when building `BitcoinCore`
2. Via `BitcoinCore`

There are multiple places that can be customized. See the modules [`bitcoinkit`](bitcoinkit), [`bitcoincashkit`](bitcoincashkit) and [`dashkit`](dashkit) for reference. If you need a new extension point please [add an issue](https://github.com/horizontalsystems/bitcoin-kit-android/issues/new).

When the module is released let us know about it. We will review it and decide whether to add it to Unstoppable wallet app.
