package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import asvid.firebaselogin.signals.Signal
import io.reactivex.subjects.PublishSubject

class AnonymousProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {

  override fun init(defaultWebClientId: String, context: Context) {
  }

  override fun login(activity: Activity?, email: String, password: String) {
    auth.signInAnonymously().addOnCompleteListener { task ->
      loginTask(task)
    }
  }
}