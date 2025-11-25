package com.example.noponto.data.repository

import android.util.Log
import com.example.noponto.data.model.FirestoreFuncionario
import com.example.noponto.data.model.toDomain
import com.example.noponto.data.model.toFirestoreModel
import com.example.noponto.domain.model.Funcionario
import com.example.noponto.domain.repository.IFuncionarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FuncionarioRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IFuncionarioRepository {

    companion object {
        private const val TAG = "FuncionarioRepository"
    }

    override suspend fun registerFuncionario(funcionario: Funcionario, password: String): Result<Unit> {
        return try {
            val user = auth.createUserWithEmailAndPassword(funcionario.email, password).await()

            val uid = user.user?.uid ?: return Result.failure(Exception("Failed to get user ID"))

            val toSave = funcionario.copy(id = uid)
            firestore.collection("funcionarios").document(uid)
                .set(toSave.toFirestoreModel())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFuncionarioByEmail(email: String): Result<Funcionario?> {
        return try {
            val querySnapshot = firestore.collection("funcionarios")
                .whereEqualTo("email", email)
                .get()
                .await()

            val funcionario = querySnapshot.documents.firstOrNull()?.toObject(FirestoreFuncionario::class.java)?.toDomain()
            Result.success(funcionario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFuncionarioStatus(
        id: String,
        status: Boolean
    ): Result<Unit> {
        return try {
            firestore.collection("funcionarios").document(id)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllFuncionarios(): Result<List<Funcionario>> {
        return try {
            val querySnapshot = firestore.collection("funcionarios")
                .get()
                .await()

            val funcionarios = querySnapshot.documents.map() { it.toObject(FirestoreFuncionario::class.java)?.toDomain()!! }

            Result.success(funcionarios)
        } catch (e: Exception) {
            Result.failure(e)

        }
    }

    override suspend fun deleteFuncionario(id: String): Result<Unit> {
        return try {
            firestore.collection("funcionarios").document(id)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFuncionario(funcionario: Funcionario): Result<Unit> {
        return try {
            firestore.collection("funcionarios").document(funcionario.id)
                .set(funcionario.toFirestoreModel())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFuncionarioById(id: String): Result<Funcionario?> {
        return try {
            Log.d(TAG, "====== INÍCIO getFuncionarioById ======")
            Log.d(TAG, "Fetching funcionario with id: $id")

            val documentSnapshot = firestore.collection("funcionarios")
                .document(id)
                .get()
                .await()

            if (!documentSnapshot.exists()) {
                Log.w(TAG, "Document does not exist for id: $id")
                return Result.success(null)
            }

            val rawData = documentSnapshot.data
            Log.d(TAG, "===== RAW FIRESTORE DATA =====")
            Log.d(TAG, "Document exists: ${documentSnapshot.exists()}")
            Log.d(TAG, "Full document data: $rawData")

            // Log cada campo individualmente
            rawData?.forEach { (key, value) ->
                Log.d(TAG, "  Field '$key': type=${value?.javaClass?.simpleName}, value=$value")
            }
            Log.d(TAG, "==============================")

            val firestoreDto = documentSnapshot.toObject(FirestoreFuncionario::class.java)

            if (firestoreDto == null) {
                Log.e(TAG, "Failed to deserialize to FirestoreFuncionario - toObject returned null")
                return Result.success(null)
            }

            // CORREÇÃO: O Firestore não inclui o ID do documento nos dados, precisamos setá-lo manualmente
            if (firestoreDto.id.isBlank()) {
                Log.d(TAG, "Setting document ID manually: ${documentSnapshot.id}")
                firestoreDto.id = documentSnapshot.id
            }

            Log.d(TAG, "===== FIRESTORE DTO =====")
            Log.d(TAG, "  id: ${firestoreDto.id}")
            Log.d(TAG, "  nome: ${firestoreDto.nome}")
            Log.d(TAG, "  email: ${firestoreDto.email}")
            Log.d(TAG, "  cpf: ${firestoreDto.cpf}")
            Log.d(TAG, "  status: ${firestoreDto.status}")
            Log.d(TAG, "  dataNascimento: type=${firestoreDto.dataNascimento?.javaClass?.simpleName}, value=${firestoreDto.dataNascimento}")
            Log.d(TAG, "  cargo: ${firestoreDto.cargo}")
            Log.d(TAG, "  endereco: ${firestoreDto.endereco}")
            Log.d(TAG, "=========================")

            Log.d(TAG, "Calling toDomain()...")
            val funcionario = firestoreDto.toDomain()

            if (funcionario == null) {
                Log.e(TAG, "❌ toDomain() returned null - check FirestoreFuncionario logs above")
            } else {
                Log.d(TAG, "✅ Successfully converted to domain Funcionario: ${funcionario.nome}")
            }

            Log.d(TAG, "====== FIM getFuncionarioById ======")
            Result.success(funcionario)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in getFuncionarioById for id: $id", e)
            Result.failure(e)
        }
    }

    override suspend fun getFuncionariosByStatus(status: Boolean): Result<List<Funcionario>> {
        return try {
            val querySnapshot = firestore.collection("funcionarios")
                .whereEqualTo("status", status)
                .get()
                .await()

            val funcionarios = querySnapshot.documents.mapNotNull { it.toObject(FirestoreFuncionario::class.java)?.toDomain() }
            Result.success(funcionarios)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchFuncionariosByName(name: String): Result<List<Funcionario>> {
        return try {
            val querySnapshot = firestore.collection("funcionarios")
                .whereGreaterThanOrEqualTo("nome", name)
                .whereLessThanOrEqualTo("nome", name + '\uf8ff')
                .get()
                .await()

            val funcionarios = querySnapshot.documents.mapNotNull { it.toObject(FirestoreFuncionario::class.java)?.toDomain() }
            Result.success(funcionarios)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}