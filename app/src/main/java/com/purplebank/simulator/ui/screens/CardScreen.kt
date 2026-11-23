package com.purplebank.simulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.data.Invoice
import com.purplebank.simulator.data.Purchase
import com.purplebank.simulator.ui.*
import com.purplebank.simulator.ui.theme.BankColors
import com.purplebank.simulator.util.Billing
import com.purplebank.simulator.util.Fmt
import java.time.LocalDate

@Composable
fun CardScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val card by vm.card.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val c = card ?: return
    var showPurchase by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val current = Billing.current(invoices, today)
    val next = Billing.next(invoices, today)
    val closed = Billing.lastClosed(invoices)
    val bank = settings?.bankName ?: "PurpleBank"
    val usedFraction = if (c.totalLimit > 0) (c.usedLimit.toFloat() / c.totalLimit).coerceIn(0f, 1f) else 0f

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Cartão", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Box(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, BankColors.DarkPurple)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(bank, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("SIMULAÇÃO", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                    }
                    Text("•••• •••• •••• ${c.cardLast4}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Titular (fictício)", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text(c.holderName, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vencimento", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text("Dia ${c.dueDay}", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Limite") {
                LinearProgressIndicator(progress = { usedFraction }, Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total"); MoneyText(c.totalLimit, hide, fontWeight = FontWeight.Medium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Disponível"); MoneyText(c.availableLimit, hide, color = BankColors.In, fontWeight = FontWeight.Medium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Utilizado"); MoneyText(c.usedLimit, hide, color = BankColors.Out, fontWeight = FontWeight.Medium) }
            }
        }
        item {
            SectionCard("Faturas e datas") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Fatura atual"); MoneyText(current?.amount ?: 0L, hide, fontWeight = FontWeight.Medium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Fatura fechada"); MoneyText(closed?.amount ?: 0L, hide, fontWeight = FontWeight.Medium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Próxima fatura"); MoneyText(next?.amount ?: 0L, hide, fontWeight = FontWeight.Medium) }
                InfoRow("Fechamento", "Dia ${c.closingDay}")
                InfoRow("Vencimento", "Dia ${c.dueDay}")
                InfoRow("Melhor dia de compra", "Dia ${if (c.closingDay >= 31) 1 else c.closingDay + 1}")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button({ showPurchase = true }, Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Nova compra") }
                OutlinedButton({ onOpen("admin") }, Modifier.weight(1f)) { Icon(Icons.Filled.AdminPanelSettings, null); Spacer(Modifier.width(6.dp)); Text("Editar cartão") }
            }
        }
        item { Text("Faturas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (invoices.isEmpty()) item { EmptyState("Nenhuma fatura. Adicione uma compra fictícia.") }
        else items(invoices.sortedByDescending { it.closingDate }, key = { it.id }) { inv ->
            Card(Modifier.fillMaxWidth().clickable { onOpen("invoice/${inv.id}") }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Fatura ${Fmt.monthLabel(inv.refMonth)}", fontWeight = FontWeight.Bold)
                        Text("Vence em ${Fmt.date(inv.dueDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(inv.amount, hide, fontWeight = FontWeight.SemiBold)
                        AssistChip(onClick = { onOpen("invoice/${inv.id}") }, label = { Text("${Billing.tag(inv, current, today)} • ${inv.status.lowercase()}") })
                    }
                }
            }
        }
    }

    if (showPurchase) PurchaseDialog(null, { showPurchase = false }) { d, a, dt, n -> vm.addPurchase(d, a, dt, n); showPurchase = false }
}

