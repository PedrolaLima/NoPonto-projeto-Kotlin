package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.noponto.R
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.databinding.FragmentMenuBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Funcionario
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MenuDialogFragment : DialogFragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val funcionarioRepository = FuncionarioRepository()
    private var currentFuncionario: Funcionario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the style for the dialog to be fullscreen
        setStyle(STYLE_NORMAL, R.style.Theme_NoPonto_FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carregar funcionário autenticado e configurar visibilidade do menu
        loadCurrentFuncionario()

        // --- Menu Navigation ---

        binding.menuHome.setOnClickListener {
            // Navigate to WelcomeActivity, clearing other activities on top of it.
            val intent = Intent(requireContext(), WelcomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            dismiss()
        }

        binding.menuRegistrarPonto.setOnClickListener {
            val intent = Intent(requireContext(), RegisterActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            dismiss()
        }

        binding.menuFuncionarios.setOnClickListener {
            val intent = Intent(requireContext(), EmployeesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            dismiss()
        }

        binding.menuRelatorios.setOnClickListener {
            val intent = Intent(requireContext(), RecordActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            dismiss()
        }

        // Set up the close button
        binding.menuFechar.setOnClickListener {
            dismiss() // Close the dialog
        }
    }

    private fun loadCurrentFuncionario() {
        Log.d("MenuDialogFragment", "Carregando funcionário autenticado...")
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("MenuDialogFragment", "Usuário não autenticado!")
                // Se não está autenticado, oculta o menu de funcionários por segurança
                binding.menuFuncionarios.isVisible = false
                return@launch
            }

            funcionarioRepository.getFuncionarioById(userId).fold(
                onSuccess = { funcionario ->
                    if (funcionario == null) {
                        Log.e("MenuDialogFragment", "Funcionário autenticado não encontrado no banco!")
                        binding.menuFuncionarios.isVisible = false
                    } else {
                        currentFuncionario = funcionario
                        Log.d("MenuDialogFragment", "Funcionário autenticado: ${funcionario.nome}, Cargo: ${funcionario.cargo}")

                        // Controla a visibilidade do menu de funcionários
                        val isAdmin = funcionario.cargo == Cargo.ADMINISTRADOR
                        binding.menuFuncionarios.isVisible = isAdmin
                        binding.menuRelatorios.isVisible = isAdmin

                        if (isAdmin) {
                            Log.d("MenuDialogFragment", "✅ Menu de funcionários VISÍVEL - Usuário é ADMINISTRADOR")
                        } else {
                            Log.d("MenuDialogFragment", "🚫 Menu de funcionários OCULTO - Usuário NÃO é ADMINISTRADOR")
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("MenuDialogFragment", "Erro ao carregar funcionário autenticado", error)
                    // Em caso de erro, oculta o menu por segurança
                    binding.menuFuncionarios.isVisible = false
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
