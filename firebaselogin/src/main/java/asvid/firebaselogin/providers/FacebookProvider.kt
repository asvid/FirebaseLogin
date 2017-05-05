package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import android.content.Intent
import asvid.firebaselogin.Logger
import asvid.firebaselogin.UserLoginService
import asvid.firebaselogin.signals.LoginFailed
import asvid.firebaselogin.signals.Signal
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.subjects.PublishSubject
import java.util.Arrays

class FacebookProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {

  private val callbackManager: CallbackManager = Factory.create()

  override fun login(activity: Activity?, email: String, password: String) {
    LoginManager.getInstance()
        .logInWithReadPermissions(activity, Arrays.asList("public_profile",
            "email"))
  }

  override fun init(defaultWebClientId: String, context: Context) {
    LoginManager.getInstance()
        .registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
              override fun onError(error: FacebookException?) {
                Logger.e("Facebook login failed")
                UserLoginService.observable.onNext(Signal(error = LoginFailed()))
              }

              override fun onCancel() {
              }

              override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken)
              }
            }
        )
    setFacebookListener()
  }


  private fun setFacebookListener() {
    val facebookListener = FirebaseAuth.AuthStateListener {
      //      TODO
    }
    auth.addAuthStateListener { facebookListener }
  }

  fun handleFacebookAccessToken(token: AccessToken) {
    val credential = FacebookAuthProvider.getCredential(token.token)
    signWithCredential(credential)
  }

  fun handleLogin(requestCode: Int, resultCode: Int, data: Intent) {
    callbackManager.onActivityResult(requestCode, resultCode, data)
  }
}