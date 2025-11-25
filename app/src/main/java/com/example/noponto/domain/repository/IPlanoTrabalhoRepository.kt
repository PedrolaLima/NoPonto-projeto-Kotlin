package com.example.noponto.domain.repository

import com.example.noponto.domain.model.DiaSemana
import com.example.noponto.domain.model.PlanoTrabalho

/**
 * Repositório para manipular planos de trabalho (PlanoTrabalho).
 *
 * As operações usam `suspend` e retornam `Result<T>` para facilitar o uso com coroutines
 * e encapsular sucesso/erro.
 */
interface IPlanoTrabalhoRepository {

    /**
     * Salva um novo plano. Retorna o id do documento criado.
     */
    suspend fun salvarPlano(plano: PlanoTrabalho): Result<String>

    /**
     * Atualiza um plano existente (espera-se plano.id preenchido).
     */
    suspend fun atualizarPlano(plano: PlanoTrabalho): Result<Unit>

    /**
     * Remove um plano pelo id.
     */
    suspend fun removerPlano(planoId: String): Result<Unit>

    /**
     * Busca um plano por id.
     */
    suspend fun buscarPlanoPorId(planoId: String): Result<PlanoTrabalho?>

    /**
     * Lista planos associados a um funcionário.
     * - `ativoOnly`: quando true, retorna apenas planos com `ativo == true`.
     */
    suspend fun listarPlanosDoFuncionario(funcionarioId: String, ativoOnly: Boolean = true, limit: Int = 50): Result<List<PlanoTrabalho>>

    /**
     * Busca planos que contenham o dia especificado (presencial ou remoto) para o funcionário.
     * Útil para verificar se o funcionário trabalha presencialmente/remotamente em um dia.
     */
    suspend fun buscarPlanosQueContemDia(funcionarioId: String, dia: DiaSemana): Result<List<PlanoTrabalho>>

    /**
     * Ativa ou desativa um plano (toggle/alteração do campo `ativo`).
     */
    suspend fun setAtivo(planoId: String, ativo: Boolean): Result<Unit>

    /**
     * Busca o plano (primeiro) ativo do funcionário, se existir.
     */
    suspend fun buscarPlanoAtivoDoFuncionario(funcionarioId: String): Result<PlanoTrabalho?>
}