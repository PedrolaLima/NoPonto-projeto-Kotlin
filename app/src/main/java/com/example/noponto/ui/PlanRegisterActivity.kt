package com.example.noponto.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.noponto.data.repository.PlanoTrabalhoRepository
import com.example.noponto.databinding.ActivityPlanRegisterBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.DiaSemana
import com.example.noponto.domain.services.PlanoTrabalhoService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.lang.NumberFormatException

class PlanRegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityPlanRegisterBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private val planoTrabalhoService: PlanoTrabalhoService by lazy {
        PlanoTrabalhoService(PlanoTrabalhoRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        setupMasks()
        setupValidation()

        binding.buttonCancelarPlano.setOnClickListener {
            finish()
        }

        binding.buttonConfirmarPlano.setOnClickListener {
            criarPlano()
        }
    }

    private fun criarPlano() {
        val presencialCheckedMap = mapOf(
            DiaSemana.SEGUNDA to binding.checkPresencialSegunda.isChecked,
            DiaSemana.TERCA to binding.checkPresencialTerca.isChecked,
            DiaSemana.QUARTA to binding.checkPresencialQuarta.isChecked,
            DiaSemana.QUINTA to binding.checkPresencialQuinta.isChecked,
            DiaSemana.SEXTA to binding.checkPresencialSexta.isChecked,
        )
        val entradaPresencial = binding.entradaPresencialEditText.text.toString()
        val saidaPresencial = binding.saidaPresencialEditText.text.toString()

        val remotoCheckedMap = mapOf(
            DiaSemana.SEGUNDA to binding.checkRemotoSegunda.isChecked,
            DiaSemana.TERCA to binding.checkRemotoTerca.isChecked,
            DiaSemana.QUARTA to binding.checkRemotoQuarta.isChecked,
            DiaSemana.QUINTA to binding.checkRemotoQuinta.isChecked,
            DiaSemana.SEXTA to binding.checkRemotoSexta.isChecked,
        )
        val entradaRemoto = binding.entradaRemotoEditText.text.toString()
        val saidaRemoto = binding.saidaRemotoEditText.text.toString()

        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(this@PlanRegisterActivity, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Deactivate existing active plan
            val activePlanResult = planoTrabalhoService.buscarPlanoAtivoDoFuncionario(user.uid)
            if (activePlanResult.isSuccess) {
                activePlanResult.getOrNull()?.let { activePlan ->
                    val deactivateResult = planoTrabalhoService.setAtivo(activePlan.id, false)
                    if (deactivateResult.isFailure) {
                        Toast.makeText(this@PlanRegisterActivity, "Erro ao atualizar plano existente.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
            } else {
                Toast.makeText(this@PlanRegisterActivity, "Erro ao verificar plano existente.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val result = planoTrabalhoService.criarPlanoFromUi(
                presencialCheckedMap = presencialCheckedMap,
                entradaPresencial = entradaPresencial,
                saidaPresencial = saidaPresencial,
                remotoCheckedMap = remotoCheckedMap,
                entradaRemoto = entradaRemoto,
                saidaRemoto = saidaRemoto
            )
            result.fold(
                onSuccess = {
                    Toast.makeText(this@PlanRegisterActivity, "Plano criado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = {
                    Toast.makeText(this@PlanRegisterActivity, "Erro ao criar plano: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun setupMasks() {
        binding.entradaPresencialEditText.addTextChangedListener(Mask.mask("##:##", binding.entradaPresencialEditText))
        binding.saidaPresencialEditText.addTextChangedListener(Mask.mask("##:##", binding.saidaPresencialEditText))
        binding.entradaRemotoEditText.addTextChangedListener(Mask.mask("##:##", binding.entradaRemotoEditText))
        binding.saidaRemotoEditText.addTextChangedListener(Mask.mask("##:##", binding.saidaRemotoEditText))
    }

    private fun setupValidation() {
        binding.buttonConfirmarPlano.isEnabled = false
        binding.buttonConfirmarPlano.alpha = 0.5f

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        binding.entradaPresencialEditText.addTextChangedListener(textWatcher)
        binding.saidaPresencialEditText.addTextChangedListener(textWatcher)
        binding.entradaRemotoEditText.addTextChangedListener(textWatcher)
        binding.saidaRemotoEditText.addTextChangedListener(textWatcher)

        val presencialCheckBoxes = mapOf(
            DiaSemana.SEGUNDA to binding.checkPresencialSegunda,
            DiaSemana.TERCA to binding.checkPresencialTerca,
            DiaSemana.QUARTA to binding.checkPresencialQuarta,
            DiaSemana.QUINTA to binding.checkPresencialQuinta,
            DiaSemana.SEXTA to binding.checkPresencialSexta
        )

        val remotoCheckBoxes = mapOf(
            DiaSemana.SEGUNDA to binding.checkRemotoSegunda,
            DiaSemana.TERCA to binding.checkRemotoTerca,
            DiaSemana.QUARTA to binding.checkRemotoQuarta,
            DiaSemana.QUINTA to binding.checkRemotoQuinta,
            DiaSemana.SEXTA to binding.checkRemotoSexta
        )

        presencialCheckBoxes.forEach { (dia, checkBox) ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    remotoCheckBoxes[dia]?.isChecked = false
                }
                validateInputs()
            }
        }

        remotoCheckBoxes.forEach { (dia, checkBox) ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    presencialCheckBoxes[dia]?.isChecked = false
                }
                validateInputs()
            }
        }
    }

    private fun parseTime(time: String): Int? {
        return try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                if (hours in 0..23 && minutes in 0..59) {
                    hours * 60 + minutes
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun validateInputs() {
        // --- Check which modes are being used ---
        val presencialIsUsed = listOf(binding.checkPresencialSegunda, binding.checkPresencialTerca, binding.checkPresencialQuarta, binding.checkPresencialQuinta, binding.checkPresencialSexta).any { it.isChecked }
        val remotoIsUsed = listOf(binding.checkRemotoSegunda, binding.checkRemotoTerca, binding.checkRemotoQuarta, binding.checkRemotoQuinta, binding.checkRemotoSexta).any { it.isChecked }

        // --- Validate Presencial Section ---
        val entradaPresencialText = binding.entradaPresencialEditText.text.toString()
        val saidaPresencialText = binding.saidaPresencialEditText.text.toString()
        val presencialTimesAreEmpty = entradaPresencialText.isEmpty() && saidaPresencialText.isEmpty()
        val presencialTimesAreFilled = entradaPresencialText.length == 5 && saidaPresencialText.length == 5

        val presencialIsValid: Boolean
        if (presencialIsUsed) {
            val entrada = parseTime(entradaPresencialText)
            val saida = parseTime(saidaPresencialText)
            // Must be used, times must be filled, and duration must be 8 hours
            presencialIsValid = presencialTimesAreFilled && entrada != null && saida != null && saida > entrada && (saida - entrada) == 480
        } else {
            // If not used, time fields must be empty
            presencialIsValid = presencialTimesAreEmpty
        }

        // --- Validate Remoto Section ---
        val entradaRemotoText = binding.entradaRemotoEditText.text.toString()
        val saidaRemotoText = binding.saidaRemotoEditText.text.toString()
        val remotoTimesAreEmpty = entradaRemotoText.isEmpty() && saidaRemotoText.isEmpty()
        val remotoTimesAreFilled = entradaRemotoText.length == 5 && saidaRemotoText.length == 5

        val remotoIsValid: Boolean
        if (remotoIsUsed) {
            val entrada = parseTime(entradaRemotoText)
            val saida = parseTime(saidaRemotoText)
            // Must be used, times must be filled, and duration must be 8 hours
            remotoIsValid = remotoTimesAreFilled && entrada != null && saida != null && saida > entrada && (saida - entrada) == 480
        } else {
            // If not used, time fields must be empty
            remotoIsValid = remotoTimesAreEmpty
        }

        // --- Final Validation ---
        val atLeastOneDayIsUsed = presencialIsUsed || remotoIsUsed
        val isEnabled = atLeastOneDayIsUsed && presencialIsValid && remotoIsValid

        binding.buttonConfirmarPlano.isEnabled = isEnabled
        binding.buttonConfirmarPlano.alpha = if (isEnabled) 1.0f else 0.5f
    }
}