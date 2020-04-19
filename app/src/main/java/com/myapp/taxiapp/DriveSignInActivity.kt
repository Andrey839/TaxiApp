package com.myapp.taxiapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_drive_sign_in.*

class DriveSignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive_sign_in)
    }

    fun inputEmail(): Boolean {
        val email = inputEmail.editText?.text.toString().trim()
        return if (email.isEmpty()) {
            inputEmail.error = "Введите ваш email"
            false
        } else {
            inputEmail.error = ""
            true
        }
    }

    fun inputName(): Boolean {
        val name = inputName.editText?.text.toString().trim()
        return when {
            name.length > 15 -> {
                inputName.error = "Имя должно біть менее 15 символов"
                false
            }
            name.isEmpty() -> {
                inputName.error = "Имя не должно біть пустім"
                false
            }
            else -> true
        }
    }

    fun inputPassword(): Boolean {
        val password = inputPassword.editText?.text.toString().trim()
        return when {
            password.length < 7 -> {
                inputName.error = "Пароль должно біть больше 8 символов"
                false
            }
            password.isEmpty() -> {
                inputName.error = "Пароль не должно біть пустім"
                false
            }
            else -> true
        }
    }

    fun inputConfirmPassword():Boolean {
        return if (inputPassword.editText?.text.toString().trim() != inputConfirmPassword.editText?.text.toString().trim()) {
            inputConfirmPassword.error = "Пароли не совпадают!"
            false
        } else {
            inputConfirmPassword.error = ""
            true
        }
    }

    fun startLoginSignIn(view: View) {}
    fun toggleLogin(view: View) {}
}