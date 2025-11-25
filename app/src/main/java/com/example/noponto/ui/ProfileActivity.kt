package com.example.noponto.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.databinding.ActivityProfileBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.Funcionario
import com.example.noponto.domain.model.Cargo
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private val funcionarioRepository = FuncionarioRepository()
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        loadFuncionarioProfile()
    }

    private fun loadFuncionarioProfile() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.e(TAG, "User not authenticated")
                    showLoadError("Usuário não autenticado")
                    finish()
                    return@launch
                }

                Log.d(TAG, "Loading profile for user: ${currentUser.uid}")

                val result = withContext(Dispatchers.IO) {
                    funcionarioRepository.getFuncionarioById(currentUser.uid)
                }

                result.onSuccess { funcionario ->
                    if (funcionario != null) {
                        Log.d(TAG, "Funcionario loaded successfully: ${funcionario.nome}")
                        populateProfileFields(funcionario)
                    } else {
                        Log.e(TAG, "Funcionario is null after deserialization")
                        showLoadError("Funcionário não encontrado ou dados inválidos no Firestore")
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Error loading funcionario", exception)
                    showLoadError("Erro ao carregar dados: ${exception.localizedMessage ?: "Desconhecido"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in loadFuncionarioProfile", e)
                showLoadError("Erro inesperado: ${e.localizedMessage}")
            }
        }
    }

    private fun populateProfileFields(funcionario: Funcionario) {
        try {
            appBarBinding.userRole.text = funcionario.nome
            binding.inputNome.editText?.setText(funcionario.nome)
            binding.inputEmail.editText?.setText(funcionario.email)
            binding.inputId.editText?.setText(funcionario.id)
            binding.inputCpf.editText?.setText(funcionario.cpf)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
                runCatching { funcionario.dataNascimento.format(formatter) }
                    .onSuccess { binding.inputData.editText?.setText(it) }
                    .onFailure {
                        Log.e(TAG, "Error formatting date", it)
                        binding.inputData.editText?.setText("")
                    }
            }

            binding.dropdownStatus.setText(if (funcionario.status) "Ativo" else "Inativo", false)
            binding.dropdownPerfil.setText(
                when (funcionario.cargo) {
                    Cargo.ADMINISTRADOR -> "Administrador"
                    Cargo.DESENVOLVEDOR -> "Desenvolvedor"
                    Cargo.DESIGNER -> "Designer"
                },
                false
            )

            binding.inputCep.editText?.setText(funcionario.endereco.cep)
            binding.inputRua.editText?.setText(funcionario.endereco.logradouro)
            binding.inputCidade.editText?.setText(funcionario.endereco.cidade)
            binding.dropdownEstado.setText(funcionario.endereco.estado, false)

            Log.d(TAG, "Profile fields populated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error populating profile fields", e)
            showLoadError("Erro ao preencher campos: ${e.localizedMessage}")
        }
    }

    private fun showLoadError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
