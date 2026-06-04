package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankCard
import com.example.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    onRequestSmsPermission: () -> Unit
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedCardId by viewModel.selectedCardId.collectAsStateWithLifecycle()
    val smsResult by viewModel.smsSimulationResult.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Analytics, 2 = SMS Integration

    // Dialog flags
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }

    // Init sample data if database is bare
    LaunchedEffect(cards) {
        if (cards.isEmpty()) {
            viewModel.generateInitialDemoData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Мои финансы",
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { onRequestSmsPermission() }) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "Предоставить права на СМС",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "АП",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(80.dp)
                    .drawBehind {
                        drawLine(
                            color = Color(0xFFCAC4D0),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(if (activeTab == 0) Icons.Default.Wallet else Icons.Outlined.Wallet, contentDescription = "Главная") },
                    label = { Text("Главная", fontSize = 11.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(if (activeTab == 1) Icons.Default.Analytics else Icons.Outlined.Analytics, contentDescription = "Аналитика") },
                    label = { Text("Лимиты", fontSize = 11.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(if (activeTab == 2) Icons.Default.NetworkCheck else Icons.Outlined.NetworkCheck, contentDescription = "СМС Банкинг") },
                    label = { Text("Профиль", fontSize = 11.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showAddTxDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить расход/доход")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> HomeScreenTab(
                    cards = cards,
                    transactions = transactions,
                    selectedCardId = selectedCardId,
                    onSelectCard = { viewModel.selectCard(it) },
                    onDeleteCard = { viewModel.deleteCard(it) },
                    onDeleteTx = { viewModel.deleteTransaction(it) },
                    onAddNewCardClick = { showAddCardDialog = true },
                    onAnalyticsClick = { activeTab = 1 }
                )
                1 -> AnalyticsScreenTab(
                    cards = cards,
                    transactions = if (selectedCardId != null) transactions else allTransactions,
                    selectedCardId = selectedCardId,
                    cardsList = cards
                )
                2 -> SmsSimulatorTab(
                    cards = cards,
                    smsResult = smsResult,
                    onSimulateSms = { text -> viewModel.simulateBankSms(text) },
                    onClearResult = { viewModel.clearSmsResult() }
                )
            }

            // --- Add Card Dialog ---
            if (showAddCardDialog) {
                AddCardDialog(
                    onDismiss = { showAddCardDialog = false },
                    onAddCard = { num, holder, bank, bal, exp, cType, color ->
                        viewModel.addCard(num, holder, bank, bal, exp, cType, color)
                        showAddCardDialog = false
                    }
                )
            }

            // --- Add Manual Transaction Dialog ---
            if (showAddTxDialog) {
                AddTransactionDialog(
                    cards = cards,
                    selectedCardId = selectedCardId,
                    onDismiss = { showAddTxDialog = false },
                    onAddTransaction = { cardId, amount, cat, desc ->
                        viewModel.addManualTransaction(cardId, amount, cat, desc)
                        showAddTxDialog = false
                    }
                )
            }
        }
    }
}

// --- HOME SCREEN TAB ---
@Composable
fun HomeScreenTab(
    cards: List<BankCard>,
    transactions: List<Transaction>,
    selectedCardId: Int?,
    onSelectCard: (Int?) -> Unit,
    onDeleteCard: (BankCard) -> Unit,
    onDeleteTx: (Transaction) -> Unit,
    onAddNewCardClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Space above
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Cards Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ВАШИ КАРТЫ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onAddNewCardClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Все", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Cards Horizontal List
        item {
            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onAddNewCardClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Добавить банковскую карту",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(cards) { card ->
                        val isSelected = selectedCardId == card.id
                        BankCardItem(
                            card = card,
                            isSelected = isSelected,
                            onCardClick = {
                                if (isSelected) {
                                    onSelectCard(null) // Unselect
                                } else {
                                    onSelectCard(card.id)
                                }
                            },
                            onDeleteClick = { onDeleteCard(card) }
                        )
                    }

                    // Add icon in row
                    item {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(170.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onAddNewCardClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Добавить",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Balance Block
        item {
            val totalBalance = remember(cards, selectedCardId) {
                if (selectedCardId != null) {
                    cards.find { it.id == selectedCardId }?.balance ?: 0.0
                } else {
                    cards.sumOf { it.balance }
                }
            }

            val cardTitle = remember(selectedCardId, cards) {
                if (selectedCardId != null) {
                    cards.find { it.id == selectedCardId }?.let { "${it.bankName} (*${it.cardNumber.takeLast(4)})" } ?: ""
                } else {
                    "Общий баланс"
                }
            }

            val monthlyIncome = remember(transactions) {
                transactions.filter { it.amount > 0 }.sumOf { it.amount }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (selectedCardId != null) cardTitle else "Общий баланс",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatPrice(totalBalance),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Доход в этом месяце",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+ ${formatPrice(monthlyIncome)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        // Analytics pill container button
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                                .clickable { onAnalyticsClick() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Аналитика",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Transaction Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (selectedCardId != null) "Транзакции по карте" else "Последние операции").uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedCardId != null) {
                    TextButton(
                        onClick = { onSelectCard(null) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Сбросить", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Transaction Feed Container Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ContentPasteSearch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Операций не найдено",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        transactions.forEachIndexed { index, tx ->
                            TransactionItem(
                                transaction = tx,
                                linkedCard = cards.find { it.id == tx.cardId },
                                onDelete = { onDeleteTx(tx) }
                            )
                            if (index < transactions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    thickness = 0.8.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

// --- VISUAL BEAUTIFUL BANK CARD ITEM ---
@Composable
fun BankCardItem(
    card: BankCard,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val baseColor = try {
        Color(android.graphics.Color.parseColor(card.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Interactive Ripple Visual Effect Container
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        baseColor.copy(alpha = 0.75f),
                        baseColor.copy(alpha = 0.5f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .border(
                width = if (isSelected) 3.1.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onCardClick() }
            .padding(18.dp)
    ) {
        // Draw decorative abstract glass shapes
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = 160f,
                center = Offset(size.width * 0.9f, size.height * 0.1f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = 320f,
                center = Offset(size.width * 0.1f, size.height * 0.9f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Bank Logo + Delete action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.bankName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить карту",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Chip and card type logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold Chip
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(26.dp)
                        .background(Color(0xFFE5A93B).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                )

                Text(
                    text = card.cardType,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            // Footer: Hidden number + Expire date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = card.cardNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.holderName,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "VALID THRU",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = card.expiryDate,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить банковскую карту?") },
            text = { Text("Вы действительно хотите удалить карту ${card.bankName} (${card.cardNumber.takeLast(4)})? Связанные транзакции останутся, но потеряют привязку.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// --- TRANSACTION LIST ITEM ---
@Composable
fun TransactionItem(
    transaction: Transaction,
    linkedCard: BankCard?,
    onDelete: () -> Unit
) {
    val isExpense = transaction.amount < 0
    val amountText = if (isExpense) {
        "- ${formatPrice(Math.abs(transaction.amount))}"
    } else {
        "+ ${formatPrice(transaction.amount)}"
    }
    
    val (categoryBg, categoryOn) = getCategoryTonalColors(transaction.category, !isExpense)
    val emoji = getCategoryEmoji(transaction.category)
    val amountColor = if (isExpense) MaterialTheme.colorScheme.onSurface else Color(0xFF2B5B30)

    val formattedDate = remember(transaction.timestamp) {
        try {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(transaction.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* Ripple feed */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(categoryBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.description,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (transaction.isSmsParsed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "СМС",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (linkedCard != null) {
                        Text(
                            text = " • Карта *${linkedCard.cardNumber.takeLast(4)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Price column & deletion
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = amountText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = amountColor
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить трату",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// --- ANALYTICS SCREEN TAB ---
@Composable
fun AnalyticsScreenTab(
    cards: List<BankCard>,
    transactions: List<Transaction>,
    selectedCardId: Int?,
    cardsList: List<BankCard>
) {
    val totalExpense = remember(transactions) {
        Math.abs(transactions.filter { it.amount < 0 }.sumOf { it.amount })
    }
    val totalIncome = remember(transactions) {
        transactions.filter { it.amount > 0 }.sumOf { it.amount }
    }

    // Expense grouped by category
    val categoryTotals = remember(transactions) {
        transactions.filter { it.amount < 0 }
            .groupBy { it.category }
            .mapValues { Math.abs(it.value.sumOf { t -> t.amount }) }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Column {
                Text(
                    text = "Экран Анализа Расходов",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (selectedCardId != null) "Статистика по активной карте" else "Статистика по всем кошелькам",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Summary Statistics Cards (Income & Expense summary side-by-side)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Расходы", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatPrice(totalExpense),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(232, 245, 233).copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(46, 125, 50))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Доходы", fontSize = 12.sp, color = Color(46, 125, 50))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatPrice(totalIncome),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(46, 125, 50)
                        )
                    }
                }
            }
        }

        if (totalExpense == 0.0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PivotTableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Недостаточно данных",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Для отображения графиков добавьте расходы.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        } else {
            // Circle Chart
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Структура трат по сегментам",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Drawn Radial Pie Chart
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var currentAngle = -90f
                                for (category in categoryTotals) {
                                    val sweep = (category.second / totalExpense).toFloat() * 360f
                                    drawArc(
                                        color = getCategoryColor(category.first),
                                        startAngle = currentAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 32f)
                                    )
                                    currentAngle += sweep
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Всего трат", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Text(
                                    formatPrice(totalExpense),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Categories Breakdown
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryTotals.forEach { (cat, amount) ->
                                val percent = (amount / totalExpense * 100).toInt()
                                val color = getCategoryColor(cat)

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(color, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Text(
                                            "${formatPrice(amount)} ($percent%)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Custom visual progress bar
                                    LinearProgressIndicator(
                                        progress = { (amount / totalExpense).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }
    }
}

// --- SMS BANK EMULATOR TAB ---
@Composable
fun SmsSimulatorTab(
    cards: List<BankCard>,
    smsResult: String?,
    onSimulateSms: (String) -> Unit,
    onClearResult: () -> Unit
) {
    var customSmsText by remember { mutableStateOf("") }

    val presetTemplates = listOf(
        "Сбербанк 900" to "MIR-4321 15:40 Покупка 450р Pyaterochka Баланс: 4850р",
        "Т-Банк" to "Pokupka 1200 RUR. Karta *7890. Balans 123800 RUR. BurgerKing",
        "Альфа-Банк" to "Alfa-Bank: Spisanie 320 RUR. Karta *9876. Proezd Metro",
        "Сбербанк (Альтернативный)" to "VISA4321 11:15 Списание 899р Ozon Баланс: 3951р"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Column {
                Text(
                    text = "Интеграция Банковских Карт через СМС",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Text(
                    text = "Используйте автоматический парсинг входящих сообщений от банков",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Informative instructional card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Как это работает?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "1. Приложение регистрирует BroadcastReceiver и слушает SMS об операциях от банков Sberbank (900), Tinkoff / Т-Банк, Alfa-Bank.\n" +
                        "2. Алгоритм сверяет последние 4 цифры карты из сообщения с номерами ваших добавленных карт.\n" +
                        "3. При совпадении баланс карты автоматически корректируется или синхронизируется, а транзакция заносится в историю.\n" +
                        "4. Вы получаете системный пуш-нотификатор о транзакции.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Live emulation workspace
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Симулятор СМС от банка",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Так как в веб-эмуляторе нет физической СМС, протестируйте парсер прямо здесь! Скопируйте шаблон или напишите свой.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Быстрые шаблоны для тест-драйва:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset template column
                    presetTemplates.forEach { (title, template) ->
                        Button(
                            onClick = {
                                customSmsText = template
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(
                                    template,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customSmsText,
                        onValueChange = { customSmsText = it },
                        label = { Text("Содержимое СМС о списании") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Введите текст СМС") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (customSmsText.isNotBlank()) {
                                onSimulateSms(customSmsText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Имитировать получение СМС")
                    }
                }
            }
        }

        // Sim parsing feedback display
        if (smsResult != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (smsResult.contains("Ошибка"))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            Color(232, 245, 233)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (smsResult.contains("Ошибка")) "Ошибка паркинга" else "Результат парсинга",
                                fontWeight = FontWeight.Bold,
                                color = if (smsResult.contains("Ошибка")) MaterialTheme.colorScheme.error else Color(46, 125, 50)
                            )

                            IconButton(onClick = onClearResult, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = smsResult,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

// --- ADD CARD DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onAddCard: (
        cardNumber: String,
        holderName: String,
        bankName: String,
        balance: Double,
        expiryDate: String,
        cardType: String,
        colorHex: String
    ) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("Т-Банк") }
    var balance by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf("MIR") }
    var selectedColorIdx by remember { mutableStateOf(0) }

    val bankOptions = listOf("Сбербанк", "Т-Банк", "Альфа-Банк", "ВТБ", "Другой")
    val cardTypeOptions = listOf("MIR", "VISA", "MASTERCARD")

    // Custom gradient palettes for cards
    val colorPalettes = listOf(
        "#2E7D32", // Green (Sberbank representation)
        "#212121", // Pitch Dark / Gold (T-bank style)
        "#D32F2F", // Red (Alfa style)
        "#0D47A1", // Blue (VTB style)
        "#7E57C2"  // Elegant Purple
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Добавление карты",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it },
                        label = { Text("Номер карты (16 цифр)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = holderName,
                        onValueChange = { holderName = it },
                        label = { Text("Имя владельца (ENG)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("IVAN IVANOV") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    // Bank Selector
                    Text("Выберите банк:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bankOptions.forEach { opt ->
                            FilterChip(
                                selected = bankName == opt,
                                onClick = {
                                    bankName = opt
                                    // Match color palette automatically index
                                    val idx = when (opt) {
                                        "Сбербанк" -> 0
                                        "Т-Банк" -> 1
                                        "Альфа-Банк" -> 2
                                        "ВТБ" -> 3
                                        else -> 4
                                    }
                                    selectedColorIdx = idx
                                },
                                label = { Text(opt) }
                            )
                        }
                    }
                }

                item {
                    // Type Selector
                    Text("Платёжная система:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cardTypeOptions.forEach { type ->
                            FilterChip(
                                selected = cardType == type,
                                onClick = { cardType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = balance,
                            onValueChange = { balance = it },
                            label = { Text("Начальный баланс") },
                            modifier = Modifier.weight(1.3f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { if (it.length <= 5) expiryDate = it },
                            label = { Text("Срок (ММ/ГГ)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("12/29") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item {
                    // Custom Color Circle Sieve
                    Text("Дизайн/Цвет карты:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorPalettes.forEachIndexed { index, hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selectedColorIdx == index) 2.5.dp else 0.dp,
                                        color = if (selectedColorIdx == index) MaterialTheme.colorScheme.outline else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIdx = index }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val balDouble = balance.toDoubleOrNull() ?: 0.0
                                val exp = if (expiryDate.isBlank()) "12/29" else expiryDate
                                val cardNo = if (cardNumber.isBlank()) "0000000000000000" else cardNumber
                                val holdName = if (holderName.isBlank()) "N/A" else holderName
                                onAddCard(
                                    cardNo,
                                    holdName,
                                    bankName,
                                    balDouble,
                                    exp,
                                    cardType,
                                    colorPalettes[selectedColorIdx]
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Создать")
                        }
                    }
                }
            }
        }
    }
}

// --- ADD TRANSACTION DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    cards: List<BankCard>,
    selectedCardId: Int?,
    onDismiss: () -> Unit,
    onAddTransaction: (
        cardId: Int?,
        amount: Double,
        category: String,
        description: String
    ) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expenseType by remember { mutableStateOf("Расход") } // "Расход" or "Доход"
    var category by remember { mutableStateOf("Продукты") }
    var selectedCardIndex by remember { mutableStateOf(0) }

    val expenseCategories = listOf("Продукты", "Кафе и рестораны", "Транспорт", "Развлечения", "Здоровье", "Красота", "Переводы", "Другое")
    val incomeCategories = listOf("Поступления", "Другое")

    // Pre-select card if passed
    LaunchedEffect(selectedCardId, cards) {
        if (selectedCardId != null) {
            val idx = cards.indexOfFirst { it.id == selectedCardId }
            if (idx >= 0) selectedCardIndex = idx
        }
    }

    val cardOptions = listOf("Наличные/Свободный") + cards.map { "${it.bankName} (*${it.cardNumber.takeLast(4)})" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Новая операция",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Expense vs Income toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Расход", "Доход").forEach { type ->
                            Button(
                                onClick = {
                                    expenseType = type
                                    category = if (type == "Расход") "Продукты" else "Поступления"
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (expenseType == type) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (expenseType == type) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(type, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Сумма (руб)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Пятерочка, Яндекс Такси и т.д.") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Card linking selector
                item {
                    Text("Выберите кошелек/карту:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cardOptions.forEachIndexed { index, title ->
                            FilterChip(
                                selected = selectedCardIndex == index,
                                onClick = { selectedCardIndex = index },
                                label = { Text(title) }
                            )
                        }
                    }
                }

                // Category selector
                item {
                    Text("Выберите категорию:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeCategories = if (expenseType == "Расход") expenseCategories else incomeCategories
                        activeCategories.forEach { cat ->
                            ElevatedFilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amtDouble = amount.toDoubleOrNull() ?: 0.0
                                val signedAmt = if (expenseType == "Расход") -amtDouble else amtDouble
                                val actualCardId = if (selectedCardIndex == 0) null else cards[selectedCardIndex - 1].id
                                val descText = if (description.isBlank()) {
                                    if (expenseType == "Расход") "Расход" else "Доход"
                                } else {
                                    description
                                }
                                onAddTransaction(
                                    actualCardId,
                                    signedAmt,
                                    category,
                                    descText
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Добавить")
                        }
                    }
                }
            }
        }
    }
}

// --- FLOW ROW FALLBACK FOR JETPACK COMPOSE FOR OLDER VERSIONS ---
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple wrapping row wrapper for our chips using custom design grid
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Put chips inside a simple scrollable row to perfectly prevent any horizontal clip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = horizontalArrangement
            ) {
                content()
            }
        }
    }
}

// --- UTILITY HELPER FUNS OR STATE PRESETS ---

fun getCategoryTonalColors(category: String, isIncome: Boolean): Pair<Color, Color> {
    if (isIncome) {
        return Pair(Color(0xFFC2F0C2), Color(0xFF072707)) // Tonal Green
    }
    return when (category) {
        "Продукты" -> Pair(Color(0xFFD3E3FD), Color(0xFF041E49)) // Tonal Blue
        "Кафе и рестораны" -> Pair(Color(0xFFFFD8E4), Color(0xFF31111D)) // Tonal Pink
        "Транспорт" -> Pair(Color(0xFFD3E3FD), Color(0xFF041E49)) // Tonal Blue
        "Развлечения" -> Pair(Color(0xFFEADDFF), Color(0xFF21005D)) // Tonal Violet
        "Здоровье" -> Pair(Color(0xFFFFD8E4), Color(0xFF31111D)) // Tonal Pink
        "Красота" -> Pair(Color(0xFFFFD8E4), Color(0xFF31111D)) // Tonal Pink
        "Переводы" -> Pair(Color(0xFFEADDFF), Color(0xFF21005D)) // Tonal Purple
        else -> Pair(Color(0xFFEADDFF), Color(0xFF21005D)) // Default Tonal
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category) {
        "Продукты" -> "🛒"
        "Кафе и рестораны" -> "☕"
        "Транспорт" -> "🚗"
        "Развлечения" -> "🎮"
        "Здоровье" -> "🩺"
        "Красота" -> "✨"
        "Переводы" -> "💸"
        "Поступления" -> "💰"
        else -> "🧾"
    }
}

fun formatPrice(value: Double): String {
    return String.format(Locale("ru", "RU"), "%,.2f ₽", value)
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Продукты" -> Color(139, 195, 74) // Light Green
        "Кафе и рестораны" -> Color(233, 30, 99) // Pink
        "Транспорт" -> Color(3, 169, 244) // Blue
        "Развлечения" -> Color(156, 39, 176) // Purple
        "Здоровье" -> Color(244, 67, 54) // Red
        "Красота" -> Color(255, 64, 129) // Deep Pink
        "Переводы" -> Color(158, 158, 158) // Grey
        "Поступления" -> Color(46, 125, 50) // Green
        else -> Color(96, 125, 139) // Slate grey
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Продукты" -> Icons.Default.ShoppingCart
        "Кафе и рестораны" -> Icons.Default.Restaurant
        "Транспорт" -> Icons.Default.DirectionsCar
        "Развлечения" -> Icons.Default.SportsEsports
        "Здоровье" -> Icons.Default.LocalHospital
        "Красота" -> Icons.Default.Face
        "Переводы" -> Icons.Default.CompareArrows
        "Поступления" -> Icons.Default.TrendingUp
        else -> Icons.Default.Category
    }
}
