package asvid.firebaselogin.providers

import android.app.Activity
import android.content.Context
import asvid.firebaselogin.Logger
import asvid.firebaselogin.UserCredentials
import asvid.firebaselogin.signals.EmailAlreadyUsed
import asvid.firebaselogin.signals.Signal
import asvid.firebaselogin.signals.UserLogged
import asvid.firebaselogin.signals.WrongPassword
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import io.reactivex.subjects.PublishSubject

abstract class BaseProvider(val observable: PublishSubject<Signal>) {

    protected val auth: FirebaseAuth = FirebaseAuth.getInstance()

    abstract fun init(defaultWebClientId: String, context: Context)
    abstract fun login(activity: Activity? = null, userCredentials: UserCredentials? = null)

    protected fun signWithCredential(credential: AuthCredential) {
        val user = auth.currentUser
        Logger.d("currentUser name: ${user?.displayName}")
        Logger.d("currentUser is annonymous: ${user?.isAnonymous}")
        Logger.d("currentUser providers: ${user?.providers}")
        Logger.d("current provider: ${credential.provider}")

        if (user?.isAnonymous == true || user?.providers?.contains(
                credential.provider) != false) {
            Logger.d("signInWithCredential")
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                loginTask(task, credential)
            }
        } else {
            Logger.d("linkWithCredential")
            user.linkWithCredential(credential).addOnCompleteListener { task ->
                loginTask(task, credential)
            }
        }
    }

    protected fun loginTask(task: Task<AuthResult>, newCredential: AuthCredential?) {
        if (task.isSuccessful) {
            observable.onNext(Signal(status = UserLogged(), data = auth.currentUser))
            val user = auth.currentUser
            Logger.d("logging succesful")

            Logger.d("currentUser name: ${user?.displayName}")
            Logger.d("currentUser is annonymous: ${user?.isAnonymous}")
            Logger.d("currentUser photo: ${user?.photoUrl}")
        } else {
            when (task.exception) {
                is FirebaseAuthUserCollisionException -> {
                    newCredential?.let { handleCreateEmailAccountError(it) }
                }
                is FirebaseAuthInvalidCredentialsException -> observable.onNext(
                        Signal(error = WrongPassword()))
            }
            Logger.d("couldn't log to account ${task.exception}")
        }
    }

    protected fun handleCreateEmailAccountError(newCredential: AuthCredential) {
        val currentUser = auth.currentUser
        Logger.d("handling user account collision")
        if (currentUser?.providers?.contains(getProviderId()) != false) {
            Logger.d("handling user account collision")
            observable.onNext(Signal(error = EmailAlreadyUsed()))
        } else {
            Logger.d("linking accounts")
            currentUser.linkWithCredential(newCredential).addOnCompleteListener { task ->
                run {
                    Logger.d("linking accounts result: ${task.isSuccessful} ${task.exception}")
                    if (task.isSuccessful)
                        loginTask(task, newCredential)
                    else observable.onNext(Signal(error = EmailAlreadyUsed()))
                }
            }
        }
    }

    abstract fun getProviderId(): String
}