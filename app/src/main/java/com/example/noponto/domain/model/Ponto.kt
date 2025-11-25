package com.example.noponto.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class StatusAprovacao {
    APROVADO,
    AGUARDANDO,
    REPROVADO
}

/**
 * Modelo de ponto compatível com Firestore (document DB).
 * - `funcionarioId` referencia o funcionário logado (obrigatório para vincular o ponto)
 * - usamos `dataHoraPonto: Timestamp?` para facilitar leitura/escrita direta no Firestore
 * - valores default permitem que o Firestore crie a instância ao desserializar
 */
data class Ponto(
    val id: String = "",
    val funcionarioRef: DocumentReference? = null,
    val funcionarioId: String = "",
    val funcionarioNome: String = "",
    val funcionarioCargo: String = "",
    val homeOffice: Boolean = false,
    val dataHoraPonto: Timestamp? = null,
    val tipoDePonto: TipoDePonto = TipoDePonto.ENTRADA,
    val observacao: String = "",
    val statusAprovacao: StatusAprovacao = StatusAprovacao.AGUARDANDO
)
{
    companion object {
        private val DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm"

        /**
         * Tenta parsear as strings de data e hora (ex.: "20/11/2025", "12:30") e retorna um [Timestamp]
         * Caso o parse falhe retorna null.
         * Usa SimpleDateFormat para compatibilidade com versões Android sem java.time.
         */
        fun parseDateAndTimeToTimestamp(dateStr: String, timeStr: String, locale: Locale = Locale.getDefault()): Timestamp? {
            return try {
                val sdf = SimpleDateFormat(DATE_TIME_PATTERN, locale)
                // define timezone do dispositivo (pode ser ajustado para UTC se preferir)
                sdf.timeZone = TimeZone.getDefault()
                val combined = "$dateStr $timeStr"
                val parsed: Date? = sdf.parse(combined)
                parsed?.let { date ->
                    val ms = date.time
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
         * Converte um [Timestamp] do Firestore em epoch millis (Long)
         */
        fun timestampToEpochMillis(ts: Timestamp?): Long {
            return ts?.let { it.seconds * 1000 + (it.nanoseconds / 1_000_000) } ?: 0L
        }

        /**
         * Cria um Ponto mínimo atrelado ao funcionário logado, recebendo as strings da UI.
         * Se o parse falhar, usa Timestamp.now() como fallback.
         */
        fun createForLoggedFuncionario(
            funcionarioId: String,
            funcionarioNome: String = "",
            funcionarioCargo: String = "",
            dateStr: String,
            timeStr: String,
            tipo: TipoDePonto = TipoDePonto.ENTRADA,
            homeOffice: Boolean = false,
            funcionarioRef: DocumentReference,
            observacao: String = ""
        ): Ponto {
            val ts = parseDateAndTimeToTimestamp(dateStr, timeStr) ?: Timestamp.now()
            return Ponto(
                id = "",
                funcionarioRef = funcionarioRef,
                funcionarioId = funcionarioId,
                funcionarioNome = funcionarioNome,
                funcionarioCargo = funcionarioCargo,
                homeOffice = homeOffice,
                dataHoraPonto = ts,
                tipoDePonto = tipo,
                observacao = observacao,
                statusAprovacao = StatusAprovacao.AGUARDANDO
            )
        }
    }
}
