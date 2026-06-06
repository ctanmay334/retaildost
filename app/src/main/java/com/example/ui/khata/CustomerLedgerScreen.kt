package com.example.ui.khata

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KiranaViewModel
import com.example.ui.Screen
import com.example.ui.khata.KhataViewModel
import com.example.utils.AutoResizingText
import java.util.Date
import java.io.File
import java.util.Calendar
import android.app.DatePickerDialog
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: KiranaViewModel,
    khataViewModel: KhataViewModel,
    customerId: String
) {
    val uiState by khataViewModel.uiState.collectAsState()

    // Set selected customer in the ViewModel for transaction filtering
    LaunchedEffect(customerId) {
        khataViewModel.setSelectedCustomer(customerId)
    }

    val customer = uiState.selectedCustomer
    val customerTx = uiState.selectedCustomerTransactions

    var inputAmountStr by remember { mutableStateOf("") }
    var inputNote by remember { mutableStateOf("") }
    var selectedDueDate by remember { mutableStateOf<Long?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showShareBottomSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val primaryIndigo = Color(0xFF0B1A7D)
    val secondaryTeal = Color(0xFF006B5E)
    val errorCrimson = Color(0xFFBA1A1A)
    val textDark = Color(0xFF1B1B21)
    val textMuted = Color(0xFF64748B)
    val bgLight = Color(0xFFF8FAFC)

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = primaryIndigo)
        }
        return
    }

    var showSettleDialog by remember { mutableStateOf(false) }

    if (showSettleDialog) {
        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = { Text("Settle Account", fontWeight = FontWeight.Bold, color = textDark) },
            text = { Text("Are you sure you want to mark all entries as settled? This will reset the balance to ₹0.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettleDialog = false
                    khataViewModel.settleCustomer(customer.id)
                }) { Text("Confirm", fontWeight = FontWeight.Bold, color = primaryIndigo) }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) { Text("Cancel", color = textMuted) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AutoResizingText(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textDark)
                        AutoResizingText(
                            customer.phone ?: "No phone",
                            fontSize = 12.sp,
                            color = textMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            khataViewModel.setSelectedCustomer(null)
                            viewModel.selectTab(2) // Select tab 2 (Khata)
                            viewModel.navigateTo(Screen.Dashboard)
                        },
                        modifier = Modifier.testTag("ledger_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = textDark
                        )
                    }
                },
                actions = {
                    if (customer.runningBalance > 0.0) {
                        IconButton(onClick = {
                            customer.phone?.let { phone ->
                                val message = "Namaste ${customer.name} ji 🙏\n\nAapke upar ₹${kotlin.math.abs(customer.runningBalance)} baaki hai hamare kirana store mein.\n\nKripya jaldi bhugtaan karein. Dhanyavaad! 🙏"
                                com.example.utils.WhatsAppHelper.sendMessage(context, phone, message)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Contact client",
                                tint = primaryIndigo
                            )
                        }
                    }
                    IconButton(onClick = {
                        // Share statement placeholder action
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp reminder report share",
                            tint = textMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgLight)
                .padding(innerPadding)
        ) {
            // Scrollable ledger contents
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("ledger_transactions_lazy_column"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 320.dp), // Safe space for bottom input sheet
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item 1: The Financial Truth Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_credit_outstanding_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AutoResizingText(
                                text = "TOTAL OUTSTANDING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMuted,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val isOverdue = customer.runningBalance >= 0
                                AutoResizingText(
                                    text = "₹",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdue) errorCrimson else secondaryTeal,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                AutoResizingText(
                                    text = String.format("%,.2f", kotlin.math.abs(customer.runningBalance)),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOverdue) errorCrimson else secondaryTeal,
                                    letterSpacing = (-1).sp
                                )
                            }

                            // Overdue indicator
                            if (customer.runningBalance > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .background(Color(0xFFFFF3CD), RoundedCornerShape(20.dp))
                                        .border(BorderStroke(1.dp, Color(0xFFFFEBAA)), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Overdue for 15 days",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Three bento actions grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Action 1: Settle
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { showSettleDialog = true }
                                        .padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primaryIndigo.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Settle",
                                            tint = primaryIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AutoResizingText("SETTLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textMuted)
                                }

                                // Action 2: Remind (Only visible if outstanding balance is > 0)
                                if (customer.runningBalance > 0.0) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                customer.phone?.let { phone ->
                                                    val message = "Namaste ${customer.name} ji 🙏\n\nAapke upar ₹${kotlin.math.abs(customer.runningBalance)} baaki hai hamare kirana store mein.\n\nKripya jaldi bhugtaan karein. Dhanyavaad! 🙏"
                                                    com.example.utils.WhatsAppHelper.sendMessage(context, phone, message)
                                                }
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF25D366).copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Remind",
                                                tint = Color(0xFF25D366),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        AutoResizingText("REMIND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textMuted)
                                    }
                                }

                                // Action 3: Report
                                val hasTx = customerTx.isNotEmpty()
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            if (hasTx) {
                                                isGeneratingPdf = true
                                                com.example.utils.PdfHelper.generateLedgerPdf(context, customer, customerTx) { file ->
                                                    isGeneratingPdf = false
                                                    if (file != null) {
                                                        generatedPdfFile = file
                                                        showShareBottomSheet = true
                                                        Toast.makeText(context, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "No transaction history to generate PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (hasTx) Color(0xFF64748B).copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Report",
                                            tint = if (hasTx) Color(0xFF64748B) else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AutoResizingText("REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (hasTx) textMuted else Color.LightGray)
                                }
                            }
                        }
                    }
                }                // Item 2: Audit Trail Title Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AutoResizingText(
                            text = "AUDIT TRAIL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMuted,
                            letterSpacing = 1.sp
                        )

                        // Filter button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                .clickable { /* Filter Action */ }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = primaryIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryIndigo
                            )
                        }
                    }
                }

                // Item 3: Empty logs state or list mapping
                if (customerTx.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "",
                                    tint = textMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No recorded transactions for this customer ledger.",
                                    fontSize = 14.sp,
                                    color = textMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(customerTx, key = { _, tx -> tx.id }) { index, tx ->
                        val txDate = Date(tx.createdAt)
                        val dateString = DateFormat.format("MMM dd • hh:mm a", txDate).toString()
                        val isDebit = tx.txType == "debit"

                        // Calculate the date section label — only show when date changes
                        val todayCal = java.util.Calendar.getInstance()
                        val txCal = java.util.Calendar.getInstance().also { it.time = txDate }
                        val dateLabel = when {
                            txCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                            txCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR) -> "TODAY"
                            txCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                            txCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR) - 1 -> "YESTERDAY"
                            else -> DateFormat.format("dd MMM", txDate).toString().uppercase()
                        }

                        // Check if we need to show a date separator
                        val showDateSeparator = if (index == 0) {
                            true // always show for first transaction
                        } else {
                            val prevTxDate = Date(customerTx[index - 1].createdAt)
                            val prevTxCal = java.util.Calendar.getInstance().also { it.time = prevTxDate }
                            // Show separator only when the calendar date changes
                            txCal.get(java.util.Calendar.YEAR) != prevTxCal.get(java.util.Calendar.YEAR) ||
                            txCal.get(java.util.Calendar.DAY_OF_YEAR) != prevTxCal.get(java.util.Calendar.DAY_OF_YEAR)
                        }

                        if (showDateSeparator) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = Color(0xFFEEF2F6),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = dateLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textMuted,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight().width(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val isVoice = tx.notes?.contains("voice", ignoreCase = true) == true || tx.rawInput != null
                                val isInvoice = tx.notes?.contains("invoice", ignoreCase = true) == true || tx.notes?.contains("inv-", ignoreCase = true) == true

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isVoice) Color(0xFFF1F5F9)
                                            else if (isInvoice) Color(0xFFF1F5F9)
                                            else if (isDebit) errorCrimson.copy(alpha = 0.05f)
                                            else secondaryTeal.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isVoice) Icons.Default.Mic
                                        else if (isInvoice) Icons.Default.Receipt
                                        else if (isDebit) Icons.Default.Warning
                                        else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (isDebit) errorCrimson else secondaryTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .weight(1f)
                                        .background(Color(0xFFE2E8F0))
                                )
                            }

                            Card(
                                modifier = Modifier.weight(1f).padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        AutoResizingText(
                                            text = tx.notes?.takeIf { it.isNotEmpty() } ?: tx.rawInput?.takeIf { it.isNotEmpty() } ?: "Manual Entry",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = textDark
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        AutoResizingText(
                                            text = dateString,
                                            fontSize = 11.sp,
                                            color = textMuted
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        AutoResizingText(
                                            text = if (isDebit) "- ₹${String.format("%,.2f", tx.amount)}" else "+ ₹${String.format("%,.2f", tx.amount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (isDebit) errorCrimson else secondaryTeal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                color = bgLight,
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                                            ) {
                                                // Show current customer running balance (khata_transactions has no per-tx balance snapshot)
                                                AutoResizingText(
                                                    text = "BAL: ₹${kotlin.math.abs(customer.runningBalance).toInt()}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Surface(
                                                color = if (isDebit) errorCrimson else secondaryTeal,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                AutoResizingText(
                                                    text = if (isDebit) "GIVE" else "GET",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Bottom Smart Entry Footer Panel
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 8.dp,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Transaction Entry Zone with container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F2FB), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color(0xFFC6C5D4).copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        // Large Amount input field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "ENTER AMOUNT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryIndigo,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = inputAmountStr,
                                        onValueChange = { inputAmountStr = it },
                                        textStyle = TextStyle(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = textDark
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ledger_amount_form"),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            if (inputAmountStr.isEmpty()) {
                                                Text(
                                                    text = "0.00",
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFCBD5E1)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seamless notes field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = inputNote,
                                onValueChange = { inputNote = it },
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ledger_notes_form"),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (inputNote.isEmpty()) {
                                        Text(
                                            text = "What was this for? (Optional)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Actions Row (Due date & Attachment)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dueDateText = selectedDueDate?.let {
                            android.text.format.DateFormat.format("dd MMM yyyy", Date(it)).toString()
                        } ?: "Set Due Date"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    selectedDueDate?.let { calendar.timeInMillis = it }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(Calendar.YEAR, year)
                                            selectedCal.set(Calendar.MONTH, month)
                                            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            selectedCal.set(Calendar.HOUR_OF_DAY, 0)
                                            selectedCal.set(Calendar.MINUTE, 0)
                                            selectedCal.set(Calendar.SECOND, 0)
                                            selectedCal.set(Calendar.MILLISECOND, 0)
                                            selectedDueDate = selectedCal.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = primaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedDueDate != null) "Due: $dueDateText" else "Set Due Date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryIndigo
                            )
                            if (selectedDueDate != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear due date",
                                    tint = errorCrimson,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            selectedDueDate = null
                                        }
                                )
                            }
                        }

                        IconButton(
                            onClick = { /* File attachment action */ },
                            modifier = Modifier
                                .size(36.dp)
                                .background(bgLight, CircleShape)
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach file",
                                tint = textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Red & Green entry buttons matching the HTML specifications perfectly
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Give Credit (Maine Diya)
                        OutlinedButton(
                            onClick = {
                                val amt = inputAmountStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    val noteText = if (inputNote.isEmpty()) "Debit Udhar book" else inputNote
                                    khataViewModel.addTransaction(
                                        customerId = customer.id,
                                        type = "debit",
                                        amount = amt,
                                        notes = noteText,
                                        dueDate = selectedDueDate
                                    )
                                    inputAmountStr = ""
                                    inputNote = ""
                                    selectedDueDate = null
                                }
                            },
                            enabled = inputAmountStr.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("give_credit_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = errorCrimson
                            ),
                            border = BorderStroke(2.dp, errorCrimson.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GIVE CREDIT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Maine Diya", fontSize = 10.sp)
                            }
                        }

                        // Get Payment (Maine Mila)
                        Button(
                            onClick = {
                                val amt = inputAmountStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    val noteText = if (inputNote.isEmpty()) "Paid Credit deposit" else inputNote
                                    khataViewModel.addTransaction(
                                        customerId = customer.id,
                                        type = "credit",
                                        amount = amt,
                                        notes = noteText
                                    )
                                    inputAmountStr = ""
                                    inputNote = ""
                                    selectedDueDate = null
                                }
                            },
                            enabled = inputAmountStr.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("receive_payment_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = secondaryTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GET PAYMENT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Maine Mila", fontSize = 10.sp)
                            }
                        }
                    }
                    }
                    }

            // PDF Generating loading spinner
            if (isGeneratingPdf) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {}, // absorb clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = primaryIndigo)
                            Text("Generating PDF Report...", fontWeight = FontWeight.Medium, color = textDark)
                        }
                    }
                }
            }

            // Bottom Share Sheet (occupies 1/6th of screen)
            if (showShareBottomSheet && generatedPdfFile != null) {
                ModalBottomSheet(
                    onDismissRequest = { showShareBottomSheet = false },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(36.dp, 4.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.166f)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Share Statement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textDark
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // WhatsApp Share
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        showShareBottomSheet = false
                                        com.example.utils.PdfHelper.sharePdfToWhatsApp(context, generatedPdfFile!!, customer.phone)
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "WhatsApp",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textDark
                                )
                            }

                            // System Share
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        showShareBottomSheet = false
                                        com.example.utils.PdfHelper.sharePdfGeneral(context, generatedPdfFile!!)
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(primaryIndigo.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "System Share",
                                        tint = primaryIndigo,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "System Share",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
