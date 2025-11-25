package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.data.repository.PlanoTrabalhoRepository
import com.example.noponto.databinding.ActivityRegisterBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.HorarioDia
import com.example.noponto.domain.model.PlanoTrabalho
import com.example.noponto.domain.services.PlanoTrabalhoService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private val planoTrabalhoService: PlanoTrabalhoService by lazy {
        PlanoTrabalhoService(PlanoTrabalhoRepository())
    }

    private val funcionarioRepository: FuncionarioRepository by lazy {
        FuncionarioRepository()
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This sets up the app bar automatically
        setupAppBar()

        binding.btnRegistrarPonto.setOnClickListener {
            val intent = Intent(this, ClockInActivity::class.java)
            startActivity(intent)
        }

        binding.btnEspelhoPonto.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                lifecycleScope.launch {
                    val result = funcionarioRepository.getFuncionarioById(user.uid)
                    result.fold(
                        onSuccess = { funcionario ->
                            if (funcionario != null) {
                                val intent = Intent(this@RegisterActivity, RecordUserActivity::class.java)

                                val employeeName = funcionario.nome
                                val employeeRole = funcionario.cargo.name

                                val calendar = Calendar.getInstance()
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                                calendar.set(Calendar.DAY_OF_MONTH, 1)
                                val firstDayOfMonth = dateFormat.format(calendar.time)

                                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                                val lastDayOfMonth = dateFormat.format(calendar.time)

                                val period = "$firstDayOfMonth - $lastDayOfMonth"

                                intent.putExtra("employeeName", employeeName)
                                intent.putExtra("employeeRole", employeeRole)
                                intent.putExtra("funcionarioId", user.uid)
                                intent.putExtra("period", period)
                                intent.putExtra("currentUserRole", funcionario.cargo.name)
                                startActivity(intent)
                            } else {
                                Log.e(TAG, "Funcionário não encontrado no Firestore para o UID: ${user.uid}")
                                Toast.makeText(this@RegisterActivity, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Erro ao buscar funcionário: ", exception)
                            Toast.makeText(this@RegisterActivity, "Erro ao buscar dados do funcionário.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                redirectToLogin()
            }
        }

        binding.btnCadastrarPlano.setOnClickListener {
            val intent = Intent(this, PlanRegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        carregarPlanoDeTrabalho()
    }

    private fun carregarPlanoDeTrabalho() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            val result = planoTrabalhoService.buscarPlanoAtivoDoFuncionario(user.uid)
            result.fold(
                onSuccess = {
                    atualizarPainelHorarios(it)
                },
                onFailure = { exception ->
                    Log.e(TAG, "Erro ao buscar plano de trabalho: ", exception)
                    Toast.makeText(this@RegisterActivity, "Erro ao carregar plano de trabalho.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun atualizarPainelHorarios(plano: PlanoTrabalho?) {
        if (plano == null) {
            binding.planoCadastradoTitle.text = "NENHUM PLANO ATIVO"
            binding.presencialLayout.visibility = View.GONE
            binding.remotoLayout.visibility = View.GONE
            return
        }

        binding.planoCadastradoTitle.text = "PLANO CADASTRADO"

        updateHorarioVis(binding.presencialLayout, binding.diasPresencialTextview, binding.horarioPresencialTextview, plano.presencial)
        updateHorarioVis(binding.remotoLayout, binding.diasRemotoTextview, binding.horarioRemotoTextview, plano.remoto)
    }

    private fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }

    private fun updateHorarioVis(layout: View, diasTextView: TextView, horarioTextView: TextView, horarios: List<HorarioDia>) {
        if (horarios.isNotEmpty()) {
            layout.visibility = View.VISIBLE
            val dias = horarios.joinToString(", ") { it.dia.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
            val horario = "${formatTime(horarios.first().inicioMinutes)} - ${formatTime(horarios.first().fimMinutes)}"
            diasTextView.text = dias
            horarioTextView.text = horario
        } else {
            layout.visibility = View.GONE
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivityWithBinding::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}