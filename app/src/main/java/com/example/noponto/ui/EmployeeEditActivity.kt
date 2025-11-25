package com.example.noponto.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.noponto.R
import com.example.noponto.databinding.ActivityEmployeeEditBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Endereco
import com.example.noponto.domain.model.Funcionario
import com.example.noponto.data.repository.FuncionarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class EmployeeEditActivity : BaseActivity() {

    private lateinit var binding: ActivityEmployeeEditBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private var funcionario: Funcionario? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            binding.profileImage.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        setupDropdowns()
        setupInputMasks()

        val employeeId = intent.getStringExtra("EMPLOYEE_ID")
        if (employeeId == null) {
            Toast.makeText(this, "ID do funcionário não encontrado", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        populateFields(employeeId)
        setupValidation()


        binding.profileImage.setOnClickListener {
            openGalleryForImage()
        }

        binding.buttonCancelar.setOnClickListener {
            finish()
        }

        binding.buttonAtualizar.setOnClickListener {
            // implementar atualização do funcionário (sem alterar email)
            binding.buttonAtualizar.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                val nome = binding.nomeEditText.text.toString().trim()
                val cpf = binding.cpfEditText.text.toString().trim()
                val statusStr = binding.statusAutocomplete.text.toString().trim()
                val cargoStr = binding.cargoAutocomplete.text.toString().trim()
                val cep = binding.cepEditText.text.toString().trim()
                val enderecoStr = binding.enderecoEditText.text.toString().trim()
                val cidade = binding.cidadeEditText.text.toString().trim()
                val estado = binding.estadoAutocomplete.text.toString().trim()
                val dataNascimentoStr = binding.dataNascimentoEditText.text.toString().trim()

                try {
                    val currentFuncionario = funcionario
                    if (currentFuncionario == null) {
                        withContext<Unit>(Dispatchers.Main) {
                            binding.buttonAtualizar.isEnabled = true
                            Toast.makeText(this@EmployeeEditActivity, "Dados do funcionário não carregados", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    // converter dataNascimento (dd/MM/yyyy) para LocalDate usando Calendar para compatibilidade API 24
                    val dataNascimentoParsed = try {
                        if (dataNascimentoStr.isNotBlank()) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val date = sdf.parse(dataNascimentoStr)
                            val calendar = Calendar.getInstance()
                            calendar.time = date ?: Calendar.getInstance().time
                            // Usa o LocalDate do funcionário atual se falhar parse, pois LocalDate.of requer API 26
                            // O desugaring do build.gradle deve permitir uso de java.time em API < 26
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                java.time.LocalDate.of(
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH) + 1,
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                            } else {
                                currentFuncionario.dataNascimento
                            }
                        } else {
                            currentFuncionario.dataNascimento
                        }
                    } catch (_: Exception) {
                        currentFuncionario.dataNascimento
                    }

                    // mapear cargo
                    val cargo = try {
                        when (cargoStr.lowercase()) {
                            "administrador" -> Cargo.ADMINISTRADOR
                            "desenvolvedor" -> Cargo.DESENVOLVEDOR
                            "designer" -> Cargo.DESIGNER
                            else -> currentFuncionario.cargo
                        }
                    } catch (_: Exception) { currentFuncionario.cargo }

                    // status boolean (Ativo/Inativo)
                    val statusBool = when (statusStr.lowercase()) {
                        "ativo" -> true
                        "inativo" -> false
                        else -> currentFuncionario.status
                    }

                    // montar endereco
                    val endereco = Endereco(
                        logradouro = enderecoStr.ifBlank { currentFuncionario.endereco.logradouro },
                        cidade = cidade.ifBlank { currentFuncionario.endereco.cidade },
                        estado = estado.ifBlank { currentFuncionario.endereco.estado },
                        cep = cep.ifBlank { currentFuncionario.endereco.cep }
                    )

                    // criar novo Funcionario (mantendo id e email originais)
                    val updated = Funcionario(
                        id = currentFuncionario.id,
                        nome = nome.ifBlank { currentFuncionario.nome },
                        email = currentFuncionario.email, // não atualizamos o email
                        cpf = cpf.ifBlank { currentFuncionario.cpf },
                        status = statusBool,
                        dataNascimento = dataNascimentoParsed ?: currentFuncionario.dataNascimento,
                        cargo = cargo,
                        endereco = endereco
                    )

                    val repo = FuncionarioRepository()
                    val updateRes = repo.updateFuncionario(updated)
                    withContext(Dispatchers.Main) {
                        if (updateRes.isSuccess) {
                            Toast.makeText(this@EmployeeEditActivity, "Funcionário atualizado com sucesso", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            binding.buttonAtualizar.isEnabled = true
                            Toast.makeText(this@EmployeeEditActivity, "Erro ao atualizar: ${updateRes.exceptionOrNull()?.localizedMessage ?: "Erro"}", Toast.LENGTH_LONG).show()
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.buttonAtualizar.isEnabled = true
                        Toast.makeText(this@EmployeeEditActivity, "Erro inesperado: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun populateFields(employeeId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val repo = FuncionarioRepository()
                val getRes = repo.getFuncionarioById(employeeId)

                if (getRes.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmployeeEditActivity, "Erro ao carregar dados: ${getRes.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }

                val loadedFuncionario = getRes.getOrNull()
                if (loadedFuncionario == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EmployeeEditActivity, "Funcionário não encontrado", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    return@launch
                }
                
                this@EmployeeEditActivity.funcionario = loadedFuncionario

                withContext(Dispatchers.Main) {
                    binding.nomeEditText.setText(loadedFuncionario.nome)
                    binding.emailEditText.setText(loadedFuncionario.email)
                    binding.emailEditText.isEnabled = false
                    binding.cpfEditText.setText(loadedFuncionario.cpf)
                    binding.statusAutocomplete.setText(if (loadedFuncionario.status) "Ativo" else "Inativo", false)

                    val cargoStr = when (loadedFuncionario.cargo) {
                        Cargo.ADMINISTRADOR -> "Administrador"
                        Cargo.DESENVOLVEDOR -> "Desenvolvedor"
                        Cargo.DESIGNER -> "Designer"
                    }
                    binding.cargoAutocomplete.setText(cargoStr, false)
                    when (loadedFuncionario.cargo) {
                        Cargo.ADMINISTRADOR -> binding.profileImage.setImageResource(R.drawable.ic_admin)
                        Cargo.DESENVOLVEDOR -> binding.profileImage.setImageResource(R.drawable.ic_developer)
                        Cargo.DESIGNER -> binding.profileImage.setImageResource(R.drawable.ic_designer)
                    }

                    // Preencher data de nascimento
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        binding.dataNascimentoEditText.setText(loadedFuncionario.dataNascimento.format(formatter))
                    } else {
                        // Fallback para API < 26
                        binding.dataNascimentoEditText.setText("")
                    }

                    // Preencher endereço
                    binding.cepEditText.setText(loadedFuncionario.endereco.cep)
                    binding.enderecoEditText.setText(loadedFuncionario.endereco.logradouro)
                    binding.cidadeEditText.setText(loadedFuncionario.endereco.cidade)
                    binding.estadoAutocomplete.setText(loadedFuncionario.endereco.estado, false)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EmployeeEditActivity, "Erro ao carregar funcionário: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    finish()
                }
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
        val isCpfValid = binding.cpfEditText.text.toString().length == 14
        val isDataNascimentoValid = binding.dataNascimentoEditText.text.toString().length == 10
        val isStatusValid = binding.statusAutocomplete.text.toString().isNotEmpty()
        val isCargoValid = binding.cargoAutocomplete.text.toString().isNotEmpty()
        val isCepValid = binding.cepEditText.text.toString().length == 9
        val isEnderecoValid = binding.enderecoEditText.text.toString().isNotEmpty()
        val isCidadeValid = binding.cidadeEditText.text.toString().isNotEmpty()
        val isEstadoValid = binding.estadoAutocomplete.text.toString().isNotEmpty()

        val allFieldsValid = isNomeValid && isEmailValid && isCpfValid &&
                isDataNascimentoValid && isStatusValid && isCargoValid && isCepValid &&
                isEnderecoValid && isCidadeValid && isEstadoValid

        binding.buttonAtualizar.isEnabled = allFieldsValid
        binding.buttonAtualizar.alpha = if (allFieldsValid) 1.0f else 0.5f
    }
}
