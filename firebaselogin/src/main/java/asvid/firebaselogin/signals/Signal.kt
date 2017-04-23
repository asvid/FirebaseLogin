package asvid.firebaselogin.signals

data class Signal(
    val status: Status? = null,
    val error: FirebaseError? = null,
    val data: Any? = null) {

  fun isError() = error.let { true }
}