package asvid.firebaseloginexample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.aswiderski.frigo.data.UserLoginService
import com.facebook.login.LoginManager
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.annonymousLogin
import kotlinx.android.synthetic.main.activity_main.btn_fb_login
import kotlinx.android.synthetic.main.activity_main.createAccount
import kotlinx.android.synthetic.main.activity_main.email
import kotlinx.android.synthetic.main.activity_main.googleButton
import kotlinx.android.synthetic.main.activity_main.loginWithEmail
import kotlinx.android.synthetic.main.activity_main.password
import java.util.Arrays
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

  var subscription: Disposable by Delegates.notNull()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    UserLoginService.initWith(this, resources.getString(R.string.default_web_client_id))


    googleButton.setOnClickListener {
      UserLoginService.loginWithGoogle(this)
    }
    btn_fb_login.setOnClickListener {
      LoginManager.getInstance()
          .logInWithReadPermissions(this, Arrays.asList("public_profile",
              "email"))
    }

    createAccount.setOnClickListener {
      UserLoginService.createAccount(email.text.toString(), password.text.toString())
    }

    loginWithEmail.setOnClickListener {
      UserLoginService.loginWithEmail(email.text.toString(), password.text.toString())
    }

    annonymousLogin.setOnClickListener {
      UserLoginService.loginAnnonymously()
    }

    UserLoginService.logout()
  }

  private fun doOnError(onError: Throwable?) {
    Toast.makeText(this, "something went wrong", Toast.LENGTH_LONG).show()
  }

  private fun doOnNext(onNext: Any?) {
    Toast.makeText(this, "user logged", Toast.LENGTH_LONG).show()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    UserLoginService.handleActivityResult(requestCode, resultCode, data)
  }

  override fun onPause() {
    super.onPause()
    subscription.dispose()
  }

  override fun onResume() {
    super.onResume()
    subscription = UserLoginService.observable.subscribe(
        { onNext -> doOnNext(onNext) },
        { onError -> doOnError(onError) }
    )
  }
}
