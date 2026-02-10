package com.example.storepromax

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _isUserBanned = MutableStateFlow(false)
    val isUserBanned = _isUserBanned.asStateFlow()
    private var userListener: ListenerRegistration? = null
    init {
        monitorUserStatus()
    }

    fun monitorUserStatus() {
        val uid = auth.currentUser?.uid ?: return
        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        return@addSnapshotListener
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _isUserBanned.value = snapshot.getBoolean("isBanned") ?: false
                }
            }
    }

    fun stopMonitoring() {
        userListener?.remove()
        userListener = null
    }
}