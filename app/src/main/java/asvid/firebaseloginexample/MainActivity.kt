package asvid.firebaseloginexample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import asvid.firebaselogin.UserLoginService
import asvid.firebaselogin.signals.AccountCreated
import asvid.firebaselogin.signals.EmailAlreadyUsed
import asvid.firebaselogin.signals.FirebaseError
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.UserLogged
import asvid.firebaselogin.signals.UserLoggedOut
import asvid.firebaselogin.signals.WeakPassword
import asvid.firebaselogin.signals.WrongPassword
import com.bumptech.glide.Glide
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.annonymousLogin
import kotlinx.android.synthetic.main.activity_main.avatarImage
import kotlinx.android.synthetic.main.activity_main.btn_fb_login
import kotlinx.android.synthetic.main.activity_main.createAccount
import kotlinx.android.synthetic.main.activity_main.displayNameTextView
import kotlinx.android.synthetic.main.activity_main.email
import kotlinx.android.synthetic.main.activity_main.googleButton
import kotlinx.android.synthetic.main.activity_main.loginWithEmail
import kotlinx.android.synthetic.main.activity_main.logoutButton
import kotlinx.android.synthetic.main.activity_main.password
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

  var subscription: Disposable by Delegates.notNull()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    UserLoginService.initWith(this, resources.getString(R.string.default_web_client_id))

    UserLoginService.logout()

    googleButton.setOnClickListener {
      UserLoginService.loginWithGoogle(this)
    }
    btn_fb_login.setOnClickListener {
      UserLoginService.loginWithFacebook(this)
    }

    createAccount.setOnClickListener {
      UserLoginService.createAccount(email.text.toString(), password.text.toString())
    }

    loginWithEmail.setOnClickListener {
      UserLoginService.loginWithEmail(email.text.toString(), password.text.toString())
    }

    annonymousLogin.setOnClickListener {
      UserLoginService.loginAnonymously()
    }

    logoutButton.setOnClickListener {
      UserLoginService.logout()
    }

    setUserData()
  }

  private fun setUserData() {
    val user = UserLoginService.getUser()
    Glide.with(this)
        .load(user?.photoUrl)
        .centerCrop()
        .placeholder(R.mipmap.ic_launcher)
        .dontAnimate()
        .into(avatarImage)

    if (user != null) {
      displayNameTextView.text = user.displayName
    } else {
      displayNameTextView.text = "John Doe"
    }
  }

  private fun doOnError(onError: Throwable) {
    Log.e("ERROR", onError.toString())
    Toast.makeText(this, "something went wrong", Toast.LENGTH_LONG).show()

    setUserData()
  }

  private fun doOnNext(signal: Signal) {
    if (signal.isError()) {
      Log.e("LOGIN_ACTIVITY", "signal is error: $signal")
      when (signal.error) {
        is WrongPassword -> showErrorToast(signal.error)
        is EmailAlreadyUsed -> showErrorToast(signal.error)
        is WeakPassword -> showErrorToast(signal.error)
      }
    } else {
      Log.d("LOGIN_ACTIVITY", "signal is OK: $signal")
      when (signal.status) {
        is UserLogged -> Toast.makeText(this, "user logged", Toast.LENGTH_LONG).show()
        is AccountCreated -> Toast.makeText(this, "account created", Toast.LENGTH_LONG).show()
        is UserLoggedOut -> Toast.makeText(this, "user logged out", Toast.LENGTH_LONG).show()
      }
    }
    setUserData()
  }

  private fun showErrorToast(second: FirebaseError?) {
    Log.d("LOGIN_ACTIVITY", "showErrorToast")
    Toast.makeText(this, second?.message, Toast.LENGTH_LONG).show()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    Log.d("LOGIN_ACTIVITY", "onActivityResult")
    super.onActivityResult(requestCode, resultCode, data)
    UserLoginService.handleActivityResult(requestCode, resultCode, data)
  }

  override fun onPause() {
    super.onPause()
    Log.d("LOGIN_ACTIVITY", "onPause")
    subscription.dispose()
  }

  override fun onResume() {
    super.onResume()
    createSubscription()
  }

  private fun createSubscription() {
    Log.d("LOGIN_ACTIVITY", "createSubscription")
    subscription = UserLoginService.observable.subscribe(
        { onNext -> doOnNext(onNext) },
        { onError -> doOnError(onError) }
    )
  }
}
