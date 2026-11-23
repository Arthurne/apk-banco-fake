package com.purplebank.simulator.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun Long.toLocalDateTime(): LocalDateTime = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

object Fmt {
    private val ptBR = Locale("pt", "BR")
    val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)

    /** 123456 -> "R$ 1.234,56" */
    fun money(cents: Long): String = NumberFormat.getCurrencyInstance(ptBR).format(BigDecimal.valueOf(cents, 2))

    /** Valor para preencher campos de edição: 123456 -> "1234,56" */
    fun moneyInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ',')

    /** Aceita "1.234,56", "1234,56", "12,5", "10.5", "1.234". Retorna centavos ou null se inválido. */
    fun parseMoney(raw: String, allowNegative: Boolean = false): Long? {
        var s = raw.trim().replace("R$", "").replace("\u00A0", "").replace(" ", "")
        if (s.isEmpty()) return null
        val neg = s.startsWith("-")
        if (neg) s = s.substring(1)
        if (neg && !allowNegative) return null
        s = when {
            s.contains(',') -> s.replace(".", "").replace(',', '.')
            s.count { it == '.' } > 1 -> s.replace(".", "")
            s.count { it == '.' } == 1 && s.substringAfter('.').length == 3 -> s.replace(".", "")
            else -> s
        }
        if (!Regex("""\d+(\.\d+)?""").matches(s)) return null
        val bd = BigDecimal(s)
        if (bd.stripTrailingZeros().scale() > 2) return null
        if (bd > BigDecimal("1000000000")) return null
        val cents = bd.movePointRight(2).toLong()
        return if (neg) -cents else cents
    }

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DATE)
    fun date(d: LocalDate): String = d.format(DATE)
    fun dateTime(millis: Long): String = millis.toLocalDateTime().let { it.format(DATE) + " " + it.format(TIME) }
    fun parseDate(s: String): LocalDate? = try { LocalDate.parse(s.trim(), DATE) } catch (e: DateTimeParseException) { null }
    fun parseTime(s: String): LocalTime? = try { LocalTime.parse(s.trim(), TIME) } catch (e: DateTimeParseException) { null }

    /** "2026-11" -> "nov/2026" */
    fun monthLabel(refMonth: String): String {
        val ym = YearMonth.parse(refMonth)
        val m = ym.month.getDisplayName(java.time.format.TextStyle.SHORT, ptBR).trimEnd('.')
        return "$m/${ym.year}"
    }

    fun typeLabel(type: String) = if (type == "ENTRADA") "Entrada" else "Saída"
}
