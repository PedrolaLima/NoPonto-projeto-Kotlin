package com.example.noponto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noponto.databinding.ActivityEmployeesBinding
import com.example.noponto.databinding.ItemEmployeeRowBinding

class EmployeesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Create dummy data
        val employeeList = listOf(
            Employee("Pedro Henrique", "111.222.333-44", "pedro@email.com", "Administrador", "Ativo"),
            Employee("Maria Joaquina", "555.666.777-88", "maria.j@email.com", "Desenvolvedor", "Ativo"),
            Employee("José Carlos", "999.000.111-22", "jose.c@email.com", "Designer", "Inativo")
        )

        binding.employeeRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.employeeRecyclerView.adapter = EmployeeAdapter(employeeList)
    }

    // --- RecyclerView Adapter and ViewHolder ---

    data class Employee(val name: String, val cpf: String, val email: String, val role: String, val status: String)

    inner class EmployeeAdapter(private val employees: List<Employee>) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

        inner class EmployeeViewHolder(val binding: ItemEmployeeRowBinding) : RecyclerView.ViewHolder(binding.root)

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

                // Set click listeners for edit/delete buttons
                btnEdit.setOnClickListener {
                    Toast.makeText(holder.itemView.context, "Editar ${employee.name}", Toast.LENGTH_SHORT).show()
                }
                btnDelete.setOnClickListener {
                    Toast.makeText(holder.itemView.context, "Deletar ${employee.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}