package com.example.noponto.ui

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.noponto.R
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.databinding.ActivityEmployeeRegisterBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Endereco
import com.example.noponto.domain.model.Funcionario
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class EmployeeRegisterActivity : BaseActivity() {

    private var funcionarioRepository = FuncionarioRepository()
    private lateinit var binding: ActivityEmployeeRegisterBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDatePicker()
        setupAppBar()
        setupDropdowns()
        setupInputMasks()
        setupValidation()

        binding.buttonCancelar.setOnClickListener {
            finish()
        }

        binding.buttonConfirmar.setOnClickListener {
            // Lógica para confirmar o registro do funcionário
            lifecycleScope.launch {
                cadastrarFuncionario()
            }
        }
    }

    private fun setupDropdowns() {
        val statusOptions = arrayOf("Ativo", "Inativo")
        val statusAdapter = ArrayAdapter(this, R.layout.dropdown_item, statusOptions)
        binding.statusAutocomplete.setAdapter(statusAdapter)

        val cargoOptions = arrayOf("Administrador", "Desenvolvedor", "Designer")
        val cargoAdapter = ArrayAdapter(this, R.layout.dropdown_item, cargoOptions)
        binding.cargoAutocomplete.setAdapter(cargoAdapter)

        binding.cargoAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedRole = parent.getItemAtPosition(position).toString()
            when (selectedRole) {
                "Administrador" -> binding.profileImage.setImageResource(R.drawable.ic_admin)
                "Desenvolvedor" -> binding.profileImage.setImageResource(R.drawable.ic_developer)
                "Designer" -> binding.profileImage.setImageResource(R.drawable.ic_designer)
            }
        }

        val estadoOptions = arrayOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        )
        val estadoAdapter = ArrayAdapter(this, R.layout.dropdown_item, estadoOptions)
        binding.estadoAutocomplete.setAdapter(estadoAdapter)
    }

    private fun setupInputMasks() {
        // CPF Mask
        binding.cpfEditText.addTextChangedListener(Mask.mask("###.###.###-##", binding.cpfEditText))

        // CEP Mask
        binding.cepEditText.addTextChangedListener(Mask.mask("#####-###", binding.cepEditText))

        // Data de Nascimento Mask
        binding.dataNascimentoEditText.addTextChangedListener(Mask.mask("##/##/####", binding.dataNascimentoEditText))
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

        binding.nomeEditText.addTextChangedListener(textWatcher)
        binding.emailEditText.addTextChangedListener(textWatcher)
        binding.senhaEditText.addTextChangedListener(textWatcher)
        binding.cpfEditText.addTextChangedListener(textWatcher)
        binding.dataNascimentoEditText.addTextChangedListener(textWatcher)
        binding.statusAutocomplete.addTextChangedListener(textWatcher)
        binding.cargoAutocomplete.addTextChangedListener(textWatcher)
        binding.cepEditText.addTextChangedListener(textWatcher)
        binding.enderecoEditText.addTextChangedListener(textWatcher)
        binding.cidadeEditText.addTextChangedListener(textWatcher)
        binding.estadoAutocomplete.addTextChangedListener(textWatcher)
    }

    private fun validateInputs() {
        val isNomeValid = binding.nomeEditText.text.toString().isNotEmpty()
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(binding.emailEditText.text.toString()).matches()
        val isSenhaValid = binding.senhaEditText.text.toString().isNotEmpty()
        val isCpfValid = binding.cpfEditText.text.toString().length == 14
        val isDataNascimentoValid = binding.dataNascimentoEditText.text.toString().length == 10
        val isStatusValid = binding.statusAutocomplete.text.toString().isNotEmpty()
        val isCargoValid = binding.cargoAutocomplete.text.toString().isNotEmpty()
        val isCepValid = binding.cepEditText.text.toString().length == 9
        val isEnderecoValid = binding.enderecoEditText.text.toString().isNotEmpty()
        val isCidadeValid = binding.cidadeEditText.text.toString().isNotEmpty()
        val isEstadoValid = binding.estadoAutocomplete.text.toString().isNotEmpty()

        val allFieldsValid = isNomeValid && isEmailValid && isSenhaValid && isCpfValid &&
                isDataNascimentoValid && isStatusValid && isCargoValid && isCepValid &&
                isEnderecoValid && isCidadeValid && isEstadoValid

        binding.buttonConfirmar.isEnabled = allFieldsValid
        binding.buttonConfirmar.alpha = if (allFieldsValid) 1.0f else 0.5f
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun cadastrarFuncionario() {
        // Lógica para cadastrar o funcionário usando funcionarioRepository
        val funcionario = criaFuncionario()
        val senha = binding.senhaEditText.text.toString().trim()

        funcionarioRepository.registerFuncionario(funcionario, senha).onSuccess {
            showMessage("Funcionário cadastrado com sucesso!")
            finish()
        }.onFailure {
            showMessage("Falha ao cadastrar funcionário")
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun criaFuncionario(): Funcionario {
        // collect values
        val nome = binding.nomeEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val cpf = binding.cpfEditText.text.toString().trim()
        val dataNascimentoStr = binding.dataNascimentoEditText.text.toString().trim()
        val statusStr = binding.statusAutocomplete.text.toString().trim()
        val cargoStr = binding.cargoAutocomplete.text.toString().trim()
        val cep = binding.cepEditText.text.toString().trim()
        val rua = binding.enderecoEditText.text.toString().trim()
        val cidade = binding.cidadeEditText.text.toString().trim()
        val estado = binding.estadoAutocomplete.text.toString().trim()

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dataNascimento = LocalDate.parse(dataNascimentoStr, formatter)

        val status = parseStatus(statusStr)

        val cargo = parseCargo(cargoStr)

        // Criar objeto Endereco
        val endereco = Endereco(
            cep = cep,
            logradouro = rua,
            cidade = cidade,
            estado = estado
        )

        // create model (adapt field names to your Funcionario data class)
        val funcionario = Funcionario(
            nome = nome,
            email = email,
            cpf = cpf,
            dataNascimento = dataNascimento,
            status = status,
            cargo = cargo,
            endereco = endereco,
            id = "" // ID will be set by the repository upon registration
        )

        return funcionario
    }

    private fun setupDatePicker() {
        binding.dataNascimentoEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format as dd/MM/yyyy
                    val formattedDate = String.format(
                        "%02d/%02d/%04d",
                        selectedDay,
                        selectedMonth + 1, // month is 0-based
                        selectedYear
                    )
                    binding.dataNascimentoEditText.setText(formattedDate)
                },
                year,
                month,
                day
            )

            // Optional: set max date to today (user can't select future dates)
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            datePickerDialog.show()
        }
    }

    private fun parseStatus(statusStr: String): Boolean {
        return when (statusStr.trim()) {
            "Ativo" -> true
            "Inativo" -> false
            else -> throw IllegalArgumentException("Status inválido: $statusStr")
        }
    }

    private fun parseCargo(cargoStr: String): Cargo {
        return when (cargoStr.trim()) {
            "Administrador" -> Cargo.ADMINISTRADOR
            "Desenvolvedor" -> Cargo.DESENVOLVEDOR
            "Desginer" -> Cargo.DESIGNER
            else -> throw IllegalArgumentException("Cargo inválido: $cargoStr")
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
