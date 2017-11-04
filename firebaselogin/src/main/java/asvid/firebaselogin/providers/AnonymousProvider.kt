package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import asvid.firebaselogin.UserCredentials
import asvid.firebaselogin.signals.Signal
import io.reactivex.subjects.PublishSubject

class AnonymousProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {
  override fun login(activity: Activity?, userCredentials: UserCredentials?) {
    auth.signInAnonymously().addOnCompleteListener { task ->
      loginTask(task)
    }
  }

  override fun init(defaultWebClientId: String, context: Context) {
  }
}