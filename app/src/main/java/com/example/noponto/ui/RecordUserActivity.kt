package com.example.noponto.ui

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noponto.R
import com.example.noponto.databinding.ActivityRecordUserBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.databinding.ItemRecordRowBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.StatusAprovacao

data class ReportEntry(
    val data: String,
    val entryTime: String?,
    val exitTime: String?,
    val pauseCount: Int,
    val registeredHours: String,
    val observations: String?,
    var status: StatusAprovacao
)

class RecordUserActivity : BaseActivity() {

    private lateinit var binding: ActivityRecordUserBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private var currentUserRole: Cargo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()

        val employeeName = intent.getStringExtra("employeeName") ?: getString(R.string.not_informed)
        val employeeRole = intent.getStringExtra("employeeRole") ?: getString(R.string.not_informed)
        val period = intent.getStringExtra("period") ?: "01/01/2024 - 31/01/2024"
        val roleString = intent.getStringExtra("currentUserRole")
        currentUserRole = if (roleString != null) Cargo.valueOf(roleString) else null


        binding.textFuncionario.text = getString(R.string.employee_name_label, employeeName)
        binding.textCargo.text = getString(R.string.employee_role_label, employeeRole)
        binding.textPeriodo.text = "Período: $period"

        val reportEntriesList = mutableListOf(
            ReportEntry("10/06/2025", "08:00", "12:00", 0, "4", null, StatusAprovacao.APROVADO),
            ReportEntry("11/06/2025", "09:00", "18:00", 1, "8", "Almoço", StatusAprovacao.APROVADO),
            ReportEntry("12/06/2025", "14:47", "18:00", 1, "-14", "Falta de ponto", StatusAprovacao.REPROVADO),
            ReportEntry("15/06/2025", "13:00", "13:15", 0, "0", null, StatusAprovacao.AGUARDANDO),
            ReportEntry("16/06/2025", "07:30", "16:30", 1, "8", null, StatusAprovacao.APROVADO)
        )

        binding.reportRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.reportRecyclerView.adapter = ReportAdapter(reportEntriesList, currentUserRole)

        binding.buttonVoltar.setOnClickListener {
            finish()
        }
    }

    inner class ReportAdapter(
        private val reportEntries: MutableList<ReportEntry>,
        private val userRole: Cargo?
    ) : RecyclerView.Adapter<ReportAdapter.RowViewHolder>() {

        inner class RowViewHolder(val binding: ItemRecordRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
            val binding = ItemRecordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RowViewHolder(binding)
        }

        override fun getItemCount() = reportEntries.size

        override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
            val item = reportEntries[position]
            holder.binding.apply {
                reportData.text = item.data
                reportEntrada.text = item.entryTime ?: ""
                reportSaida.text = item.exitTime ?: ""
                reportPausas.text = item.pauseCount.toString()
                reportHorasRegistradas.text = item.registeredHours
                reportObservacoes.text = item.observations ?: ""
                reportHomologacao.text = item.status.name

                when (item.status) {
                    StatusAprovacao.APROVADO -> reportHomologacao.setTextColor(Color.parseColor("#006400")) // DarkGreen
                    StatusAprovacao.REPROVADO -> reportHomologacao.setTextColor(Color.RED)
                    StatusAprovacao.AGUARDANDO -> reportHomologacao.setTextColor(Color.BLACK)
                }

                if (userRole == Cargo.ADMINISTRADOR) {
                    reportHomologacao.setOnClickListener(object : View.OnClickListener {
                        private var lastClickTime: Long = 0
                        private val DOUBLE_CLICK_TIME_DELTA: Long = 300 //milliseconds

                        override fun onClick(v: View) {
                            val clickTime = SystemClock.uptimeMillis()
                            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                                val currentEntry = reportEntries[holder.adapterPosition]
                                val newStatus = when (currentEntry.status) {
                                    StatusAprovacao.AGUARDANDO -> StatusAprovacao.APROVADO
                                    StatusAprovacao.APROVADO -> StatusAprovacao.REPROVADO
                                    StatusAprovacao.REPROVADO -> StatusAprovacao.AGUARDANDO
                                }
                                currentEntry.status = newStatus
                                Log.d("ReportAdapter", "Updated status for item at position $position to $newStatus")

                                notifyItemChanged(holder.adapterPosition)
                            }
                            lastClickTime = clickTime
                        }
                    })
                }
            }
        }
    }
}
