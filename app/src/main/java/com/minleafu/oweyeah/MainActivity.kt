package com.minleafu.oweyeah

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ── Colors ──────────────────────────────────────────────────────────────────
val CreamColor      = Color(0xFF2ECCC4)   // teal-turquoise
val CreamDark       = Color(0xFF1A9E97)
val CreamLight      = Color(0xFFAAF0ED)
val FennColor       = Color(0xFF6C4FD9)   // blue-purple
val FennDark        = Color(0xFF4E37A8)
val FennLight       = Color(0xFFBFAFF5)
val BackgroundDark  = Color(0xFF0E0E14)
val SurfaceDark     = Color(0xFF181825)
val SurfaceMid      = Color(0xFF22223A)
val TextPrimary     = Color(0xFFF0F0FF)
val TextSecondary   = Color(0xFF9090B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DebtTrackerTheme {
                DebtTrackerApp()
            }
        }
    }
}

@Composable
fun DebtTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = BackgroundDark,
            surface = SurfaceDark,
            primary = CreamColor,
            secondary = FennColor,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────
fun formatDebt(value: Double): String {
    val abs = kotlin.math.abs(value)
    return when {
        abs == kotlin.math.floor(abs) -> "%.0f".format(abs)          // 5.000 → "5"
        (abs * 10) == kotlin.math.floor(abs * 10) -> "%.1f".format(abs) // 5.100 → "5.1" (shouldn't happen but safe)
        (abs * 100) % 1.0 == 0.0 -> "%.2f".format(abs)              // 5.120 → "5.12"
        else -> "%.3f".format(abs)                                    // 5.123 → "5.123"
    }
}

fun formatRaw(value: Double): String {
    return when {
        value == kotlin.math.floor(value) -> "%.0f".format(value)
        (value * 100) % 1.0 == 0.0 -> "%.2f".format(value)
        else -> "%.3f".format(value)
    }
}

// ── Persistence ──────────────────────────────────────────────────────────────
fun saveDebt(context: Context, value: Double) {
    context.getSharedPreferences("debt_prefs", Context.MODE_PRIVATE)
        .edit().putString("debt", value.toString()).apply()
}

fun loadDebt(context: Context): Double {
    val str = context.getSharedPreferences("debt_prefs", Context.MODE_PRIVATE)
        .getString("debt", "0.0") ?: "0.0"
    return str.toDoubleOrNull() ?: 0.0
}

