package asvid.firebaselogin.signals

abstract class Error(message: String) : Throwable(message)

class NotInitializedException : Error("UserLoginService was not initialized before call")
class WrongPassword : Error("Provided password is wrong")
class EmailAlreadyUsed : Error("Email already used")
class WeakPassword : Error("Password is too weak")