package io.horizontalsystems.bitcoincore

import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers

object RxTestRule {

    fun setup() {
        //https://medium.com/@fabioCollini/testing-asynchronous-rxjava-code-using-mockito-8ad831a16877
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
    }

}
