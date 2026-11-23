package com.purplebank.simulator.util

import com.purplebank.simulator.data.Invoice
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

/** Regras de fatura (funções puras, cobertas por testes unitários). */
object Billing {
    /** Compra até o dia de fechamento (inclusive) cai na fatura do mês; depois, na do mês seguinte. */
    fun refMonthFor(date: LocalDate, closingDay: Int): YearMonth {
        val ym = YearMonth.from(date)
        return if (date.dayOfMonth <= closingDay) ym else ym.plusMonths(1)
    }

    fun closingDate(ym: YearMonth, closingDay: Int): LocalDate =
        ym.atDay(min(closingDay, ym.lengthOfMonth()))

    /** Se o dia de vencimento é maior que o de fechamento, vence no mesmo mês; senão, no seguinte. */
    fun dueDate(ym: YearMonth, closingDay: Int, dueDay: Int): LocalDate {
        val base = if (dueDay > closingDay) ym else ym.plusMonths(1)
        return base.atDay(min(dueDay, base.lengthOfMonth()))
    }

    /** Divide em parcelas sem perder centavos: a primeira parcela absorve o resto. */
    fun splitInstallments(total: Long, n: Int): List<Long> {
        require(n >= 1)
        val base = total / n
        val rest = total - base * n
        return List(n) { if (it == 0) base + rest else base }
    }

    private fun unpaidSorted(list: List<Invoice>) =
        list.filter { it.status != Invoice.PAID }.sortedBy { it.closingDate }

    /** Fatura "atual": a primeira não paga cujo fechamento ainda não passou. */
    fun current(list: List<Invoice>, today: LocalDate): Invoice? =
        unpaidSorted(list).firstOrNull { it.closingDate >= today.toEpochDay() }

    fun next(list: List<Invoice>, today: LocalDate): Invoice? =
        unpaidSorted(list).filter { it.closingDate >= today.toEpochDay() }.getOrNull(1)

    fun lastClosed(list: List<Invoice>): Invoice? =
        list.filter { it.status == Invoice.CLOSED }.maxByOrNull { it.closingDate }

    fun nextDue(list: List<Invoice>, today: LocalDate): Invoice? =
        list.filter { it.status != Invoice.PAID && it.amount > 0 && it.dueDate >= today.toEpochDay() }
            .minByOrNull { it.dueDate }

    fun tag(inv: Invoice, current: Invoice?, today: LocalDate): String = when {
        inv.status == Invoice.PAID -> "Paga"
        inv.id == current?.id -> "Atual"
        inv.closingDate >= today.toEpochDay() && inv.status == Invoice.OPEN -> "Futura"
        else -> "Fechada"
    }

    /** Limite usado = soma das faturas ainda não pagas. */
    fun usedLimit(list: List<Invoice>): Long = list.filter { it.status != Invoice.PAID }.sumOf { it.amount }
}
