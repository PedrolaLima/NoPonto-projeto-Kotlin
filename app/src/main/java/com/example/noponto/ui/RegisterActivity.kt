package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.noponto.data.repository.PlanoTrabalhoRepository
import com.example.noponto.databinding.ActivityRegisterBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.HorarioDia
import com.example.noponto.domain.model.PlanoTrabalho
import com.example.noponto.domain.services.PlanoTrabalhoService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private val planoTrabalhoService: PlanoTrabalhoService by lazy {
        PlanoTrabalhoService(PlanoTrabalhoRepository())
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

        binding.btnRegistrarOcorrencia.setOnClickListener {
            val intent = Intent(this, OccurrenceActivity::class.java)
            startActivity(intent)
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
            // TODO: Tratar caso de usuário não logado
            return
        }

        lifecycleScope.launch {
            val result = planoTrabalhoService.buscarPlanoAtivoDoFuncionario(user.uid)
            result.fold(
                onSuccess = {
                    atualizarPainelHorarios(it)
                },
                onFailure = {
                    // TODO: Tratar erro ao buscar plano
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
}