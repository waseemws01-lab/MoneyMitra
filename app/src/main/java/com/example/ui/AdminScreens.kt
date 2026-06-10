package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import com.example.ui.theme.*
import com.example.data.MoneyMitraGlobalPlan
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val currentRole by viewModel.currentUserRole.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    
    // State of Firestore collections
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) } // withdrawals
    var investments by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var referrals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Tab tracking
    var selectedTab by remember { mutableStateOf("Dashboard") }
    val tabs = listOf("Dashboard", "Users Management", "Deposits", "Withdrawals", "Plans", "Notifications", "Referrals")
    
    // Dialog triggers
    var showEditUserDialog by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showAddDepositDialog by remember { mutableStateOf(false) }
    var showSendNotificationDialog by remember { mutableStateOf(false) }
    
    fun refreshData() {
        // Firestore snapshot listeners handle synchronization in real-time
    }
    
    DisposableEffect(Unit) {
        isLoading = true
        errorMessage = null
        val usersReg = firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                errorMessage = "Failed to listen to users collection: ${error.localizedMessage}"
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                users = snapshot.documents.map { doc ->
                    (doc.data ?: emptyMap<String, Any>()) + ("id" to doc.id)
                }
                isLoading = false
            }
        }
        val transReg = firestore.collection("transactions").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("AdminScreens", "Failed to listen to transactions: ${error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                transactions = snapshot.documents.map { doc ->
                    (doc.data ?: emptyMap<String, Any>()) + ("id" to doc.id)
                }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
            }
        }
        val withdrawalsReg = firestore.collection("withdrawals").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("AdminScreens", "Failed to listen to withdrawals: ${error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                bookings = snapshot.documents.map { doc ->
                    (doc.data ?: emptyMap<String, Any>()) + ("id" to doc.id)
                }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
            }
        }
        val investmentsReg = firestore.collection("investments").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("AdminScreens", "Failed to listen to investments: ${error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                investments = snapshot.documents.map { doc ->
                    (doc.data ?: emptyMap<String, Any>()) + ("id" to doc.id)
                }
            }
        }
        val notificationsReg = firestore.collection("notifications").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("AdminScreens", "Failed to listen to notifications: ${error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                notifications = snapshot.documents.map { doc ->
                    (doc.data ?: emptyMap<String, Any>()) + ("id" to doc.id)
                }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
            }
        }
        val referralsReg = firestore.collection("referrals").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("AdminScreens", "Failed to listen to referrals: ${error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                referrals = snapshot.documents.map { doc ->
                    (doc.data ?: emptyMap<String, Any>()) + ("id" to doc.id)
                }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
            }
        }
        
        onDispose {
            usersReg.remove()
            transReg.remove()
            withdrawalsReg.remove()
            investmentsReg.remove()
            notificationsReg.remove()
            referralsReg.remove()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("MoneyMitra Command Center", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MitraTextMain)
                        Text("Role: $currentRole (Authorized)", fontSize = 11.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MitraPrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = MitraBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                edgePadding = 12.dp,
                containerColor = Color.White,
                contentColor = MitraPrimaryGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                        color = MitraPrimaryGreen
                    )
                }
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { 
                            Text(
                                text = tab, 
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) MitraPrimaryGreen else MitraTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MitraPrimaryGreen)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = MitraErrorRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage!!, color = MitraTextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { refreshData() },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen)
                        ) {
                            Text("Retry Synchronization", color = Color.White)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.0f)
                ) {
                    when (selectedTab) {
                        "Dashboard" -> AdminDashboardTab(
                            users = users,
                            transactions = transactions,
                            withdrawals = bookings,
                            investments = investments,
                            referrals = referrals,
                            onNavigateToTab = { selectedTab = it }
                        )
                        "Users Management" -> AdminUsersTab(
                            users = users,
                            transactions = transactions,
                            withdrawals = bookings,
                            investments = investments,
                            currentRole = currentRole ?: "admin",
                            onStatusChanged = { refreshData() }
                        )
                        "Deposits" -> AdminDepositsTab(
                            transactions = transactions,
                            viewModel = viewModel,
                            coroutineScope = coroutineScope,
                            onAddDepositClick = { showAddDepositDialog = true },
                            onStatusChanged = { 
                                Toast.makeText(context, "Deposit transaction updated successfully", Toast.LENGTH_SHORT).show()
                                refreshData()
                            }
                        )
                        "Withdrawals" -> AdminWithdrawalsTab(
                            withdrawals = bookings,
                            firestore = firestore,
                            viewModel = viewModel,
                            coroutineScope = coroutineScope,
                            onStatusChanged = { 
                                Toast.makeText(context, "Withdrawal transaction updated successfully", Toast.LENGTH_SHORT).show()
                                refreshData() 
                            }
                        )
                        "Plans" -> AdminPlansTab(
                            investments = investments,
                            viewModel = viewModel
                        )
                        "Notifications" -> AdminNotificationsTab(
                            notifications = notifications,
                            onSendNotificationClick = { showSendNotificationDialog = true }
                        )
                        "Referrals" -> AdminReferralsTab(
                            referrals = referrals,
                            firestore = firestore,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
        }
    }

    // Edit User Dialog
    showEditUserDialog?.let { user ->
        var balanceField by remember { mutableStateOf(user["walletBalance"]?.toString() ?: "0") }
        var coinField by remember { mutableStateOf(user["coins"]?.toString() ?: "0") }
        var savingUser by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!savingUser) showEditUserDialog = null },
            title = { Text("Edit Member Wallet State", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MitraTextMain) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Uid: ${user["id"]}", fontSize = 11.sp, color = MitraTextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Name: ${user["name"] ?: "User"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MitraTextMain, modifier = Modifier.padding(bottom = 16.dp))
                    
                    OutlinedTextField(
                        value = balanceField,
                        onValueChange = { balanceField = it },
                        label = { Text("Account Balance (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("edit_balance_input")
                    )
                    
                    OutlinedTextField(
                        value = coinField,
                        onValueChange = { coinField = it },
                        label = { Text("Mitra Coins") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_coins_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBal = balanceField.toDoubleOrNull() ?: 0.0
                        val newCoins = coinField.toIntOrNull() ?: 0
                        val userId = user["id"].toString()
                        
                        savingUser = true
                        coroutineScope.launch {
                            try {
                                val oldBal = (user["walletBalance"] as? Number)?.toDouble() ?: 0.0
                                val diff = newBal - oldBal

                                firestore.collection("users").document(userId).update(
                                    mapOf(
                                        "walletBalance" to newBal,
                                        "balance" to com.google.firebase.firestore.FieldValue.delete(),
                                        "coins" to newCoins
                                    )
                                ).await()
                                
                                if (diff != 0.0) {
                                    val txType = if (diff > 0) "ADMIN_CREDIT" else "ADMIN_DEBIT"
                                    val txDesc = if (diff > 0) "Wallet balance updated by administrator (Credit)" else "Wallet balance updated by administrator (Debit)"
                                    val txData = mapOf(
                                        "transactionId" to java.util.UUID.randomUUID().toString(),
                                        "uid" to userId,
                                        "type" to txType,
                                        "amount" to java.lang.Math.abs(diff),
                                        "description" to txDesc,
                                        "status" to "SUCCESS",
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    firestore.collection("transactions").add(txData).await()
                                }
                                
                                Toast.makeText(context, "Member account updated!", Toast.LENGTH_SHORT).show()
                                showEditUserDialog = null
                                refreshData()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Update failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                savingUser = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    enabled = !savingUser,
                    modifier = Modifier.testTag("save_user_button")
                ) {
                    if (savingUser) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save Wallet State", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUserDialog = null }, enabled = !savingUser) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // Add Deposit Dialog
    if (showAddDepositDialog) {
        var selectedUserUid by remember { mutableStateOf("") }
        var depositAmt by remember { mutableStateOf("") }
        var depositLabel by remember { mutableStateOf("Manual Admin Credit") }
        var addingDeposit by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!addingDeposit) showAddDepositDialog = false },
            title = { Text("Add Manual Deposit", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MitraTextMain) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select a subscriber UID below, or type a UID manually to inject capital.", fontSize = 12.sp, color = MitraTextSecondary, modifier = Modifier.padding(bottom = 16.dp))
                    
                    OutlinedTextField(
                        value = selectedUserUid,
                        onValueChange = { selectedUserUid = it },
                        label = { Text("User UID *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("deposit_uid_input")
                    )
                    
                    OutlinedTextField(
                        value = depositAmt,
                        onValueChange = { depositAmt = it },
                        label = { Text("Deposit Amount (₹) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("deposit_amount_input")
                    )
                    
                    OutlinedTextField(
                        value = depositLabel,
                        onValueChange = { depositLabel = it },
                        label = { Text("Description / Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("deposit_label_input")
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Quick Pick Subscriber:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MitraTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(users) { usr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedUserUid = usr["id"].toString() }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .background(if (selectedUserUid == usr["id"]) MitraLightGreenBg else Color.Transparent, RoundedCornerShape(4.dp))
                                ) {
                                    Text("${usr["name"] ?: "User"} (${usr["id"].toString().take(6)}...)", fontSize = 12.sp, color = MitraTextMain)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountDoc = depositAmt.toDoubleOrNull() ?: 0.0
                        if (selectedUserUid.trim().isEmpty() || amountDoc <= 0.0) {
                            Toast.makeText(context, "Fill in UID and matching non-zero amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        addingDeposit = true
                        coroutineScope.launch {
                            try {
                                val userDocRef = firestore.collection("users").document(selectedUserUid)
                                val userDoc = userDocRef.get().await()
                                if (!userDoc.exists()) {
                                    Toast.makeText(context, "Error: User UID does not exist in users collection", Toast.LENGTH_LONG).show()
                                    addingDeposit = false
                                    return@launch
                                }
                                
                                val currentBalance = userDoc.getDouble("walletBalance") ?: 0.0
                                val updatedBalance = currentBalance + amountDoc
                                
                                // Update User Balance
                                userDocRef.update(
                                    mapOf(
                                        "walletBalance" to updatedBalance,
                                        "balance" to com.google.firebase.firestore.FieldValue.delete()
                                    )
                                ).await()
                                
                                 // Create Transaction Record
                                 val txData = mapOf(
                                     "transactionId" to java.util.UUID.randomUUID().toString(),
                                     "uid" to selectedUserUid,
                                     "type" to "ADMIN_CREDIT",
                                     "amount" to amountDoc,
                                     "description" to (if (depositLabel.isBlank()) "Admin Capital Credit" else depositLabel),
                                     "status" to "SUCCESS",
                                     "createdAt" to System.currentTimeMillis()
                                 )
                                 firestore.collection("transactions").add(txData).await()
                                
                                Toast.makeText(context, "Deposited ₹$amountDoc successfully!", Toast.LENGTH_SHORT).show()
                                showAddDepositDialog = false
                                refreshData()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Deposit failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                addingDeposit = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    enabled = !addingDeposit,
                    modifier = Modifier.testTag("submit_deposit_button")
                ) {
                    if (addingDeposit) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Add Capital", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDepositDialog = false }, enabled = !addingDeposit) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // Send Notification dialog
    if (showSendNotificationDialog) {
        var targetUid by remember { mutableStateOf("") }
        var noteTitle by remember { mutableStateOf("") }
        var noteMessage by remember { mutableStateOf("") }
        var sendingNote by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!sendingNote) showSendNotificationDialog = false },
            title = { Text("Send Notification", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MitraTextMain) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Leave target UID empty to construct a global notification broadcast, or assign a specific user.", fontSize = 12.sp, color = MitraTextSecondary, modifier = Modifier.padding(bottom = 16.dp))
                    
                    OutlinedTextField(
                        value = targetUid,
                        onValueChange = { targetUid = it },
                        label = { Text("Target User UID (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("notification_target_uid_input")
                    )
                    
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Notification Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("notification_title_input")
                    )
                    
                    OutlinedTextField(
                        value = noteMessage,
                        onValueChange = { noteMessage = it },
                        label = { Text("Notification Body *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("notification_body_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.trim().isEmpty() || noteMessage.trim().isEmpty()) {
                            Toast.makeText(context, "Fill in title and body", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        sendingNote = true
                        coroutineScope.launch {
                            try {
                                val tUid = if (targetUid.trim().isEmpty()) "broadcast" else targetUid.trim()
                                val data = mapOf(
                                    "uid" to tUid,
                                    "title" to noteTitle.trim(),
                                    "message" to noteMessage.trim(),
                                    "timestamp" to System.currentTimeMillis()
                                )
                                firestore.collection("notifications").add(data).await()
                                
                                Toast.makeText(context, "Notification sent systemwide!", Toast.LENGTH_SHORT).show()
                                showSendNotificationDialog = false
                                refreshData()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Broadcast failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                sendingNote = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    enabled = !sendingNote,
                    modifier = Modifier.testTag("submit_notification_button")
                ) {
                    if (sendingNote) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Broadcast", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendNotificationDialog = false }, enabled = !sendingNote) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun AdminDashboardTab(
    users: List<Map<String, Any>>,
    transactions: List<Map<String, Any>>,
    withdrawals: List<Map<String, Any>>,
    investments: List<Map<String, Any>>,
    referrals: List<Map<String, Any>>,
    onNavigateToTab: (String) -> Unit
) {
    val totalUsers = users.size
    val totalBalance = users.sumOf { (it["walletBalance"] as? Number)?.toDouble() ?: 0.0 }
    val totalCoins = users.sumOf { (it["coins"] as? Number)?.toInt() ?: 0 }
    
    val depositTransactions = remember(transactions) {
        transactions.filter {
            val type = it["type"]?.toString() ?: ""
            type == "DEPOSIT" || type == "ADMIN_CREDIT" || type == "CREDIT"
        }
    }
    
    val approvedDepositsSum = remember(depositTransactions) {
        depositTransactions
            .filter { (it["status"]?.toString() ?: "").uppercase() in listOf("SUCCESS", "APPROVED") }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    }
    
    val approvedDepositsCount = remember(depositTransactions) {
        depositTransactions
            .filter { (it["status"]?.toString() ?: "").uppercase() in listOf("SUCCESS", "APPROVED") }
            .size
    }
    
    val pendingDepositsCount = remember(depositTransactions) {
        depositTransactions.filter { (it["status"]?.toString() ?: "").uppercase() == "PENDING" }.size
    }
    
    val pendingDepositsSum = remember(depositTransactions) {
        depositTransactions
            .filter { (it["status"]?.toString() ?: "").uppercase() == "PENDING" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    }

    val approvedWithdrawalsSum = remember(withdrawals) {
        withdrawals
            .filter { (it["status"]?.toString() ?: "").uppercase() == "APPROVED" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    }
    
    val approvedWithdrawalsCount = remember(withdrawals) {
        withdrawals
            .filter { (it["status"]?.toString() ?: "").uppercase() == "APPROVED" }
            .size
    }
    
    val pendingWithdrawalCount = remember(withdrawals) {
        withdrawals.filter { (it["status"]?.toString() ?: "").uppercase() == "PENDING" }.size
    }
    
    val pendingWithdrawalSum = remember(withdrawals) {
        withdrawals
            .filter { (it["status"]?.toString() ?: "").uppercase() == "PENDING" }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    }

    val activeInvestmentsCount = remember(investments) {
        investments.filter { (it["status"]?.toString() ?: "").uppercase() == "ACTIVE" }.size
    }
    
    val activeInvestmentsSum = remember(investments) {
        investments
            .filter { (it["status"]?.toString() ?: "").uppercase() == "ACTIVE" }
            .sumOf { (it["investedAmount"] as? Number)?.toDouble() ?: 0.0 }
    }

    val completedReferralsCount = remember(referrals) {
        referrals.filter { (it["status"]?.toString() ?: "").uppercase() == "COMPLETED" }.size
    }
    
    val totalReferralRewardsVal = remember(referrals) {
        referrals
            .filter { (it["status"]?.toString() ?: "").uppercase() == "COMPLETED" }
            .sumOf {
                val rAmt = (it["amount"] as? Number)?.toDouble() ?: 50.0
                val rfAmt = (it["referredRewardAmount"] as? Number)?.toDouble() ?: 20.0
                rAmt + rfAmt
            }
    }

    val recentDeposits = remember(depositTransactions) {
        depositTransactions
            .sortedByDescending { it["createdAt"] as? Long ?: it["timestamp"] as? Long ?: 0L }
            .take(5)
    }
    
    val recentWithdrawals = remember(withdrawals) {
        withdrawals
            .sortedByDescending { it["createdAt"] as? Long ?: it["timestamp"] as? Long ?: 0L }
            .take(5)
    }
    
    val recentRegistrations = remember(users) {
        users
            .sortedByDescending { it["createdAt"] as? Long ?: it["timestamp"] as? Long ?: 0L }
            .take(5)
    }

    var activityTab by remember { mutableStateOf("Deposits") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Command Center Analytics",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MitraTextMain
            )
        }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Total Users & Total Deposits
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        title = "Total Users",
                        value = "$totalUsers Member${if (totalUsers != 1) "s" else ""}",
                        subtitle = "Registered Profiles",
                        icon = Icons.Default.People,
                        color = MitraPrimaryGreen,
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Users Management") }.testTag("metric_total_users")
                    )
                    DashboardMetricCard(
                        title = "Total Deposits",
                        value = "₹${String.format("%,.0f", approvedDepositsSum)}",
                        subtitle = "$approvedDepositsCount Approved",
                        icon = Icons.Default.AddCard,
                        color = MitraSuccessGreen,
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Deposits") }.testTag("metric_total_deposits")
                    )
                }
                
                // Row 2: Total Withdrawals & Total Investments
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        title = "Total Withdrawals",
                        value = "₹${String.format("%,.0f", approvedWithdrawalsSum)}",
                        subtitle = "$approvedWithdrawalsCount Approved Payouts",
                        icon = Icons.Default.CheckCircle,
                        color = MitraDeepGreen,
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Withdrawals") }.testTag("metric_total_withdrawals")
                    )
                    DashboardMetricCard(
                        title = "Total Investments",
                        value = "₹${String.format("%,.0f", activeInvestmentsSum)}",
                        subtitle = "$activeInvestmentsCount Active Plans",
                        icon = Icons.Default.ShowChart,
                        color = MitraAccentGold,
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Plans") }.testTag("metric_total_investments")
                    )
                }

                // Row 3: Total Referral Rewards & Pending Deposits
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        title = "Total Referrals",
                        value = "₹${String.format("%,.0f", totalReferralRewardsVal)}",
                        subtitle = "$completedReferralsCount Paid Invites",
                        icon = Icons.Default.Redeem,
                        color = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Referrals") }.testTag("metric_total_referrals")
                    )
                    DashboardMetricCard(
                        title = "Pending Deposits",
                        value = "₹${String.format("%,.0f", pendingDepositsSum)}",
                        subtitle = "$pendingDepositsCount Awaiting Approval",
                        icon = Icons.Default.Pending,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Deposits") }.testTag("metric_pending_deposits")
                    )
                }

                // Row 4: Pending Withdrawals & Wallet Ledger State
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard(
                        title = "Pending Withdrawals",
                        value = "₹${String.format("%,.0f", pendingWithdrawalSum)}",
                        subtitle = "$pendingWithdrawalCount Awaiting Release",
                        icon = Icons.Default.HourglassEmpty,
                        color = MitraErrorRed,
                        modifier = Modifier.weight(1f).clickable { onNavigateToTab("Withdrawals") }.testTag("metric_pending_withdrawals")
                    )
                    DashboardMetricCard(
                        title = "Ledger Balance",
                        value = "₹${String.format("%,.0f", totalBalance)}",
                        subtitle = "$totalCoins System Coins",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = MitraPrimaryGreen,
                        modifier = Modifier.weight(1f).testTag("metric_ledger_balance")
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MitraBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Latest Activity Stream",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MitraTextMain
                        )
                        Box(
                            modifier = Modifier
                                .background(MitraLightGreenBg, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Real-time updates",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraPrimaryGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activityTabs = listOf("Deposits", "Withdrawals", "Registrations")
                        activityTabs.forEach { tab ->
                            val isSelected = activityTab == tab
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MitraPrimaryGreen else Color(0xFFF1F5F9),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activityTab = tab }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                                    .weight(1f)
                                    .testTag("tab_activity_$tab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MitraTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (activityTab) {
                        "Deposits" -> {
                            if (recentDeposits.isEmpty()) {
                                Text(
                                    text = "No recent deposits recorded.",
                                    color = MitraTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    recentDeposits.forEach { dep ->
                                        RecentActivityRow(
                                            title = "₹${String.format("%,.0f", (dep["amount"] as? Number)?.toDouble() ?: 0.0)} credited",
                                            subtitle = "UID: ${dep["uid"]?.toString()?.take(10) ?: "Unknown"}...",
                                            timestamp = dep["createdAt"] as? Long ?: dep["timestamp"] as? Long ?: 0L,
                                            status = (dep["status"] as? String) ?: "PENDING",
                                            icon = Icons.Default.Add,
                                            iconBgColor = Color(0xFFE8F5E9),
                                            iconColor = MitraPrimaryGreen
                                        )
                                    }
                                }
                            }
                        }
                        "Withdrawals" -> {
                            if (recentWithdrawals.isEmpty()) {
                                Text(
                                    text = "No recent withdrawals requested.",
                                    color = MitraTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    recentWithdrawals.forEach { wth ->
                                        val status = (wth["status"] as? String) ?: "PENDING"
                                        RecentActivityRow(
                                            title = "₹${String.format("%,.0f", (wth["amount"] as? Number)?.toDouble() ?: 0.0)} payout",
                                            subtitle = "${wth["bankName"] ?: "Bank"} to UID: ${wth["uid"]?.toString()?.take(8) ?: "..."}",
                                            timestamp = wth["createdAt"] as? Long ?: wth["timestamp"] as? Long ?: 0L,
                                            status = status,
                                            icon = Icons.Default.Remove,
                                            iconBgColor = if (status == "APPROVED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            iconColor = if (status == "APPROVED") MitraSuccessGreen else MitraErrorRed
                                        )
                                    }
                                }
                            }
                        }
                        "Registrations" -> {
                            if (recentRegistrations.isEmpty()) {
                                Text(
                                    text = "No recent member registrations.",
                                    color = MitraTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    recentRegistrations.forEach { usr ->
                                        val phone = usr["emailOrPhone"]?.toString() ?: usr["phone"]?.toString() ?: "No contact info"
                                        RecentActivityRow(
                                            title = usr["name"]?.toString()?.ifBlank { "Anonymous user" } ?: "New Member",
                                            subtitle = phone,
                                            timestamp = usr["createdAt"] as? Long ?: usr["timestamp"] as? Long ?: 0L,
                                            status = "Joined",
                                            icon = Icons.Default.Person,
                                            iconBgColor = Color(0xFFE1F5FE),
                                            iconColor = Color(0xFF0288D1)
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
}

@Composable
fun RecentActivityRow(
    title: String,
    subtitle: String,
    timestamp: Long,
    status: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MitraTextMain
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MitraTextSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val statusColor = when (status.uppercase()) {
                "SUCCESS", "APPROVED", "COMPLETED", "JOINED" -> MitraSuccessGreen
                "PENDING" -> MitraAccentGold
                else -> MitraErrorRed
            }
            Box(
                modifier = Modifier
                    .background(statusColor.copy(0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatActivityTime(timestamp),
                fontSize = 10.sp,
                color = MitraTextSecondary
            )
        }
    }
}

fun formatActivityTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Just now"
    return try {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Just now"
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MitraTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(color.copy(0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MitraTextMain,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MitraTextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersTab(
    users: List<Map<String, Any>>,
    transactions: List<Map<String, Any>>,
    withdrawals: List<Map<String, Any>>,
    investments: List<Map<String, Any>>,
    currentRole: String,
    onStatusChanged: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Aggregate overall users metrics for Users Management Dashboard Cards
    val totalUsersCount = users.size
    val activeUsersCount = users.count { !(it["blocked"] as? Boolean ?: false) && (it["status"]?.toString() ?: "").uppercase() != "BLOCKED" }
    val totalWalletBalanceSum = users.sumOf { (it["walletBalance"] as? Number)?.toDouble() ?: 0.0 }
    
    val activeInvestmentsVolume = investments.filter { (it["status"]?.toString() ?: "").uppercase() == "ACTIVE" }.sumOf { (it["investedAmount"] as? Number)?.toDouble() ?: 0.0 }
    val activePlansCount = investments.count { (it["status"]?.toString() ?: "").uppercase() == "ACTIVE" }
    
    val totalApprovedDepositsSum = transactions.filter { 
        val type = it["type"]?.toString() ?: ""
        (it["status"]?.toString() ?: "").uppercase() in listOf("SUCCESS", "APPROVED") && (type == "DEPOSIT" || type == "ADMIN_CREDIT" || type == "CREDIT") 
    }.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    val approvedDepositsCount = transactions.count { 
        val type = it["type"]?.toString() ?: ""
        (it["status"]?.toString() ?: "").uppercase() in listOf("SUCCESS", "APPROVED") && (type == "DEPOSIT" || type == "ADMIN_CREDIT" || type == "CREDIT") 
    }

    val totalApprovedWithdrawalsSum = withdrawals.filter { (it["status"]?.toString() ?: "").uppercase() == "APPROVED" }.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
    val approvedWithdrawalsCount = withdrawals.count { (it["status"]?.toString() ?: "").uppercase() == "APPROVED" }

    // Helpers to parse registration date and mask accountNumber
    fun formatRegDate(timestamp: Any?): String {
        val ms = when (timestamp) {
            is Number -> timestamp.toLong()
            is String -> timestamp.toLongOrNull() ?: 0L
            else -> 0L
        }
        if (ms == 0L) return "N/A"
        return java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(ms))
    }

    fun maskAccNumber(accountNumber: Any?): String {
        val accStr = accountNumber?.toString() ?: ""
        if (accStr.isBlank()) return "N/A"
        return if (accStr.length > 4) {
            "•".repeat(accStr.length - 4) + accStr.substring(accStr.length - 4)
        } else {
            accStr
        }
    }

    // Derive live selected user data dynamically
    val liveSelectedUser = remember(users, selectedUserForDetails) {
        derivedStateOf {
            if (selectedUserForDetails == null) null else {
                users.find { it["id"]?.toString() == selectedUserForDetails!!["id"]?.toString() } ?: selectedUserForDetails
            }
        }
    }

    if (liveSelectedUser.value != null) {
        // --- 1. USER DETAILS VIEW ---
        val user = liveSelectedUser.value!!
        val userId = user["id"]?.toString() ?: ""
        
        var historyFilterTab by remember { mutableStateOf("Deposits") }

        // Setup unified history transactions for this specific user
        val userHistoryItems = remember(transactions, withdrawals, investments, userId) {
            val items = mutableListOf<AdminUserHistoryItem>()
            
            // Add native transactions
            transactions.filter { it["uid"]?.toString() == userId }.forEach { tx ->
                val amount = (tx["amount"] as? Number)?.toDouble() ?: 0.0
                val typeStr = tx["type"]?.toString() ?: ""
                val status = tx["status"]?.toString() ?: "SUCCESS"
                val timestamp = (tx["createdAt"] as? Number)?.toLong() ?: (tx["timestamp"] as? Number)?.toLong() ?: 0L
                val description = tx["description"]?.toString() ?: ""
                
                val category = when (typeStr) {
                    "DEPOSIT", "ADMIN_CREDIT", "CREDIT" -> "DEPOSIT"
                    "WITHDRAWAL", "ADMIN_DEBIT", "DEBIT" -> "WITHRAWAL"
                    "INVESTMENT_PURCHASE", "INVESTMENT_RETURN", "INVESTMENT_EARNING" -> "INVESTMENT"
                    "COIN_CHECKIN", "COIN_VIDEO", "COIN_REDEEM", "COIN_REDEMPTION" -> "COIN"
                    "REFERRAL_BONUS", "REFERRAL_REWARD" -> "REFERRAL"
                    else -> "OTHER"
                }
                
                val displayType = when (typeStr) {
                    "DEPOSIT" -> "Deposit"
                    "ADMIN_CREDIT" -> "Admin Credit"
                    "COIN_REDEMPTION", "COIN_REDEEM" -> "Coin Redeem"
                    "COIN_CHECKIN" -> "Daily Check-In Reward"
                    "COIN_VIDEO" -> "Watch Video Earn"
                    "REFERRAL_BONUS", "REFERRAL_REWARD" -> "Referral Reward"
                    "INVESTMENT_PURCHASE" -> "Investment Purchase"
                    "INVESTMENT_RETURN", "INVESTMENT_EARNING" -> "Investment Return"
                    "ADMIN_DEBIT" -> "Admin Debit"
                    "WITHDRAWAL" -> "Withdrawal"
                    else -> typeStr.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
                }
                
                items.add(
                    AdminUserHistoryItem(
                        dateMs = timestamp,
                        amount = amount,
                        type = displayType,
                        category = category,
                        status = status,
                        description = description
                    )
                )
            }
            
            // Add separate withdrawals entries (preventing duplicate timestamp overlaps)
            withdrawals.filter { it["uid"]?.toString() == userId }.forEach { wd ->
                val amount = (wd["amount"] as? Number)?.toDouble() ?: 0.0
                val status = wd["status"]?.toString() ?: "PENDING"
                val timestamp = (wd["createdAt"] as? Number)?.toLong() ?: (wd["timestamp"] as? Number)?.toLong() ?: 0L
                val bank = wd["bankName"]?.toString() ?: wd["bank"]?.toString() ?: "Bank Out"
                
                val exists = items.any { it.category == "WITHDRAWAL" && Math.abs(it.amount - amount) < 0.02 && Math.abs(it.dateMs - timestamp) < 10000 }
                if (!exists) {
                    items.add(
                        AdminUserHistoryItem(
                            dateMs = timestamp,
                            amount = amount,
                            type = "Withdrawal Out",
                            category = "WITHDRAWAL",
                            status = status,
                            description = "Payout via Bank/UPI: $bank"
                        )
                    )
                }
            }
            
            // Add investments entries
            investments.filter { it["uid"]?.toString() == userId }.forEach { inv ->
                val amount = (inv["investedAmount"] as? Number)?.toDouble() ?: 0.0
                val status = inv["status"]?.toString() ?: "ACTIVE"
                val name = inv["name"]?.toString() ?: "Investment Plan"
                val timestamp = (inv["createdAt"] as? Number)?.toLong() ?: 0L
                
                val exists = items.any { it.category == "INVESTMENT" && Math.abs(it.amount - amount) < 0.02 && Math.abs(it.dateMs - timestamp) < 10000 }
                if (!exists) {
                    items.add(
                        AdminUserHistoryItem(
                            dateMs = timestamp,
                            amount = amount,
                            type = "Plan Purchase",
                            category = "INVESTMENT",
                            status = status,
                            description = "Investment Plan: $name"
                        )
                    )
                }
            }
            
            items.sortedByDescending { it.dateMs }
        }

        // Filter operations history inside user details based on filter selection
        val filteredHistoryItems = remember(userHistoryItems, historyFilterTab) {
            val cat = when (historyFilterTab) {
                "Deposits" -> "DEPOSIT"
                "Withdrawals" -> "WITHDRAWAL"
                "Investments" -> "INVESTMENT"
                "Coin Redemptions" -> "COIN"
                "Referral Rewards" -> "REFERRAL"
                else -> "DEPOSIT"
            }
            userHistoryItems.filter { it.category == cat }
        }

        // Calculate direct derived metrics for the specific selected user details
        val userApprovedDepositsVal = transactions.filter { it["uid"]?.toString() == userId && (it["status"]?.toString() ?: "").uppercase() in listOf("SUCCESS", "APPROVED") && (it["type"]?.toString() ?: "") == "DEPOSIT" }.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
        val userApprovedWithdrawalsVal = withdrawals.filter { it["uid"]?.toString() == userId && (it["status"]?.toString() ?: "").uppercase() == "APPROVED" }.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
        
        val userLifetimeInvestEarnings = investments.filter { it["uid"]?.toString() == userId }.sumOf { (it["totalEarnings"] as? Number)?.toDouble() ?: 0.0 }
        val userActiveInvestmentPlan = investments.find { it["uid"]?.toString() == userId && (it["status"]?.toString() ?: "").uppercase() == "ACTIVE" }

        val isBlocked = user["blocked"] as? Boolean ?: false || (user["status"]?.toString() ?: "").uppercase() == "BLOCKED"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = { selectedUserForDetails = null },
                    modifier = Modifier
                        .background(MitraLightGreenBg, CircleShape)
                        .size(40.dp)
                        .testTag("back_to_users_list_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MitraPrimaryGreen)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "User Profile Details",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MitraTextMain
                    )
                    Text(
                        text = user["name"]?.toString() ?: "Member Account",
                        fontSize = 13.sp,
                        color = MitraTextSecondary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Basic Profile Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Basic Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraPrimaryGreen)
                                // Status Pill
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isBlocked) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBlocked) "Blocked" else "Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isBlocked) Color(0xFFEF4444) else MitraSuccessGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ProfileDetailRow(label = "Full Name", value = user["name"]?.toString() ?: "N/A")
                            ProfileDetailRow(label = "Mobile Number", value = user["phoneNumber"]?.toString() ?: user["emailOrPhone"]?.toString() ?: "N/A")
                            ProfileDetailRow(label = "Email Address", value = user["email"]?.toString() ?: "N/A")
                            ProfileDetailRow(label = "User ID", value = userId)
                            ProfileDetailRow(label = "Registration Date", value = formatRegDate(user["createdAt"] ?: user["timestamp"]))
                        }
                    }
                }

                // Section 2: Wallet Balances & Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Wallet Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraPrimaryGreen, modifier = Modifier.padding(bottom = 12.dp))
                            
                            ProfileDetailRow(
                                label = "Available Balance", 
                                value = "₹${String.format(java.util.Locale.US, "%,.2f", (user["walletBalance"] as? Number)?.toDouble() ?: 0.0)}",
                                isBoldValue = true,
                                valueColor = MitraSuccessGreen
                            )
                            ProfileDetailRow(
                                label = "Locked Balance", 
                                value = "₹${String.format(java.util.Locale.US, "%,.2f", (user["lockedBalance"] as? Number)?.toDouble() ?: 0.0)}",
                                valueColor = Color(0xFFF59E0B)
                            )
                            ProfileDetailRow(
                                label = "Total Approved Deposits", 
                                value = "₹${String.format(java.util.Locale.US, "%,.2f", userApprovedDepositsVal)}"
                            )
                            ProfileDetailRow(
                                label = "Total Approved Withdrawals", 
                                value = "₹${String.format(java.util.Locale.US, "%,.2f", userApprovedWithdrawalsVal)}"
                            )
                            ProfileDetailRow(
                                label = "Total Referral Earnings", 
                                value = "₹${String.format(java.util.Locale.US, "%,.2f", (user["referralEarnings"] as? Number)?.toDouble() ?: 0.0)}"
                            )
                            ProfileDetailRow(
                                label = "Coin Wallet Balance", 
                                value = "${user["coins"] ?: 0} Coins",
                                valueColor = MitraAccentGold,
                                isBoldValue = true
                            )
                        }
                    }
                }

                // Section 3: Investment Information
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Investment Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraPrimaryGreen, modifier = Modifier.padding(bottom = 12.dp))
                            
                            if (userActiveInvestmentPlan != null) {
                                ProfileDetailRow(label = "Active Plan", value = userActiveInvestmentPlan["name"]?.toString() ?: "Investment Plan", isBoldValue = true, valueColor = MitraPrimaryGreen)
                                ProfileDetailRow(label = "Invested Amount", value = "₹${String.format(java.util.Locale.US, "%,.2f", (userActiveInvestmentPlan["investedAmount"] as? Number)?.toDouble() ?: 0.0)}")
                                ProfileDetailRow(label = "Daily Returns", value = "₹${String.format(java.util.Locale.US, "%,.2f", (userActiveInvestmentPlan["dailyEarnings"] as? Number)?.toDouble() ?: 0.0)}")
                                ProfileDetailRow(label = "Activation Date", value = userActiveInvestmentPlan["activationDate"]?.toString() ?: "N/A")
                                ProfileDetailRow(label = "Maturity Date", value = userActiveInvestmentPlan["expiryDate"]?.toString() ?: "N/A")
                            } else {
                                ProfileDetailRow(label = "Active Investment", value = "No Active Investment Plan")
                            }
                            Divider(color = MitraBorder.copy(0.5f), modifier = Modifier.padding(vertical = 10.dp))
                            ProfileDetailRow(label = "Total Investment Plans", value = investments.count { it["uid"]?.toString() == userId }.toString())
                            ProfileDetailRow(label = "Lifetime Investment Returns", value = "₹${String.format(java.util.Locale.US, "%,.2f", userLifetimeInvestEarnings)}", isBoldValue = true, valueColor = MitraPrimaryGreen)
                        }
                    }
                }

                // Section 4: Referral Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Referral Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraPrimaryGreen, modifier = Modifier.padding(bottom = 12.dp))
                            
                            ProfileDetailRow(label = "My Referral Code", value = user["referralCode"]?.toString() ?: "N/A", isBoldValue = true)
                            ProfileDetailRow(label = "Total Referrals Count", value = "${user["totalReferrals"] ?: 0} users")
                            ProfileDetailRow(label = "Referral Commission Earned", value = "₹${String.format(java.util.Locale.US, "%,.2f", (user["referralEarnings"] as? Number)?.toDouble() ?: 0.0)}", isBoldValue = true, valueColor = MitraPrimaryGreen)
                        }
                    }
                }

                // Section 5: Bank Details Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bank Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraPrimaryGreen, modifier = Modifier.padding(bottom = 12.dp))
                            
                            ProfileDetailRow(label = "Account Holder", value = user["accountHolderName"]?.toString() ?: "N/A")
                            ProfileDetailRow(label = "Bank Name", value = user["bankName"]?.toString() ?: "N/A")
                            ProfileDetailRow(label = "Account Number", value = maskAccNumber(user["accountNumber"]))
                            ProfileDetailRow(label = "IFSC Code", value = user["ifscCode"]?.toString() ?: "N/A")
                            ProfileDetailRow(label = "UPI ID", value = user["upiId"]?.toString() ?: "N/A")
                        }
                    }
                }

                // Section 6: Security and Account Controls (Block/Unblock)
                // - Do NOT allow editing balance or investments as requested
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Account Security & Controls", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp, 
                                color = MitraTextMain, 
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                "Locked/Blocked members are instantly locked out and restricted from depositing, withdrawing, investing, and redeeming coins.", 
                                fontSize = 11.sp, 
                                color = MitraTextSecondary, 
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                            
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            firestore.collection("users").document(userId).update(
                                                mapOf(
                                                    "blocked" to !isBlocked,
                                                    "status" to if (isBlocked) "Active" else "Blocked"
                                                )
                                            ).await()
                                            Toast.makeText(context, if (isBlocked) "Account Unblocked!" else "Account BLOCKED!", Toast.LENGTH_SHORT).show()
                                            onStatusChanged()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Operation failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBlocked) MitraPrimaryGreen else Color(0xFFDC2626)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("block_unblock_user_action_button")
                            ) {
                                Icon(
                                    imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block, 
                                    contentDescription = "Status lock",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBlocked) "Unblock Member Account" else "Block Member Account", 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Section 6.5: Super Admin Test Reset
                if (currentRole == "super_admin") {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Reset Test Data",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE11D48),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = "Allow resetting a test user account to a clean state for end-to-end testing. This will set all financial and coin balances to 0, and clear all history entries.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9F1239),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Button(
                                    onClick = { showResetConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("reset_test_data_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reset Test Data",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (showResetConfirmDialog) {
                    item {
                        AlertDialog(
                            onDismissRequest = { showResetConfirmDialog = false },
                            title = {
                                Text(
                                    text = "Confirm Reset",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MitraTextMain
                                )
                            },
                            text = {
                                Text(
                                    text = "Reset all test data for this user?\n\nThis action cannot be undone.",
                                    fontSize = 14.sp,
                                    color = MitraTextMain
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                // Delete all transactions (Deposits, wallet history, coin earnings, coin redemptions) for this user
                                                val txQuery = firestore.collection("transactions").whereEqualTo("uid", userId).get().await()
                                                txQuery.documents.forEach { doc ->
                                                    doc.reference.delete().await()
                                                }

                                                // Delete all withdrawals for this user
                                                val wdQuery = firestore.collection("withdrawals").whereEqualTo("uid", userId).get().await()
                                                wdQuery.documents.forEach { doc ->
                                                    doc.reference.delete().await()
                                                }

                                                // Delete all investments for this user
                                                val invQuery = firestore.collection("investments").whereEqualTo("uid", userId).get().await()
                                                invQuery.documents.forEach { doc ->
                                                    doc.reference.delete().await()
                                                }

                                                // Delete all notifications for this user
                                                val notifQuery = firestore.collection("notifications").whereEqualTo("uid", userId).get().await()
                                                notifQuery.documents.forEach { doc ->
                                                    doc.reference.delete().await()
                                                }

                                                // Reset balances in user document
                                                firestore.collection("users").document(userId).update(
                                                    mapOf(
                                                        "walletBalance" to 0.0,
                                                        "balance" to 0.0,
                                                        "lockedBalance" to 0.0,
                                                        "investedAmount" to 0.0,
                                                        "earnings" to 0.0,
                                                        "totalEarnings" to 0.0,
                                                        "coins" to 0,
                                                        "coinBalance" to 0,
                                                        "totalCoinsEarned" to 0,
                                                        "totalCoinsRedeemed" to 0,
                                                        "activePlan" to null,
                                                        "activeInvestmentId" to null,
                                                        "investmentStartDate" to null,
                                                        "investmentEndDate" to null,
                                                        "daysRemaining" to 0,
                                                        "currentValue" to 0.0,
                                                        "lastCheckInDate" to "",
                                                        "lastVideoResetDate" to "",
                                                        "videosWatchedToday" to 0,
                                                        "dailyCoinsEarned" to 0
                                                    )
                                                ).await()

                                                Toast.makeText(context, "Test data reset successfully.", Toast.LENGTH_LONG).show()
                                                showResetConfirmDialog = false
                                                onStatusChanged()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Reset failed: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                    modifier = Modifier.testTag("admin_reset_confirm_button")
                                ) {
                                    Text("Reset", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showResetConfirmDialog = false },
                                    modifier = Modifier.testTag("admin_reset_cancel_button")
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Section 7: User Transaction History with Filters
                item {
                    Text(
                        "Member Transactions History", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 15.sp, 
                        color = MitraTextMain,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // Filters Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val historyTabs = listOf("Deposits", "Withdrawals", "Investments", "Coin Redemptions", "Referral Rewards")
                        historyTabs.forEach { tab ->
                            val isSelected = tab == historyFilterTab
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MitraPrimaryGreen else Color.White,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSelected) MitraPrimaryGreen else MitraBorder, RoundedCornerShape(8.dp))
                                    .clickable { historyFilterTab = tab }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else MitraTextSecondary
                                )
                            }
                        }
                    }
                }

                // Dynamic Transactions list
                if (filteredHistoryItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp)
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions recorded for this category", 
                                color = MitraTextSecondary, 
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    items(filteredHistoryItems) { hItem ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hItem.type,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MitraTextMain
                                    )
                                    Text(
                                        text = hItem.description,
                                        fontSize = 11.sp,
                                        color = MitraTextSecondary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(hItem.dateMs)),
                                        fontSize = 10.sp,
                                        color = MitraTextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format(java.util.Locale.US, "%,.2f", hItem.amount)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = when (hItem.category) {
                                            "DEPOSIT", "REFERRAL" -> MitraSuccessGreen
                                            "WITHDRAWAL", "INVESTMENT" -> Color(0xFFEF4444)
                                            else -> MitraTextMain
                                        }
                                    )
                                    // Status
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(
                                                when (hItem.status.uppercase()) {
                                                    "SUCCESS", "APPROVED" -> Color(0xFFDCFCE7)
                                                    "PENDING" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFFEE2E2)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = hItem.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (hItem.status.uppercase()) {
                                                "SUCCESS", "APPROVED" -> MitraSuccessGreen
                                                "PENDING" -> Color(0xFFD97706)
                                                else -> Color(0xFFDC2626)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    } else {
        // --- 2. LIST VIEW & DASHBOARD METRICS ---
        val filteredUsers = remember(users, searchQuery) {
            if (searchQuery.isBlank()) users else {
                users.filter { usr ->
                    val name = usr["name"]?.toString()?.lowercase() ?: ""
                    val phone = usr["phoneNumber"]?.toString()?.lowercase() ?: ""
                    val emailOrPhone = usr["emailOrPhone"]?.toString()?.lowercase() ?: ""
                    val email = usr["email"]?.toString()?.lowercase() ?: ""
                    val id = usr["id"]?.toString()?.lowercase() ?: ""
                    
                    name.contains(searchQuery.lowercase()) || 
                    phone.contains(searchQuery.lowercase()) || 
                    emailOrPhone.contains(searchQuery.lowercase()) || 
                    email.contains(searchQuery.lowercase()) ||
                    id.contains(searchQuery.lowercase())
                }
            }
        }

        var adminPageSize by remember { mutableStateOf(20) }
        val paginatedUsers = remember(filteredUsers, adminPageSize) {
            filteredUsers.take(adminPageSize)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Dashboard Metrics Grid
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Main Dashboard Panel Title
                item {
                    Text(
                        "Users Audit Command Center", 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 18.sp, 
                        color = MitraTextMain
                    )
                }

                // Grid 2 Columns of Dashboard Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardMetricCard(
                                title = "Total Users",
                                value = "$totalUsersCount Members",
                                subtitle = "Registered",
                                icon = Icons.Default.Group,
                                color = MitraPrimaryGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = "Active Users",
                                value = "$activeUsersCount Members",
                                subtitle = "Unrestricted",
                                icon = Icons.Default.CheckCircle,
                                color = MitraSuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardMetricCard(
                                title = "Total Wallet Balance",
                                value = "₹${String.format(java.util.Locale.US, "%,.0f", totalWalletBalanceSum)}",
                                subtitle = "Total Held Cash",
                                icon = Icons.Default.AccountBalanceWallet,
                                color = MitraPrimaryGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = "Total Active Investments",
                                value = "₹${String.format(java.util.Locale.US, "%,.0f", activeInvestmentsVolume)}",
                                subtitle = "$activePlansCount Active Plans",
                                icon = Icons.Default.ShowChart,
                                color = MitraAccentGold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardMetricCard(
                                title = "Total Approved Deposits",
                                value = "₹${String.format(java.util.Locale.US, "%,.0f", totalApprovedDepositsSum)}",
                                subtitle = "$approvedDepositsCount Payments Successful",
                                icon = Icons.Default.TrendingUp,
                                color = MitraSuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = "Total Withdrawals",
                                value = "₹${String.format(java.util.Locale.US, "%,.0f", totalApprovedWithdrawalsSum)}",
                                subtitle = "$approvedWithdrawalsCount Paid Out",
                                icon = Icons.Default.Payment,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Header for Search & List
                item {
                    Divider(color = MitraBorder.copy(0.6f), modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Registered Members Registry", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 15.sp, 
                        color = MitraTextMain,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by: Name, Phone or User ID...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Users") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = MitraPrimaryGreen,
                            unfocusedIndicatorColor = MitraBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_search_input")
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Displaying ${filteredUsers.size} of $totalUsersCount Users", 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 12.sp, 
                        color = MitraTextSecondary
                    )
                }

                // Users Paginated List
                items(paginatedUsers) { usr ->
                    val uid = usr["id"]?.toString() ?: ""
                    val isBlockedUser = usr["blocked"] as? Boolean ?: false || (usr["status"]?.toString() ?: "").uppercase() == "BLOCKED"
                    val phone = usr["phoneNumber"]?.toString() ?: usr["emailOrPhone"]?.toString() ?: "No Number"
                    val regDate = usr["createdAt"] ?: usr["timestamp"]

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        onClick = { selectedUserForDetails = usr },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = usr["name"]?.toString() ?: "No Name", 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 15.sp, 
                                        color = MitraTextMain
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Status tag inside row
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isBlockedUser) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isBlockedUser) "Blocked" else "Active",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = if (isBlockedUser) Color(0xFFEF4444) else MitraSuccessGreen
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ID: $uid", fontSize = 11.sp, color = MitraTextSecondary, fontFamily = FontFamily.Monospace)
                                Text("Phone: $phone", fontSize = 12.sp, color = MitraTextSecondary)
                                Text("Registered: ${formatRegDate(regDate)}", fontSize = 11.sp, color = MitraTextSecondary)
                            }
                            
                            IconButton(
                                onClick = { selectedUserForDetails = usr },
                                modifier = Modifier
                                    .background(MitraLightGreenBg, CircleShape)
                                    .size(36.dp)
                                    .testTag("view_user_details_button_$uid")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight, 
                                    contentDescription = "View Details", 
                                    tint = MitraPrimaryGreen, 
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Show Load More
                if (filteredUsers.size > adminPageSize) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { adminPageSize += 20 },
                                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                modifier = Modifier.testTag("load_more_admin_users")
                            ) {
                                Text("Load More Users", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    label: String, 
    value: String, 
    isBoldValue: Boolean = false, 
    valueColor: Color = MitraTextMain
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            fontSize = 12.sp, 
            color = MitraTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value, 
            fontSize = 12.sp, 
            color = valueColor, 
            fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}

data class AdminUserHistoryItem(
    val dateMs: Long,
    val amount: Double,
    val type: String,
    val category: String,
    val status: String,
    val description: String
)

@Composable
fun AdminDepositsTab(
    transactions: List<Map<String, Any>>,
    viewModel: MoneyMitraViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onAddDepositClick: () -> Unit,
    onStatusChanged: () -> Unit
) {
    var actingId by remember { mutableStateOf<String?>(null) }

    val depositTransactions = remember(transactions) {
        transactions.filter {
            val type = it["type"]?.toString() ?: ""
            type == "DEPOSIT" || type == "ADMIN_CREDIT" || type == "CREDIT"
        }
    }

    val pendingDeposits = remember(depositTransactions) {
        depositTransactions.filter { (it["status"]?.toString() ?: "").uppercase() == "PENDING" }
    }

    val completedDeposits = remember(depositTransactions) {
        depositTransactions.filter { (it["status"]?.toString() ?: "").uppercase() != "PENDING" }
    }

    var completedDepositsPageSize by remember { mutableStateOf(20) }
    val paginatedCompletedDeposits = remember(completedDeposits, completedDepositsPageSize) {
        completedDeposits.take(completedDepositsPageSize)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Deposit Management", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MitraTextMain)
                Button(
                    onClick = onAddDepositClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_deposit_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Deposit", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manual Credit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (pendingDeposits.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Deposit Requests (${pendingDeposits.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(pendingDeposits) { tx ->
                        val txId = tx["id"]?.toString() ?: ""
                        val amount = (tx["amount"] as? Number)?.toDouble() ?: 0.0
                        val uid = tx["uid"]?.toString() ?: "Unknown"
                        val desc = tx["description"]?.toString() ?: "Wallet Deposit"
                        val isProcessing = actingId == txId

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(desc, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)
                                        Text("User UID: $uid", fontSize = 11.sp, color = MitraTextSecondary)
                                        val createdAt = tx["createdAt"] as? Long ?: tx["timestamp"] as? Long ?: 0L
                                        val dateStr = if (createdAt > 0L) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(createdAt))
                                            } catch (e: Exception) { "Just now" }
                                        } else {
                                            "Just now"
                                        }
                                        Text(dateStr, fontSize = 11.sp, color = MitraTextSecondary)
                                    }
                                    Text(
                                        text = "₹${String.format("%,.2f", amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (txId.isNotEmpty() && !isProcessing) {
                                                actingId = txId
                                                viewModel.approveDeposit(txId) {
                                                    actingId = null
                                                    onStatusChanged()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        enabled = !isProcessing
                                    ) {
                                        if (isProcessing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                                        } else {
                                            Text("Approve", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (txId.isNotEmpty() && !isProcessing) {
                                                actingId = txId
                                                viewModel.rejectDeposit(txId) {
                                                    actingId = null
                                                    onStatusChanged()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        enabled = !isProcessing
                                    ) {
                                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Completed & Past Deposits",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraTextMain,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                if (completedDeposits.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No recorded completed deposits.", color = MitraTextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(paginatedCompletedDeposits) { tx ->
                        val amount = (tx["amount"] as? Number)?.toDouble() ?: 0.0
                        val status = tx["status"]?.toString() ?: "SUCCESS"
                        val uid = tx["uid"]?.toString() ?: "Unknown"
                        val desc = tx["description"]?.toString() ?: "Fund Deposit"
                        
                        val statusColor = when (status.uppercase()) {
                            "SUCCESS", "COMPLETED", "APPROVED" -> MitraPrimaryGreen
                            "REJECTED", "FAILED" -> Color(0xFFDC2626)
                            else -> MitraTextSecondary
                        }
                        
                        val statusBg = when (status.uppercase()) {
                            "SUCCESS", "COMPLETED", "APPROVED" -> Color(0xFFDCFCE7)
                            "REJECTED", "FAILED" -> Color(0xFFFEE2E2)
                            else -> Color(0xFFF1F5F9)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(desc, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)
                                    Text("User ID: $uid", fontSize = 11.sp, color = MitraTextSecondary)
                                    val createdAt = tx["createdAt"] as? Long ?: tx["timestamp"] as? Long ?: 0L
                                    val dateStr = if (createdAt > 0L) {
                                        try {
                                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                            sdf.format(java.util.Date(createdAt))
                                        } catch (e: Exception) { "Just now" }
                                    } else {
                                        "Just now"
                                    }
                                    Text(dateStr, fontSize = 11.sp, color = MitraTextSecondary)
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format("%,.2f", amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (status.uppercase() in listOf("SUCCESS", "COMPLETED", "APPROVED")) MitraPrimaryGreen else Color.Gray,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(statusBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = status.uppercase(),
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (completedDeposits.size > completedDepositsPageSize) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { completedDepositsPageSize += 20 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                    modifier = Modifier.testTag("load_more_admin_deposits")
                                ) {
                                    Text("Load More", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWithdrawalsTab(
    withdrawals: List<Map<String, Any>>,
    firestore: FirebaseFirestore,
    viewModel: MoneyMitraViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onStatusChanged: () -> Unit
) {
    var actingId by remember { mutableStateOf<String?>(null) }
    
    var adminWithdrawalsPageSize by remember { mutableStateOf(20) }
    val paginatedWithdrawals = remember(withdrawals, adminWithdrawalsPageSize) {
        withdrawals.take(adminWithdrawalsPageSize)
    }
    
    if (withdrawals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No withdrawals requested in history.", color = MitraTextSecondary, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(paginatedWithdrawals) { wr ->
                val status = wr["status"]?.toString() ?: "PENDING"
                val withdrawId = wr["id"].toString()
                val uid = wr["uid"]?.toString() ?: ""
                val amount = (wr["amount"] as? Number)?.toDouble() ?: 0.0
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Payout Request: ₹${String.format("%,.2f", amount)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MitraTextMain)
                                val bankText = wr["bankName"]?.toString() ?: wr["bank"]?.toString() ?: "Bank"
                                Text("Account: $bankText", fontSize = 12.sp, color = MitraTextSecondary)
                                Text("UID: $uid", fontSize = 11.sp, color = MitraTextSecondary)
                            }
                            
                            val tagColor = when (status) {
                                "PENDING" -> MitraAccentGold
                                "APPROVED" -> MitraSuccessGreen
                                else -> MitraErrorRed
                            }
                            Box(
                                modifier = Modifier
                                    .background(tagColor.copy(0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(status, color = tagColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        
                        if (status == "PENDING") {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (actingId == withdrawId) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MitraPrimaryGreen)
                                } else {
                                    Button(
                                        onClick = {
                                            actingId = withdrawId
                                            coroutineScope.launch {
                                                try {
                                                    viewModel.approveWithdrawal(withdrawId)
                                                    onStatusChanged()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                } finally {
                                                    actingId = null
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MitraSuccessGreen),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp).testTag("approve_btn_$withdrawId")
                                    ) {
                                        Text("Approve Payout", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(10.dp))
                                    
                                    Button(
                                        onClick = {
                                            actingId = withdrawId
                                            coroutineScope.launch {
                                                try {
                                                    viewModel.rejectWithdrawal(withdrawId)
                                                    onStatusChanged()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                } finally {
                                                    actingId = null
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MitraErrorRed),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp).testTag("reject_btn_$withdrawId")
                                    ) {
                                        Text("Reject & Refund", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (withdrawals.size > adminWithdrawalsPageSize) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { adminWithdrawalsPageSize += 20 },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                            modifier = Modifier.testTag("load_more_admin_withdrawals")
                        ) {
                            Text("Load More", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPlansTab(
    investments: List<Map<String, Any>>,
    viewModel: MoneyMitraViewModel
) {
    DynamicAdminPlansTab(investments = investments, viewModel = viewModel)
}

@Composable
fun DynamicAdminPlansTab(
    investments: List<Map<String, Any>>,
    viewModel: MoneyMitraViewModel
) {
    val globalInvestmentPlans by viewModel.globalInvestmentPlans.collectAsState()
    var adminSelectedSubTab by remember { mutableStateOf("Global Plans") }
    var editPlanToEdit by remember { mutableStateOf<MoneyMitraGlobalPlan?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state for add/edit plan
    var nameInput by remember { mutableStateOf("") }
    var minAmountInput by remember { mutableStateOf("") }
    var maxAmountInput by remember { mutableStateOf("") }
    var durationDaysInput by remember { mutableStateOf("") }
    var dailyReturnInput by remember { mutableStateOf("") }
    var activeInput by remember { mutableStateOf(true) }

    fun resetInputs() {
        nameInput = ""
        minAmountInput = ""
        maxAmountInput = ""
        durationDaysInput = ""
        dailyReturnInput = ""
        activeInput = true
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false 
                resetInputs()
            },
            title = { Text("Add Investment Plan", fontWeight = FontWeight.Bold, color = MitraTextMain) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Plan Name") },
                        modifier = Modifier.fillMaxWidth().testTag("add_plan_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minAmountInput,
                        onValueChange = { minAmountInput = it },
                        label = { Text("Min Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_plan_min_amount_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmountInput,
                        onValueChange = { maxAmountInput = it },
                        label = { Text("Max Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_plan_max_amount_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = durationDaysInput,
                        onValueChange = { durationDaysInput = it },
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_plan_duration_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyReturnInput,
                        onValueChange = { dailyReturnInput = it },
                        label = { Text("Daily Return (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_plan_return_input"),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status", color = MitraTextSecondary, fontSize = 14.sp)
                        Switch(
                            checked = activeInput,
                            onCheckedChange = { activeInput = it },
                            modifier = Modifier.testTag("add_plan_active_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minVal = minAmountInput.toDoubleOrNull() ?: 0.0
                        val maxVal = maxAmountInput.toDoubleOrNull() ?: 0.0
                        val duration = durationDaysInput.toIntOrNull() ?: 0
                        val dailyReturn = dailyReturnInput.toDoubleOrNull() ?: 0.0
                        if (nameInput.isNotBlank() && minVal > 0 && maxVal >= minVal && duration > 0 && dailyReturn > 0) {
                            viewModel.addGlobalPlan(
                                MoneyMitraGlobalPlan(
                                    id = "plan_${System.currentTimeMillis()}",
                                    name = nameInput,
                                    amount = minVal,
                                    durationDays = duration,
                                    dailyReturn = dailyReturn,
                                    isActive = activeInput,
                                    minAmount = minVal,
                                    maxAmount = maxVal
                                )
                            )
                            showAddDialog = false
                            resetInputs()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    modifier = Modifier.testTag("add_plan_save_button")
                ) {
                    Text("Save Plan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    resetInputs()
                }) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            }
        )
    }

    if (editPlanToEdit != null) {
        val editingPlan = editPlanToEdit!!
        
        // Initialize fields if not done yet
        var initialized by remember(editingPlan.id) { mutableStateOf(false) }
        if (!initialized) {
            nameInput = editingPlan.name
            minAmountInput = editingPlan.minAmount.toString()
            maxAmountInput = editingPlan.maxAmount.toString()
            durationDaysInput = editingPlan.durationDays.toString()
            dailyReturnInput = editingPlan.dailyReturn.toString()
            activeInput = editingPlan.isActive
            initialized = true
        }

        AlertDialog(
            onDismissRequest = { 
                editPlanToEdit = null 
                resetInputs()
                initialized = false
            },
            title = { Text("Edit Investment Plan", fontWeight = FontWeight.Bold, color = MitraTextMain) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Plan Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minAmountInput,
                        onValueChange = { minAmountInput = it },
                        label = { Text("Min Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_min_amount_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmountInput,
                        onValueChange = { maxAmountInput = it },
                        label = { Text("Max Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_max_amount_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = durationDaysInput,
                        onValueChange = { durationDaysInput = it },
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_duration_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyReturnInput,
                        onValueChange = { dailyReturnInput = it },
                        label = { Text("Daily Return (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("edit_plan_return_input"),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status", color = MitraTextSecondary, fontSize = 14.sp)
                        Switch(
                            checked = activeInput,
                            onCheckedChange = { activeInput = it },
                            modifier = Modifier.testTag("edit_plan_active_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minVal = minAmountInput.toDoubleOrNull() ?: 0.0
                        val maxVal = maxAmountInput.toDoubleOrNull() ?: 0.0
                        val duration = durationDaysInput.toIntOrNull() ?: 0
                        val dailyReturn = dailyReturnInput.toDoubleOrNull() ?: 0.0
                        if (nameInput.isNotBlank() && minVal > 0 && maxVal >= minVal && duration > 0 && dailyReturn > 0) {
                            viewModel.updateGlobalPlan(
                                MoneyMitraGlobalPlan(
                                    id = editingPlan.id,
                                    name = nameInput,
                                    amount = minVal,
                                    durationDays = duration,
                                    dailyReturn = dailyReturn,
                                    isActive = activeInput,
                                    minAmount = minVal,
                                    maxAmount = maxVal
                                )
                            )
                            editPlanToEdit = null
                            resetInputs()
                            initialized = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    modifier = Modifier.testTag("edit_plan_save_button")
                ) {
                    Text("Update Plan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    editPlanToEdit = null
                    resetInputs()
                    initialized = false
                }) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab row switchers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Global Plans", "User Investments").forEach { tab ->
                val isSelected = adminSelectedSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MitraPrimaryGreen else Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { adminSelectedSubTab = tab }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MitraTextSecondary
                    )
                }
            }
        }

        if (adminSelectedSubTab == "Global Plans") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dynamic Investment Plans",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MitraTextMain
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_add_plan_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Plan", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Add Plan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (globalInvestmentPlans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "No Plans", tint = MitraTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("No global plans added yet", fontWeight = FontWeight.Bold, color = MitraTextMain)
                        Text("Click 'Add Plan' above to initialize investment pathways.", fontSize = 11.sp, color = MitraTextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(globalInvestmentPlans) { plan ->
                        val statusText = if (plan.isActive) "ACTIVE" else "INACTIVE"
                        val statusColor = if (plan.isActive) MitraPrimaryGreen else Color(0xFFEA4335)
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, MitraBorder), RoundedCornerShape(12.dp))
                                .testTag("admin_global_plan_card_${plan.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MitraTextMain)
                                        Text("ID: ${plan.id}", fontSize = 11.sp, color = MitraTextSecondary)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("LIMITS", fontSize = 9.sp, color = MitraTextSecondary)
                                        Text("₹${String.format("%,.0f", plan.minAmount)} - ₹${String.format("%,.0f", plan.maxAmount)}", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("DURATION", fontSize = 9.sp, color = MitraTextSecondary)
                                        Text("${plan.durationDays} Days", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("DAILY RETURN", fontSize = 9.sp, color = MitraTextSecondary)
                                        Text("${plan.dailyReturn}%", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen, fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.toggleGlobalPlanStatus(plan.id, !plan.isActive) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.testTag("admin_toggle_status_button_${plan.id}")
                                    ) {
                                        Text(
                                            text = if (plan.isActive) "Deactivate Plan" else "Activate Plan",
                                            color = if (plan.isActive) Color(0xFFEA4335) else MitraPrimaryGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { editPlanToEdit = plan },
                                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen.copy(0.12f)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("admin_edit_plan_button_${plan.id}")
                                        ) {
                                            Text("Edit", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { viewModel.deleteGlobalPlan(plan.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335).copy(0.12f)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("admin_delete_plan_button_${plan.id}")
                                        ) {
                                            Text("Delete", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // User Subscriptions (Original logic)
            OldAdminPlansTabRender(investments = investments)
        }
    }
}

@Composable
fun OldAdminPlansTabRender(
    investments: List<Map<String, Any>>
) {
    var userPlansPageSize by remember { mutableStateOf(20) }
    val paginatedUserPlans = remember(investments, userPlansPageSize) {
        investments.take(userPlansPageSize)
    }

    if (investments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active user investment plans subscribed.", color = MitraTextSecondary, fontSize = 13.sp)
        }
    } else {
         LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(paginatedUserPlans) { inv ->
                val planId = inv["planId"]?.toString() ?: "growth"
                val status = inv["status"]?.toString() ?: "ACTIVE"
                val amountStr = String.format("%,.2f", (inv["investedAmount"] as? Number)?.toDouble() ?: 0.0)
                val rate = inv["returnsRange"]?.toString() ?: "12% Returns"
                val daysR = (inv["daysRemaining"] as? Number)?.toInt() ?: 30
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv["name"]?.toString() ?: "Wealth Build Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)
                                val subId = inv["uid"]?.toString() ?: "None"
                                Text("Plan Key: $planId (ROI: $rate)", fontSize = 12.sp, color = MitraTextSecondary)
                                Text("Subscriber: $subId", fontSize = 11.sp, color = MitraTextSecondary)
                            }
                            
                            val statusCol = if (status == "ACTIVE") MitraSuccessGreen else MitraTextSecondary
                            Box(
                                modifier = Modifier
                                    .background(statusCol.copy(0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(status, color = statusCol, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        
                        HorizontalDivider(color = MitraBorder, modifier = Modifier.padding(vertical = 10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Capital Locked: ₹$amountStr", fontSize = 12.sp, color = MitraTextMain, fontWeight = FontWeight.Bold)
                            Text("Days remaining: $daysR", fontSize = 12.sp, color = MitraTextSecondary)
                        }
                    }
                }
            }
            if (investments.size > userPlansPageSize) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { userPlansPageSize += 20 },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                            modifier = Modifier.testTag("load_more_admin_user_plans")
                        ) {
                            Text("Load More", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNotificationsTab(
    notifications: List<Map<String, Any>>,
    onSendNotificationClick: () -> Unit
) {
    var adminNotificationsPageSize by remember { mutableStateOf(20) }
    val paginatedNotifications = remember(notifications, adminNotificationsPageSize) {
        notifications.take(adminNotificationsPageSize)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Broadcast Alerts System", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MitraTextMain)
                Button(
                    onClick = onSendNotificationClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("send_notification_btn")
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = "Send Broadcast", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New System Alert", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No outgoing notifications broadcasted yet.", color = MitraTextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(paginatedNotifications) { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(note["title"]?.toString() ?: "Global System Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)
                                    val noteUid = note["uid"]?.toString() ?: "broadcast"
                                    val chipLabel = if (noteUid == "broadcast") "system" else "single"
                                    val chipColor = if (noteUid == "broadcast") MitraPrimaryGreen else MitraAccentGold
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(chipColor.copy(0.12f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(chipLabel, fontSize = 10.sp, color = chipColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(note["message"]?.toString() ?: "", fontSize = 12.sp, color = MitraTextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Target UID: ${note["uid"] ?: "broadcast"}", fontSize = 10.sp, color = MitraTextSecondary)
                            }
                        }
                    }
                    if (notifications.size > adminNotificationsPageSize) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { adminNotificationsPageSize += 20 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                    modifier = Modifier.testTag("load_more_admin_notifications")
                                ) {
                                    Text("Load More", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReferralsTab(
    referrals: List<Map<String, Any>>,
    firestore: FirebaseFirestore,
    coroutineScope: CoroutineScope
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredReferrals = remember(searchQuery, referrals) {
        if (searchQuery.isBlank()) {
            referrals
        } else {
            val q = searchQuery.lowercase()
            referrals.filter {
                (it["referrerName"] as? String)?.lowercase()?.contains(q) == true ||
                (it["referredName"] as? String)?.lowercase()?.contains(q) == true ||
                (it["referredEmailOrPhone"] as? String)?.lowercase()?.contains(q) == true ||
                (it["status"] as? String)?.lowercase()?.contains(q) == true
            }
        }
    }

    var adminReferralsPageSize by remember { mutableStateOf(20) }
    val paginatedReferrals = remember(filteredReferrals, adminReferralsPageSize) {
        filteredReferrals.take(adminReferralsPageSize)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search by name or email...", color = MitraTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MitraPrimaryGreen) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MitraPrimaryGreen,
                unfocusedBorderColor = MitraBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredReferrals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No referrals found.",
                    color = MitraTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(paginatedReferrals) { referral ->
                    val status = (referral["status"] as? String) ?: "PENDING"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Referrer: ${referral["referrerName"] ?: "Unknown"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MitraTextMain
                                )
                                Surface(
                                    color = if (status == "COMPLETED") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = if (status == "COMPLETED") MitraPrimaryGreen else Color(0xFFE65100),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Referred: ${referral["referredName"] ?: "Unknown"} (${referral["referredEmailOrPhone"] ?: ""})", fontSize = 14.sp, color = MitraTextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Reward: ₹${referral["amount"] ?: 0.0} (Referrer), ₹${referral["referredRewardAmount"] ?: 0.0} (Referred)", fontSize = 14.sp, color = MitraTextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            val timestamp = referral["timestamp"] as? Long ?: 0L
                            val dateStr = if (timestamp > 0) SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp)) else "N/A"
                            Text("Created: $dateStr", fontSize = 12.sp, color = MitraTextSecondary)
                        }
                    }
                }
                if (filteredReferrals.size > adminReferralsPageSize) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { adminReferralsPageSize += 20 },
                                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                modifier = Modifier.testTag("load_more_admin_referrals")
                            ) {
                                Text("Load More", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}