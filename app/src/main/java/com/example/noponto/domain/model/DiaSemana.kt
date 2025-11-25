package com.example.noponto.domain.model

import java.util.Locale

enum class DiaSemana {
    SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO, DOMINGO;

    companion object {
        fun fromIndex(index: Int): DiaSemana = when (index) {
            2 -> SEGUNDA
            3 -> TERCA
            4 -> QUARTA
            5 -> QUINTA
            6 -> SEXTA
            7 -> SABADO
            1 -> DOMINGO
            else -> SEGUNDA
        }

        // parse a partir do nome exibido (ex.: "Segunda-feira" ou "Segunda")
        fun fromString(name: String): DiaSemana {
            val s = name.trim().lowercase(Locale.getDefault())
            return when {
                s.startsWith("seg") -> SEGUNDA
                s.startsWith("ter") -> TERCA
                s.startsWith("qua") -> QUARTA
                s.startsWith("qui") -> QUINTA
                s.startsWith("sex") -> SEXTA
                s.startsWith("sab") -> SABADO
                s.startsWith("dom") -> DOMINGO
                else -> SEGUNDA
            }
        }
    }
}

