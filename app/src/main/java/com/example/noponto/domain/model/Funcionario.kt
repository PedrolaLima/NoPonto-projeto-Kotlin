package com.example.noponto.domain.model

import java.time.LocalDate

data class Funcionario(
    val id: String,
    val nome: String,
    val email: String,
    val cpf: String,
    val status: Boolean,
    val dataNascimento: LocalDate,
    val cargo: Cargo = Cargo.DESENVOLVEDOR,
    val endereco: Endereco
)