@Composable
fun InvoiceScreen(vm: MainViewModel, invoiceId: Long, onBack: () -> Unit) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val purchases by remember(invoiceId) { vm.purchases(invoiceId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val inv = invoices.firstOrNull { it.id == invoiceId }
    if (inv == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val today = LocalDate.now()
    val current = Billing.current(invoices, today)

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Purchase?>(null) }
    var deleting by remember { mutableStateOf<Purchase?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var delInvoice by remember { mutableStateOf(false) }
    var payInvoice by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                    Text("Fatura ${Fmt.monthLabel(inv.refMonth)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            item {
                SectionCard("${Billing.tag(inv, current, today)} • ${inv.status.lowercase()}") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Valor total"); MoneyText(inv.amount, hide, fontWeight = FontWeight.Bold) }
                    InfoRow("Fechamento", Fmt.date(inv.closingDate))
                    InfoRow("Vencimento", Fmt.date(inv.dueDate))
                    if (inv.adjustment != 0L) InfoRow("Ajuste manual", Fmt.money(inv.adjustment))
                    Text("Status (simulado)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Invoice.ALL.forEach { s ->
                            FilterChip(inv.status == s, { vm.setInvoiceStatus(inv.id, s) }, label = { Text(s.lowercase()) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (inv.status != Invoice.PAID) Button({ payInvoice = true }) { Text("Pagar com saldo") }
                        OutlinedButton({ showEdit = true }) { Text("Editar") }
                        TextButton({ delInvoice = true }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item { Text("Compras fictícias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (purchases.isEmpty()) item { EmptyState("Nenhuma compra nesta fatura.") }
            else items(purchases, key = { it.id }) { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.description + if (p.installmentTotal > 1) " (${p.installmentNumber}/${p.installmentTotal})" else "", fontWeight = FontWeight.Medium)
                        Text("Compra em ${Fmt.date(p.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MoneyText(p.amount, hide, fontWeight = FontWeight.SemiBold)
                    IconButton({ editing = p }) { Icon(Icons.Filled.Edit, contentDescription = "Editar compra") }
                    IconButton({ deleting = p }) { Icon(Icons.Filled.Delete, contentDescription = "Excluir compra") }
                }
            }
        }
        FloatingActionButton({ showAdd = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Filled.Add, contentDescription = "Adicionar compra") }
    }

    if (showAdd) PurchaseDialog(null, { showAdd = false }) { d, a, dt, n -> vm.addPurchase(d, a, dt, n); showAdd = false }
    editing?.let { p -> PurchaseDialog(p, { editing = null }) { d, a, dt, _ -> vm.updatePurchase(p, d, a, dt); editing = null } }
    deleting?.let { p -> ConfirmDialog("Excluir compra?", "\"${p.description}\" será removida da fatura.", "Excluir", { vm.deletePurchase(p) }, { deleting = null }) }
    if (delInvoice) ConfirmDialog("Excluir fatura?", "A fatura e todas as suas compras serão removidas.", "Excluir", { vm.deleteInvoice(inv.id) }, { delInvoice = false })
    if (payInvoice) ConfirmDialog("Pagar fatura (simulado)?", "Será debitado ${Fmt.money(inv.amount)} do saldo fictício e registrada uma movimentação simulada.", "Pagar", { vm.payInvoice(inv.id) }, { payInvoice = false })
    if (showEdit) InvoiceEditDialog(inv, { showEdit = false }) { st, due, close, amount -> vm.updateInvoice(inv.id, st, due, close, amount); showEdit = false }
}

@Composable
fun PurchaseDialog(initial: Purchase?, onDismiss: () -> Unit, onSave: (String, Long, LocalDate, Int) -> Unit) {
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.let { Fmt.moneyInput(it.amount) } ?: "") }
    var date by remember { mutableStateOf(Fmt.date(initial?.let { LocalDate.ofEpochDay(it.date) } ?: LocalDate.now())) }
    var inst by remember { mutableStateOf("1") }
    var showErrors by remember { mutableStateOf(false) }
    val cents = Fmt.parseMoney(amount)
    val d = Fmt.parseDate(date)
    val n = inst.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova compra fictícia" else "Editar compra") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(desc, { desc = it }, "Descrição", if (showErrors && desc.isBlank()) "Informe uma descrição" else null)
                MoneyField(amount, { amount = it }, if (initial == null) "Valor total" else "Valor da parcela", if (showErrors && (cents == null || cents <= 0)) "Valor inválido" else null)
                AppField(date, { date = it }, "Data da compra", if (showErrors && d == null) "dd/mm/aaaa" else null, KeyboardType.Number)
                if (initial == null) AppField(inst, { inst = it }, "Parcelas (1 a 24)", if (showErrors && (n == null || n !in 1..24)) "Use de 1 a 24" else null, KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton({
                showErrors = true
                if (desc.isNotBlank() && cents != null && cents > 0 && d != null && (initial != null || (n != null && n in 1..24))) {
                    onSave(desc.trim(), cents, d, n ?: 1)
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun InvoiceEditDialog(inv: Invoice, onDismiss: () -> Unit, onSave: (String, LocalDate, LocalDate, Long) -> Unit) {
    var status by remember { mutableStateOf(inv.status) }
    var amount by remember { mutableStateOf(Fmt.moneyInput(inv.amount)) }
    var due by remember { mutableStateOf(Fmt.date(inv.dueDate)) }
    var closing by remember { mutableStateOf(Fmt.date(inv.closingDate)) }
    var showErrors by remember { mutableStateOf(false) }
    val cents = Fmt.parseMoney(amount)
    val dd = Fmt.parseDate(due)
    val cd = Fmt.parseDate(closing)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar fatura") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("Status", status, Invoice.ALL, { it.lowercase() }, { status = it })
                MoneyField(amount, { amount = it }, "Valor total", if (showErrors && cents == null) "Valor inválido" else null)
                Text("A diferença para a soma das compras vira um ajuste manual.", style = MaterialTheme.typography.bodySmall)
                AppField(closing, { closing = it }, "Data de fechamento", if (showErrors && cd == null) "dd/mm/aaaa" else null, KeyboardType.Number)
                AppField(due, { due = it }, "Data de vencimento", if (showErrors && dd == null) "dd/mm/aaaa" else null, KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton({
                showErrors = true
                if (cents != null && dd != null && cd != null) onSave(status, dd, cd, cents)
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}
