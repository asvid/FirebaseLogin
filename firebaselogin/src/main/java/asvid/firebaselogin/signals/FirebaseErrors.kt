package asvid.firebaselogin.signals

abstract class FirebaseError(message: String) : Throwable(message)

class NotInitializedException : FirebaseError("UserLoginService was not initialized before call")
class WrongPassword : FirebaseError("Provided password is wrong")
class EmailAlreadyUsed : FirebaseError("Email already used")
class WeakPassword : FirebaseError("Password is too weak")
class LoginFailed : FirebaseError("Login to selected provider failed")
class DeveloperError : FirebaseError("Check your settings in Firebase Console - possibly SHA keys")