package com.fatsnakes.splitstupid

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.javamoney.moneta.Money
import java.lang.Exception
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount
import kotlin.random.Random

data class UserModel(
    val name: String,
    val deposit: MonetaryAmount,
    var balance: MonetaryAmount
) {
    fun isSatisfied() = balance.isZero
    fun isBorrower() = balance.isPositive
}

@Serializable
data class Participant(
    val name: String,
    val amount: Float
)

@Serializable
data class Transaction(
    val from: String,
    val to: String,
    val amount: MonetaryAmount
) {
    override fun toString(): String {
        return "$from -> $to: $amount"
    }
}

val rur: CurrencyUnit = Monetary.getCurrency("RUR")
val testData = mapOf(
    "Жека" to 5100.25f,
    "Макс" to 3550.32f,
    "Илюха" to 0.0f,
    "Олег" to 0.0f,
    "Данич" to 0.0f,
    "Стасян" to 0.0f,
    "Кабанич" to 1000.0f,
    "Поша" to 0.0f,
    "Кораллыч" to 500.0f
)

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        routing {
            post("/split") {
                val participants = call.receiveOrNull<List<Participant>>()
                participants?.let {
                    try {
                        call.respond(split(participants))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }.start(wait = true)
}

fun split(participants: List<Participant>): List<Transaction> {
    val transactions = mutableListOf<Transaction>()
    val total = Money.of(participants.map { it.amount }.reduce { acc, amount -> acc + amount }, rur)
    val divResult = total.divideAndRemainder(participants.size)
    val average = divResult[0]
    val remainder = divResult[1]

    val users = participants.map {
        val deposit = Money.of(it.amount, rur)
        val balance = average.subtract(deposit)
        UserModel(name = it.name, deposit = deposit, balance = balance)
    }

    randomizeRemainderDebt(users, remainder)

    println("------------- Users: ------------")
    users.forEach { println(it) }

    val start = System.currentTimeMillis()
    do {
        users.filter { !it.isSatisfied() }.forEach {
            transactions.addAll(satisfyUser(it, users, average))
        }
    } while (users.hasUnsatisfied())
    val time = System.currentTimeMillis() - start

    println("--------- Transactions: --------- (time: $time ms)")
    transactions.forEach { println(it) }

    return transactions
}

fun randomizeRemainderDebt(users: List<UserModel>, remainder: MonetaryAmount) {
    val randomUser = users[Random(System.currentTimeMillis()).nextInt(users.size)]
    randomUser.balance = randomUser.balance.add(remainder)
}

fun satisfyUser(user: UserModel, users: List<UserModel>, average: MonetaryAmount): List<Transaction> {
    val transactions = mutableListOf<Transaction>()
    while (!user.isSatisfied()) {
        var credit = if (user.balance.abs() >= average) average else user.balance.abs()
        var partner = users.firstOrNull { isSuitablePartner(user, it, credit) }
        if (partner == null) {
            partner = users.first { user.balance.isPositive != it.balance.isPositive }
            credit = partner.balance
        }
        if (user.isBorrower()) {
            user.balance = user.balance.subtract(credit)
            partner.balance = partner.balance.add(credit)
            transactions.add(Transaction(from = user.name, to = partner.name, amount = credit))
        } else {
            user.balance = user.balance.add(credit)
            partner.balance = partner.balance.subtract(credit)
            transactions.add(Transaction(from = partner.name, to = user.name, amount = credit))
        }
    }
    return transactions
}

fun isSuitablePartner(user: UserModel, partner: UserModel, credit: MonetaryAmount): Boolean {
    return user.name != partner.name && user.balance.isPositive != partner.balance.isPositive && partner.balance.abs() >= credit
}

fun List<UserModel>.hasUnsatisfied() = this.firstOrNull { !it.isSatisfied() } != null
