package com.example.noponto.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.noponto.R
import com.example.noponto.data.repository.PlanoTrabalhoRepository
import com.example.noponto.data.repository.PontoRepository
import com.example.noponto.databinding.ActivityClockInBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.DiaSemana
import com.example.noponto.domain.model.TipoDePonto
import com.example.noponto.domain.services.PlanoTrabalhoService
import com.example.noponto.domain.services.PontoService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockInActivity : BaseActivity() {

    private lateinit var binding: ActivityClockInBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private val pontoService = PontoService(PontoRepository())
    private val planoTrabalhoService = PlanoTrabalhoService(PlanoTrabalhoRepository())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClockInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        setupInitialValues()
        setupInputMasks()
        setupDropdown()
        setupValidation()

        binding.buttonCancelar.setOnClickListener {
            finish()
        }

        binding.buttonConfirmar.setOnClickListener {
            if (validateFinalInputs()) {
                val dateStr = binding.inputData.editText?.text.toString()
                val timeStr = binding.inputHora.editText?.text.toString()
                val tipoPontoStr = (binding.dropdownTipoPonto.editText as? AutoCompleteTextView)?.text.toString()
                val isHomeOffice = binding.checkboxHomeOffice.isChecked

                val tipoPonto = when (tipoPontoStr) {
                    "Entrada" -> TipoDePonto.ENTRADA
                    "Saída" -> TipoDePonto.SAIDA
                    "Início do intervalo" -> TipoDePonto.INICIO_DE_INTERVALO
                    "Fim do intervalo" -> TipoDePonto.FIM_DE_INTERVALO
                    else -> null
                }

                if (tipoPonto == null) {
                    Toast.makeText(this, "Tipo de ponto inválido.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        Toast.makeText(this@ClockInActivity, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val planResult = planoTrabalhoService.buscarPlanoAtivoDoFuncionario(user.uid)
                    planResult.fold(
                        onSuccess = { plano ->
                            if (plano == null) {
                                Toast.makeText(this@ClockInActivity, "Nenhum plano de trabalho ativo encontrado.", Toast.LENGTH_LONG).show()
                                return@fold
                            }

                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val date = try { sdf.parse(dateStr) } catch (e: Exception) { null }
                            if (date == null) {
                                Toast.makeText(this@ClockInActivity, "Data inválida.", Toast.LENGTH_LONG).show()
                                return@fold
                            }

                            val calendar = Calendar.getInstance()
                            calendar.time = date
                            val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)

                            val diaSemana = when (dayOfWeekInt) {
                                Calendar.MONDAY -> DiaSemana.SEGUNDA
                                Calendar.TUESDAY -> DiaSemana.TERCA
                                Calendar.WEDNESDAY -> DiaSemana.QUARTA
                                Calendar.THURSDAY -> DiaSemana.QUINTA
                                Calendar.FRIDAY -> DiaSemana.SEXTA
                                else -> null
                            }

                            if (diaSemana == null) {
                                Toast.makeText(this@ClockInActivity, "Ponto não pode ser registrado em fins de semana.", Toast.LENGTH_LONG).show()
                                return@fold
                            }

                            val horarios = if (isHomeOffice) plano.remoto else plano.presencial
                            val horarioDia = horarios.find { it.dia == diaSemana }

                            if (horarioDia == null) {
                                val mode = if(isHomeOffice) "remoto" else "presencial"
                                Toast.makeText(this@ClockInActivity, "Hoje não é um dia de trabalho ${mode} no seu plano.", Toast.LENGTH_LONG).show()
                                return@fold
                            }

                            val timeParts = timeStr.split(":")
                            val currentTimeInMinutes = timeParts[0].toInt() * 60 + timeParts[1].toInt()

                            val validTime = when (tipoPonto) {
                                TipoDePonto.ENTRADA -> currentTimeInMinutes >= (horarioDia.inicioMinutes - 30) && currentTimeInMinutes <= horarioDia.fimMinutes
                                TipoDePonto.SAIDA -> currentTimeInMinutes >= horarioDia.inicioMinutes && currentTimeInMinutes <= (horarioDia.fimMinutes + 30)
                                TipoDePonto.INICIO_DE_INTERVALO, TipoDePonto.FIM_DE_INTERVALO -> currentTimeInMinutes >= horarioDia.inicioMinutes && currentTimeInMinutes <= horarioDia.fimMinutes
                            }

                            if (!validTime) {
                                Toast.makeText(this@ClockInActivity, "O horário do ponto não corresponde ao seu plano de trabalho.", Toast.LENGTH_LONG).show()
                                return@fold
                            }

                            val result = pontoService.registrarPonto(dateStr, timeStr, tipoPonto, isHomeOffice)
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(this@ClockInActivity, "Ponto registrado com sucesso!", Toast.LENGTH_SHORT).show()
                                    finish()
                                },
                                onFailure = { e ->
                                    Toast.makeText(this@ClockInActivity, "Erro ao registrar ponto: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onFailure = { e ->
                            Toast.makeText(this@ClockInActivity, "Erro ao buscar plano de trabalho: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }

    private fun setupInitialValues() {
        val brazilTimeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = brazilTimeZone }
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = brazilTimeZone }

        binding.inputData.editText?.setText(sdfDate.format(Date()))
        binding.inputHora.editText?.setText(sdfTime.format(Date()))
    }

    private fun setupInputMasks() {
        binding.inputData.editText?.addTextChangedListener(Mask.mask("##/##/####", binding.inputData.editText!!))
        binding.inputHora.editText?.addTextChangedListener(Mask.mask("##:##", binding.inputHora.editText!!))
    }

    private fun setupDropdown() {
        val pointTypes = arrayOf("Entrada", "Saída", "Início do intervalo", "Fim do intervalo")
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, pointTypes)
        (binding.dropdownTipoPonto.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupValidation() {
        binding.buttonConfirmar.isEnabled = false
        binding.buttonConfirmar.alpha = 0.5f

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        binding.inputData.editText?.addTextChangedListener(textWatcher)
        binding.inputHora.editText?.addTextChangedListener(textWatcher)
        (binding.dropdownTipoPonto.editText as? AutoCompleteTextView)?.addTextChangedListener(textWatcher)
    }

    private fun validateInputs() {
        val isDateValid = binding.inputData.editText?.text.toString().length == 10
        val isTimeValid = binding.inputHora.editText?.text.toString().length == 5
        val isPointTypeValid = (binding.dropdownTipoPonto.editText as? AutoCompleteTextView)?.text.toString().isNotEmpty()

        val allFieldsValid = isDateValid && isTimeValid && isPointTypeValid

        binding.buttonConfirmar.isEnabled = allFieldsValid
        binding.buttonConfirmar.alpha = if (allFieldsValid) 1.0f else 0.5f
    }

    private fun validateFinalInputs(): Boolean {
        binding.inputData.error = null
        binding.inputHora.error = null
        binding.dropdownTipoPonto.error = null

        var isValid = true
        if (binding.inputData.editText?.text.toString().length != 10) {
            binding.inputData.error = " "
            isValid = false
        }
        if (binding.inputHora.editText?.text.toString().length != 5) {
            binding.inputHora.error = " "
            isValid = false
        }
        if ((binding.dropdownTipoPonto.editText as? AutoCompleteTextView)?.text.toString().isEmpty()) {
            binding.dropdownTipoPonto.error = " "
            isValid = false
        }
        return isValid
    }
}