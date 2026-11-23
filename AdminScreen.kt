package com.purplebank.simulator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.data.Category
import com.purplebank.simulator.ui.*
import com.purplebank.simulator.ui.theme.BankColors
import com.purplebank.simulator.util.Fmt

@Composable
fun AdminScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    // Sai do painel => bloqueia novamente
    DisposableEffect(Unit) { onDispose { vm.lock() } }

    if (!vm.adminUnlocked) {
        PinGate(vm, onCancel = onBack)
        return
    }

    val profile by vm.profile.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val card by vm.card.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val log by vm.balanceLog.collectAsStateWithLifecycle()
    val p = profile ?: return
    val a = account ?: return
    val c = card ?: return
    val s = settings ?: return

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Painel do Simulador", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Tudo aqui é fictício e fica salvo apenas neste aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { ProfileSection(vm, p.name, p.avatar, s.bankName, s.primaryColor, s.theme) }
        item { AccountSection(vm, a.balance, a.savingsBalance) }
        item { CardSection(vm, c.holderName, c.cardLast4, c.totalLimit, c.closingDay, c.dueDay) }
        item {
            SectionCard("Faturas e movimentações") {
                Text("Compras, parcelas, datas e status são editados na aba Cartão; movimentações na aba Histórico.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ onBack(); onOpen("card") }) { Text("Gerenciar faturas") }
                    OutlinedButton({ onBack(); onOpen("history") }) { Text("Gerenciar histórico") }
                }
            }
        }
        item { CategorySection(vm, cats) }
        item {
            SectionCard("Histórico de alterações de saldo") {
                if (log.isEmpty()) Text("Sem registros.")
                log.take(20).forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(it.description, style = MaterialTheme.typography.bodyMedium)
                            Text(Fmt.dateTime(it.at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Fmt.money(it.balance), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        item { SectionCard("Dados") { DataActions(vm, admin = true) } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PinGate(vm: MainViewModel, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Painel do Simulador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Digite o PIN administrativo local.")
                PinField(pin, { pin = it; error = false }, "PIN")
                if (error) Text("PIN incorreto", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ vm.tryUnlock(pin) { ok -> if (!ok) { error = true; pin = "" } } }) { Text("Entrar") } },
        dismissButton = { TextButton(onCancel) { Text("Cancelar") } }
    )
}

@Composable
private fun ProfileSection(vm: MainViewModel, name0: String, avatar: String?, bank0: String, color0: String, theme0: String) {
    var name by remember(name0, bank0, color0, theme0) { mutableStateOf(name0) }
    var bank by remember(name0, bank0, color0, theme0) { mutableStateOf(bank0) }
    var color by remember(name0, bank0, color0, theme0) { mutableStateOf(color0) }
    var theme by remember(name0, bank0, color0, theme0) { mutableStateOf(theme0) }
    var showErrors by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.setAvatar(uri) }
    val presets = listOf("#820AD1", "#6200EA", "#0B7A75", "#D81B60", "#1565C0", "#EF6C00")
    val colorValid = runCatching { android.graphics.Color.parseColor(color) }.isSuccess

    SectionCard("Perfil e aparência") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatar, name0, 64.dp)
            Spacer(Modifier.width(12.dp))
            OutlinedButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Foto de perfil")
            }
        }
        AppField(name, { name = it }, "Nome fictício do usuário", if (showErrors && name.isBlank()) "Informe um nome" else null)
        AppField(bank, { bank = it }, "Nome do banco fictício", if (showErrors && bank.isBlank()) "Informe um nome" else null)
        Text("Cor principal", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { hex ->
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(BankColors.parse(hex))
                        .border(if (hex.equals(color, true)) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { color = hex }
                )
            }
        }
        AppField(color, { color = it.trim() }, "Cor em hexadecimal (#RRGGBB)", if (!colorValid) "Cor inválida" else null)
        Dropdown(
            "Tema", when (theme) { "LIGHT" -> "Claro"; "DARK" -> "Escuro"; else -> "Seguir o sistema" },
            listOf("SYSTEM", "LIGHT", "DARK"), { when (it) { "LIGHT" -> "Claro"; "DARK" -> "Escuro"; else -> "Seguir o sistema" } }, { theme = it }
        )
        SaveCancel(
            onSave = {
                showErrors = true
                if (name.isNotBlank() && bank.isNotBlank() && colorValid) vm.saveAppearance(name.trim(), bank.trim(), color, theme)
            },
            onCancel = { name = name0; bank = bank0; color = color0; theme = theme0; showErrors = false }
        )
    }
}

