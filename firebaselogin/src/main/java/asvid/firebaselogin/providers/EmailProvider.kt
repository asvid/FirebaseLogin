package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import asvid.firebaselogin.Logger
import asvid.firebaselogin.signals.AccountCreated
import asvid.firebaselogin.signals.EmailAlreadyUsed
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.WeakPassword
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import io.reactivex.subjects.PublishSubject

class EmailProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {
  override fun login(activity: Activity?, email: String, password: String) {
    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      loginTask(task)
    }
  }

  override fun init(defaultWebClientId: String, context: Context) {

  }

  fun createAccount(email: String, password: String) {
    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      Logger.d("creating finished ${task.isSuccessful}")
      if (task.isSuccessful) observable.onNext(Signal(status = AccountCreated()))
      else {
        Logger.d("creating finished ${task.exception}")
        when (task.exception) {
          is FirebaseAuthUserCollisionException -> handleCreateEmailAccountError(
              email, password)
          is FirebaseAuthWeakPasswordException -> observable.onNext(Signal(error = WeakPassword()))
          else -> handleError(task.exception as FirebaseException)
        }
      }
    }
  }

  private fun handleError(exception: FirebaseException) {
    val reason = exception.message!!
    Logger.d("${exception.message}")
    reason.contains("WEAK_PASSWORD").let { observable.onNext(Signal(error = WeakPassword())) }
  }

  private fun handleCreateEmailAccountError(email: String, password: String) {
    val currentUser = auth.currentUser
    if (currentUser?.providers?.contains(EmailAuthProvider.PROVIDER_ID) ?: true) {
      observable.onNext(Signal(error = EmailAlreadyUsed()))
    } else {
      val credential = EmailAuthProvider.getCredential(email, password)
      currentUser?.linkWithCredential(credential)?.addOnCompleteListener { task ->
        loginTask(task)
      }
    }
  }
}