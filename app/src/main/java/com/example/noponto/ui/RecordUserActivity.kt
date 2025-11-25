package com.example.noponto.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noponto.R
import com.example.noponto.data.repository.OcorrenciaRepository
import com.example.noponto.data.repository.PontoRepository
import com.example.noponto.databinding.ActivityRecordUserBinding
import com.example.noponto.databinding.AppBarBinding
import com.example.noponto.databinding.ItemRecordRowBinding
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Ponto
import com.example.noponto.domain.model.StatusAprovacao
import com.example.noponto.domain.model.TipoDePonto
import com.example.noponto.domain.services.PontoService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ReportEntry(
    val pontoId: String,
    val data: String,
    val entryTime: String?,
    val exitTime: String?,
    val pauseCount: Int,
    val registeredHours: String,
    val observations: String?,
    var status: StatusAprovacao,
    val descricao: String = "+",
    val hasOcorrencia: Boolean = false
)

class RecordUserActivity : BaseActivity() {

    private lateinit var binding: ActivityRecordUserBinding
    override val appBarBinding: AppBarBinding
        get() = binding.appBarLayout

    private var currentUserRole: Cargo? = null
    private var currentFuncionarioId: String = ""
    private val pontoService: PontoService by lazy {
        PontoService(PontoRepository())
    }
    private val ocorrenciaRepository: OcorrenciaRepository by lazy {
        OcorrenciaRepository()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()

        val employeeName = intent.getStringExtra("employeeName") ?: getString(R.string.not_informed)
        val employeeRole = intent.getStringExtra("employeeRole") ?: getString(R.string.not_informed)
        currentFuncionarioId = intent.getStringExtra("funcionarioId") ?: ""
        val period = intent.getStringExtra("period") ?: "01/01/2024 - 31/01/2024"
        val roleString = intent.getStringExtra("currentUserRole")
        currentUserRole = if (roleString != null) Cargo.valueOf(roleString) else null

        binding.textFuncionario.text = getString(R.string.employee_name_label, employeeName)
        binding.textCargo.text = getString(R.string.employee_role_label, employeeRole)
        binding.textPeriodo.text = "Período: $period"

        binding.reportRecyclerView.layoutManager = LinearLayoutManager(this)

        // Carrega os pontos do funcionário
        if (currentFuncionarioId.isNotEmpty()) {
            loadPontos(currentFuncionarioId)
        } else {
            Toast.makeText(this, "ID do funcionário não encontrado", Toast.LENGTH_SHORT).show()
        }

        binding.buttonVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarrega os pontos quando o usuário voltar da tela de ocorrência
        if (currentFuncionarioId.isNotEmpty()) {
            loadPontos(currentFuncionarioId)
        }
    }

