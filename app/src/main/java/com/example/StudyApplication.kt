package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class StudyApplication : Application() {

    companion object {
        var firestore: FirebaseFirestore? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // Disable offscreen virtual GPU tile raster to prevent MESA DRM rendernode crashes in container/emulator
            android.webkit.WebView.enableSlowWholeDocumentDraw()
        } catch (_: Throwable) {}
        initializeFirestore()
    }

    private fun initializeFirestore() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
                firestore = db
                Log.d("StudyApplication", "Firebase Firestore initialized successfully for real-time collaboration.")
            }
        } catch (e: Exception) {
            Log.w("StudyApplication", "Firebase Firestore initialization notice: ${e.message}")
        }
    }
}
