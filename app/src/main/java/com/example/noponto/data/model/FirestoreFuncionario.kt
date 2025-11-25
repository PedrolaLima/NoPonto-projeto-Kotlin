package com.example.noponto.data.model

import android.os.Build
import android.util.Log
import com.example.noponto.domain.model.Cargo
import com.example.noponto.domain.model.Endereco
import com.example.noponto.domain.model.Funcionario
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FirestoreEndereco(
    @get:PropertyName("logradouro") @set:PropertyName("logradouro")
    var logradouro: String = "",
    @get:PropertyName("cidade") @set:PropertyName("cidade")
    var cidade: String = "",
    @get:PropertyName("estado") @set:PropertyName("estado")
    var estado: String = "",
    @get:PropertyName("cep") @set:PropertyName("cep")
    var cep: String = ""
) {
    // Constructor vazio necessário para Firestore
    constructor() : this("", "", "", "")
}

data class FirestoreFuncionario(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("nome") @set:PropertyName("nome")
    var nome: String = "",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("cpf") @set:PropertyName("cpf")
    var cpf: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: Boolean = true,
    @get:PropertyName("dataNascimento") @set:PropertyName("dataNascimento")
    var dataNascimento: Any? = null,  // Aceita String ou Timestamp
    @get:PropertyName("cargo") @set:PropertyName("cargo")
    var cargo: String = Cargo.DESENVOLVEDOR.name,
    @get:PropertyName("endereco") @set:PropertyName("endereco")
    var endereco: FirestoreEndereco = FirestoreEndereco()
) {
    // Constructor vazio necessário para Firestore
    constructor() : this("", "", "", "", true, null, Cargo.DESENVOLVEDOR.name, FirestoreEndereco())
}

fun Funcionario.toFirestoreModel(): FirestoreFuncionario {
    val dateString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        dataNascimento.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } else {
        // Fallback para API < 26
        ""
    }

    return FirestoreFuncionario(
        id = id,
        nome = nome,
        email = email,
        cpf = cpf,
        status = status,
        dataNascimento = dateString,
        cargo = cargo.name,
        endereco = FirestoreEndereco(
            logradouro = endereco.logradouro,
            cidade = endereco.cidade,
            estado = endereco.estado,
            cep = endereco.cep
        )
    )
}

