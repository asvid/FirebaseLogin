package asvid.firebaselogin.exceptions

class NotInitializedException : Exception("UserLoginService was not initialized before call")
class WrongPassword : Throwable("Provided password is wrong")
class EmailAlreadyUsed : Throwable("Email already used")
class UserLoggedOut : Throwable("User logged out")