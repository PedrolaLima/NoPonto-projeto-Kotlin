package com.example.noponto.domain.model

/**
 * Representa o horário de trabalho para um dia da semana.
 * - inicioMinutes/fimMinutes são inteiros em minutos desde 00:00 (fáceis de comparar)
 */
data class HorarioDia(
    val dia: DiaSemana = DiaSemana.SEGUNDA,
    val inicioMinutes: Int = 9 * 60, // default 09:00
    val fimMinutes: Int = 18 * 60 // default 18:00
)

