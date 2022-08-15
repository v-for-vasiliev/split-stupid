package com.fatsnakes.splitstupid

import org.javamoney.moneta.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount
import kotlin.random.Random

data class User(
    val name: String,
    val deposit: MonetaryAmount,
    var balance: MonetaryAmount
) {
    fun isSatisfied() = balance.isZero
    fun isCreditor() = balance.isNegative
    fun isBorrower() = balance.isPositive
}

val rur: CurrencyUnit = Monetary.getCurrency("RUR")
val searchDelta: MonetaryAmount = Money.of(0.01, rur)

val transactions = mutableListOf<String>()

fun main() {
    val data = mapOf(
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

    val total = Money.of(data.values.reduce { acc, p -> acc + p }, rur)
    val divResult = total.divideAndRemainder(data.size)
    val average = divResult[0]
    val remainder = divResult[1]

    val users = data.map {
        val deposit = Money.of(it.value, rur)
        val balance = average.subtract(deposit)
        User(name = it.key, deposit = deposit, balance = balance)
    }

    randomizeRemainderDebt(users, remainder)

    println("--------- Users: ---------")
    users.forEach { println(it) }

    val start = System.currentTimeMillis()
    do {
        users.filter { !it.isSatisfied() }.forEach { satisfyUser(it, users, average) }
    } while (users.hasUnsatisfied())
    val time = System.currentTimeMillis() - start

    println("--------- Transactions: --------- (time: $time ms)")
    transactions.forEach { println(it) }
}

fun randomizeRemainderDebt(users: List<User>, remainder: MonetaryAmount) {
    val randomUser = users[Random(System.currentTimeMillis()).nextInt(users.size)]
    randomUser.balance = randomUser.balance.add(remainder)
}

fun satisfyUser(user: User, users: List<User>, average: MonetaryAmount) {
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
            transactions.add("${user.name} -> ${partner.name}: $credit")
        } else {
            user.balance = user.balance.add(credit)
            partner.balance = partner.balance.subtract(credit)
            transactions.add("${partner.name} -> ${user.name}: $credit")
        }
    }
}

fun isSuitablePartner(user: User, partner: User, credit: MonetaryAmount): Boolean {
    return user.name != partner.name && user.balance.isPositive != partner.balance.isPositive && partner.balance.abs() >= credit
}

fun List<User>.hasUnsatisfied() = this.firstOrNull { !it.isSatisfied() } != null
