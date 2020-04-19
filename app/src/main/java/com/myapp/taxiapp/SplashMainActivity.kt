package com.myapp.taxiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.lang.Exception
import java.lang.Thread.sleep

class SplashMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity_main)

        val thread = Thread {
            try {
                sleep(5000)
            } catch (e: Exception) {e.printStackTrace()}
            finally {
                startActivity(Intent(this, ChooseModeActivity::class.java))
            }
        }
        thread.start()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}