@Composable
private fun AccountSection(vm: MainViewModel, balance0: Long, savings0: Long) {
    var balance by remember(balance0, savings0) { mutableStateOf(Fmt.moneyInput(balance0)) }
    var savings by remember(balance0, savings0) { mutableStateOf(Fmt.moneyInput(savings0)) }
    var delta by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val b = Fmt.parseMoney(balance, allowNegative = true)
    val sv = Fmt.parseMoney(savings)
    val dv = Fmt.parseMoney(delta)

    SectionCard("Conta (saldos fictícios)") {
        MoneyField(balance, { balance = it }, "Saldo disponível", if (showErrors && b == null) "Valor inválido" else null)
        MoneyField(savings, { savings = it }, "Poupança simulada", if (showErrors && sv == null) "Valor inválido" else null)
        SaveCancel(
            onSave = { showErrors = true; if (b != null && sv != null) vm.saveBalances(b, sv) },
            onCancel = { balance = Fmt.moneyInput(balance0); savings = Fmt.moneyInput(savings0); showErrors = false }
        )
        HorizontalDivider()
        MoneyField(delta, { delta = it }, "Valor para adicionar ou remover", if (delta.isNotEmpty() && (dv == null || dv <= 0)) "Valor inválido" else null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ if (dv != null && dv > 0) { vm.adjustBalance(dv); delta = "" } }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Adicionar") }
            OutlinedButton({ if (dv != null && dv > 0) { vm.adjustBalance(-dv); delta = "" } }) { Icon(Icons.Filled.Remove, null); Spacer(Modifier.width(4.dp)); Text("Remover") }
        }
    }
}

@Composable
private fun CardSection(vm: MainViewModel, holder0: String, last40: String, limit0: Long, closing0: Int, due0: Int) {
    val key = listOf(holder0, last40, limit0, closing0, due0)
    var holder by remember(key) { mutableStateOf(holder0) }
    var last4 by remember(key) { mutableStateOf(last40) }
    var limit by remember(key) { mutableStateOf(Fmt.moneyInput(limit0)) }
    var closing by remember(key) { mutableStateOf(closing0.toString()) }
    var due by remember(key) { mutableStateOf(due0.toString()) }
    var showErrors by remember { mutableStateOf(false) }
    val l = Fmt.parseMoney(limit)
    val cd = closing.toIntOrNull()
    val dd = due.toIntOrNull()
    val last4Ok = last4.length == 4 && last4.all(Char::isDigit)

    SectionCard("Cartão fictício") {
        AppField(holder, { holder = it }, "Nome do titular (fictício)", if (showErrors && holder.isBlank()) "Informe um nome" else null)
        AppField(last4, { if (it.length <= 4) last4 = it }, "Final demonstrativo (4 dígitos)", if (showErrors && !last4Ok) "Use 4 dígitos" else null, KeyboardType.Number)
        MoneyField(limit, { limit = it }, "Limite total", if (showErrors && l == null) "Valor inválido" else null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppField(closing, { closing = it }, "Dia de fechamento", if (showErrors && (cd == null || cd !in 1..28)) "1 a 28" else null, KeyboardType.Number, Modifier.weight(1f))
            AppField(due, { due = it }, "Dia de vencimento", if (showErrors && (dd == null || dd !in 1..28)) "1 a 28" else null, KeyboardType.Number, Modifier.weight(1f))
        }
        Text("O limite disponível e o utilizado são calculados automaticamente a partir das faturas não pagas. Novas datas valem para faturas criadas daqui em diante.", style = MaterialTheme.typography.bodySmall)
        SaveCancel(
            onSave = {
                showErrors = true
                if (holder.isNotBlank() && last4Ok && l != null && cd != null && cd in 1..28 && dd != null && dd in 1..28)
                    vm.saveCard(holder.trim().uppercase(), last4, l, cd, dd)
            },
            onCancel = {
                holder = holder0; last4 = last40; limit = Fmt.moneyInput(limit0); closing = closing0.toString(); due = due0.toString(); showErrors = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(vm: MainViewModel, cats: List<Category>) {
    var newCat by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Category?>(null) }
    SectionCard("Categorias") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEach { c ->
                InputChip(selected = false, onClick = { deleting = c }, label = { Text(c.name) }, trailingIcon = { Text("✕") })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppField(newCat, { newCat = it }, "Nova categoria", modifier = Modifier.weight(1f))
            Button({
                val n = newCat.trim()
                if (n.isNotEmpty() && cats.none { it.name.equals(n, true) }) { vm.addCategory(n); newCat = "" }
            }) { Text("Adicionar") }
        }
    }
    deleting?.let { c ->
        ConfirmDialog("Excluir categoria?", "\"${c.name}\" será removida da lista (movimentações existentes mantêm o nome).", "Excluir", { vm.deleteCategory(c) }, { deleting = null })
    }
}

@Composable
private fun SaveCancel(onSave: () -> Unit, onCancel: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onSave) { Text("Salvar alterações") }
        OutlinedButton(onCancel) { Text("Cancelar") }
    }
}
