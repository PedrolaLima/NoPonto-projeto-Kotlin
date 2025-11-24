package com.example.noponto.domain.services

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

    /**
     * Cria e salva uma ocorrência a partir da UI. Se `atestadoPath` for informado, será salvo
     * no documento (não fazemos upload aqui).
     * dateStr: "dd/MM/yyyy" ; timeStr: "HH:mm"
     */
    suspend fun criarOcorrenciaFromUi(
        justificativa: String,
        dateStr: String,
        timeStr: String,
        atestadoPath: String? = null
    ): Result<String> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Usuário não autenticado"))
        val uid = user.uid
        val funcRef = firestore.collection("funcionarios").document(uid)

        if (justificativa.isBlank()) return Result.failure(IllegalArgumentException("Justificativa é obrigatória"))

        val ocorr = Ocorrencia.createFromUi(
            funcionarioId = uid,
            funcionarioRef = funcRef,
            funcionarioNome = user.displayName ?: "",
            justificativa = justificativa,
            dateStr = dateStr,
            timeStr = timeStr,
            hasAtestado = atestadoPath != null,
            atestadoStoragePath = atestadoPath
        )

        return repo.salvarOcorrencia(ocorr)
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