    private fun loadPontos(funcionarioId: String) {
        lifecycleScope.launch {
            try {
                val result = pontoService.listarPontosDoFuncionario(funcionarioId, limit = 100)
                result.fold(
                    onSuccess = { pontos ->
                        processarPontos(pontos) { reportEntries ->
                            binding.reportRecyclerView.adapter = ReportAdapter(reportEntries.toMutableList(), currentUserRole)
                        }
                    },
                    onFailure = { error ->
                        Log.e("RecordUserActivity", "Erro ao carregar pontos", error)
                        Toast.makeText(
                            this@RecordUserActivity,
                            "Erro ao carregar pontos: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("RecordUserActivity", "Erro ao carregar pontos", e)
                Toast.makeText(
                    this@RecordUserActivity,
                    "Erro ao carregar pontos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Processa a lista de pontos e agrupa por dia, calculando as horas trabalhadas
     */
    private fun processarPontos(pontos: List<Ponto>, onComplete: (List<ReportEntry>) -> Unit) {
        lifecycleScope.launch {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            // Agrupa pontos por data
            val pontosPorDia = pontos.groupBy { ponto ->
                ponto.dataHoraPonto?.let {
                    val millis = it.seconds * 1000
                    dateFormat.format(millis)
                } ?: ""
            }

            val reportEntries = pontosPorDia.map { (data, pontosDoDia) ->
                var entrada: String? = null
                var saida: String? = null
                var pausas = 0
                val observacoes = mutableListOf<String>()
                var status = StatusAprovacao.AGUARDANDO
                var primeiroPontoId = ""

                pontosDoDia.forEach { ponto ->
                    if (primeiroPontoId.isEmpty()) {
                        primeiroPontoId = ponto.id
                    }

                    val millis = ponto.dataHoraPonto?.seconds?.times(1000) ?: 0L
                    val horario = timeFormat.format(millis)

                    when (ponto.tipoDePonto) {
                        TipoDePonto.ENTRADA -> {
                            if (entrada == null) entrada = horario
                        }
                        TipoDePonto.SAIDA -> {
                            saida = horario
                        }
                        TipoDePonto.INICIO_DE_INTERVALO, TipoDePonto.FIM_DE_INTERVALO -> {
                            pausas++
                        }
                    }

                    if (ponto.observacao.isNotBlank()) {
                        observacoes.add(ponto.observacao)
                    }

                    // Pega o status mais recente
                    status = ponto.statusAprovacao
                }

                // Calcula horas trabalhadas
                val horasRegistradas = calcularHorasTrabalhadas(entrada, saida)

                // Busca ocorrência relacionada ao ponto
                val ocorrenciaResult = ocorrenciaRepository.buscarOcorrenciaPorPontoId(primeiroPontoId)
                val ocorrencia = ocorrenciaResult.getOrNull()

                // Se tem ocorrência, trunca a justificativa para caber na tabela
                val descricao = if (ocorrencia != null) {
                    val justificativa = ocorrencia.justificativa
                    if (justificativa.length > 20) {
                        justificativa.take(17) + "..."
                    } else {
                        justificativa
                    }
                } else {
                    "+"
                }

                ReportEntry(
                    pontoId = primeiroPontoId,
                    data = data,
                    entryTime = entrada,
                    exitTime = saida,
                    pauseCount = pausas / 2, // Divide por 2 pois cada pausa tem início e fim
                    registeredHours = horasRegistradas,
                    observations = if (observacoes.isNotEmpty()) observacoes.joinToString(", ") else null,
                    status = status,
                    descricao = descricao,
                    hasOcorrencia = ocorrencia != null
                )
            }.sortedByDescending { it.data } // Ordena do mais recente para o mais antigo

            onComplete(reportEntries)
        }
    }

    /**
     * Calcula as horas trabalhadas entre entrada e saída
     */
    private fun calcularHorasTrabalhadas(entrada: String?, saida: String?): String {
        if (entrada == null || saida == null) return "0"

        return try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val entradaDate = timeFormat.parse(entrada)
            val saidaDate = timeFormat.parse(saida)

            if (entradaDate != null && saidaDate != null) {
                val diffMillis = saidaDate.time - entradaDate.time
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60

                if (minutes > 0) {
                    "$hours:${String.format(Locale.getDefault(), "%02d", minutes)}"
                } else {
                    hours.toString()
                }
            } else {
                "0"
            }
        } catch (e: Exception) {
            Log.e("RecordUserActivity", "Erro ao calcular horas", e)
            "0"
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

        override fun onBindViewHolder(holder: RowViewHolder, @SuppressLint("RecyclerView") position: Int) {
            val item = reportEntries[position]
            holder.binding.apply {
                reportData.text = item.data
                reportEntrada.text = item.entryTime ?: ""
                reportSaida.text = item.exitTime ?: ""
                reportPausas.text = item.pauseCount.toString()
                reportHorasRegistradas.text = item.registeredHours
                reportObservacoes.text = item.observations ?: ""
                reportHomologacao.text = item.status.name
                reportDescricao.text = item.descricao

                // Configura o comportamento da descrição
                if (item.hasOcorrencia) {
                    // Se já tem ocorrência, mostra a justificativa e não é clicável
                    reportDescricao.setTextColor(Color.GRAY)
                    reportDescricao.isClickable = false
                    reportDescricao.setOnClickListener(null)
                } else {
                    // Se não tem ocorrência, mostra o "+" e torna clicável
                    reportDescricao.setTextColor(Color.BLUE)
                    reportDescricao.isClickable = true
                    reportDescricao.setOnClickListener {
                        // Redireciona para OccurrenceActivity passando o pontoId
                        val intent = Intent(holder.itemView.context, OccurrenceActivity::class.java)
                        intent.putExtra("PONTO_ID", item.pontoId)
                        holder.itemView.context.startActivity(intent)
                    }
                }

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
