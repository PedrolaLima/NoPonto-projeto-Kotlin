package com.example.noponto.data.repository

import com.example.noponto.domain.model.Ponto
import com.example.noponto.domain.repository.IPontoRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Date

class PontoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IPontoRepository {

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

    private suspend fun Task<Void>.awaitVoid(): Unit = suspendCancellableCoroutine { cont ->
        addOnSuccessListener {
            cont.resume(Unit)
        }
        addOnFailureListener { exc ->
            if (!cont.isCompleted) cont.resumeWithException(exc)
        }
        addOnCanceledListener {
            if (!cont.isCompleted) cont.cancel()
        }
    }

    override suspend fun salvarPonto(ponto: Ponto): Result<String> {
        return try {
            // cria id manualmente para permitir set(id = ...)
            val docRef = db.collection("pontos").document()
            val id = docRef.id
            val toSave = ponto.copy(id = id)
            docRef.set(toSave).awaitVoid()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun atualizarPonto(ponto: Ponto): Result<Unit> {
        return try {
            if (ponto.id.isBlank()) return Result.failure(IllegalArgumentException("ponto.id is blank"))
            val docRef = db.collection("pontos").document(ponto.id)
            docRef.set(ponto).awaitVoid()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removerPonto(pontoId: String): Result<Unit> {
        return try {
            if (pontoId.isBlank()) return Result.failure(IllegalArgumentException("pontoId is blank"))
            db.collection("pontos").document(pontoId).delete().awaitVoid()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun buscarPontoPorId(pontoId: String): Result<Ponto?> {
        return try {
            if (pontoId.isBlank()) return Result.success(null)
            val snap = db.collection("pontos").document(pontoId).get().await()
            val ponto = snap.toObject(Ponto::class.java)
            Result.success(ponto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listarPontosDoFuncionario(funcionarioId: String, limit: Int): Result<List<Ponto>> {
        return try {
            // Query simples sem orderBy para evitar necessidade de índice composto
            val query = db.collection("pontos")
                .whereEqualTo("funcionarioId", funcionarioId)
            val snap = query.get().await()
            // Ordena no código e aplica limit
            val list = snap.documents
                .mapNotNull { it.toObject(Ponto::class.java) }
                .sortedByDescending { it.dataHoraPonto }
                .take(limit)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun buscarPorPeriodo(funcionarioId: String, startMillis: Long, endMillis: Long): Result<List<Ponto>> {
        return try {
            val startTs = Timestamp(Date(startMillis))
            val endTs = Timestamp(Date(endMillis))

            // Query simples sem orderBy para evitar necessidade de índice composto
            val query = db.collection("pontos")
                .whereEqualTo("funcionarioId", funcionarioId)
                .whereGreaterThanOrEqualTo("dataHoraPonto", startTs)
                .whereLessThanOrEqualTo("dataHoraPonto", endTs)

            val snap = query.get().await()
            // Ordena no código
            val list = snap.documents
                .mapNotNull { it.toObject(Ponto::class.java) }
                .sortedBy { it.dataHoraPonto }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun buscarUltimosPontos(funcionarioId: String, limit: Int): Result<List<Ponto>> {
        return try {
            // Query simples sem orderBy para evitar necessidade de índice composto
            val query = db.collection("pontos")
                .whereEqualTo("funcionarioId", funcionarioId)
            val snap = query.get().await()
            // Ordena no código e aplica limit
            val list = snap.documents
                .mapNotNull { it.toObject(Ponto::class.java) }
                .sortedByDescending { it.dataHoraPonto }
                .take(limit)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}