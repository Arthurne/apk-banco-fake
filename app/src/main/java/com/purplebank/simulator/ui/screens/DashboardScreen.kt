package com.purplebank.simulator.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.ui.*
import com.purplebank.simulator.ui.theme.BankColors
import com.purplebank.simulator.util.Billing
import com.purplebank.simulator.util.Fmt
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DashboardScreen(vm: MainViewModel, onTab: (String) -> Unit, onOpen: (String) -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val card by vm.card.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val p = profile ?: return
    val a = account ?: return
    val c = card ?: return
    val bank = settings?.bankName ?: "PurpleBank"
    val today = LocalDate.now()
    val current = Billing.current(invoices, today)
    val nextDue = Billing.nextDue(invoices, today)
    val hour = LocalTime.now().hour
    val greeting = when { hour < 12 -> "Bom dia"; hour < 18 -> "Boa tarde"; else -> "Boa noite" }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(bank, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(3.dp))
                    Text(greeting + ", " + p.name.substringBefore(' '), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.setHide(!hide) }) {
                    Icon(if (hide) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "Ocultar ou mostrar valores")
                }
                Avatar(p.avatar, p.name, 42.dp)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Minha conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(Modifier.fillMaxWidth().clickable { onTab("account") }, shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Box(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, BankColors.DarkPurple)))) {
                        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Saldo disponível", color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodyMedium)
                            }
                            MoneyText(a.balance, hide, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Color.White.copy(alpha = .22f), modifier = Modifier.padding(vertical = 5.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Poupança simulada", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium)
                                    MoneyText(a.savingsBalance, hide, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Acessos rápidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Shortcut(Icons.Filled.AccountBalanceWallet, "Conta") { onTab("account") }
                    Shortcut(Icons.Filled.CreditCard, "Cartão") { onTab("card") }
                    Shortcut(Icons.Filled.ReceiptLong, "Extrato") { onTab("history") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Cartão de crédito", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onTab("card") }) { Text("Acessar") }
                }
                Card(Modifier.fillMaxWidth().clickable { onTab("card") }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(bank, fontWeight = FontWeight.Bold)
                                Text("•••• •••• •••• " + c.cardLast4, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Fatura atual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(current?.amount ?: 0L, hide, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Limite disponível", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(c.availableLimit, hide, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text("Vencimento: " + (nextDue?.let { Fmt.date(it.dueDate) } ?: "—"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Últimas movimentações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onTab("history") }) { Text("Ver extrato") }
            }
        }
        if (txs.isEmpty()) item { EmptyState("Nenhuma movimentação ainda.") }
        else items(txs.take(5), key = { it.id }) { TransactionRow(it, hide) { onTab("history") } }
        item { Text("Ambiente demonstrativo • valores e operações fictícios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth()) }
    }
}
@Composable
private fun Shortcut(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(72.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}
