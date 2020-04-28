package com.myapp.taxiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_drive_sign_in.*
import kotlinx.android.synthetic.main.activity_drive_sign_in.inputConfirmPassword
import kotlinx.android.synthetic.main.activity_drive_sign_in.inputEmail
import kotlinx.android.synthetic.main.activity_drive_sign_in.inputName
import kotlinx.android.synthetic.main.activity_drive_sign_in.inputPassword
import kotlinx.android.synthetic.main.activity_drive_sign_in.loginButton
import kotlinx.android.synthetic.main.activity_drive_sign_in.toggleLoginText
import kotlinx.android.synthetic.main.activity_passenger_sign_in.*

class PassengerSignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null

    var signInBoolean: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger_sign_in)

        auth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser != null) startActivity(Intent(this, PassengerMapsActivity::class.java))
    }

    private fun inputEmail(): Boolean {
        val email = inputEmail.editText?.text.toString().trim()
        return if (email.isEmpty()) {
            inputEmail.error = "Введите ваш email"
            false
        } else {
            inputEmail.error = ""
            true
        }
    }

    private fun inputName(): Boolean {
        val name = inputName.editText?.text.toString().trim()
        return when {
            name.length > 15 -> {
                inputName.error = "Имя должно біть не больше 15 символов"
                false
            }
            name.isEmpty() -> {
                inputName.error = "Имя не должно біть пустім"
                false
            }
            else -> {
                inputName.error = ""
                true
            }
        }
    }

    private fun inputPassword(): Boolean {
        val password = inputPassword.editText?.text.toString().trim()
        return if (password.length < 8) {
            inputPassword.error = "Пароль должно біть больше 7 символов"
            false
        } else {
            inputConfirmPassword.error = ""
            true
        }
    }

    private fun inputConfirmPassword(): Boolean {
        return if (inputConfirmPassword.editText?.text.toString().trim()
                .equals(inputPassword.editText?.text.toString().trim())
        ) {
            inputConfirmPassword.error = ""
            true
        } else {
            inputConfirmPassword.error = "Пароли не совпадают"
            false
        }
    }

    fun startLoginSignIn(view: View) {
        if (!inputEmail() or !inputName() or !inputPassword() or !inputConfirmPassword()) return
        else if (!signInBoolean) {
            auth.createUserWithEmailAndPassword(
                inputEmail.editText?.text.toString().trim(),
                inputPassword.editText?.text.toString().trim()
            ).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "Аккаунт пользователя создан")
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Аккаунт пользователя создан", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this, PassengerMapsActivity::class.java))
                    //                   updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "${task.exception}", Toast.LENGTH_SHORT).show()
//                updateUI(null)
                }
            }
        } else if (!inputEmail() or !inputName() or !inputPassword()) return
        else {
            auth.signInWithEmailAndPassword(
                inputEmail.editText?.text.toString().trim(),
                inputPassword.editText?.text.toString().trim()
            ).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "Вход выполнен")
                    Toast.makeText(baseContext, "Вход выполнен", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser
                    startActivity(
                        Intent(
                            this
                            , PassengerMapsActivity::class.java
                        )
                    )
                    //               updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("TAG", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "${task.exception}", Toast.LENGTH_SHORT).show()
                    //               updateUI(null)
                }
            }
        }
    }

    fun toggleLogin(view: View) {
        if (signInBoolean) {
            signInBoolean = false
            loginButton.text = getString(R.string.text_sign_up)
            toggleLoginText.text = getString(R.string.or_log_in)
            inputConfirmPassword.visibility = View.VISIBLE
        } else {
            signInBoolean = true
            loginButton.text = getString(R.string.log_in)
            toggleLoginText.text = getString(R.string.or_sign_up)
            inputConfirmPassword.visibility = View.GONE
        }
    }
}