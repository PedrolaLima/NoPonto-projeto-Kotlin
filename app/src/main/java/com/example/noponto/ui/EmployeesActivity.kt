package com.example.noponto.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noponto.R
import com.example.noponto.databinding.ActivityEmployeesBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.databinding.ItemEmployeeRowBinding
import java.io.Serializable
import java.util.*
import kotlin.Comparator

class EmployeesActivity : BaseActivity() {

    private lateinit var binding: ActivityEmployeesBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private lateinit var originalEmployeeList: List<Employee>
    private lateinit var displayedEmployeeList: MutableList<Employee>
    private lateinit var adapter: EmployeeAdapter

    private var currentComparator: Comparator<Employee> = compareBy { it.name.lowercase() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()
        loadEmployeeData()
        setupRecyclerView()
        setupSearch()

        binding.btnAddEmployee.setOnClickListener {
            val intent = Intent(this, EmployeeRegisterActivity::class.java)
            startActivity(intent)
        }

        binding.btnFilter.setOnClickListener {
            showFilterMenu()
        }

        updateDisplayedList()
    }

    private fun loadEmployeeData() {
        originalEmployeeList = listOf(
            Employee("Pedro Henrique de Lima Franca", "111.222.333-44", "pedro@email.com", "Administrador", "Ativo"),
            Employee("Maria Joaquina", "555.666.777-88", "maria.j@email.com", "Desenvolvedor", "Ativo"),
            Employee("José Carlos", "999.000.111-22", "jose.c@email.com", "Designer", "Inativo"),
            Employee("Ana Vitoria", "333.444.555-66", "ana.v@email.com", "Gerente", "Ativo")
        )
        displayedEmployeeList = mutableListOf()
    }

    private fun setupRecyclerView() {
        adapter = EmployeeAdapter(displayedEmployeeList)
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
        adapter.notifyDataSetChanged()
    }

    data class Employee(
        val name: String, val cpf: String, val email: String, val role: String, val status: String
    ) : Serializable

    class EmployeeAdapter(private val employees: MutableList<Employee>) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

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

                btnEdit.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = Intent(context, EmployeeEditActivity::class.java).apply {
                        putExtra("employee", employee)
                    }
                    context.startActivity(intent)
                }
                btnDelete.setOnClickListener {
                    Toast.makeText(holder.itemView.context, "Deletar ${employee.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}