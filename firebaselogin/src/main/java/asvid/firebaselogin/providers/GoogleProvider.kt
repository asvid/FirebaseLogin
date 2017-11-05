package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import asvid.firebaselogin.Logger
import asvid.firebaselogin.UserCredentials
import asvid.firebaselogin.signals.DeveloperError
import asvid.firebaselogin.signals.LoginFailed
import asvid.firebaselogin.signals.Signal
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.firebase.auth.GoogleAuthProvider
import io.reactivex.subjects.PublishSubject
import kotlin.properties.Delegates


const val GOOGLE_LOGIN_CODE = 123

class GoogleProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {

    private var gso: GoogleSignInOptions by Delegates.notNull()
    private var mGoogleApiClient: GoogleApiClient by Delegates.notNull()

    override fun getProviderId() = GoogleAuthProvider.PROVIDER_ID

    override fun login(activity: Activity?, userCredentials: UserCredentials?) {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        activity?.startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun init(defaultWebClientId: String, context: Context) {
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

    fun handleLogin(data: Intent) {
        val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
        val account = result.signInAccount
        if (account == null) {
            Logger.e("Google login failed ${result.status}")
            when (result.status.statusCode) {
                10 -> observable.onNext(Signal(error = DeveloperError()))
                else -> observable.onNext(Signal(error = LoginFailed()))
            }
            return
        }
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        signWithCredential(credential)
    }
}