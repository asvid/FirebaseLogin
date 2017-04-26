package asvid.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import asvid.firebaselogin.signals.AccountCreated
import asvid.firebaselogin.signals.EmailAlreadyUsed
import asvid.firebaselogin.signals.NotInitializedException
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.UserLogged
import asvid.firebaselogin.signals.UserLoggedOut
import asvid.firebaselogin.signals.WeakPassword
import asvid.firebaselogin.signals.WrongPassword
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.ResultCodes
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.reactivex.subjects.PublishSubject
import java.util.Arrays
import kotlin.properties.Delegates

object UserLoginService {
  private val GOOGLE_LOGIN_CODE = 123
  private var wasInitialized: Boolean = false
  private var defaultWebClientId: String? = null
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  private var facebookListener: AuthStateListener by Delegates.notNull()
  private var gso: GoogleSignInOptions by Delegates.notNull()
  private val callbackManager: CallbackManager = Factory.create()
  private var mGoogleApiClient: GoogleApiClient by Delegates.notNull()
  val observable = PublishSubject.create<Signal>()!!
  private var ctx: Context by Delegates.notNull()

  fun initWith(context: Context, default_web_client_id: String) {
    defaultWebClientId = default_web_client_id
    ctx = context
    initGoogle(context)
    initFacebook()
    setFacebookListener()

    wasInitialized = true
    Logger.d("current user: ${auth.currentUser?.photoUrl}")
  }

  private fun initGoogle(context: Context) {
    gso = Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
      requestIdToken(defaultWebClientId)
      requestEmail()
    }.build()

    mGoogleApiClient = GoogleApiClient.Builder(context).apply {
      addApi(Auth.GOOGLE_SIGN_IN_API, gso)
      addOnConnectionFailedListener { p0 ->
        Logger.d("error logging to google: onConnectionFailed: $p0")
      }
    }.build()

    mGoogleApiClient.connect()
    mGoogleApiClient.registerConnectionCallbacks(object : ConnectionCallbacks {
      override fun onConnected(p0: Bundle?) {
        Logger.d("google onConnected: $p0")
      }

      override fun onConnectionSuspended(p0: Int) {
        Logger.d("google onConnectionSuspended: $p0")
      }
    })
  }

  private fun initFacebook() {
    LoginManager.getInstance()
        .registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
              override fun onError(error: FacebookException?) {
              }

              override fun onCancel() {
              }

              override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken)
              }
            }
        )
  }

  private fun setFacebookListener() {
    facebookListener = AuthStateListener {
      //      TODO
    }
    auth.addAuthStateListener { facebookListener }
  }

  fun loginWithGoogle(activity: Activity) {
    checkInit()
    val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
    activity.startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
  }

  fun loginWithFacebook(activity: Activity) {
    checkInit()
    LoginManager.getInstance()
        .logInWithReadPermissions(activity, Arrays.asList("public_profile",
            "email"))
  }

  private fun checkInit() {
    if (!wasInitialized) throw NotInitializedException()
  }

  fun logout() {
    checkInit()
    LoginManager.getInstance().logOut()
    auth.signOut()
    observable.onNext(Signal(status = UserLoggedOut()))
  }

  private fun handleFacebookAccessToken(token: AccessToken) {
    val credential = FacebookAuthProvider.getCredential(token.token)
    signWithCredential(credential)
  }

  private fun handleGoogleLogin(data: Intent) {
    val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
    val account = result.signInAccount
    val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
    signWithCredential(credential)
  }

  private fun signWithCredential(credential: AuthCredential) {
    val user = auth.currentUser
    Logger.d("currentUser name: ${user?.displayName}")
    Logger.d("currentUser is annonymous: ${user?.isAnonymous}")
    Logger.d("currentUser providers: ${user?.providers}")
    Logger.d("current provider: ${credential.provider}")

    if (user?.isAnonymous ?: false || user?.providers?.contains(
        credential.provider) ?: true) {
      Logger.d("signInWithCredential")
      auth.signInWithCredential(credential).addOnCompleteListener { task ->
        loginTask(task)
      }
    } else {
      Logger.d("linkWithCredential")
      user?.linkWithCredential(credential)?.addOnCompleteListener { task ->
        loginTask(task)
      }
    }
  }

  private fun loginTask(task: Task<AuthResult>) {
    if (task.isSuccessful) {
      observable.onNext(Signal(status = UserLogged()))
      val user = auth.currentUser
      Logger.d("logging succesful")

      Logger.d("currentUser name: ${user?.displayName}")
      Logger.d("currentUser is annonymous: ${user?.isAnonymous}")
      Logger.d("currentUser photo: ${user?.photoUrl}")
    } else {
      when (task.exception) {
        is FirebaseAuthUserCollisionException -> observable.onNext(
            Signal(error = EmailAlreadyUsed()))
        is FirebaseAuthInvalidCredentialsException -> observable.onNext(
            Signal(error = WrongPassword()))
      }
      Logger.d("couldn't log to account ${task.exception}")
    }
  }

  fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    checkInit()
    if (requestCode == GOOGLE_LOGIN_CODE) {
      (resultCode == ResultCodes.OK).let { handleGoogleLogin(data) }
    } else {
      callbackManager.onActivityResult(requestCode, resultCode, data)
    }
  }

  fun createAccount(email: String, password: String) {
    checkInit()
    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      Logger.d("creating finished ${task.isSuccessful}")
      if (task.isSuccessful) observable.onNext(Signal(status = AccountCreated()))
      else {
        Logger.d("creating finished ${task.exception}")
        when (task.exception) {
          is FirebaseAuthUserCollisionException -> handleCreateEmailAccountError(email, password)
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

  fun loginWithEmail(email: String, password: String) {
    checkInit()
    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      loginTask(task)
    }
  }

  fun loginAnonymously() {
    checkInit()
    auth.signInAnonymously().addOnCompleteListener { task ->
      loginTask(task)
    }
  }

  fun getUser(): FirebaseUser? {
    checkInit()
    return auth.currentUser
  }
}