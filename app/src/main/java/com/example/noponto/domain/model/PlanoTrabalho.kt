package com.example.noponto.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Plano de Trabalho
 */
data class PlanoTrabalho(
    val id: String = "",
    val funcionarioRef: DocumentReference? = null,
    val funcionarioId: String = "",
    val presencial: List<HorarioDia> = emptyList(),
    val remoto: List<HorarioDia> = emptyList(),
    val ativo: Boolean = true,
    val criadoEm: Timestamp? = null
) {
    companion object {
        private val TIME_PATTERN = "HH:mm"

        /**
         * Converte "HH:mm" para minutos desde meia-noite. Retorna null se o parse falhar.
         */
        fun timeStringToMinutes(timeStr: String?): Int? {
            if (timeStr.isNullOrBlank()) return null
            return try {
                val sdf = SimpleDateFormat(TIME_PATTERN, Locale.getDefault())
                sdf.parse(timeStr)?.let { (it.time / (60 * 1000)).toInt() }
            } catch (e: ParseException) {
                null
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Converte minutos para string "HH:mm"
         */
        fun minutesToTimeString(minutes: Int): String {
            val h = minutes / 60
            val m = minutes % 60
            return String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }

        /**
         * Monta uma lista de HorarioDia a partir de um mapa de checks por DiaSemana e um par de horários (HH:mm).
         * - checkedMap: Map<DiaSemana, Boolean> indicando se o dia foi selecionado
         * - startStr / endStr: horários em "HH:mm"
         */
        fun buildHorariosFromUi(
            checkedMap: Map<DiaSemana, Boolean>,
            startStr: String,
            endStr: String
        ): List<HorarioDia> {
            val inicio = timeStringToMinutes(startStr) ?: return emptyList()
            val fim = timeStringToMinutes(endStr) ?: return emptyList()
            return checkedMap.entries
                .filter { it.value }
                .map { HorarioDia(dia = it.key, inicioMinutes = inicio, fimMinutes = fim) }
        }

        /**
         * Factory simples: cria um PlanoTrabalho a partir de dados da UI.
         * - presencialCheckedMap / remotoCheckedMap: Map<DiaSemana, Boolean>
         * - horarios de entrada/saída para presencial e remoto no formato "HH:mm"
         */
        fun createFromUi(
            funcionarioId: String = "",
            funcionarioRef: DocumentReference? = null,
            presencialCheckedMap: Map<DiaSemana, Boolean>,
            entradaPresencial: String,
            saidaPresencial: String,
            remotoCheckedMap: Map<DiaSemana, Boolean>,
            entradaRemoto: String,
            saidaRemoto: String,
            ativo: Boolean = true
        ): PlanoTrabalho {
            val presencialList = buildHorariosFromUi(presencialCheckedMap, entradaPresencial, saidaPresencial)
            val remotoList = buildHorariosFromUi(remotoCheckedMap, entradaRemoto, saidaRemoto)
            return PlanoTrabalho(
                id = "",
                funcionarioRef = funcionarioRef,
                funcionarioId = funcionarioId,
                presencial = presencialList,
                remoto = remotoList,
                ativo = ativo,
                criadoEm = Timestamp.now()
            )
        }
    }
}
