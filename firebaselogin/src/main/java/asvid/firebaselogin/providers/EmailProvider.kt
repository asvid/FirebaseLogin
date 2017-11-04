package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import asvid.firebaselogin.Logger
import asvid.firebaselogin.UserCredentials
import asvid.firebaselogin.signals.AccountCreated
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.WeakPassword
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import io.reactivex.subjects.PublishSubject

class EmailProvider(observable: PublishSubject<Signal>) : BaseProvider(observable) {
    override fun getProviderId() = EmailAuthProvider.PROVIDER_ID

    override fun login(activity: Activity?, userCredentials: UserCredentials?) {
        if (TextUtils.isEmpty(userCredentials?.email) || TextUtils.isEmpty(userCredentials?.password)) return
        if (userCredentials != null) {
            userCredentials.email?.let {
                userCredentials.password?.let { it1 ->
                    auth.signInWithEmailAndPassword(it, it1).addOnCompleteListener { task ->
                        loginTask(task, getAuthCredential(it, it1))
                    }
                }
            }
        }
    }

    private fun getAuthCredential(email: String, password: String) =
            EmailAuthProvider.getCredential(email, password)

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
                            getAuthCredential(email, password))
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
}