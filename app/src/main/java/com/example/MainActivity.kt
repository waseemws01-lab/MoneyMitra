package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.MoneyMitraApp
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this)
      }
      
      // Initialize Mobile Ads SDK for AdMob Rewarded Ads
      try {
        MobileAds.initialize(this) {}
      } catch (ex: Throwable) {
        ex.printStackTrace()
      }

      // Initialize Firebase App Check for production security hardening
      val firebaseAppCheck = FirebaseAppCheck.getInstance()
      if (com.example.BuildConfig.DEBUG) {
        firebaseAppCheck.installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance()
        )
      } else {
        firebaseAppCheck.installAppCheckProviderFactory(
          PlayIntegrityAppCheckProviderFactory.getInstance()
        )
      }
    } catch (e: Throwable) {
      e.printStackTrace()
    }
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          // Render the master MoneyMitra application
          MoneyMitraApp()
        }
      }
    }
  }
}
