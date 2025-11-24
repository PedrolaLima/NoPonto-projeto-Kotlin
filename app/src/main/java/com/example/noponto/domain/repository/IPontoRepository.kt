package com.example.noponto.domain.repository

import com.example.noponto.domain.model.Ponto

/**
 * Interface do repositório de pontos (Ponto).
 *
 * Observações/assunções:
 * - Usei funções `suspend` retornando `Result<T>` para integração com coroutines (padrão moderno em Android).
 * - Se você prefere callbacks (addOnSuccessListener/addOnFailureListener) posso adicionar versões com callbacks.
 */
interface IPontoRepository {

    /**
     * Salva um novo ponto no banco e retorna o id do documento criado (Result.success(id)).
     * Em caso de erro retorna Result.failure(Throwable).
     */
    suspend fun salvarPonto(ponto: Ponto): Result<String>

    /**
     * Atualiza um ponto existente. Espera-se que `ponto.id` contenha o id do documento.
     * Retorna Result.success(Unit) em sucesso.
     */
    suspend fun atualizarPonto(ponto: Ponto): Result<Unit>

    /**
     * Remove um ponto pelo id do documento.
     */
    suspend fun removerPonto(pontoId: String): Result<Unit>

    /**
     * Busca um ponto pelo id. Retorna null dentro de Result.success quando não existir.
     */
    suspend fun buscarPontoPorId(pontoId: String): Result<Ponto?>

    /**
     * Lista pontos associados a um funcionário (por funcionarioId).
     * `limit` é um limite opcional para paginação.
     */
    suspend fun listarPontosDoFuncionario(funcionarioId: String, limit: Int = 100): Result<List<Ponto>>

    /**
     * Busca pontos de um funcionário em um intervalo de tempo (startMillis/endMillis em epoch millis UTC).
     * Isso facilita consultas baseadas em data sem depender de formatos de string.
     */
    suspend fun buscarPorPeriodo(funcionarioId: String, startMillis: Long, endMillis: Long): Result<List<Ponto>>

    /**
     * Retorna os últimos pontos de um funcionário (ordenados por dataHoraPonto desc).
     */
    suspend fun buscarUltimosPontos(funcionarioId: String, limit: Int = 20): Result<List<Ponto>>
}