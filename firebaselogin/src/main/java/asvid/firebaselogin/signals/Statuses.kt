package asvid.firebaselogin.signals

sealed class Status
class UserLogged : Status()
class AccountCreated : Status()
class UserLoggedOut : Status()
class ResetPasswordEmailSend : Status()
class VerificationEmailSend : Status()
class VerificationCodeSend : Status()
