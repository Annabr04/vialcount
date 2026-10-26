package dev.annabrackat.vialcount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.annabrackat.vialcount.CountLine
import dev.annabrackat.vialcount.CycleCount
import dev.annabrackat.vialcount.DomainResult
import dev.annabrackat.vialcount.Role
import dev.annabrackat.vialcount.SeedData
import dev.annabrackat.vialcount.Status
import dev.annabrackat.vialcount.VarianceReason

class CountViewModel : ViewModel() {
    var role by mutableStateOf(Role.TECH)
        private set
    var lines by mutableStateOf(SeedData.lines())
        private set
    var selectedSku by mutableStateOf(SeedData.lines().first().sku)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun toggleRole() {
        role = if (role == Role.TECH) Role.LEAD else Role.TECH
        notice = null
    }

    fun select(sku: String) {
        selectedSku = sku
        notice = null
    }

    fun submit(sku: String, qty: Int) {
        val line = lines.find { it.sku == sku } ?: return
        val result = if (line.status == Status.OPEN) {
            CycleCount.submitFirstCount(line, qty)
        } else {
            CycleCount.submitSecondCount(line, qty, role)
        }
        apply(sku, result)
    }

    fun accept(sku: String, reason: VarianceReason) {
        val line = lines.find { it.sku == sku } ?: return
        apply(sku, CycleCount.acceptVariance(line, role, reason))
    }

    fun reject(sku: String) {
        val line = lines.find { it.sku == sku } ?: return
        apply(sku, CycleCount.rejectCount(line, role))
    }

    fun reset() {
        lines = SeedData.lines()
        role = Role.TECH
        selectedSku = lines.first().sku
        notice = null
    }

    private fun apply(sku: String, result: DomainResult<CountLine>) {
        when (result) {
            is DomainResult.Ok -> {
                lines = lines.map { if (it.sku == sku) result.value else it }
                notice = null
            }
            is DomainResult.Err -> notice = result.message
        }
    }
}

@Composable
fun VialcountApp(model: CountViewModel = viewModel()) {
    val selected = model.lines.find { it.sku == model.selectedSku }
    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Vialcount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Cycle count · training data", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = model::toggleRole) {
                Text(if (model.role == Role.TECH) "Tech" else "Lead")
            }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "The expected quantity stays hidden until the first count is submitted.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(model.lines, key = { it.sku }) { line ->
                LineCard(line, selected = line.sku == model.selectedSku, onClick = { model.select(line.sku) })
            }
            if (selected != null) {
                item { CountEditor(model, selected) }
            }
            item {
                TextButton(onClick = model::reset) { Text("Reset training counts") }
            }
        }
    }
}

@Composable
private fun LineCard(line: CountLine, selected: Boolean, onClick: () -> Unit) {
    val expected = CycleCount.displayExpected(line, revealed = false)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) Green else Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(line.sku, fontFamily = FontFamily.Monospace)
        Text(line.name, fontWeight = FontWeight.Medium)
        Text(
            "${line.location} · ${statusLabel(line.status)} · ${expectedLabel(expected)}",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CountEditor(model: CountViewModel, line: CountLine) {
    var draft by remember(line.sku) { mutableStateOf("") }
    var entryNote by remember(line.sku) { mutableStateOf<String?>(null) }
    var reason by remember(line.sku) { mutableStateOf(VarianceReason.DAMAGED) }
    val expected = CycleCount.displayExpected(line, revealed = false)
    val counting = line.status == Status.OPEN || line.status == Status.NEEDS_RECOUNT
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(line.name, fontWeight = FontWeight.SemiBold)
        Text(expectedLabel(expected), color = Muted, style = MaterialTheme.typography.bodySmall)
        line.firstCount?.let { Text("First count $it", style = MaterialTheme.typography.bodyMedium) }
        line.secondCount?.let { Text("Second count $it", style = MaterialTheme.typography.bodyMedium) }
        line.reason?.let { Text(reasonLabel(it), color = Muted, style = MaterialTheme.typography.bodySmall) }
        (entryNote ?: model.notice)?.let { Text(it, color = Stat) }
        if (counting) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(if (line.status == Status.OPEN) "First count" else "Recount") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val qty = draft.toIntOrNull()
                    if (qty == null) {
                        entryNote = "Enter a whole number."
                    } else {
                        entryNote = null
                        model.submit(line.sku, qty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Surface),
            ) {
                Text(if (line.status == Status.OPEN) "Submit first count" else "Submit recount")
            }
        }
        if (line.status == Status.NEEDS_RECOUNT) {
            Text("Variance reason", color = Muted, style = MaterialTheme.typography.bodySmall)
            VarianceReason.entries.forEach { choice ->
                TextButton(onClick = { reason = choice }) {
                    Text(
                        reasonLabel(choice),
                        color = if (reason == choice) Green else Muted,
                        fontWeight = if (reason == choice) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
            Button(
                onClick = { model.accept(line.sku, reason) },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Surface),
            ) { Text("Accept variance") }
            TextButton(onClick = { model.reject(line.sku) }) { Text("Reject count") }
        }
    }
}

private fun expectedLabel(expected: Int?) = if (expected == null) "Expected hidden" else "Expected $expected"

private fun statusLabel(status: Status) = when (status) {
    Status.OPEN -> "Open"
    Status.NEEDS_RECOUNT -> "Needs recount"
    Status.ACCEPTED -> "Accepted"
    Status.REJECTED -> "Rejected"
}

private fun reasonLabel(reason: VarianceReason) = when (reason) {
    VarianceReason.DAMAGED -> "Damaged"
    VarianceReason.FOUND_IN_OTHER_BIN -> "Found in another bin"
    VarianceReason.DATA_ERROR -> "Data error"
}

private val Paper = Color(0xFFF3EFE6)
private val Surface = Color(0xFFFFFDF8)
private val Green = Color(0xFF1E4D3A)
private val Muted = Color(0xFF6F685F)
private val Line = Color(0xFFE3DCD0)
private val Stat = Color(0xFF8C2F2F)
