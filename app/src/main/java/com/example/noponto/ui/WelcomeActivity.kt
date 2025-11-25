package com.example.noponto.ui

import android.os.Bundle
import com.example.noponto.databinding.ActivityWelcomeBinding
import com.example.noponto.databinding.AppBarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : BaseActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()

        displayWelcomeMessage()
    }

    private fun displayWelcomeMessage() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")
                        binding.welcomeMessageTextView.text = "BEM VINDO,\n${role?.uppercase() ?: ""}"
                    }
                }
        }
    }
}
