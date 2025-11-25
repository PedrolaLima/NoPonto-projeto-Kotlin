package com.example.noponto.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.databinding.ActivityWelcomeBinding
import com.example.noponto.databinding.AppBarBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class WelcomeActivity : BaseActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val funcionarioRepository = FuncionarioRepository()
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
            lifecycleScope.launch {
                funcionarioRepository.getFuncionarioById(userId).fold(
                    onSuccess = { funcionario ->
                        if (funcionario != null) {
                            binding.welcomeMessageTextView.text = "BEM VINDO,\n${funcionario.cargo.name}"
                        } else {
                            binding.welcomeMessageTextView.text = "BEM VINDO,\nFUNCIONÁRIO"
                        }
                    },
                    onFailure = {
                        binding.welcomeMessageTextView.text = "BEM VINDO,\nFUNCIONÁRIO"
                    }
                )
            }
        }
    }
}
