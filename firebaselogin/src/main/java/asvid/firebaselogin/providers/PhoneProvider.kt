package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import asvid.firebaselogin.Logger
import asvid.firebaselogin.UserCredentials
import asvid.firebaselogin.signals.EmptyPhoneNumber
import asvid.firebaselogin.signals.Signal
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

    override fun init(defaultWebClientId: String, context: Context) {
        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                Logger.d("onVerificationCompleted:" + credential)

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Logger.d("onVerificationFailed $e")

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                    Logger.e("invalid request")
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                    Logger.e("timeout")
                }

                // Show a message and update the UI
                // ...
            }

            override fun onCodeSent(verificationId: String,
                                    token: PhoneAuthProvider.ForceResendingToken) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Logger.d("onCodeSent: " + verificationId)

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token

                // ...
            }
        }

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

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
}