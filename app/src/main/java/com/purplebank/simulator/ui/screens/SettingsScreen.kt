package com.purplebank.simulator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.ui.*

@Composable
fun SettingsScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val s = settings ?: return
    var showPin by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Configurações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "AMBIENTE DE DEMONSTRAÇÃO — este aplicativo é um simulador independente. Nenhum dado é real, " +
                        "não há conexão com bancos e nada aqui representa operações financeiras verdadeiras.",
                    Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            SectionCard("Aparência") {
                Dropdown(
                    "Tema", when (s.theme) { "LIGHT" -> "Claro"; "DARK" -> "Escuro"; else -> "Seguir o sistema" },
                    listOf("SYSTEM", "LIGHT", "DARK"),
                    { when (it) { "LIGHT" -> "Claro"; "DARK" -> "Escuro"; else -> "Seguir o sistema" } },
                    { vm.setTheme(it) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ocultar valores na tela", Modifier.weight(1f))
                    Switch(hide, { vm.setHide(it) })
                }
                InfoRow("Moeda", "Real brasileiro (BRL)")
            }
        }
        item {
            SectionCard("Painel do Simulador") {
                Text("Edite saldo, cartão, faturas, perfil e categorias. Protegido por PIN local.", style = MaterialTheme.typography.bodySmall)
                Button({ onOpen("admin") }) { Icon(Icons.Filled.AdminPanelSettings, null); Spacer(Modifier.width(6.dp)); Text("Abrir painel") }
                OutlinedButton({ showPin = true }) { Text("Alterar PIN administrativo") }
            }
        }
        item { SectionCard("Dados locais e backup") { DataActions(vm, admin = false) } }
        item {
            SectionCard("Sobre") {
                InfoRow("Aplicativo", "PurpleBank Simulator")
                InfoRow("Versão", "1.0")
                Text(
                    "Funciona 100% offline. Os dados ficam somente neste aparelho (Room/SQLite). " +
                        "Marca, nome e identidade visual são originais; não há relação com nenhuma instituição financeira.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showPin) ChangePinDialog(vm) { showPin = false }
}

/** Exportar / importar (e, no painel, restaurar e limpar). Reutilizado em Configurações e no Painel. */
@Composable
fun DataActions(vm: MainViewModel, admin: Boolean) {
    var confirmImport by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ exportLauncher.launch("purplebank-dados-ficticios.json") }) { Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(6.dp)); Text("Exportar") }
        OutlinedButton({ confirmImport = true }) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text("Importar") }
    }
    if (admin) {
        OutlinedButton({ confirmReset = true }) { Text("Restaurar configuração (dados de exemplo)") }
        OutlinedButton({ confirmClear = true }) { Text("Limpar banco de dados", color = MaterialTheme.colorScheme.error) }
    }

    if (confirmImport) ConfirmDialog("Importar dados fictícios?", "Todos os dados atuais serão substituídos pelo conteúdo do arquivo escolhido.", "Escolher arquivo",
        { importLauncher.launch(arrayOf("*/*")) }, { confirmImport = false })
    if (confirmReset) ConfirmDialog("Restaurar configuração?", "Apaga tudo e recria os dados de exemplo.", "Restaurar", { vm.resetToDefaults() }, { confirmReset = false })
    if (confirmClear) ConfirmDialog("Limpar banco de dados?", "Apaga TODAS as movimentações, faturas e compras e zera os saldos. Não pode ser desfeito.", "Limpar", { vm.clearAll() }, { confirmClear = false })
}

@Composable
private fun ChangePinDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var cur by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var conf by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(cur, { cur = it }, "PIN atual")
                PinField(new, { new = it }, "Novo PIN (4 a 8 dígitos)")
                PinField(conf, { conf = it }, "Confirmar novo PIN")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton({
                when {
                    new.length !in 4..8 -> error = "O novo PIN deve ter de 4 a 8 dígitos"
                    new != conf -> error = "A confirmação não confere"
                    else -> vm.changePin(cur, new) { ok -> if (ok) onDismiss() else error = "PIN atual incorreto" }
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value, { if (it.length <= 8 && it.all(Char::isDigit)) onChange(it) }, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth()
    )
}
