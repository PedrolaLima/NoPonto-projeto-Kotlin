package com.example.noponto.domain.services

import com.example.noponto.domain.model.Ponto
import com.example.noponto.domain.model.TipoDePonto
import com.example.noponto.domain.repository.IPontoRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Serviço de alto nível para manipular pontos usando o repositório.
 * Fornece métodos suspend para uso em coroutines/ViewModel.
 */
class PontoService(
    private val repo: IPontoRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result)
        }
        addOnFailureListener { exc ->
            if (!cont.isCompleted) cont.resumeWithException(exc)
        }
        addOnCanceledListener {
            if (!cont.isCompleted) cont.cancel()
        }
    }

    /**
     * Registra um ponto para o funcionário atualmente autenticado.
     * dateStr: "dd/MM/yyyy" ; timeStr: "HH:mm"
     */
    suspend fun registrarPonto(
        dateStr: String,
        timeStr: String,
        tipo: TipoDePonto = TipoDePonto.ENTRADA,
        homeOffice: Boolean = false,
        observacao: String = ""
    ): Result<String> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Usuário não autenticado"))

        val uid = user.uid
        val funcRef: DocumentReference = firestore.collection("funcionarios").document(uid)

        // tenta ler nome/cargo do documento do funcionário para preencher snapshot
        val (nome, cargo) = try {
            val snap = funcRef.get().await()
            val n = snap.getString("nome") ?: ""
            val c = snap.getString("cargo") ?: ""
            Pair(n, c)
        } catch (e: Exception) {
            Pair("", "")
        }

        // cria o Ponto usando factory do model
        val ponto = Ponto.createForLoggedFuncionario(
            funcionarioId = uid,
            funcionarioNome = nome,
            funcionarioCargo = cargo,
            dateStr = dateStr,
            timeStr = timeStr,
            tipo = tipo,
            homeOffice = homeOffice,
            funcionarioRef = funcRef,
            observacao = observacao
        )

        return repo.salvarPonto(ponto)
    }

    suspend fun atualizarPonto(ponto: Ponto): Result<Unit> = repo.atualizarPonto(ponto)

    suspend fun removerPonto(pontoId: String): Result<Unit> = repo.removerPonto(pontoId)

    suspend fun obterPontoPorId(pontoId: String): Result<Ponto?> = repo.buscarPontoPorId(pontoId)

    suspend fun listarUltimosPontos(funcionarioId: String, limit: Int = 20): Result<List<Ponto>> =
        repo.buscarUltimosPontos(funcionarioId, limit)

    suspend fun listarPontosDoFuncionario(funcionarioId: String, limit: Int = 100): Result<List<Ponto>> =
        repo.listarPontosDoFuncionario(funcionarioId, limit)

    suspend fun buscarPorPeriodo(funcionarioId: String, startMillis: Long, endMillis: Long): Result<List<Ponto>> =
        repo.buscarPorPeriodo(funcionarioId, startMillis, endMillis)
}