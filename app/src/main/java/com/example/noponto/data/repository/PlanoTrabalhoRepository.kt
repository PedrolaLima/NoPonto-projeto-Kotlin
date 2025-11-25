package com.example.noponto.data.repository

import com.example.noponto.domain.model.DiaSemana
import com.example.noponto.domain.model.PlanoTrabalho
import com.example.noponto.domain.model.HorarioDia
import com.example.noponto.domain.repository.IPlanoTrabalhoRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação do repositório de PlanoTrabalho usando Firestore.
 * - Ao salvar/atualizar criamos também campos auxiliares `presencialDias` e `remotoDias`
 *   (List<String>) para permitir consultas com whereArrayContains.
 */
class PlanoTrabalhoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IPlanoTrabalhoRepository {

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

    private fun horarioListToDias(list: List<HorarioDia>): List<String> =
        list.map { it.dia.name }

    override suspend fun salvarPlano(plano: PlanoTrabalho): Result<String> {
        return try {
            val docRef = db.collection("planos").document()
            val id = docRef.id

            val presencialDias = horarioListToDias(plano.presencial)
            val remotoDias = horarioListToDias(plano.remoto)

            val toSave = mapOf(
                "id" to id,
                "funcionarioRef" to plano.funcionarioRef,
                "funcionarioId" to plano.funcionarioId,
                "presencial" to plano.presencial,
                "presencialDias" to presencialDias,
                "remoto" to plano.remoto,
                "remotoDias" to remotoDias,
                "ativo" to plano.ativo,
                "criadoEm" to (plano.criadoEm ?: Timestamp.now())
            )

            docRef.set(toSave).awaitVoid()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun atualizarPlano(plano: PlanoTrabalho): Result<Unit> {
        return try {
            if (plano.id.isBlank()) return Result.failure(IllegalArgumentException("plano.id is blank"))
            val docRef = db.collection("planos").document(plano.id)

            val presencialDias = horarioListToDias(plano.presencial)
            val remotoDias = horarioListToDias(plano.remoto)

            val toSave = mapOf(
                "funcionarioRef" to plano.funcionarioRef,
                "funcionarioId" to plano.funcionarioId,
                "presencial" to plano.presencial,
                "presencialDias" to presencialDias,
                "remoto" to plano.remoto,
                "remotoDias" to remotoDias,
                "ativo" to plano.ativo,
                "criadoEm" to (plano.criadoEm ?: Timestamp.now())
            )

            docRef.set(toSave).awaitVoid()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removerPlano(planoId: String): Result<Unit> {
        return try {
            if (planoId.isBlank()) return Result.failure(IllegalArgumentException("planoId is blank"))
            db.collection("planos").document(planoId).delete().awaitVoid()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun buscarPlanoPorId(planoId: String): Result<PlanoTrabalho?> {
        return try {
            if (planoId.isBlank()) return Result.success(null)
            val snap = db.collection("planos").document(planoId).get().await()
            val plano = snap.toObject(PlanoTrabalho::class.java)
            Result.success(plano)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listarPlanosDoFuncionario(funcionarioId: String, ativoOnly: Boolean, limit: Int): Result<List<PlanoTrabalho>> {
        return try {
            var query: Query = db.collection("planos").whereEqualTo("funcionarioId", funcionarioId)
            if (ativoOnly) query = query.whereEqualTo("ativo", true)
            query = query.orderBy("criadoEm", Query.Direction.DESCENDING).limit(limit.toLong())
            val snap = query.get().await()
            val list = snap.documents.mapNotNull { it.toObject(PlanoTrabalho::class.java) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun buscarPlanosQueContemDia(funcionarioId: String, dia: DiaSemana): Result<List<PlanoTrabalho>> {
        return try {
            // Realiza duas queries (presencial e remoto) e combina resultados
            val diaStr = dia.name
            val presencialQuery = db.collection("planos")
                .whereEqualTo("funcionarioId", funcionarioId)
                .whereArrayContains("presencialDias", diaStr)

            val remotoQuery = db.collection("planos")
                .whereEqualTo("funcionarioId", funcionarioId)
                .whereArrayContains("remotoDias", diaStr)

            val presencialSnap = presencialQuery.get().await()
            val remotoSnap = remotoQuery.get().await()

            val combined = (presencialSnap.documents + remotoSnap.documents)
                .distinctBy { it.id }
                .mapNotNull { it.toObject(PlanoTrabalho::class.java) }

            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setAtivo(planoId: String, ativo: Boolean): Result<Unit> {
        return try {
            if (planoId.isBlank()) return Result.failure(IllegalArgumentException("planoId is blank"))
            db.collection("planos").document(planoId).update(mapOf("ativo" to ativo)).awaitVoid()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun buscarPlanoAtivoDoFuncionario(funcionarioId: String): Result<PlanoTrabalho?> {
        return try {
            val query = db.collection("planos")
                .whereEqualTo("funcionarioId", funcionarioId)
                .whereEqualTo("ativo", true)
                .limit(1)
            val snap = query.get().await()
            val plano = snap.documents.firstOrNull()?.toObject(PlanoTrabalho::class.java)
            Result.success(plano)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}