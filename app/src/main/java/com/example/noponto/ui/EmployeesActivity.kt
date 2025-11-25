package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noponto.R
import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.databinding.ActivityEmployeesBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.databinding.ItemEmployeeRowBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Funcionario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Comparator

class EmployeesActivity : BaseActivity() {

    private lateinit var binding: ActivityEmployeesBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private val auth = FirebaseAuth.getInstance()
    private val funcionarioRepository = FuncionarioRepository()
    private var currentFuncionario: Funcionario? = null

    private var originalEmployeeList: List<Employee> = emptyList()
    private val displayedEmployeeList = mutableListOf<Employee>()
    private lateinit var adapter: EmployeeAdapter

    private var currentComparator: Comparator<Employee> = compareBy { it.name.lowercase() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        loadCurrentFuncionario()
        setupRecyclerView()
        setupSearch()

        binding.btnAddEmployee.setOnClickListener {
            checkPermissionAndCreateEmployee()
        }

        binding.btnFilter.setOnClickListener {
            showFilterMenu()
        }

        updateDisplayedList()
    }

    override fun onResume() {
        super.onResume()
        loadEmployeeData()
    }

    private fun loadCurrentFuncionario() {
        Log.d("EmployeesActivity", "Carregando funcionário autenticado...")
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("EmployeesActivity", "Usuário não autenticado!")
                return@launch
            }

