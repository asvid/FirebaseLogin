package asvid.firebaselogin.signals

abstract class FirebaseError(message: String) : Throwable(message)

class NotInitializedException : FirebaseError("UserLoginService was not initialized before call")
class WrongPassword : FirebaseError("Provided password is wrong")
class EmailAlreadyUsed : FirebaseError("Email already used")
class WeakPassword : FirebaseError("Password is too weak")
class LoginFailed : FirebaseError("Login to selected provider failed")
class ResetPasswordError : FirebaseError("Sending reset password email failed")
class UserNotLoggedIn : FirebaseError("User not logged, try logging before using this method")
class DeveloperError : FirebaseError("Check your settings in Firebase Console - possibly SHA keys")
class VerificationEmailSendingError : FirebaseError("Sending verification email failed")
class EmptyEmail : FirebaseError("Given email was empty or null")