package com.example.noponto.domain.services

import android.util.Log
import com.example.noponto.domain.model.Ocorrencia
import com.example.noponto.domain.repository.IOcorrenciaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Serviço de alto nível para operações de Ocorrencia.
 * - NÃO faz upload do atestado (projeto sem Firebase Storage).
 * - Recebe um `atestadoPath: String?` opcional (ex.: URL externa ou caminho já gerenciado pelo app)
 * - Valida inputs básicos antes de salvar
 */
class OcorrenciaService(
    private val repo: IOcorrenciaRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "OcorrenciaService"
    }

    /**
     * Cria e salva uma ocorrência a partir da UI. Se `atestadoPath` for informado, será salvo
     * no documento (não fazemos upload aqui).
     * dateStr: "dd/MM/yyyy" ; timeStr: "HH:mm"
     * pontoId: ID do ponto relacionado (opcional)
     */
    suspend fun criarOcorrenciaFromUi(
        justificativa: String,
        dateStr: String,
        timeStr: String,
        pontoId: String? = null,
        atestadoPath: String? = null
    ): Result<String> {
        Log.d(TAG, "criarOcorrenciaFromUi: iniciando")
        Log.d(TAG, "  justificativa: $justificativa")
        Log.d(TAG, "  dateStr: $dateStr")
        Log.d(TAG, "  timeStr: $timeStr")
        Log.d(TAG, "  pontoId: $pontoId")

        val user = auth.currentUser
        if (user == null) {
            Log.e(TAG, "Usuário não autenticado")
            return Result.failure(IllegalStateException("Usuário não autenticado"))
        }

        val uid = user.uid
        Log.d(TAG, "  uid: $uid")
        Log.d(TAG, "  displayName: ${user.displayName}")

        val funcRef = firestore.collection("funcionarios").document(uid)

        if (justificativa.isBlank()) {
            Log.e(TAG, "Justificativa vazia")
            return Result.failure(IllegalArgumentException("Justificativa é obrigatória"))
        }

        Log.d(TAG, "Criando objeto Ocorrencia...")
        val ocorr = Ocorrencia.createFromUi(
            funcionarioId = uid,
            funcionarioRef = funcRef,
            funcionarioNome = user.displayName ?: "",
            justificativa = justificativa,
            dateStr = dateStr,
            timeStr = timeStr,
            hasAtestado = atestadoPath != null,
            atestadoStoragePath = atestadoPath,
            pontoId = pontoId
        )

        Log.d(TAG, "Ocorrencia criada:")
        Log.d(TAG, "  id: ${ocorr.id}")
        Log.d(TAG, "  funcionarioId: ${ocorr.funcionarioId}")
        Log.d(TAG, "  justificativa: ${ocorr.justificativa}")
        Log.d(TAG, "  dataHora: ${ocorr.dataHora}")
        Log.d(TAG, "  pontoId: ${ocorr.pontoId}")

        Log.d(TAG, "Chamando repo.salvarOcorrencia...")
        val result = repo.salvarOcorrencia(ocorr)
        Log.d(TAG, "Resultado: isSuccess=${result.isSuccess}, value=${result.getOrNull()}")

        return result
    }

    suspend fun atualizarOcorrencia(ocorrencia: Ocorrencia): Result<Unit> {
        // valida básica
        if (ocorrencia.justificativa.isBlank()) return Result.failure(IllegalArgumentException("Justificativa é obrigatória"))
        return repo.atualizarOcorrencia(ocorrencia)
    }

    suspend fun removerOcorrencia(ocorrenciaId: String): Result<Unit> = repo.removerOcorrencia(ocorrenciaId)

    suspend fun buscarOcorrenciaPorId(ocorrenciaId: String): Result<Ocorrencia?> = repo.buscarOcorrenciaPorId(ocorrenciaId)

    suspend fun listarUltimasOcorrencias(funcionarioId: String, limit: Int = 20): Result<List<Ocorrencia>> = repo.buscarUltimasOcorrencias(funcionarioId, limit)

    suspend fun listarPorStatus(status: Ocorrencia.StatusOcorrencia, limit: Int = 50): Result<List<Ocorrencia>> = repo.listarPorStatus(status, limit)

    suspend fun setStatus(ocorrenciaId: String, status: Ocorrencia.StatusOcorrencia): Result<Unit> = repo.setStatus(ocorrenciaId, status)
}
