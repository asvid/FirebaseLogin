package asvid.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import asvid.firebaselogin.providers.AnonymousProvider
import asvid.firebaselogin.providers.EmailProvider
import asvid.firebaselogin.providers.FacebookProvider
import asvid.firebaselogin.providers.GOOGLE_LOGIN_CODE
import asvid.firebaselogin.providers.GoogleProvider
import asvid.firebaselogin.signals.EmptyEmail
import asvid.firebaselogin.signals.NotInitializedException
import asvid.firebaselogin.signals.ResetPasswordEmailSend
import asvid.firebaselogin.signals.ResetPasswordError
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.UserLoggedOut
import asvid.firebaselogin.signals.UserNotLoggedIn
import asvid.firebaselogin.signals.VerificationEmailSend
import asvid.firebaselogin.signals.VerificationEmailSendingError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.reactivex.subjects.PublishSubject

object UserLoginService {
  private var wasInitialized: Boolean = false
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  val observable = PublishSubject.create<Signal>()!!
  private val googleProvider = GoogleProvider(observable)
  private val facebookProvider = FacebookProvider(observable)
  private val emailProvider = EmailProvider(observable)
  private val anonymousProvider = AnonymousProvider(observable)

  fun initWith(context: Context, default_web_client_id: String) {
    googleProvider.init(default_web_client_id, context)
    facebookProvider.init(default_web_client_id, context)
    wasInitialized = true
  }

  fun loginWithGoogle(activity: Activity) {
    checkInit()
    googleProvider.login(activity)
  }

  fun loginWithFacebook(activity: Activity) {
    checkInit()
    facebookProvider.login(activity)
  }

  private fun checkInit() {
    if (!wasInitialized) throw NotInitializedException()
  }

  fun logout() {
    checkInit()
    auth.signOut()
    observable.onNext(Signal(status = UserLoggedOut()))
  }

  fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    checkInit()
    if (requestCode == GOOGLE_LOGIN_CODE) {
      googleProvider.handleLogin(data)
    } else {
      facebookProvider.handleLogin(requestCode, resultCode, data)
    }
  }

  fun createAccount(email: String, password: String) {
    checkInit()
    emailProvider.createAccount(email, password)
  }

  fun loginWithEmail(email: String, password: String) {
    checkInit()
    emailProvider.login(email = email, password = password)
  }

  fun loginAnonymously() {
    checkInit()
    anonymousProvider.login()
  }

  fun getUser(): FirebaseUser? {
    checkInit()
    return auth.currentUser
  }

  fun resetPassword(email: String) {
    checkInit()
    if (TextUtils.isEmpty(email)) {
      observable.onNext(Signal(error = EmptyEmail()))
      return
    }
    auth.sendPasswordResetEmail(email).addOnCompleteListener {
      task ->
      if (task.isSuccessful) {
        observable.onNext(Signal(status = ResetPasswordEmailSend()))
      } else {
        observable.onNext(Signal(error = ResetPasswordError()))
      }
    }
  }

  fun resendVerificationEmail() {
    checkInit()
    if (getUser() != null) {
      getUser()!!.sendEmailVerification().addOnCompleteListener { task ->
        if (task.isSuccessful) {
          observable.onNext(Signal(status = VerificationEmailSend()))
        } else {
          observable.onNext(Signal(error = VerificationEmailSendingError()))
        }
      }
    } else {
      observable.onNext(Signal(error = UserNotLoggedIn()))
    }
  }
}