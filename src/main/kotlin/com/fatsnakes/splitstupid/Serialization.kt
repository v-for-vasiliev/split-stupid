package com.fatsnakes.splitstupid

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.javamoney.moneta.Money
import javax.money.MonetaryAmount

object MoneySerializer : KSerializer<MonetaryAmount> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Money", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MonetaryAmount) {
        encoder.encodeString("${value.number.doubleValueExact()};${value.currency.currencyCode}")
    }

    override fun deserialize(decoder: Decoder): MonetaryAmount {
        val data = decoder.decodeString().split(";")
        return Money.of(data[0].toDouble(), data[1])
    }
}