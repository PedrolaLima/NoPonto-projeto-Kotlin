package com.example.noponto.ui

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.ArrayAdapter
import com.example.noponto.R
import com.example.noponto.databinding.ActivityEmployeeEditBinding
import com.example.noponto.databinding.AppBarBinding

class EmployeeEditActivity : BaseActivity() {

    private lateinit var binding: ActivityEmployeeEditBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        setupDropdowns()
        setupInputMasks()
        populateFields()
        setupValidation()

        binding.buttonCancelar.setOnClickListener {
            finish()
        }
    }

    private fun populateFields() {
        val employee = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("employee", EmployeesActivity.Employee::class.java)
        } else {
            intent.getSerializableExtra("employee") as EmployeesActivity.Employee
        }

        employee?.let {
            binding.nomeEditText.setText(it.name)
            binding.emailEditText.setText(it.email)
            binding.cpfEditText.setText(it.cpf)
            binding.statusAutocomplete.setText(it.status, false)
            binding.cargoAutocomplete.setText(it.role, false)

            when (it.role) {
                "Administrador" -> binding.profileImage.setImageResource(R.drawable.ic_admin)
                "Desenvolvedor" -> binding.profileImage.setImageResource(R.drawable.ic_developer)
            }
            // TODO: Preencher os campos restantes (CEP, Endereço, etc.)
        }
    }

    private fun setupDropdowns() {
        val statusOptions = arrayOf("Ativo", "Inativo")
        val statusAdapter = ArrayAdapter(this, R.layout.dropdown_item, statusOptions)
        binding.statusAutocomplete.setAdapter(statusAdapter)

        val cargoOptions = arrayOf("Administrador", "Desenvolvedor")
        val cargoAdapter = ArrayAdapter(this, R.layout.dropdown_item, cargoOptions)
        binding.cargoAutocomplete.setAdapter(cargoAdapter)

        binding.cargoAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedRole = parent.getItemAtPosition(position).toString()
            when (selectedRole) {
                "Administrador" -> binding.profileImage.setImageResource(R.drawable.ic_admin)
                "Desenvolvedor" -> binding.profileImage.setImageResource(R.drawable.ic_developer)
            }
        }

        val estadoOptions = arrayOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        )
        val estadoAdapter = ArrayAdapter(this, R.layout.dropdown_item, estadoOptions)
        binding.estadoAutocomplete.setAdapter(estadoAdapter)
    }

    private fun setupInputMasks() {
        binding.cpfEditText.addTextChangedListener(Mask.mask("###.###.###-##", binding.cpfEditText))
        binding.cepEditText.addTextChangedListener(Mask.mask("#####-###", binding.cepEditText))
        binding.dataNascimentoEditText.addTextChangedListener(Mask.mask("##/##/####", binding.dataNascimentoEditText))
    }

    private fun setupValidation() {
        binding.buttonAtualizar.isEnabled = false
        binding.buttonAtualizar.alpha = 0.5f

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

        binding.buttonAtualizar.isEnabled = allFieldsValid
        binding.buttonAtualizar.alpha = if (allFieldsValid) 1.0f else 0.5f
    }
}
