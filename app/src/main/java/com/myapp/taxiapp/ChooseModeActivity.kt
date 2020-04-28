package com.myapp.taxiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth

class ChooseModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_mode)
    }

    fun goToPassenger(view: View) {
        startActivity(Intent(this, PassengerSignInActivity::class.java))
    }
    fun goTODrive(view: View) {
        startActivity(Intent(this, DriveSignInActivity::class.java))
    }
}