package com.example.noponto.domain.repository

import com.example.noponto.domain.model.Ocorrencia

/**
 * Interface do repositório para Ocorrência.
 * - Métodos `suspend` que retornam `Result<T>` para uso com coroutines
 * - Responsabilidade: persistir e consultar ocorrências no backend (Firestore)
 */
interface IOcorrenciaRepository {

    /**
     * Salva uma nova ocorrência e retorna o id do documento criado.
     */
    suspend fun salvarOcorrencia(ocorrencia: Ocorrencia): Result<String>

    /**
     * Atualiza uma ocorrência existente (espera-se ocorrencia.id preenchido).
     */
    suspend fun atualizarOcorrencia(ocorrencia: Ocorrencia): Result<Unit>

    /**
     * Remove uma ocorrência pelo id.
     */
    suspend fun removerOcorrencia(ocorrenciaId: String): Result<Unit>

    /**
     * Busca uma ocorrência por id.
     */
    suspend fun buscarOcorrenciaPorId(ocorrenciaId: String): Result<Ocorrencia?>

    /**
     * Lista ocorrências de um funcionário (ordenadas por criadoEm desc). Limite opcional.
     */
    suspend fun listarOcorrenciasDoFuncionario(funcionarioId: String, limit: Int = 50): Result<List<Ocorrencia>>

    /**
     * Lista ocorrências por status (PENDENTE/APROVADO/REJEITADO). Limite opcional.
     */
    suspend fun listarPorStatus(status: Ocorrencia.StatusOcorrencia, limit: Int = 50): Result<List<Ocorrencia>>

    /**
     * Atualiza apenas o status da ocorrência (aprovado/rejeitado/pendente).
     */
    suspend fun setStatus(ocorrenciaId: String, status: Ocorrencia.StatusOcorrencia): Result<Unit>

    /**
     * Retorna as últimas ocorrências de um funcionário (limit padrão 20).
     */
    suspend fun buscarUltimasOcorrencias(funcionarioId: String, limit: Int = 20): Result<List<Ocorrencia>>

    /**
     * Busca ocorrência relacionada a um ponto específico.
     */
    suspend fun buscarOcorrenciaPorPontoId(pontoId: String): Result<Ocorrencia?>
}

