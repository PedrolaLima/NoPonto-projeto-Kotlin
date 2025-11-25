package com.example.noponto.domain.usecase

import com.example.noponto.data.repository.FuncionarioRepository
import com.example.noponto.domain.model.Funcionario
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthenticationUseCase(
    private val auth: FirebaseAuth = Firebase.auth,
    private val funcionarioRepository: FuncionarioRepository
) {
    suspend fun signUp(funcionario: Funcionario, password: String) {
        funcionarioRepository.registerFuncionario(funcionario, password)
    }

    fun signIn(email: String, password: String, callback: (success: Boolean, user: FirebaseUser?, error: String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Verifica o status do funcionário no Firestore
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = funcionarioRepository.getFuncionarioById(user.uid)
                            withContext(Dispatchers.Main) {
                                result.fold(
                                    onSuccess = { funcionario ->
                                        if (funcionario == null) {
                                            // Funcionário não encontrado no banco
                                            auth.signOut()
                                            callback(false, null, "Funcionário não encontrado no sistema")
                                        } else if (!funcionario.status) {
                                            // Funcionário inativo
                                            auth.signOut()
                                            callback(false, null, "Sua conta está inativa. Entre em contato com o administrador.")
                                        } else {
                                            // Funcionário ativo, pode fazer login
                                            callback(true, user, null)
                                        }
                                    },
                                    onFailure = { error ->
                                        auth.signOut()
                                        callback(false, null, "Erro ao verificar status: ${error.localizedMessage}")
                                    }
                                )
                            }
                        }
                    } else {
                        callback(false, null, "Erro ao autenticar usuário")
                    }
                } else {
                    callback(false, null, task.exception?.localizedMessage)
                }
            }
    }

    fun signOut() { auth.signOut() }

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun sendPasswordReset(email: String, callback: (success: Boolean, error: String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.localizedMessage)
            }
    }
}