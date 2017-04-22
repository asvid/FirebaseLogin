package asvid.firebaselogin.exceptions

class NotInitializedException : Exception("UserLoginService was not initialized before call")
class WrongPassword : Throwable("Provided password is wrong")