// ── Main UI ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtTrackerApp() {
    val context = LocalContext.current

    // State
    var debt           by remember { mutableStateOf(loadDebt(context)) }
    var payer          by remember { mutableStateOf("Cream") }
    var amount         by remember { mutableStateOf("") }
    var splitMode      by remember { mutableStateOf(false) }
    var editingBalance by remember { mutableStateOf(false) }
    var balanceInput   by remember { mutableStateOf("") }

    // Derived colours based on who owes whom
    val isPositive = debt >= 0.0  // +ve => Cream owes Fenn
    val owingColor  = if (isPositive) CreamColor  else FennColor
    val owingLabel  = if (isPositive) "Cream owes Fenn" else "Fenn owes Cream"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Title ────────────────────────────────────────────────────
            Text(
                "SPLIT TRACKER",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            // ── Debt Display Card ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(SurfaceDark, SurfaceMid)))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(owingColor.copy(0.4f), Color.Transparent)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        owingLabel.uppercase(),
                        color = owingColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    if (editingBalance) {
                        OutlinedTextField(
                            value = balanceInput,
                            onValueChange = { balanceInput = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = owingColor,
                                textAlign = TextAlign.Center
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = owingColor,
                                unfocusedBorderColor = owingColor.copy(0.5f),
                                focusedTextColor = owingColor,
                                unfocusedTextColor = owingColor,
                                cursorColor = owingColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { editingBalance = false },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, TextSecondary.copy(0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    val parsed = balanceInput.toDoubleOrNull()
                                    if (parsed != null) {
                                        debt = Math.round(parsed * 1000) / 1000.0
                                        saveDebt(context, debt)
                                        editingBalance = false
                                        Toast.makeText(context, "Balance updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = owingColor),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Save", color = Color.White) }
                        }
                    } else {
                        Text(
                            formatDebt(debt),
                            color = owingColor,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "raw balance: ${formatRaw(debt)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                balanceInput = debt.toString()
                                editingBalance = true
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("✏ Edit balance", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Copy Button ───────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    val text = "Total: ${formatRaw(debt)}\n-ve: fenn -> cream\n+ve: cream -> fenn"
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("debt", text))
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TextSecondary.copy(0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy Balance", fontSize = 14.sp)
            }

            // ── Payer Selector ────────────────────────────────────────────
            SectionLabel("Who's Paying?")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PersonButton(
                    label = "Cream",
                    selected = payer == "Cream",
                    activeColor = CreamColor,
                    activeGlow = CreamDark,
                    modifier = Modifier.weight(1f)
                ) { payer = "Cream" }

                PersonButton(
                    label = "Fenn",
                    selected = payer == "Fenn",
                    activeColor = FennColor,
                    activeGlow = FennDark,
                    modifier = Modifier.weight(1f)
                ) { payer = "Fenn" }
            }

            // ── Amount Input ──────────────────────────────────────────────
            SectionLabel("Amount")
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = { Text("0.000", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (payer == "Cream") CreamColor else FennColor,
                    unfocusedBorderColor = TextSecondary.copy(0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = if (payer == "Cream") CreamColor else FennColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Split Toggle ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceMid)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Split 50/50", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (splitMode) "Halving the amount" else "Using full amount",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = splitMode,
                    onCheckedChange = { splitMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = if (payer == "Cream") CreamColor else FennColor,
                        uncheckedTrackColor = SurfaceDark
                    )
                )
            }

            // ── Action Buttons ────────────────────────────────────────────
            val activeColor = if (payer == "Cream") CreamColor else FennColor
            val activeDark  = if (payer == "Cream") CreamDark  else FennDark

            Button(
                onClick = {
                    val raw = amount.toDoubleOrNull() ?: run {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val effective = if (splitMode) raw / 2.0 else raw
                    // Cream paying reduces debt (+ve -> less positive means Cream owes less)
                    // Fenn paying increases debt
                    debt = if (payer == "Cream") debt - effective else debt + effective
                    // Round to 3 decimal places to avoid floating point drift
                    debt = Math.round(debt * 1000) / 1000.0
                    saveDebt(context, debt)
                    amount = ""
                    Toast.makeText(context, "$payer paid ${formatDebt(effective)}", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp), spotColor = activeColor)
            ) {
                val displayAmt = amount.toDoubleOrNull()?.let {
                    val v = if (splitMode) it / 2.0 else it
                    formatDebt(v)
                } ?: "—"
                Text(
                    "$payer paid  •  $displayAmt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // ── Reset ─────────────────────────────────────────────────────
            TextButton(
                onClick = {
                    debt = 0.0
                    saveDebt(context, 0.0)
                    amount = ""
                    Toast.makeText(context, "Debt cleared", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Reset Balance", color = TextSecondary.copy(0.7f), fontSize = 13.sp)
            }

            // ── Legend ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceMid)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("+ve", "Cream → Fenn", CreamColor)
                Spacer(modifier = Modifier.width(1.dp).background(TextSecondary.copy(0.2f)))
                LegendItem("-ve", "Fenn → Cream", FennColor)
            }
        }
    }
}

// ── Small Composables ─────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PersonButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeGlow: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) activeColor else SurfaceMid
    val fg = if (selected) Color.White else TextSecondary

    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(if (selected) 12.dp else 0.dp, RoundedCornerShape(14.dp), spotColor = activeColor)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun LegendItem(sign: String, description: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(sign, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextSecondary, fontSize = 10.sp)
        }
    }
}