            funcionarioRepository.getFuncionarioById(userId).fold(
                onSuccess = { funcionario ->
                    if (funcionario == null) {
                        Log.e("EmployeesActivity", "Funcionário autenticado não encontrado no banco!")
                    } else {
                        currentFuncionario = funcionario
                        Log.d("EmployeesActivity", "Funcionário autenticado: ${funcionario.nome}, Cargo: ${funcionario.cargo}")
                    }
                },
                onFailure = { error ->
                    Log.e("EmployeesActivity", "Erro ao carregar funcionário autenticado", error)
                }
            )
        }
    }

    private fun checkPermissionAndCreateEmployee() {
        val funcionario = currentFuncionario

        if (funcionario == null) {
            Toast.makeText(
                this,
                "Erro ao verificar permissões. Tente novamente.",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("EmployeesActivity", "currentFuncionario é null ao tentar criar funcionário")
            return
        }

        if (funcionario.cargo != Cargo.ADMINISTRADOR) {
            Toast.makeText(
                this,
                "Você não tem permissão para criar funcionários. Apenas administradores podem realizar esta ação.",
                Toast.LENGTH_LONG
            ).show()
            Log.d("EmployeesActivity", "Tentativa de criar funcionário negada - Usuário não é ADMINISTRADOR")
            return
        }

        // Se chegou aqui, é administrador - pode criar
        Log.d("EmployeesActivity", "Permissão concedida - Abrindo tela de registro")
        val intent = Intent(this, EmployeeRegisterActivity::class.java)
        startActivity(intent)
    }

    private fun loadEmployeeData() {
        originalEmployeeList = emptyList()

        // Busca funcionários do Firestore (coleção `funcionarios`)
        val db = FirebaseFirestore.getInstance()
        db.collection("funcionarios")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("nome") ?: doc.getString("name") ?: ""
                    val cpf = doc.getString("cpf") ?: ""
                    val email = doc.getString("email") ?: ""
                    val role = doc.getString("cargo") ?: doc.getString("role") ?: ""
                    val statusField = doc.get("status")
                    val status = when (statusField) {
                        is Boolean -> if (statusField) "Ativo" else "Inativo"
                        is String -> statusField
                        else -> ""
                    }
                    // se nenhum campo essencial estiver presente, ignore o doc
                    if (name.isBlank() && email.isBlank()) return@mapNotNull null
                    Employee(id, name, cpf, email, role, status)
                }
                originalEmployeeList = list
                updateDisplayedList()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar funcionários: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupRecyclerView() {
        adapter = EmployeeAdapter(displayedEmployeeList) { employee ->
            AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza de que deseja deletar o funcionário ${employee.name}?")
                .setPositiveButton("Sim") { _, _ ->
                    lifecycleScope.launch {
                        funcionarioRepository.deleteFuncionario(employee.id).fold(
                            onSuccess = {
                                Toast.makeText(this@EmployeesActivity, "Funcionário ${employee.name} deletado", Toast.LENGTH_SHORT).show()
                                originalEmployeeList = originalEmployeeList.filter { it.id != employee.id }
                                updateDisplayedList()
                            },
                            onFailure = { error ->
                                Log.e("EmployeesActivity", "Erro ao deletar funcionário ${employee.id}", error)
                                Toast.makeText(this@EmployeesActivity, "Erro ao deletar ${employee.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                .setNegativeButton("Não", null)
                .show()
        }
        binding.employeeRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.employeeRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.btnSearch.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateDisplayedList()
            }
        })
    }

    private fun showFilterMenu() {
        val popupMenu = PopupMenu(this, binding.btnFilter)
        popupMenu.menuInflater.inflate(R.menu.employee_filter_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter_by_name -> currentComparator = compareBy { it.name.lowercase() }
                R.id.filter_by_cpf -> currentComparator = compareBy { it.cpf }
                R.id.filter_by_email -> currentComparator = compareBy { it.email.lowercase() }
                R.id.filter_by_role -> currentComparator = compareBy { it.role.lowercase() }
                R.id.filter_by_status -> currentComparator = compareBy { it.status.lowercase() }
            }
            updateDisplayedList()
            true
        }
        popupMenu.show()
    }

    private fun updateDisplayedList() {
        val query = binding.btnSearch.editText?.text.toString().lowercase(Locale.getDefault())

        val filteredList = if (query.isEmpty()) {
            originalEmployeeList
        } else {
            originalEmployeeList.filter {
                it.name.lowercase(Locale.getDefault()).contains(query) ||
                        it.cpf.contains(query) ||
                        it.email.lowercase(Locale.getDefault()).contains(query) ||
                        it.role.lowercase(Locale.getDefault()).contains(query) ||
                        it.status.lowercase(Locale.getDefault()).contains(query)
            }
        }

        val sortedList = filteredList.sortedWith(currentComparator)

        displayedEmployeeList.clear()
        displayedEmployeeList.addAll(sortedList)
        if (::adapter.isInitialized) { // Avoid crash on first load
            adapter.notifyDataSetChanged()
        }
    }

    data class Employee(
        val id: String,
        val name: String, val cpf: String, val email: String, val role: String, val status: String
    )

    class EmployeeAdapter(
        private val employees: MutableList<Employee>,
        private val onDeleteClick: (Employee) -> Unit
    ) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

        class EmployeeViewHolder(val binding: ItemEmployeeRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
            val binding = ItemEmployeeRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return EmployeeViewHolder(binding)
        }

        override fun getItemCount() = employees.size

        override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
            val employee = employees[position]
            with(holder.binding) {
                employeeName.text = employee.name
                employeeCpf.text = employee.cpf
                employeeEmail.text = employee.email
                employeeRole.text = employee.role
                employeeStatus.text = employee.status

                val imageRes = when (employee.role.lowercase(Locale.getDefault())) {
                    "administrador" -> R.drawable.ic_admin
                    "desenvolvedor" -> R.drawable.ic_developer
                    "designer" -> R.drawable.ic_designer
                    else -> R.drawable.ic_developer
                }
                employeeImage.setImageResource(imageRes)

                btnEdit.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = Intent(context, EmployeeEditActivity::class.java).apply {
                        putExtra("EMPLOYEE_ID", employee.id)
                    }
                    context.startActivity(intent)
                }
                btnDelete.setOnClickListener {
                    onDeleteClick(employee)
                }
            }
        }
    }
}
