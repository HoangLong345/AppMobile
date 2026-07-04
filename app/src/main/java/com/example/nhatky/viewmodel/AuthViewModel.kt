package com.example.nhatky.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nhatky.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    fun login(identifier: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (identifier.isBlank() || password.isBlank()) {
            onResult(false, "Vui lòng nhập đầy đủ thông tin")
            return
        }

        // Check if it's an email or phone number
        if (identifier.contains("@")) {
            // Login with Email
            auth.signInWithEmailAndPassword(identifier, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _currentUser.value = auth.currentUser
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.message)
                    }
                }
        } else {
            // Login with Phone Number (Requires Firestore lookup to get email)
            firestore.collection("users")
                .whereEqualTo("phoneNumber", identifier)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        onResult(false, "Số điện thoại không tồn tại")
                    } else {
                        val email = documents.documents[0].getString("email") ?: ""
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    _currentUser.value = auth.currentUser
                                    onResult(true, null)
                                } else {
                                    onResult(false, task.exception?.message)
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    onResult(false, it.message)
                }
        }
    }

    fun register(user: User, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result.user?.uid ?: ""
                    val finalUser = user.copy(uid = uid)
                    
                    // Save additional info to Firestore
                    firestore.collection("users").document(uid).set(finalUser)
                        .addOnSuccessListener {
                            _currentUser.value = auth.currentUser
                            onResult(true, null)
                        }
                        .addOnFailureListener {
                            onResult(false, "Lưu thông tin thất bại: ${it.message}")
                        }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Vui lòng nhập email")
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Mã khôi phục đã được gửi đến email của bạn")
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    _currentUser.value = firebaseUser
                    
                    // Check if user exists in Firestore, if not create entry
                    firebaseUser?.let {
                        firestore.collection("users").document(it.uid).get()
                            .addOnSuccessListener { doc ->
                                if (!doc.exists()) {
                                    val newUser = User(
                                        uid = it.uid,
                                        name = it.displayName ?: "Người dùng mới",
                                        email = it.email ?: ""
                                    )
                                    firestore.collection("users").document(it.uid).set(newUser)
                                }
                            }
                    }
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}
