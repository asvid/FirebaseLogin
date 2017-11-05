package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import asvid.firebaselogin.Logger
import asvid.firebaselogin.UserCredentials
import asvid.firebaselogin.signals.EmptyPhoneNumber
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.VerificationCodeSend
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit


class PhoneProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {

    private lateinit var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var mVerificationId: String
    private lateinit var mResendToken: PhoneAuthProvider.ForceResendingToken

    override fun getProviderId() = PhoneAuthProvider.PROVIDER_ID

    override fun init(defaultWebClientId: String, context: Context) {
        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Logger.d("onVerificationCompleted:" + credential)
                signWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Logger.d("onVerificationFailed $e")
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Logger.e("invalid request")
                } else if (e is FirebaseTooManyRequestsException) {
                    Logger.e("timeout")
                }
            }

            override fun onCodeSent(verificationId: String,
                                    token: PhoneAuthProvider.ForceResendingToken) {
                Logger.d("onCodeSent: " + verificationId)
                mVerificationId = verificationId
                mResendToken = token
                observable.onNext(Signal(status = VerificationCodeSend()))
            }
        }
    }

    override fun login(activity: Activity?, userCredentials: UserCredentials?) {
        if (activity != null && !TextUtils.isEmpty(userCredentials?.phone)) {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    userCredentials?.phone ?: "",        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    activity,               // Activity (for callback binding)
                    mCallbacks)
        } else {
            observable.onNext(Signal(error = EmptyPhoneNumber()))
        }
    }

    fun checkVerificationCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(mVerificationId, code)
        credential.let { signWithCredential(it) }
    }
}