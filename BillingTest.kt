package com.purplebank.simulator

import com.purplebank.simulator.data.Invoice
import com.purplebank.simulator.util.Billing
import com.purplebank.simulator.util.Fmt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BillingTest {
    @Test fun installmentsKeepEveryCent() {
        val parts = Billing.splitInstallments(10000, 3)
        assertEquals(listOf(3334L, 3333L, 3333L), parts)
        assertEquals(10000L, parts.sum())
    }

    @Test fun purchaseGoesToCorrectInvoice() {
        assertEquals(YearMonth.of(2026, 11), Billing.refMonthFor(LocalDate.of(2026, 11, 20), 20))
        assertEquals(YearMonth.of(2026, 12), Billing.refMonthFor(LocalDate.of(2026, 11, 21), 20))
        assertEquals(YearMonth.of(2027, 1), Billing.refMonthFor(LocalDate.of(2026, 12, 28), 20))
    }

    @Test fun dueDates() {
        assertEquals(LocalDate.of(2026, 11, 27), Billing.dueDate(YearMonth.of(2026, 11), 20, 27))
        assertEquals(LocalDate.of(2026, 12, 5), Billing.dueDate(YearMonth.of(2026, 11), 25, 5))
    }

    @Test fun usedLimitIgnoresPaidInvoices() {
        val a = Invoice(id = 1, refMonth = "2026-10", amount = 1000, status = Invoice.PAID, dueDate = 1, closingDate = 1)
        val b = Invoice(id = 2, refMonth = "2026-11", amount = 2500, status = Invoice.OPEN, dueDate = 2, closingDate = 2)
        val c = Invoice(id = 3, refMonth = "2026-12", amount = 700, status = Invoice.CLOSED, dueDate = 3, closingDate = 3)
        assertEquals(3200L, Billing.usedLimit(listOf(a, b, c)))
    }

    @Test fun parseMoney() {
        assertEquals(123456L, Fmt.parseMoney("1.234,56"))
        assertEquals(1250L, Fmt.parseMoney("12,5"))
        assertEquals(1050L, Fmt.parseMoney("10.5"))
        assertEquals(123400L, Fmt.parseMoney("1.234"))
        assertEquals(0L, Fmt.parseMoney("0"))
        assertNull(Fmt.parseMoney("abc"))
        assertNull(Fmt.parseMoney("1e5"))
        assertNull(Fmt.parseMoney("-5"))
        assertEquals(-500L, Fmt.parseMoney("-5", allowNegative = true))
        assertNull(Fmt.parseMoney("1,234"))
    }

    @Test fun dates() {
        assertEquals(LocalDate.of(2026, 2, 28), Fmt.parseDate("28/02/2026"))
        assertNull(Fmt.parseDate("31/02/2026"))
        assertEquals("1234,56", Fmt.moneyInput(123456))
    }
}
