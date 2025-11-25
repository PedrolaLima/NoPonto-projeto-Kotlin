package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.noponto.R
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.databinding.ActivityRecordBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Funcionario
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordActivity : BaseActivity() {

    private lateinit var binding: ActivityRecordBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private var lastClickTime: Long = 0
    private val auth = FirebaseAuth.getInstance()
    private val funcionarioRepository = FuncionarioRepository()
    private var employeeList: List<Funcionario> = emptyList()
    private var currentFuncionario: Funcionario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()

        // Disable confirm button initially
        binding.buttonConfirmar.isEnabled = false
        binding.buttonConfirmar.alpha = 0.5f

        // Load funcionários from database
        loadFuncionarios()

        // Add text changed listeners
        (binding.dropdownUsuario.editText as? AutoCompleteTextView)?.doAfterTextChanged {
            validateInputs()
        }
        binding.periodoEditText.doAfterTextChanged {
            validateInputs()
        }


        binding.periodoEditText.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 2000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()
            showDateRangePicker()
        }

        binding.buttonConfirmar.setOnClickListener {
            val selectedEmployeeName = binding.dropdownUsuario.editText?.text.toString()
            val selectedEmployee = employeeList.find { it.nome == selectedEmployeeName }
            val selectedPeriod = binding.periodoEditText.text.toString()

            val intent = Intent(this, RecordUserActivity::class.java).apply {
                putExtra("employeeName", selectedEmployee?.nome)
                putExtra("employeeRole", selectedEmployee?.cargo?.name)
                putExtra("funcionarioId", selectedEmployee?.id)
                putExtra("period", selectedPeriod)
                putExtra("currentUserRole", currentFuncionario?.cargo?.name)
            }
            startActivity(intent)
        }
    }

    private fun validateInputs() {
        val isUserSelected = binding.dropdownUsuario.editText?.text?.isNotEmpty() == true
        val isPeriodSelected = binding.periodoEditText.text?.isNotEmpty() == true
        val isEnabled = isUserSelected && isPeriodSelected
        binding.buttonConfirmar.isEnabled = isEnabled
        binding.buttonConfirmar.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Selecione o período")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDate = sdf.format(Date(it.first))
            val endDate = sdf.format(Date(it.second))
            binding.periodoEditText.setText("$startDate - $endDate")
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER_TAG")
    }

    private fun loadFuncionarios() {
        Log.d("RecordActivity", "Iniciando carregamento de funcionários...")
        lifecycleScope.launch {
            // Primeiro, carrega o funcionário autenticado
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("RecordActivity", "Usuário não autenticado!")
                employeeList = emptyList()
                setupDropdown()
                return@launch
            }

            funcionarioRepository.getFuncionarioById(userId).fold(
                onSuccess = { funcionario ->
                    if (funcionario == null) {
                        Log.e("RecordActivity", "Funcionário autenticado não encontrado no banco!")
                        employeeList = emptyList()
                        setupDropdown()
                        return@fold
                    }

                    currentFuncionario = funcionario
                    Log.d("RecordActivity", "Funcionário autenticado: ${funcionario.nome}, Cargo: ${funcionario.cargo}")

                    // Verifica se é ADMINISTRADOR
                    if (funcionario.cargo == Cargo.ADMINISTRADOR) {
                        Log.d("RecordActivity", "Usuário é ADMINISTRADOR - carregando todos os funcionários ativos")
                        loadAllActiveFuncionarios()
                    } else {
                        Log.d("RecordActivity", "Usuário NÃO é ADMINISTRADOR - mostrando apenas ele mesmo")
                        employeeList = listOf(funcionario)
                        setupDropdown()
                    }
                },
                onFailure = { error ->
                    Log.e("RecordActivity", "Erro ao carregar funcionário autenticado", error)
                    employeeList = emptyList()
                    setupDropdown()
                }
            )
        }
    }

    private fun loadAllActiveFuncionarios() {
        lifecycleScope.launch {
            funcionarioRepository.getFuncionariosByStatus(true).fold(
                onSuccess = { funcionarios ->
                    Log.d("RecordActivity", "Funcionários ativos carregados: ${funcionarios.size}")
                    funcionarios.forEach { funcionario ->
                        Log.d("RecordActivity", "  - ${funcionario.nome} (${funcionario.cargo})")
                    }
                    employeeList = funcionarios
                    setupDropdown()
                },
                onFailure = { error ->
                    Log.e("RecordActivity", "Erro ao carregar funcionários ativos", error)
                    // Em caso de erro, mostra apenas o usuário atual
                    employeeList = currentFuncionario?.let { listOf(it) } ?: emptyList()
                    setupDropdown()
                }
            )
        }
    }

    private fun setupDropdown() {
        Log.d("RecordActivity", "Configurando dropdown...")
        Log.d("RecordActivity", "Quantidade de funcionários: ${employeeList.size}")

        val employeeNames = employeeList.map { it.nome }
        Log.d("RecordActivity", "Nomes dos funcionários: $employeeNames")

        val adapter = ArrayAdapter(this, R.layout.dropdown_item, employeeNames)
        val autoCompleteTextView = (binding.dropdownUsuario.editText as? AutoCompleteTextView)

        if (autoCompleteTextView != null) {
            Log.d("RecordActivity", "AutoCompleteTextView encontrado, configurando adapter...")
            autoCompleteTextView.setAdapter(adapter)
            Log.d("RecordActivity", "Adapter configurado com sucesso")
        } else {
            Log.e("RecordActivity", "AutoCompleteTextView é null! Verificar layout.")
        }
    }
}
