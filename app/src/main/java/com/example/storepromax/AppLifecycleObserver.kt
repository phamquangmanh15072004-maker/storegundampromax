package com.example.storepromax

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : DefaultLifecycleObserver {

    private var lastUpdateTime: Long = 0
    private val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L
    private val SPAM_PREVENTION_MS = 10_000L
    private var heartbeatJob: Job? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                lastUpdateTime = 0
                forceUpdateFirebase()
                startHeartbeat()
            } else {
                stopHeartbeat()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > SPAM_PREVENTION_MS) {
            forceUpdateFirebase()
        }
        startHeartbeat()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        stopHeartbeat()
        forceUpdateFirebase()
    }

    private fun startHeartbeat() {
        if (auth.currentUser == null) return
        heartbeatJob?.cancel()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                forceUpdateFirebase()
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
    private fun forceUpdateFirebase() {
        val uid = auth.currentUser?.uid ?: return
        val currentTime = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users").document(uid)
                    .update("lastActive", currentTime)
                lastUpdateTime = currentTime
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}