fun FirestoreFuncionario.toDomain(): Funcionario? {
    val TAG = "FirestoreFuncionario"

    Log.d(TAG, "==== INÍCIO toDomain() ====")
    Log.d(TAG, "Input values:")
    Log.d(TAG, "  id: '$id' (blank=${id.isBlank()})")
    Log.d(TAG, "  nome: '$nome' (blank=${nome.isBlank()})")
    Log.d(TAG, "  email: '$email' (blank=${email.isBlank()})")
    Log.d(TAG, "  cpf: '$cpf' (blank=${cpf.isBlank()})")
    Log.d(TAG, "  status: $status")
    Log.d(TAG, "  dataNascimento: type=${dataNascimento?.javaClass?.simpleName}, value=$dataNascimento")
    Log.d(TAG, "  cargo: '$cargo'")
    Log.d(TAG, "  endereco: $endereco")

    // Validação de campos obrigatórios
    if (id.isBlank()) {
        Log.e(TAG, "❌ Campo 'id' está vazio!")
        return null
    }
    if (nome.isBlank()) {
        Log.e(TAG, "❌ Campo 'nome' está vazio!")
        return null
    }
    if (email.isBlank()) {
        Log.e(TAG, "❌ Campo 'email' está vazio!")
        return null
    }
    if (cpf.isBlank()) {
        Log.e(TAG, "❌ Campo 'cpf' está vazio!")
        return null
    }

    Log.d(TAG, "✅ Todos os campos obrigatórios estão preenchidos")
    Log.d(TAG, "Tentando converter dataNascimento...")

    // Converter dataNascimento de diversos formatos para LocalDate
    val parsedDataNascimento = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (val rawDate = dataNascimento) {
            is String -> {
                // Se for String, tenta parsear como ISO_LOCAL_DATE
                Log.d(TAG, "→ dataNascimento é String: '$rawDate'")
                if (rawDate.isNotBlank()) {
                    runCatching {
                        LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE)
                    }.onSuccess {
                        Log.d(TAG, "✅ String parseada com sucesso: $it")
                    }.onFailure { ex ->
                        Log.e(TAG, "❌ Falha ao parsear String: ${ex.message}", ex)
                    }.getOrNull()
                } else {
                    Log.w(TAG, "⚠️ dataNascimento String está vazia")
                    null
                }
            }
            is Timestamp -> {
                // Se for Timestamp do Firebase, converte para LocalDate
                Log.d(TAG, "→ dataNascimento é Timestamp: $rawDate")
                try {
                    val localDate = rawDate.toDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    Log.d(TAG, "✅ Timestamp convertido com sucesso: $localDate")
                    localDate
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Falha ao converter Timestamp: ${e.message}", e)
                    null
                }
            }
            is Map<*, *> -> {
                // Se for HashMap (Timestamp serializado OU LocalDate serializado), extrai os campos
                Log.d(TAG, "→ dataNascimento é Map com ${rawDate.size} campos: $rawDate")
                try {
                    // Verifica se é um LocalDate serializado (tem 'year', 'monthValue', 'dayOfMonth')
                    if (rawDate.containsKey("year") && rawDate.containsKey("monthValue") && rawDate.containsKey("dayOfMonth")) {
                        val year = (rawDate["year"] as? Number)?.toInt()
                        val month = (rawDate["monthValue"] as? Number)?.toInt()
                        val day = (rawDate["dayOfMonth"] as? Number)?.toInt()

                        Log.d(TAG, "  É LocalDate serializado: year=$year, month=$month, day=$day")

                        if (year != null && month != null && day != null) {
                            val localDate = LocalDate.of(year, month, day)
                            Log.d(TAG, "✅ LocalDate reconstruído com sucesso: $localDate")
                            localDate
                        } else {
                            Log.e(TAG, "❌ Campos de LocalDate incompletos!")
                            null
                        }
                    }
                    // Senão, trata como Timestamp (tem 'seconds' e 'nanoseconds')
                    else {
                        val seconds = (rawDate["seconds"] as? Number)?.toLong()
                        val nanos = (rawDate["nanoseconds"] as? Number)?.toInt() ?: 0

                        Log.d(TAG, "  É Timestamp serializado: seconds=$seconds, nanos=$nanos")

                        if (seconds == null) {
                            Log.e(TAG, "❌ Campo 'seconds' não encontrado no Map!")
                            null
                        } else {
                            val localDate = Instant.ofEpochSecond(seconds, nanos.toLong())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Log.d(TAG, "✅ Map convertido com sucesso: $localDate")
                            localDate
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Falha ao converter Map: ${e.message}", e)
                    null
                }
            }
            null -> {
                Log.e(TAG, "❌ dataNascimento é NULL!")
                null
            }
            else -> {
                Log.e(TAG, "❌ dataNascimento é de tipo desconhecido: ${rawDate.javaClass.name}")
                null
            }
        }
    } else {
        Log.e(TAG, "❌ API level < 26 (atual: ${Build.VERSION.SDK_INT}), LocalDate não disponível")
        null
    }

    if (parsedDataNascimento == null) {
        Log.e(TAG, "❌ FALHA: dataNascimento não pôde ser convertido - RETORNANDO NULL")
        Log.d(TAG, "==== FIM toDomain() - FALHOU ====")
        return null
    }

    Log.d(TAG, "✅ dataNascimento convertido: $parsedDataNascimento")
    Log.d(TAG, "Criando objeto Funcionario...")

    val funcionario = Funcionario(
        id = id,
        nome = nome,
        email = email,
        cpf = cpf,
        status = status,
        dataNascimento = parsedDataNascimento,
        cargo = runCatching { Cargo.valueOf(cargo.uppercase()) }.getOrDefault(Cargo.DESENVOLVEDOR),
        endereco = Endereco(
            logradouro = endereco.logradouro,
            cidade = endereco.cidade,
            estado = endereco.estado,
            cep = endereco.cep
        )
    )

    Log.d(TAG, "✅ SUCESSO: Funcionario criado: nome=${funcionario.nome}, cargo=${funcionario.cargo}")
    Log.d(TAG, "==== FIM toDomain() - SUCESSO ====")
    return funcionario
}
