package asvid.firebaselogin

import android.util.Log


object Logger {

  val TAG = "LOGGER"

  fun d(message: String, args: List<Any>? = null) {
    if (BuildConfig.DEBUG) {
      val (methodName, simpleClassName) = extractClass()
      Log.d(TAG, "$simpleClassName $methodName | $message ${args ?: ""}")
    }
  }

  fun v(message: String, args: List<Any>? = null) {
    if (BuildConfig.DEBUG) {
      val (methodName, simpleClassName) = extractClass()
      Log.v(TAG, "$simpleClassName $methodName | $message ${args ?: ""}")
    }
  }

  fun e(message: String, args: List<Any>? = null) {
    if (BuildConfig.DEBUG) {
      val (methodName, simpleClassName) = extractClass()
      Log.e(TAG, "$simpleClassName $methodName | $message ${args ?: ""}")
    }
  }

  private fun extractClass(): Pair<String, String> {
    val stackTrace = Thread.currentThread().stackTrace
    val caller = stackTrace[5]
    val methodName = caller.methodName
    val fullClassName = caller.className
    val simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
    return Pair(methodName, simpleClassName)
  }

}