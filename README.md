# FirebaseLogin

[![](https://jitpack.io/v/asvid/FirebaseLogin.svg)](https://jitpack.io/#asvid/FirebaseLogin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## How to make it work

You need to have properly configured Firebase, which means:
- package name of app has to be **THE SAME** in firebase project
- added SHA1 keys so you can render proper google-services.json
  - remember to add your debugging key 
  ```bash
  keytool -list -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android
  ```
- configured logging methods
  - email
  - google
  - facebook - https://firebase.google.com/docs/auth/android/facebook-login
  - anonymous

In app manifest add

```xml
    <meta-data
        android:name="com.facebook.sdk.ApplicationId"
        android:value="@string/facebook_application_id"/>
```
And create XML file in *values* folder containing
<?xml version="1.0" encoding="utf-8"?>
```xml
<resources>
  <string name="facebook_application_id" translatable="false">1808813719132692</string>
  <string name="facebook_login_protocol_scheme" translatable="false">fb1808813719132692</string>
</resources>
```
in yout project build.gradle add
```groovy
dependencies {
    classpath 'com.google.gms:google-services:3.0.0'
    ...
```

in yout app build.gradle on the **very end** add
```groovy
apply plugin: 'com.google.gms.google-services'
```

## How to use

First you need to initialize library in your login activity with giving context and client_id generated from google-services.json
```kotlin
    UserLoginService.initWith(this, resources.getString(R.string.default_web_client_id))
```
Google and Facebook logging is using *activity results* so you need to add this in your login activity
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    UserLoginService.handleActivityResult(requestCode, resultCode, data)
  }
```
Next, you should subscribe to libs observable
```kotlin
UserLoginService.observable.subscribe(
        { onNext -> doOnNext(onNext) },
        { onError -> doOnError(onError) }
    )
```
to cover all possible signals do something similar to:
```kotlin
private fun doOnNext(signal: Signal) {
    if (signal.isError()) {
      when (signal.error) {
        is WrongPassword -> showErrorToast(signal.error)
        is EmailAlreadyUsed -> showErrorToast(signal.error)
        is WeakPassword -> showErrorToast(signal.error)
      }
    } else {
      when (signal.status) {
        is UserLogged -> Toast.makeText(this, "user logged", Toast.LENGTH_LONG).show()
        is AccountCreated -> Toast.makeText(this, "account created", Toast.LENGTH_LONG).show()
        is UserLoggedOut -> Toast.makeText(this, "user logged out", Toast.LENGTH_LONG).show()
      }
    }
    setUserData()
  }
```
And it should work :)

## Licence

MIT License

Copyright (c) 2018 Adam Świderski <adam.swiderski89@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
