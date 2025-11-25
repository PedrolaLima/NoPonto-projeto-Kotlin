package com.example.noponto.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Modelo para Ocorrência (Cadastro de ocorrência a partir da tela activity_occurrence.xml).
 * Observações:
 * - Não armazenamos a imagem diretamente no documento (isso deixaria o banco pesado).
 *   Em vez disso usamos `atestadoStoragePath` que pode ser um caminho/URL no Firebase Storage (opcional).
 * - `dataHora` é um `Timestamp` do Firestore para facilitar consultas/ordenações.
 */
data class Ocorrencia(
    val id: String = "",
    val funcionarioRef: DocumentReference? = null,
    val funcionarioId: String = "",
    val funcionarioNome: String = "",
    val justificativa: String = "",
    val dataHora: Timestamp? = null,
    val hasAtestado: Boolean = false,
    val atestadoStoragePath: String? = null,
    val status: StatusOcorrencia = StatusOcorrencia.PENDENTE,
    val criadoEm: Timestamp? = null,
    val pontoId: String? = null  // ID do ponto relacionado (se houver)
)
{
    enum class StatusOcorrencia {
        PENDENTE,
        APROVADO,
        REJEITADO
    }

    companion object {
        private const val DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm"

        /**
         * Parse data (dd/MM/yyyy) + hora (HH:mm) em Timestamp do Firestore.
         * Retorna null se o parse falhar.
         */
        fun parseDateAndTimeToTimestamp(dateStr: String, timeStr: String, locale: Locale = Locale.getDefault()): Timestamp? {
            return try {
                val sdf = SimpleDateFormat(DATE_TIME_PATTERN, locale)
                sdf.timeZone = TimeZone.getDefault()
                val combined = "$dateStr $timeStr"
                val parsed: Date? = sdf.parse(combined)
                parsed?.let {
                    val ms = it.time
                    val seconds = ms / 1000
                    val nanos = ((ms % 1000) * 1_000_000).toInt()
                    Timestamp(seconds, nanos)
                }
            } catch (e: ParseException) {
                null
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Converte Timestamp para epoch millis
         */
        fun timestampToEpochMillis(ts: Timestamp?): Long = ts?.let { it.seconds * 1000 + it.nanoseconds / 1_000_000 } ?: 0L

        /**
         * Factory para criar a Ocorrencia a partir da UI (dateStr: "dd/MM/yyyy", timeStr: "HH:mm").
         * - Se o parse falhar, usa Timestamp.now() como fallback para `dataHora`.
         * - `atestadoStoragePath` deve ser o caminho/URL no Storage (se houver). Não armazene a imagem direta.
         */
        fun createFromUi(
            funcionarioId: String,
            funcionarioRef: DocumentReference? = null,
            funcionarioNome: String = "",
            justificativa: String,
            dateStr: String,
            timeStr: String,
            hasAtestado: Boolean = false,
            atestadoStoragePath: String? = null,
            status: StatusOcorrencia = StatusOcorrencia.PENDENTE,
            pontoId: String? = null
        ): Ocorrencia {
            val ts = parseDateAndTimeToTimestamp(dateStr, timeStr) ?: Timestamp.now()
            return Ocorrencia(
                id = "",
                funcionarioRef = funcionarioRef,
                funcionarioId = funcionarioId,
                funcionarioNome = funcionarioNome,
                justificativa = justificativa,
                dataHora = ts,
                hasAtestado = hasAtestado,
                atestadoStoragePath = atestadoStoragePath,
                status = status,
                criadoEm = Timestamp.now(),
                pontoId = pontoId
            )
        }
    }
}