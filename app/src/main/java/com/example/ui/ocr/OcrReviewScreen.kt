package com.example.ui.ocr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    invoice: ScannedInvoice,
    reviewableItems: List<ReviewableItem>,
    onEditItem: (String, ScannedItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    var editingItemId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review Scanned Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        invoice.supplierName?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val newCount = reviewableItems.count { it.matchedProductId == null }
                        val updateCount = reviewableItems.count { it.matchedProductId != null }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (newCount > 0) SummaryChip("$newCount New", MaterialTheme.colorScheme.tertiary)
                            if (updateCount > 0) SummaryChip("$updateCount Updates", MaterialTheme.colorScheme.primary)
                        }
                        Text("${reviewableItems.size} items", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = reviewableItems.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Add to Stock", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InvoiceHeaderCard(invoice) }

            item {
                Text(
                    "Line Items (${reviewableItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(reviewableItems, key = { it.scannedItem.id }) { reviewable ->
                ReviewableItemCard(
                    reviewable = reviewable,
                    onEdit = { editingItemId = reviewable.scannedItem.id },
                    onRemove = { onRemoveItem(reviewable.scannedItem.id) }
                )
            }
        }
    }

    editingItemId?.let { id ->
        val item = reviewableItems.find { it.scannedItem.id == id }?.scannedItem
        if (item != null) {
            EditItemDialog(
                item = item,
                onDismiss = { editingItemId = null },
                onSave = { updatedItem -> onEditItem(id, updatedItem); editingItemId = null }
            )
        }
    }
}

@Composable
fun ReviewableItemCard(
    reviewable: ReviewableItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val isNew = reviewable.matchedProductId == null
    val matchColor = if (isNew) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val matchLabel = if (isNew) "NEW PRODUCT" else "UPDATE STOCK"
    val matchIcon  = if (isNew) Icons.Default.AddBox else Icons.Default.Inventory

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    reviewable.scannedItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(matchLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(matchIcon, null, modifier = Modifier.size(14.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = matchColor.copy(alpha = 0.12f),
                        labelColor = matchColor,
                        leadingIconContentColor = matchColor
                    ),
                    border = null
                )
            }

            if (!isNew && reviewable.matchedProductName != null) {
                Text(
                    "Matches: ${reviewable.matchedProductName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (reviewable.isEdited) {
                Text("✎ Edited", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ItemDetailColumn(Modifier.weight(1f), "Quantity",
                    "${reviewable.scannedItem.quantity} ${reviewable.scannedItem.unit ?: ""}".trim())
                reviewable.scannedItem.unitPrice?.let {
                    ItemDetailColumn(Modifier.weight(1f), "Unit Price", "₹${String.format("%.2f", it)}")
                }
                reviewable.scannedItem.totalPrice?.let {
                    ItemDetailColumn(Modifier.weight(1f), "Total", "₹${String.format("%.2f", it)}")
                }
            }

            reviewable.scannedItem.hsnCode?.let {
                Text("HSN: $it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
fun ItemDetailColumn(modifier: Modifier = Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SummaryChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InvoiceHeaderCard(invoice: ScannedInvoice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Invoice Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Supplier:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(invoice.supplierName ?: "Unknown Supplier", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            invoice.invoiceNumber?.let {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Invoice #:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            invoice.invoiceDate?.let {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            invoice.totalAmount?.let {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", it)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
