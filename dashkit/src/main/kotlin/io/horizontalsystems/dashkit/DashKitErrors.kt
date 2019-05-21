package io.horizontalsystems.dashkit

object DashKitErrors {
    sealed class LockVoteValidation : Exception() {
        class MasternodeNotFound : LockVoteValidation()
        class MasternodeNotInTop : LockVoteValidation()
        class TxInputNotFound : LockVoteValidation()
        class SignatureNotValid : LockVoteValidation()
    }
}
