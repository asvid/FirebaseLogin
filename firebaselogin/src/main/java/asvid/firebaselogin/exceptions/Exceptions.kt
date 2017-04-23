package asvid.firebaselogin.exceptions

class NotInitializedException : Throwable("UserLoginService was not initialized before call")
class WrongPassword : Throwable("Provided password is wrong")
class EmailAlreadyUsed : Throwable("Email already used")
class WeakPassword : Throwable("Password is too weak")