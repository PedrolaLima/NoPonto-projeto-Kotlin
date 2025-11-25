package com.example.noponto.data.repository

import android.util.Log
import com.example.noponto.domain.model.Ocorrencia
import com.example.noponto.domain.repository.IOcorrenciaRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcorrenciaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IOcorrenciaRepository {

    companion object {
        private const val TAG = "OcorrenciaRepository"
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exc -> if (!cont.isCompleted) cont.resumeWithException(exc) }
        addOnCanceledListener { if (!cont.isCompleted) cont.cancel() }
    }

    private suspend fun Task<Void>.awaitVoid(): Unit = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(Unit) }
        addOnFailureListener { exc -> if (!cont.isCompleted) cont.resumeWithException(exc) }
        addOnCanceledListener { if (!cont.isCompleted) cont.cancel() }
    }

    private fun docToOcorrencia(doc: DocumentSnapshot): Ocorrencia? {
        if (doc.data == null) return null
        try {
            val id = doc.getString("id") ?: doc.id
            val funcionarioRef = doc.getDocumentReference("funcionarioRef")
            val funcionarioId = doc.getString("funcionarioId") ?: ""
            val funcionarioNome = doc.getString("funcionarioNome") ?: ""
            val justificativa = doc.getString("justificativa") ?: ""
            val dataHora = doc.getTimestamp("dataHora")
            val hasAtestado = doc.getBoolean("hasAtestado") ?: false
            val atestadoStoragePath = doc.getString("atestadoStoragePath")
            val statusStr = doc.getString("status") ?: Ocorrencia.StatusOcorrencia.PENDENTE.name
            val status = try { Ocorrencia.StatusOcorrencia.valueOf(statusStr) } catch (_: Exception) { Ocorrencia.StatusOcorrencia.PENDENTE }
            val criadoEm = doc.getTimestamp("criadoEm")

            return Ocorrencia(
                id = id,
                funcionarioRef = funcionarioRef,
                funcionarioId = funcionarioId,
                funcionarioNome = funcionarioNome,
                justificativa = justificativa,
                dataHora = dataHora,
                hasAtestado = hasAtestado,
                atestadoStoragePath = atestadoStoragePath,
                status = status,
                criadoEm = criadoEm
            )
        } catch (_: Exception) {
            return null
        }
    }

    override suspend fun salvarOcorrencia(ocorrencia: Ocorrencia): Result<String> {
        return try {
            Log.d(TAG, "salvarOcorrencia: iniciando")
            val docRef = db.collection("ocorrencias").document()
            val id = docRef.id
            Log.d(TAG, "  ID gerado: $id")

            val toSave = mapOf(
                "id" to id,
                "funcionarioRef" to ocorrencia.funcionarioRef,
                "funcionarioId" to ocorrencia.funcionarioId,
                "funcionarioNome" to ocorrencia.funcionarioNome,
                "justificativa" to ocorrencia.justificativa,
                "dataHora" to (ocorrencia.dataHora ?: Timestamp.now()),
                "hasAtestado" to ocorrencia.hasAtestado,
                "atestadoStoragePath" to ocorrencia.atestadoStoragePath,
                "status" to (ocorrencia.status.name ?: Ocorrencia.StatusOcorrencia.PENDENTE.name),
                "criadoEm" to (ocorrencia.criadoEm ?: Timestamp.now()),
                "pontoId" to ocorrencia.pontoId
            )

            Log.d(TAG, "  Dados a salvar:")
            Log.d(TAG, "    funcionarioId: ${toSave["funcionarioId"]}")
            Log.d(TAG, "    justificativa: ${toSave["justificativa"]}")
            Log.d(TAG, "    pontoId: ${toSave["pontoId"]}")

            docRef.set(toSave).awaitVoid()
            Log.d(TAG, "  Ocorrência salva com sucesso!")
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar ocorrência", e)
            Result.failure(Exception("Falha ao salvar ocorrência: ${e.message}"))
        }
    }

    override suspend fun atualizarOcorrencia(ocorrencia: Ocorrencia): Result<Unit> {
        return try {
            if (ocorrencia.id.isBlank()) return Result.failure(IllegalArgumentException("ocorrencia.id is blank"))
            val docRef = db.collection("ocorrencias").document(ocorrencia.id)
            val toSave = mapOf(
                "funcionarioRef" to ocorrencia.funcionarioRef,
                "funcionarioId" to ocorrencia.funcionarioId,
                "funcionarioNome" to ocorrencia.funcionarioNome,
                "justificativa" to ocorrencia.justificativa,
                "dataHora" to (ocorrencia.dataHora ?: Timestamp.now()),
                "hasAtestado" to ocorrencia.hasAtestado,
                "atestadoStoragePath" to ocorrencia.atestadoStoragePath,
                "status" to (ocorrencia.status.name ?: Ocorrencia.StatusOcorrencia.PENDENTE.name),
                "criadoEm" to (ocorrencia.criadoEm ?: Timestamp.now()),
                "pontoId" to ocorrencia.pontoId
            )
            docRef.set(toSave).awaitVoid()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao atualizar ocorrência"))
        }
    }

    override suspend fun removerOcorrencia(ocorrenciaId: String): Result<Unit> {
        return try {
            if (ocorrenciaId.isBlank()) return Result.failure(IllegalArgumentException("ocorrenciaId is blank"))
            db.collection("ocorrencias").document(ocorrenciaId).delete().awaitVoid()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao remover ocorrência"))
        }
    }

    override suspend fun buscarOcorrenciaPorId(ocorrenciaId: String): Result<Ocorrencia?> {
        return try {
            if (ocorrenciaId.isBlank()) return Result.success(null)
            val snap = db.collection("ocorrencias").document(ocorrenciaId).get().await()
            val occ = docToOcorrencia(snap)
            Result.success(occ)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao buscar ocorrência"))
        }
    }

    override suspend fun listarOcorrenciasDoFuncionario(funcionarioId: String, limit: Int): Result<List<Ocorrencia>> {
        return try {
            val query = db.collection("ocorrencias")
                .whereEqualTo("funcionarioId", funcionarioId)
                .orderBy("criadoEm", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            val snap = query.get().await()
            val list = snap.documents.mapNotNull { docToOcorrencia(it) }
            Result.success(list)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao listar ocorrências"))
        }
    }

    override suspend fun listarPorStatus(status: Ocorrencia.StatusOcorrencia, limit: Int): Result<List<Ocorrencia>> {
        return try {
            val query = db.collection("ocorrencias")
                .whereEqualTo("status", status.name)
                .orderBy("criadoEm", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            val snap = query.get().await()
            val list = snap.documents.mapNotNull { docToOcorrencia(it) }
            Result.success(list)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao listar ocorrências por status"))
        }
    }

    override suspend fun setStatus(ocorrenciaId: String, status: Ocorrencia.StatusOcorrencia): Result<Unit> {
        return try {
            if (ocorrenciaId.isBlank()) return Result.failure(IllegalArgumentException("ocorrenciaId is blank"))
            db.collection("ocorrencias").document(ocorrenciaId).update(mapOf("status" to status.name)).awaitVoid()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao atualizar status"))
        }
    }

    override suspend fun buscarUltimasOcorrencias(funcionarioId: String, limit: Int): Result<List<Ocorrencia>> {
        return try {
            val query = db.collection("ocorrencias")
                .whereEqualTo("funcionarioId", funcionarioId)
                .orderBy("criadoEm", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            val snap = query.get().await()
            val list = snap.documents.mapNotNull { docToOcorrencia(it) }
            Result.success(list)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao buscar últimas ocorrências"))
        }
    }

    override suspend fun buscarOcorrenciaPorPontoId(pontoId: String): Result<Ocorrencia?> {
        return try {
            if (pontoId.isBlank()) return Result.success(null)
            val query = db.collection("ocorrencias")
                .whereEqualTo("pontoId", pontoId)
                .limit(1)
            val snap = query.get().await()
            val ocorrencia = snap.documents.firstOrNull()?.let { docToOcorrencia(it) }
            Result.success(ocorrencia)
        } catch (_: Exception) {
            Result.failure(Exception("Falha ao buscar ocorrência por ponto"))
        }
    }
}
