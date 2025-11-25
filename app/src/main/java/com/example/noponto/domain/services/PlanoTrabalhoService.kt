package com.example.noponto.domain.services

import com.example.noponto.domain.model.DiaSemana
import com.example.noponto.domain.model.PlanoTrabalho
import com.example.noponto.domain.repository.IPlanoTrabalhoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Serviço de alto nível para operações de PlanoTrabalho.
 * - Valida regras básicas antes de delegar ao repositório
 * - Fornece helpers para criar Plano a partir de dados da UI
 */
class PlanoTrabalhoService(
    private val repo: IPlanoTrabalhoRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Valida regras básicas do plano: ao menos um dia selecionado e horários válidos.
     */
    private fun validarPlanoInterno(plano: PlanoTrabalho): Result<Unit> {
        // deve ter pelo menos um dia em presencial ou remoto
        if (plano.presencial.isEmpty() && plano.remoto.isEmpty()) {
            return Result.failure(IllegalArgumentException("Plano deve ter ao menos um dia presencial ou remoto"))
        }

        // cada HorarioDia: inicio < fim
        val invalid = (plano.presencial + plano.remoto).firstOrNull { it.inicioMinutes >= it.fimMinutes }
        if (invalid != null) {
            return Result.failure(IllegalArgumentException("Horário inválido para ${invalid.dia}: início >= fim"))
        }

        // funcionarioId obrigatorio para persistência
        if (plano.funcionarioId.isBlank()) {
            return Result.failure(IllegalArgumentException("funcionarioId não informado"))
        }

        return Result.success(Unit)
    }

    /**
     * Cria e salva um Plano a partir dos dados da UI. Espera as maps de dias (presencial/remoto)
     * e horários em "HH:mm". Retorna Result com id do plano criado.
     */
    suspend fun criarPlanoFromUi(
        presencialCheckedMap: Map<DiaSemana, Boolean>,
        entradaPresencial: String,
        saidaPresencial: String,
        remotoCheckedMap: Map<DiaSemana, Boolean>,
        entradaRemoto: String,
        saidaRemoto: String,
        ativo: Boolean = true
    ): Result<String> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Usuário não autenticado"))
        val uid = user.uid
        val funcRef = firestore.collection("funcionarios").document(uid)

        val plano = PlanoTrabalho.createFromUi(
            funcionarioId = uid,
            funcionarioRef = funcRef,
            presencialCheckedMap = presencialCheckedMap,
            entradaPresencial = entradaPresencial,
            saidaPresencial = saidaPresencial,
            remotoCheckedMap = remotoCheckedMap,
            entradaRemoto = entradaRemoto,
            saidaRemoto = saidaRemoto,
            ativo = ativo
        )

        // valida
        val v = validarPlanoInterno(plano)
        if (v.isFailure) return Result.failure(v.exceptionOrNull()!!)

        // salva via repo
        return repo.salvarPlano(plano)
    }

    suspend fun atualizarPlano(plano: PlanoTrabalho): Result<Unit> {
        val v = validarPlanoInterno(plano)
        if (v.isFailure) return Result.failure(v.exceptionOrNull()!!)
        return repo.atualizarPlano(plano)
    }

    suspend fun removerPlano(planoId: String): Result<Unit> = repo.removerPlano(planoId)

    suspend fun obterPlanoPorId(planoId: String): Result<PlanoTrabalho?> = repo.buscarPlanoPorId(planoId)

    suspend fun listarPlanosDoFuncionario(funcionarioId: String, ativoOnly: Boolean = true, limit: Int = 50): Result<List<PlanoTrabalho>> =
        repo.listarPlanosDoFuncionario(funcionarioId, ativoOnly, limit)

    suspend fun buscarPlanosQueContemDia(funcionarioId: String, dia: DiaSemana): Result<List<PlanoTrabalho>> =
        repo.buscarPlanosQueContemDia(funcionarioId, dia)

    suspend fun setAtivo(planoId: String, ativo: Boolean): Result<Unit> = repo.setAtivo(planoId, ativo)

    suspend fun buscarPlanoAtivoDoFuncionario(funcionarioId: String): Result<PlanoTrabalho?> = repo.buscarPlanoAtivoDoFuncionario(funcionarioId)

}