package asvid.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import asvid.firebaselogin.exceptions.AccountCreated
import asvid.firebaselogin.exceptions.EmailAlreadyUsed
import asvid.firebaselogin.exceptions.NotInitializedException
import asvid.firebaselogin.exceptions.Status
import asvid.firebaselogin.exceptions.UserLogged
import asvid.firebaselogin.exceptions.UserLoggedOut
import asvid.firebaselogin.exceptions.WeakPassword
import asvid.firebaselogin.exceptions.WrongPassword
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
  val observable = PublishSubject.create<Pair<Status?, Throwable?>>()!!
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
    gso = Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(defaultWebClientId)
        .requestEmail()
        .build()

    mGoogleApiClient = GoogleApiClient.Builder(context)
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
        .addOnConnectionFailedListener { p0 ->
          Logger.d("error logging to google: onConnectionFailed: $p0")
        }
        .build()

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

  private fun checkInit() {
    if (!wasInitialized) throw NotInitializedException()
  }

  fun logout() {
    checkInit()
    LoginManager.getInstance().logOut()
    auth.signOut()
    observable.onNext(Pair(UserLoggedOut(), null))
  }

  private fun handleFacebookAccessToken(token: AccessToken) {
    val credential = FacebookAuthProvider.getCredential(token.token)
    signWithCredential(credential)
  }

  private fun handleGoogleLogin(data: Intent) {
    val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
    val account = result.signInAccount
    val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
    signWithCredential(credential)
  }

  private fun signWithCredential(credential: AuthCredential) {
    val user = auth.currentUser
    Logger.d("user name: ${user?.displayName}")
    Logger.d("user is annonymous: ${user?.isAnonymous}")
    Logger.d("user providers: ${user?.providers}")
    Logger.d("current provider: ${credential.provider}")

    if (user != null && !user.providers?.contains(credential.provider)!!) {
      user.linkWithCredential(credential).addOnCompleteListener { task ->
        loginTask(task)
      }
    } else {
      auth.signInWithCredential(credential).addOnCompleteListener { task ->
        loginTask(task)
      }
    }

  }

  private fun loginTask(
      task: Task<AuthResult>) {
    if (!task.isSuccessful) {
      when (task.exception) {
        is FirebaseAuthUserCollisionException -> observable.onNext(Pair(null, EmailAlreadyUsed()))
        is FirebaseAuthInvalidCredentialsException -> observable.onNext(Pair(null, WrongPassword()))
      }
      Logger.d("loginTask couldn't log to account")
    } else {
      observable.onNext(Pair(UserLogged(), null))
      Logger.d("loginTask logging succesful")
    }
  }

  fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    checkInit()
    if (requestCode == GOOGLE_LOGIN_CODE) {
      if (resultCode.equals(ResultCodes.OK)) handleGoogleLogin(data)
    } else {
      callbackManager.onActivityResult(requestCode, resultCode, data)
    }
  }

  fun createAccount(email: String, password: String) {
    checkInit()
    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      Logger.d("creating finished ${task.isSuccessful}")
      if (!task.isSuccessful) {
        Logger.d("creating finished ${task.exception}")
        when (task.exception) {
          is FirebaseAuthUserCollisionException -> handleCreateEmailAccountError(email, password)
          is FirebaseAuthWeakPasswordException -> observable.onNext(Pair(null, WeakPassword()))
          else -> handleError(task.exception as FirebaseException)
        }
      } else observable.onNext(Pair(AccountCreated(), null))
    }
  }

  private fun handleError(exception: FirebaseException) {
    val reason = exception.message!!
    Logger.d("${exception.message}")
    if (reason.contains("WEAK_PASSWORD")) observable.onNext(Pair(null, WeakPassword()))
  }

  private fun handleCreateEmailAccountError(email: String, password: String) {
    val currentUser = auth.currentUser
    if (currentUser != null && !currentUser.providers!!.contains(EmailAuthProvider.PROVIDER_ID)) {
      val credential = EmailAuthProvider.getCredential(email, password)
      currentUser.linkWithCredential(credential).addOnCompleteListener { task ->
        loginTask(task)
      }
    } else observable.onNext(Pair(null, EmailAlreadyUsed()))
  }

  fun loginWithEmail(email: String, password: String) {
    checkInit()
    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
      loginTask(task)
    }
  }

  fun loginAnnonymously() {
    logout()
    auth.signInAnonymously().addOnCompleteListener { task ->
      loginTask(task)
    }
  }

  fun getUser(): FirebaseUser? {
    checkInit()
    return auth.currentUser
  }
}