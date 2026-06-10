package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MoneyMitraRepository(private val dao: MoneyMitraDao) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val referralAuditState = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentUserRole = MutableStateFlow<String>("user")

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var userSnapshotListener: ListenerRegistration? = null
    private val purchaseMutex = Mutex()

    suspend fun fetchAndSetUserRole(uid: String) {
        if (uid.isEmpty()) {
            currentUserRole.value = "user"
            return
        }
        try {
            val adminSnapshot = firestore.collection("admins").document(uid).get().await()
            if (adminSnapshot.exists()) {
                val dbRole = adminSnapshot.getString("role")
                if (!dbRole.isNullOrBlank()) {
                    currentUserRole.value = dbRole
                    return
                }
            }
            val userSnapshot = firestore.collection("users").document(uid).get().await()
            if (userSnapshot.exists()) {
                val dbRole = userSnapshot.getString("role")
                if (!dbRole.isNullOrBlank()) {
                    currentUserRole.value = dbRole
                    return
                }
            }
            currentUserRole.value = "user"
        } catch (e: Exception) {
            e.printStackTrace()
            currentUserRole.value = "user"
        }
    }

    fun startUserSnapshotListener(uid: String) {
        userSnapshotListener?.remove()
        userSnapshotListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val dbName = snapshot.getString("name") ?: ""
                    val dbEmail = snapshot.getString("email") ?: ""
                    val dbPhone = snapshot.getString("phone") ?: ""
                    val dbBalance = snapshot.getDouble("walletBalance") ?: snapshot.getDouble("balance") ?: 0.0
                    val dbLockedBalance = snapshot.getDouble("lockedBalance") ?: 0.0
                    val dbEarnings = snapshot.getDouble("totalEarnings") ?: 0.0
                    val dbInvested = snapshot.getDouble("investedAmount") ?: 0.0
                    val dbCoins = snapshot.getLong("coins")?.toInt() ?: 0
                    val dbLastCheckInDate = snapshot.getString("lastCheckInDate") ?: ""
                    val dbVideosWatchedToday = snapshot.getLong("videosWatchedToday")?.toInt() ?: 0
                    val dbLastVideoResetDate = snapshot.getString("lastVideoResetDate") ?: ""
                    val dbTotalCoinsEarned = snapshot.getLong("totalCoinsEarned")?.toInt() ?: 0
                    val dbTotalCoinsRedeemed = snapshot.getLong("totalCoinsRedeemed")?.toInt() ?: 0
                    val dbBankName = (snapshot.get("bankDetails") as? Map<*, *>)?.get("bankName") as? String ?: snapshot.getString("bankName") ?: ""
                    val dbAccNum = (snapshot.get("bankDetails") as? Map<*, *>)?.get("accountNumber") as? String ?: snapshot.getString("accountNumber") ?: ""
                    val dbIfsc = (snapshot.get("bankDetails") as? Map<*, *>)?.get("ifscCode") as? String ?: snapshot.getString("ifscCode") ?: ""
                    val dbAccHolder = (snapshot.get("bankDetails") as? Map<*, *>)?.get("accountHolderName") as? String ?: snapshot.getString("accountHolderName") ?: ""
                    val dbUpiId = snapshot.getString("upiId") ?: ""
                    val isProfileCreated = snapshot.getBoolean("isProfileCreated") ?: true
                    val dbReferralCode = snapshot.getString("referralCode") ?: ""
                    val dbReferredBy = snapshot.getString("referredBy") ?: ""
                    val dbTotalReferrals = snapshot.getLong("totalReferrals")?.toInt() ?: 0
                    val dbReferralEarnings = snapshot.getDouble("referralEarnings") ?: 0.0

                    repositoryScope.launch {
                        fetchAndSetUserRole(uid)
                        val local = dao.getUserSessionSync()
                        if (local != null) {
                            val session = local.copy(
                                phoneNumber = if (dbEmail.isNotEmpty()) dbEmail else dbPhone,
                                name = dbName,
                                email = dbEmail,
                                balance = dbBalance,
                                lockedBalance = dbLockedBalance,
                                investedAmount = dbInvested,
                                earnings = dbEarnings,
                                coins = dbCoins,
                                isLoggedIn = true,
                                isProfileCreated = isProfileCreated,
                                accountHolderName = dbAccHolder,
                                bankName = dbBankName,
                                accountNumber = dbAccNum,
                                ifscCode = dbIfsc,
                                upiId = dbUpiId,
                                referralCode = dbReferralCode,
                                referredBy = dbReferredBy,
                                totalReferrals = dbTotalReferrals,
                                referralEarnings = dbReferralEarnings,
                                lastCheckInDate = dbLastCheckInDate,
                                videosWatchedToday = dbVideosWatchedToday,
                                lastVideoResetDate = dbLastVideoResetDate,
                                totalCoinsEarned = dbTotalCoinsEarned,
                                totalCoinsRedeemed = dbTotalCoinsRedeemed
                            )
                            dao.updateUserSession(session)

                            // Real-time local state reset when admin triggers remote test reset
                            if (dbInvested == 0.0) {
                                try {
                                    val localPlans = dao.getAllInvestmentPlansSync(uid)
                                    for (p in localPlans) {
                                        dao.updateInvestmentPlan(
                                            p.copy(
                                                status = "AVAILABLE",
                                                investedAmount = 0.0,
                                                activationDate = "",
                                                expiryDate = "",
                                                dailyEarnings = 0.0,
                                                totalEarnings = 0.0,
                                                daysCompleted = 0,
                                                daysRemaining = if (p.planId == "starter") 30 else if (p.planId == "growth") 45 else 60
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // Clean/reset daily tasks locally if check-in date is blank/reset remotely
                            if (dbLastCheckInDate.isEmpty()) {
                                try {
                                    val tasks = dao.getAllDailyTasksSync()
                                    for (task in tasks) {
                                        dao.updateDailyTask(task.copy(isCompleted = false))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopUserSnapshotListener() {
        userSnapshotListener?.remove()
        userSnapshotListener = null
    }

    private suspend fun uploadTransactionToFirestore(tx: DbTransaction) {
        try {
            val firebaseUser = auth.currentUser ?: return
            val data = mapOf(
                "uid" to firebaseUser.uid,
                "title" to tx.title,
                "dateText" to tx.dateText,
                "amount" to tx.amount,
                "type" to tx.type,
                "timestamp" to tx.timestamp
            )
            firestore.collection("transactions").add(data).await()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private suspend fun insertLocalAndRemoteTransaction(tx: DbTransaction) {
        dao.insertTransaction(tx)
        uploadTransactionToFirestore(tx)
    }

    private suspend fun uploadInvestmentToFirestore(plan: DbInvestmentPlan) {
        try {
            val firebaseUser = auth.currentUser ?: return
            val data = mapOf(
                "uid" to firebaseUser.uid,
                "planId" to plan.planId,
                "name" to plan.name,
                "returnsRange" to plan.returnsRange,
                "minInvestment" to plan.minInvestment,
                "investedAmount" to plan.investedAmount,
                "riskLevel" to plan.riskLevel,
                "status" to plan.status,
                "activationDate" to plan.activationDate,
                "expiryDate" to plan.expiryDate,
                "dailyEarnings" to plan.dailyEarnings,
                "totalEarnings" to plan.totalEarnings,
                "daysCompleted" to plan.daysCompleted,
                "daysRemaining" to plan.daysRemaining,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("investments").document("${firebaseUser.uid}_${plan.planId}").set(data).await()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private suspend fun updateLocalAndRemoteInvestmentPlan(plan: DbInvestmentPlan) {
        android.util.Log.d("InvestFlow", "updateLocalAndRemoteInvestmentPlan: planId=${plan.planId}, investedAmount=${plan.investedAmount}")
        dao.updateInvestmentPlan(plan)
        uploadInvestmentToFirestore(plan)
    }

    private suspend fun uploadWithdrawalToFirestore(amount: Double, upiId: String, bankName: String) {
        try {
            val firebaseUser = auth.currentUser ?: return
            val data = mapOf(
                "uid" to firebaseUser.uid,
                "amount" to amount,
                "upiId" to upiId,
                "bankName" to bankName,
                "timestamp" to System.currentTimeMillis(),
                "status" to "PENDING"
            )
            firestore.collection("withdrawals").add(data).await()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun sendNotification(
        uid: String,
        title: String,
        message: String,
        type: String
    ) {
        try {
            val docRef = firestore.collection("notifications").document()
            val data = mapOf(
                "notificationId" to docRef.id,
                "uid" to uid,
                "title" to title,
                "message" to message,
                "type" to type,
                "isRead" to false,
                "createdAt" to System.currentTimeMillis(),
                "timestamp" to System.currentTimeMillis()
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val snapshot = firestore.collection("notifications")
                .whereIn("uid", listOf(uid, "broadcast"))
                .get().await()
            firestore.runBatch { batch ->
                for (doc in snapshot.documents) {
                    val isRead = doc.getBoolean("isRead") ?: doc.get("isRead") as? Boolean ?: false
                    if (!isRead) {
                        batch.update(doc.reference, "isRead", true)
                    }
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun uploadNotificationToFirestore(title: String, message: String) {
        try {
            val firebaseUser = auth.currentUser ?: return
            val type = when {
                title.contains("Activated", ignoreCase = true) || title.contains("Purchased", ignoreCase = true) -> "INVESTMENT_PURCHASED"
                title.contains("Matured", ignoreCase = true) -> "INVESTMENT_MATURED"
                else -> "GENERAL"
            }
            sendNotification(firebaseUser.uid, title, message, type)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun parseActivationDateToMillis(activationDateStr: String): Long {
        try {
            val cleanStr = activationDateStr.replace(" IST", "").trim()
            val formats = listOf(
                "dd MMM yyyy, hh:mm a",
                "dd MMM yyyy, HH:mm",
                "yyyy-MM-dd",
                "dd MMM yyyy"
            )
            for (fmt in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                    }
                    val date = sdf.parse(cleanStr)
                    if (date != null) return date.time
                } catch (e: Exception) {
                    // ignore and try next
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return System.currentTimeMillis()
    }

    private fun getDaysBetweenInIst(activateDateStr: String, currentMillis: Long): Int {
        if (activateDateStr.isBlank()) return 0
        try {
            val activateTime = parseActivationDateToMillis(activateDateStr)
            val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            
            val calActivate = java.util.Calendar.getInstance(tz).apply {
                timeInMillis = activateTime
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val calToday = java.util.Calendar.getInstance(tz).apply {
                timeInMillis = currentMillis
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val diffMs = calToday.timeInMillis - calActivate.timeInMillis
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            return if (diffDays < 0) 0 else diffDays
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    suspend fun autoCreditDueInvestmentEarnings() = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            val querySnapshot = firestore.collection("investments")
                .whereEqualTo("uid", uid)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            for (doc in querySnapshot.documents) {
                val planId = doc.getString("planId") ?: continue
                val activationDate = doc.getString("activationDate") ?: ""
                val daysCompleted = doc.getLong("daysCompleted")?.toInt() ?: 0
                val daysRemaining = doc.getLong("daysRemaining")?.toInt() ?: 0
                val investedAmount = doc.getDouble("investedAmount") ?: 0.0
                val dailyEarnings = doc.getDouble("dailyEarnings") ?: 0.0
                val totalEarnings = doc.getDouble("totalEarnings") ?: 0.0
                val name = doc.getString("name") ?: ""

                if (activationDate.isBlank()) continue

                val elapsedDays = getDaysBetweenInIst(activationDate, System.currentTimeMillis())
                val validityDays = daysCompleted + daysRemaining
                val expectedCredits = elapsedDays.coerceAtMost(validityDays)

                if (daysCompleted < expectedCredits) {
                    val missingCredits = expectedCredits - daysCompleted
                    val additionalEarnings = dailyEarnings * missingCredits
                    val newDaysCompleted = daysCompleted + missingCredits
                    val newDaysRemaining = (daysRemaining - missingCredits).coerceAtLeast(0)
                    val newTotalEarnings = totalEarnings + additionalEarnings

                    android.util.Log.d("AutoCredit", "Crediting $missingCredits days of earnings for $name: ₹$additionalEarnings")

                    val userRef = firestore.collection("users").document(uid)
                    val investRef = doc.reference

                    try {
                        firestore.runTransaction { transaction ->
                            val userSnap = transaction.get(userRef)
                            val investSnap = transaction.get(investRef)

                            if (userSnap.exists() && investSnap.exists()) {
                                val currentBalance = userSnap.getDouble("walletBalance") ?: userSnap.getDouble("balance") ?: 0.0
                                val currentTotalEarnings = userSnap.getDouble("totalEarnings") ?: userSnap.getDouble("earnings") ?: 0.0
                                val currentCoins = userSnap.getLong("coins")?.toInt() ?: 0
                                val currentUserInvested = userSnap.getDouble("investedAmount") ?: 0.0

                                var finalStatus = "ACTIVE"
                                var finalInvestedAmount = investedAmount
                                var isMatured = false
                                var refundAmount = 0.0

                                if (newDaysRemaining == 0) {
                                    finalStatus = "COMPLETED"
                                    finalInvestedAmount = 0.0
                                    isMatured = true
                                    refundAmount = investedAmount
                                }

                                val nextBalance = currentBalance + additionalEarnings + refundAmount
                                val nextUserInvested = (currentUserInvested - refundAmount).coerceAtLeast(0.0)
                                val nextUserEarnings = currentTotalEarnings + additionalEarnings
                                val nextUserCoins = currentCoins + (10 * missingCredits) // +10 Coins per day

                                transaction.update(investRef, mapOf(
                                    "daysCompleted" to newDaysCompleted,
                                    "daysRemaining" to newDaysRemaining,
                                    "totalEarnings" to newTotalEarnings,
                                    "status" to finalStatus,
                                    "investedAmount" to finalInvestedAmount,
                                    "updatedAt" to System.currentTimeMillis()
                                ))

                                transaction.update(userRef, mapOf(
                                    "walletBalance" to nextBalance,
                                    "balance" to com.google.firebase.firestore.FieldValue.delete(),
                                    "investedAmount" to nextUserInvested,
                                    "totalEarnings" to nextUserEarnings,
                                    "earnings" to nextUserEarnings,
                                    "coins" to nextUserCoins
                                ))
                            }
                            null
                        }.await()

                        // Write transaction histories
                        for (i in 1..missingCredits) {
                            saveTransactionToFirestore(
                                uid = uid,
                                type = "INVESTMENT_EARNING",
                                amount = dailyEarnings,
                                description = "Daily profit credited for $name (Auto)",
                                status = "SUCCESS"
                            )
                        }

                        if (newDaysRemaining == 0) {
                            saveTransactionToFirestore(
                                uid = uid,
                                type = "INVESTMENT_RETURN",
                                amount = investedAmount,
                                description = "Matured principal refund for $name (Auto)",
                                status = "SUCCESS"
                            )
                            uploadNotificationToFirestore("Investment Matured", "$name has completed and principal of ₹${String.format("%,.2f", investedAmount)} has been returned to your wallet.")
                        } else {
                            uploadNotificationToFirestore("Daily Return Credited", "Received ₹${String.format("%,.2f", additionalEarnings)} return from $name plus ${10 * missingCredits} bonus coins.")
                        }

                    } catch (txEx: Exception) {
                        android.util.Log.e("AutoCredit", "Failed to run auto-credit transaction: ${txEx.message}", txEx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoCredit", "Error querying active investments for auto-credit: ${e.message}", e)
        }
    }

    suspend fun syncUserInvestmentsFromFirestore() = withContext(Dispatchers.IO) {
        try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@withContext
            val currentUid = firebaseUser.uid

            // Auto-credit any eligible active investment earnings before syncing
            try {
                autoCreditDueInvestmentEarnings()
            } catch (ce: Exception) {
                ce.printStackTrace()
            }

            // Reset local plans to "AVAILABLE" state before syncing from Firestore
            try {
                val localPlans = dao.getAllInvestmentPlansSync(currentUid)
                for (p in localPlans) {
                    dao.updateInvestmentPlan(
                        p.copy(
                            status = "AVAILABLE",
                            investedAmount = 0.0,
                            activationDate = "",
                            expiryDate = "",
                            dailyEarnings = 0.0,
                            totalEarnings = 0.0,
                            daysCompleted = 0,
                            daysRemaining = if (p.planId == "starter") 30 else if (p.planId == "growth") 45 else 60
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Query with requirement-compliant filters
            val querySnapshot = firestore.collection("investments")
                .whereEqualTo("uid", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser!!.uid)
                .get()
                .await()

            for (document in querySnapshot.documents) {
                val planId = document.getString("planId") ?: continue
                val name = document.getString("name") ?: ""
                val returnsRange = document.getString("returnsRange") ?: ""
                val minInvestment = document.getDouble("minInvestment") ?: 100.0
                val investedAmount = document.getDouble("investedAmount") ?: 0.0
                val riskLevel = document.getString("riskLevel") ?: "MEDIUM"
                val status = document.getString("status") ?: "AVAILABLE"
                val activationDate = document.getString("activationDate") ?: ""
                val expiryDate = document.getString("expiryDate") ?: ""
                val dailyEarnings = document.getDouble("dailyEarnings") ?: 0.0
                val totalEarnings = document.getDouble("totalEarnings") ?: 0.0
                val daysCompleted = document.getLong("daysCompleted")?.toInt() ?: 0
                val daysRemaining = document.getLong("daysRemaining")?.toInt() ?: 30

                val plan = DbInvestmentPlan(
                    uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser!!.uid,
                    planId = planId,
                    name = name,
                    returnsRange = returnsRange,
                    minInvestment = minInvestment,
                    investedAmount = investedAmount,
                    riskLevel = riskLevel,
                    status = status,
                    activationDate = activationDate,
                    expiryDate = expiryDate,
                    dailyEarnings = dailyEarnings,
                    totalEarnings = totalEarnings,
                    daysCompleted = daysCompleted,
                    daysRemaining = daysRemaining
                )
                dao.insertInvestmentPlan(plan)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val currentUidState = MutableStateFlow(auth.currentUser?.uid ?: "")

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                currentUidState.value = user.uid
                startUserSnapshotListener(user.uid)
                repositoryScope.launch {
                    try {
                        ensureSessionInitialized()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                currentUidState.value = ""
                stopUserSnapshotListener()
            }
        }
    }

    val userSession: Flow<UserSession?> = dao.getUserSession()
    val allTransactions: Flow<List<DbTransaction>> = dao.getAllTransactions()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val realTimeTransactions: Flow<List<MoneyMitraTransaction>> = currentUidState.flatMapLatest { uid ->
        callbackFlow {
            if (uid.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listenerRegistration = firestore.collection("transactions")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                MoneyMitraTransaction(
                                    transactionId = doc.getString("transactionId") ?: doc.id,
                                    uid = doc.getString("uid") ?: "",
                                    type = doc.getString("type") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    description = doc.getString("description") ?: "",
                                    status = doc.getString("status") ?: "SUCCESS",
                                    createdAt = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.createdAt }
                        trySend(list)
                    }
                }
            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val realTimeWithdrawals: Flow<List<MoneyMitraWithdrawal>> = currentUidState.flatMapLatest { uid ->
        callbackFlow {
            if (uid.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listenerRegistration = firestore.collection("withdrawals")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                MoneyMitraWithdrawal(
                                    id = doc.id,
                                    uid = doc.getString("uid") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    status = doc.getString("status") ?: "PENDING",
                                    createdAt = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    upiId = doc.getString("upiId") ?: "",
                                    bankName = doc.getString("bankName") ?: "",
                                    accountHolderName = doc.getString("accountHolderName") ?: "",
                                    accountNumber = doc.getString("accountNumber") ?: "",
                                    ifscCode = doc.getString("ifscCode") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.createdAt }
                        trySend(list)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val realTimeReferrals: Flow<List<MoneyMitraReferral>> = currentUidState.flatMapLatest { uid ->
        callbackFlow {
            if (uid.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listenerRegistration = firestore.collection("referrals")
                .whereEqualTo("referrerUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                MoneyMitraReferral(
                                    id = doc.id,
                                    referrerUid = doc.getString("referrerUid") ?: "",
                                    referrerName = doc.getString("referrerName") ?: "",
                                    referredUid = doc.getString("referredUid") ?: "",
                                    referredName = doc.getString("referredName") ?: "",
                                    referredEmailOrPhone = doc.getString("referredEmailOrPhone") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    status = doc.getString("status") ?: "PENDING",
                                    amount = doc.getDouble("amount") ?: 50.0,
                                    referredRewardAmount = doc.getDouble("referredRewardAmount") ?: 20.0
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.timestamp }
                        trySend(list)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val realTimeNotifications: Flow<List<MoneyMitraNotification>> = currentUidState.flatMapLatest { uid ->
        callbackFlow {
            if (uid.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listenerRegistration = firestore.collection("notifications")
                .whereIn("uid", listOf(uid, "broadcast"))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val isReadVal = doc.getBoolean("isRead") ?: doc.get("isRead") as? Boolean ?: false
                                val createdAtVal = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                                MoneyMitraNotification(
                                    notificationId = doc.id,
                                    uid = doc.getString("uid") ?: "",
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    type = doc.getString("type") ?: "GENERAL",
                                    isRead = isReadVal,
                                    createdAt = createdAtVal
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.createdAt }
                        trySend(list)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    val adminRealTimeWithdrawals: Flow<List<MoneyMitraWithdrawal>> = callbackFlow {
        val listenerRegistration = firestore.collection("withdrawals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            MoneyMitraWithdrawal(
                                id = doc.id,
                                uid = doc.getString("uid") ?: "",
                                amount = doc.getDouble("amount") ?: 0.0,
                                status = doc.getString("status") ?: "PENDING",
                                createdAt = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                upiId = doc.getString("upiId") ?: "",
                                bankName = doc.getString("bankName") ?: "",
                                accountHolderName = doc.getString("accountHolderName") ?: "",
                                accountNumber = doc.getString("accountNumber") ?: "",
                                ifscCode = doc.getString("ifscCode") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    trySend(list)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    val staticGlobalPlans = listOf(
        MoneyMitraGlobalPlan("starter_plan", "Starter Plan", 500.0, 30, 1.0, 30.0, true, 500.0, 5000.0),
        MoneyMitraGlobalPlan("growth_plan", "Growth Plan", 5001.0, 45, 1.75, 78.75, true, 5001.0, 50000.0),
        MoneyMitraGlobalPlan("premium_plan", "Premium Plan", 50001.0, 60, 2.5, 150.0, true, 50001.0, 100000.0)
    )

    val globalInvestmentPlans: Flow<List<MoneyMitraGlobalPlan>> = kotlinx.coroutines.flow.flowOf(staticGlobalPlans)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val realTimeActiveInvestments: Flow<List<MoneyMitraActiveInvestment>> = currentUidState.flatMapLatest { uid ->
        callbackFlow {
            if (uid.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listenerRegistration = firestore.collection("investments")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                MoneyMitraActiveInvestment(
                                    id = doc.id,
                                    uid = doc.getString("uid") ?: "",
                                    planId = doc.getString("planId") ?: "",
                                    name = doc.getString("name") ?: "",
                                    investedAmount = doc.getDouble("investedAmount") ?: 0.0,
                                    activationDate = doc.getString("activationDate") ?: "",
                                    expiryDate = doc.getString("expiryDate") ?: "",
                                    status = doc.getString("status") ?: "ACTIVE",
                                    dailyEarnings = doc.getDouble("dailyEarnings") ?: 0.0,
                                    totalEarnings = doc.getDouble("totalEarnings") ?: 0.0,
                                    daysCompleted = doc.getLong("daysCompleted")?.toInt() ?: 0,
                                    daysRemaining = doc.getLong("daysRemaining")?.toInt() ?: 0,
                                    createdAt = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.createdAt }
                        trySend(list)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    val adminRealTimeActiveInvestments: Flow<List<MoneyMitraActiveInvestment>> = callbackFlow {
        val listenerRegistration = firestore.collection("investments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            MoneyMitraActiveInvestment(
                                id = doc.id,
                                uid = doc.getString("uid") ?: "",
                                planId = doc.getString("planId") ?: "",
                                name = doc.getString("name") ?: "",
                                investedAmount = doc.getDouble("investedAmount") ?: 0.0,
                                activationDate = doc.getString("activationDate") ?: "",
                                expiryDate = doc.getString("expiryDate") ?: "",
                                status = doc.getString("status") ?: "ACTIVE",
                                dailyEarnings = doc.getDouble("dailyEarnings") ?: 0.0,
                                totalEarnings = doc.getDouble("totalEarnings") ?: 0.0,
                                daysCompleted = doc.getLong("daysCompleted")?.toInt() ?: 0,
                                daysRemaining = doc.getLong("daysRemaining")?.toInt() ?: 0,
                                createdAt = doc.getLong("createdAt") ?: doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    trySend(list)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addGlobalPlan(plan: MoneyMitraGlobalPlan) = withContext(Dispatchers.IO) {
        // No-op: plans are static and offline
    }

    suspend fun updateGlobalPlan(plan: MoneyMitraGlobalPlan) = withContext(Dispatchers.IO) {
        // No-op: plans are static and offline
    }

    suspend fun deleteGlobalPlan(planId: String) = withContext(Dispatchers.IO) {
        // No-op: plans are static and offline
    }

    suspend fun toggleGlobalPlanStatus(planId: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        // No-op: plans are static and offline
    }

    suspend fun buyGlobalPlan(planId: String, planName: String, planAmount: Double, durationDays: Int, dailyReturn: Double) = withContext(Dispatchers.IO) {
        purchaseMutex.withLock {
            ensureSessionInitialized()
            val current = dao.getUserSessionSync() ?: throw Exception("Session not initialized!")
            val firebaseUid = auth.currentUser?.uid ?: throw Exception("User not authenticated!")
            checkIfBlocked(firebaseUid)

            // 0. Plan Status and Boundary Validation using static plans
            val localPlans = dao.getAllInvestmentPlansSync(firebaseUid)
            val hasLocalActive = localPlans.any { it.status == "ACTIVE" }
            val remoteActiveQuery = firestore.collection("investments")
                .whereEqualTo("uid", firebaseUid)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()
            val hasRemoteActive = !remoteActiveQuery.isEmpty
            if (hasLocalActive || hasRemoteActive) {
                throw Exception("You already have an active investment. Wait until your current plan matures before starting a new investment.")
            }

            val staticPlan = staticGlobalPlans.find { it.id == planId }
            if (staticPlan != null) {
                if (!staticPlan.isActive) {
                    throw Exception("This investment plan is currently inactive and cannot be purchased.")
                }
                if (planAmount < staticPlan.minAmount || planAmount > staticPlan.maxAmount) {
                    throw Exception("Investment amount ₹$planAmount is outside of allowed limits (₹${staticPlan.minAmount} - ₹${staticPlan.maxAmount}).")
                }
            } else {
                throw Exception("The selected investment plan does not exist.")
            }

            if (current.balance < planAmount) {
                throw Exception("Insufficient wallet balance!")
            }

            // 1. Deduct amount from wallet & Increase investedAmount
            val newBalance = current.balance - planAmount
            val newInvested = current.investedAmount + planAmount
            val updated = current.copy(balance = newBalance, investedAmount = newInvested)
            dao.updateUserSession(updated)
            saveActiveSessionToUser(updated)

            // 2. Create Wallet History transaction entry
            saveTransactionToFirestore(
                uid = firebaseUid,
                type = "INVESTMENT_PURCHASE",
                amount = planAmount,
                description = "Invested ₹${String.format("%,.2f", planAmount)} in $planName",
                status = "SUCCESS"
            )

            // 3. Create Active Investment doc (ACTIVE state, Cloud Function ready)
            val df = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US)
            df.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            val today = java.util.Date()
            val activationDateStr = df.format(today) + " IST"
            
            val dfExpiry = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
            dfExpiry.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            cal.time = today
            cal.add(java.util.Calendar.DAY_OF_YEAR, durationDays)
            val expiryDateStr = dfExpiry.format(cal.time)

            val data = mapOf(
                "uid" to firebaseUid,
                "planId" to planId,
                "name" to planName,
                "planName" to planName,
                "investedAmount" to planAmount,
                "activationDate" to activationDateStr,
                "startDate" to activationDateStr,
                "expiryDate" to expiryDateStr,
                "maturityDate" to expiryDateStr,
                "status" to "ACTIVE",
                "dailyReturn" to dailyReturn,
                "dailyReturnPercent" to dailyReturn,
                "dailyEarnings" to (planAmount * (dailyReturn / 100.0)),
                "totalEarnings" to 0.0,
                "daysCompleted" to 0,
                "daysRemaining" to durationDays,
                "durationDays" to durationDays,
                "returnsRange" to "${dailyReturn}% Daily (${durationDays} Days)",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("investments").add(data).await()

            try {
                syncUserInvestmentsFromFirestore()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                checkAndApplyReferralReward(planAmount)
            } catch (e: Exception) {
                logReferralAudit("Failed to apply referral reward in dynamic buyGlobalPlan: ${e.message}", "WARN", e)
            }

            uploadNotificationToFirestore("Plan Activated", "Successfully invested ₹${String.format("%,.2f", planAmount)} in $planName!")
        }
    }

    suspend fun saveTransactionToFirestore(
        uid: String,
        type: String, // DEPOSIT, WITHDRAWAL, REFERRAL_BONUS, REFERRAL_REWARD, INVESTMENT_PURCHASE, INVESTMENT_RETURN, ADMIN_CREDIT, ADMIN_DEBIT
        amount: Double,
        description: String,
        status: String = "SUCCESS",
        targetUid: String? = null
    ) {
        try {
            val targetUserUid = targetUid ?: uid
            val collection = firestore.collection("transactions")
            val docRef = collection.document()
            val txId = docRef.id
            val data = mapOf(
                "transactionId" to txId,
                "uid" to targetUserUid,
                "type" to type,
                "amount" to amount,
                "description" to description,
                "status" to status,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(data).await()

            // Map standard transaction types back to local Room database for secondary backup:
            val localType = when (type) {
                "DEPOSIT", "REFERRAL_BONUS", "REFERRAL_REWARD", "INVESTMENT_RETURN", "INVESTMENT_EARNING", "ADMIN_CREDIT", "COIN_CHECKIN", "COIN_VIDEO", "COIN_REDEEM", "COIN_REDEMPTION" -> "CREDIT"
                "WITHDRAWAL", "INVESTMENT_PURCHASE", "ADMIN_DEBIT" -> "DEBIT"
                else -> "CREDIT"
            }
            val localTitle = when (type) {
                "DEPOSIT" -> "Wallet Deposit"
                "WITHDRAWAL" -> "Withdrawal ($status)"
                "REFERRAL_BONUS" -> "Referral Bonus"
                "REFERRAL_REWARD" -> "Referral Reward"
                "INVESTMENT_PURCHASE" -> "Investment Purchase"
                "INVESTMENT_RETURN", "INVESTMENT_EARNING" -> "Investment Return"
                "ADMIN_CREDIT" -> "Admin Credit"
                "ADMIN_DEBIT" -> "Admin Debit"
                "COIN_CHECKIN" -> "Daily Check-In Reward"
                "COIN_VIDEO" -> "Watch & Earn Reward"
                "COIN_REDEEM", "COIN_REDEMPTION" -> "Coin Redemption Credit"
                else -> description
            }
            dao.insertTransaction(
                DbTransaction(
                    title = localTitle,
                    dateText = "Just now",
                    amount = amount,
                    type = localType,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allInvestmentPlans: Flow<List<DbInvestmentPlan>> = currentUidState.flatMapLatest { uid ->
        dao.getAllInvestmentPlans(uid)
    }
    val allDailyTasks: Flow<List<DbDailyTask>> = dao.getAllDailyTasks()

    suspend fun ensureSessionInitialized() = withContext(Dispatchers.IO) {
        try {
            val firebaseUid = auth.currentUser?.uid ?: ""
            currentUidState.value = firebaseUid

            val current = dao.getUserSessionSync()
            if (current == null) {
                val defaultSession = UserSession(
                    id = 1,
                    balance = 0.0,
                    investedAmount = 0.0,
                    earnings = 0.0,
                    coins = 0,
                    isOnboarded = false,
                    isLoggedIn = false,
                    isProfileCreated = false
                )
                dao.insertUserSession(defaultSession)

                // Also check other data is initialized
                val plans = dao.getAllInvestmentPlansSync(firebaseUid)
                if (plans.isEmpty()) {
                    initPlansAndTasks(firebaseUid)
                }
            } else {
                val plans = dao.getAllInvestmentPlansSync(firebaseUid)
                if (plans.isEmpty()) {
                    initPlansAndTasks(firebaseUid)
                }
            }

            // Check and auto-seed Firestore global_plans has been disabled. Plans are static and offline now.

            // AUTO-LOGIN LOGIC: Restore persistent session from Firebase Auth on startup
            val firebaseUser = auth.currentUser
            val local = dao.getUserSessionSync()
            if (firebaseUser != null && local != null && !local.isLoggedIn) {
                try {
                    val uid = firebaseUser.uid
                    val doc = firestore.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val dbName = doc.getString("name") ?: ""
                        val dbEmail = doc.getString("email") ?: ""
                        val dbPhone = doc.getString("phone") ?: ""
                        val dbBalance = doc.getDouble("walletBalance") ?: 0.0
                        val dbLockedBalance = doc.getDouble("lockedBalance") ?: 0.0
                        val dbEarnings = doc.getDouble("totalEarnings") ?: 0.0
                        val dbInvested = doc.getDouble("investedAmount") ?: 0.0
                        val dbCoins = doc.getLong("coins")?.toInt() ?: 0
                        val dbBankName = (doc.get("bankDetails") as? Map<*, *>)?.get("bankName") as? String ?: doc.getString("bankName") ?: ""
                        val dbAccNum = (doc.get("bankDetails") as? Map<*, *>)?.get("accountNumber") as? String ?: doc.getString("accountNumber") ?: ""
                        val dbIfsc = (doc.get("bankDetails") as? Map<*, *>)?.get("ifscCode") as? String ?: doc.getString("ifscCode") ?: ""
                        val dbAccHolder = (doc.get("bankDetails") as? Map<*, *>)?.get("accountHolderName") as? String ?: doc.getString("accountHolderName") ?: ""
                        val dbUpiId = doc.getString("upiId") ?: ""
                        val isProfileCreated = doc.getBoolean("isProfileCreated") ?: true
                        val dbReferralCode = doc.getString("referralCode") ?: ""
                        val dbReferredBy = doc.getString("referredBy") ?: ""
                        val dbTotalReferrals = doc.getLong("totalReferrals")?.toInt() ?: 0
                        val dbReferralEarnings = doc.getDouble("referralEarnings") ?: 0.0

                        val session = local.copy(
                            phoneNumber = if (dbEmail.isNotEmpty()) dbEmail else dbPhone,
                            name = dbName,
                            email = dbEmail,
                            balance = dbBalance,
                            lockedBalance = dbLockedBalance,
                            investedAmount = dbInvested,
                            earnings = dbEarnings,
                            coins = dbCoins,
                            isLoggedIn = true,
                            isProfileCreated = isProfileCreated,
                            accountHolderName = dbAccHolder,
                            bankName = dbBankName,
                            accountNumber = dbAccNum,
                            ifscCode = dbIfsc,
                            upiId = dbUpiId,
                            referralCode = dbReferralCode,
                            referredBy = dbReferredBy,
                            totalReferrals = dbTotalReferrals,
                            referralEarnings = dbReferralEarnings
                        )
                        dao.updateUserSession(session)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (firebaseUser != null) {
                syncUserInvestmentsFromFirestore()
                startUserSnapshotListener(firebaseUser.uid)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private suspend fun initPlansAndTasks(uid: String) = withContext(Dispatchers.IO) {
        // Plans (All available by default in production)
        dao.insertInvestmentPlan(DbInvestmentPlan(uid, "starter", "Starter Plan", "Up to 1% Daily (30 Days)", 100.0, 0.0, "MEDIUM", status = "AVAILABLE", daysCompleted = 0, daysRemaining = 30))
        dao.insertInvestmentPlan(DbInvestmentPlan(uid, "growth", "Growth Plan", "Up to 1.75% Daily (45 Days)", 5000.0, 0.0, "HIGH", status = "AVAILABLE", daysCompleted = 0, daysRemaining = 45))
        dao.insertInvestmentPlan(DbInvestmentPlan(uid, "premium", "Premium Plan", "Up to 2.5% Daily (60 Days)", 50000.0, 0.0, "VERY_HIGH", status = "AVAILABLE", daysCompleted = 0, daysRemaining = 60))

        // Tasks
        dao.insertDailyTask(DbDailyTask("checkin", "Daily Check-in", "Check-in in the app daily", 10, false, "Claim"))
        dao.insertDailyTask(DbDailyTask("watch_ad", "Watch Ad", "Watch an ad and earn coins", 20, false, "Watch"))
        dao.insertDailyTask(DbDailyTask("invite", "Invite a Friend", "Invite a friend and earn", 100, false, "Invite"))
        dao.insertDailyTask(DbDailyTask("article", "Read Article", "Read finance article", 15, false, "Read"))
        dao.insertDailyTask(DbDailyTask("quiz", "Quiz", "Answer quiz and earn", 25, false, "Start Quiz"))
    }

    suspend fun completeOnboarding() = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        dao.updateUserSession(current.copy(isOnboarded = true))
    }

    suspend fun login(phoneNumber: String) = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        dao.updateUserSession(current.copy(phoneNumber = phoneNumber, isLoggedIn = true))
    }

    suspend fun loginWithPassword(emailOrPhone: String, password: String): Boolean = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val email = if (emailOrPhone.contains("@")) emailOrPhone.trim() else "${emailOrPhone.trim()}@moneymitra.com"
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return@withContext false

            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val dbName = doc.getString("name") ?: ""
                val dbEmail = doc.getString("email") ?: ""
                val dbPhone = doc.getString("phone") ?: ""
                val dbBalance = doc.getDouble("walletBalance") ?: 0.0
                val dbEarnings = doc.getDouble("totalEarnings") ?: 0.0
                val dbInvested = doc.getDouble("investedAmount") ?: 0.0
                val dbCoins = doc.get("coins") as? Number ?: 0
                val dbLastCheckInDate = doc.getString("lastCheckInDate") ?: ""
                val dbVideosWatchedToday = (doc.get("videosWatchedToday") as? Number)?.toInt() ?: 0
                val dbLastVideoResetDate = doc.getString("lastVideoResetDate") ?: ""
                val dbTotalCoinsEarned = (doc.get("totalCoinsEarned") as? Number)?.toInt() ?: 0
                val dbTotalCoinsRedeemed = (doc.get("totalCoinsRedeemed") as? Number)?.toInt() ?: 0
                val dbBankName = (doc.get("bankDetails") as? Map<*, *>)?.get("bankName") as? String ?: doc.getString("bankName") ?: ""
                val dbAccNum = (doc.get("bankDetails") as? Map<*, *>)?.get("accountNumber") as? String ?: doc.getString("accountNumber") ?: ""
                val dbIfsc = (doc.get("bankDetails") as? Map<*, *>)?.get("ifscCode") as? String ?: doc.getString("ifscCode") ?: ""
                val dbAccHolder = (doc.get("bankDetails") as? Map<*, *>)?.get("accountHolderName") as? String ?: doc.getString("accountHolderName") ?: ""
                val dbUpiId = doc.getString("upiId") ?: ""
                val isProfileCreated = doc.getBoolean("isProfileCreated") ?: true
                val dbReferralCode = doc.getString("referralCode") ?: ""
                val dbReferredBy = doc.getString("referredBy") ?: ""
                val dbTotalReferrals = doc.getLong("totalReferrals")?.toInt() ?: 0
                val dbReferralEarnings = doc.getDouble("referralEarnings") ?: 0.0

                val current = dao.getUserSessionSync() ?: return@withContext false
                val session = current.copy(
                    phoneNumber = emailOrPhone,
                    name = dbName,
                    email = dbEmail,
                    balance = dbBalance,
                    investedAmount = dbInvested,
                    earnings = dbEarnings,
                    coins = dbCoins.toInt(),
                    isLoggedIn = true,
                    isProfileCreated = isProfileCreated,
                    accountHolderName = dbAccHolder,
                    bankName = dbBankName,
                    accountNumber = dbAccNum,
                    ifscCode = dbIfsc,
                    upiId = dbUpiId,
                    referralCode = dbReferralCode,
                    referredBy = dbReferredBy,
                    totalReferrals = dbTotalReferrals,
                    referralEarnings = dbReferralEarnings,
                    lastCheckInDate = dbLastCheckInDate,
                    videosWatchedToday = dbVideosWatchedToday,
                    lastVideoResetDate = dbLastVideoResetDate,
                    totalCoinsEarned = dbTotalCoinsEarned,
                    totalCoinsRedeemed = dbTotalCoinsRedeemed
                )
                dao.updateUserSession(session)

                val localUser = DbUser(
                    emailOrPhone = emailOrPhone,
                    name = dbName,
                    email = dbEmail,
                    passwordHash = password,
                    balance = dbBalance,
                    investedAmount = dbInvested,
                    earnings = dbEarnings,
                    coins = dbCoins.toInt(),
                    isProfileCreated = isProfileCreated,
                    accountHolderName = dbAccHolder,
                    bankName = dbBankName,
                    accountNumber = dbAccNum,
                    ifscCode = dbIfsc,
                    upiId = dbUpiId,
                    lastCheckInDate = dbLastCheckInDate,
                    videosWatchedToday = dbVideosWatchedToday,
                    lastVideoResetDate = dbLastVideoResetDate,
                    totalCoinsEarned = dbTotalCoinsEarned,
                    totalCoinsRedeemed = dbTotalCoinsRedeemed
                )
                dao.insertUser(localUser)
                currentUidState.value = uid
                val plans = dao.getAllInvestmentPlansSync(uid)
                if (plans.isEmpty()) {
                    initPlansAndTasks(uid)
                }
                syncUserInvestmentsFromFirestore()
                startUserSnapshotListener(uid)
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            val user = dao.getUserSync(emailOrPhone)
            if (user != null && user.passwordHash == password) {
                val current = dao.getUserSessionSync() ?: return@withContext false
                val session = current.copy(
                    phoneNumber = user.emailOrPhone,
                    name = user.name,
                    email = user.email,
                    balance = user.balance,
                    investedAmount = user.investedAmount,
                    earnings = user.earnings,
                    coins = user.coins,
                    isLoggedIn = true,
                    isProfileCreated = user.isProfileCreated,
                    bankName = user.bankName,
                    accountNumber = user.accountNumber,
                    ifscCode = user.ifscCode,
                    upiId = user.upiId,
                    lastCheckInDate = user.lastCheckInDate,
                    videosWatchedToday = user.videosWatchedToday,
                    lastVideoResetDate = user.lastVideoResetDate,
                    totalCoinsEarned = user.totalCoinsEarned,
                    totalCoinsRedeemed = user.totalCoinsRedeemed
                )
                dao.updateUserSession(session)
                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun signUpUser(name: String, emailOrPhone: String, passwordHash: String, referralCodeEntered: String = ""): Boolean = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val email = if (emailOrPhone.contains("@")) emailOrPhone.trim() else "${emailOrPhone.trim()}@moneymitra.com"
        android.util.Log.d("ReferralAudit", "Repository signUpUser: referralCode received: '$referralCodeEntered'")
        
        val auditLogs = mutableMapOf<String, String>()
        auditLogs["signupReferralCode"] = referralCodeEntered
        
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, passwordHash).await()
            val uid = authResult.user?.uid ?: return@withContext false
            android.util.Log.d("ReferralAudit", "User created in Auth with uid: $uid")

            var referredByUid = ""
            if (referralCodeEntered.isNotBlank()) {
                val searchCode = referralCodeEntered.trim().uppercase()
                android.util.Log.d("ReferralAudit", "Executing Firestore Query for referralCode: '$searchCode'")
                try {
                    val qSnapshot = firestore.collection("users")
                        .whereEqualTo("referralCode", searchCode)
                        .get().await()
                    android.util.Log.d("ReferralAudit", "Query result count: ${qSnapshot.size()}")
                    auditLogs["queryResultCount"] = qSnapshot.size().toString()
                    
                    if (!qSnapshot.isEmpty) {
                        referredByUid = qSnapshot.documents.first().id
                        android.util.Log.d("ReferralAudit", "referredByUid value set to: $referredByUid")
                        auditLogs["referredByUid"] = referredByUid
                    } else {
                        android.util.Log.d("ReferralAudit", "No user found with referralCode: $searchCode")
                        auditLogs["referredByUid"] = "NOT_FOUND"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReferralAudit", "Error querying referrer: ${e.message}", e)
                    e.printStackTrace()
                    auditLogs["queryError"] = e.message ?: "Unknown error"
                }
            } else {
                auditLogs["referredByUid"] = "BLANK_INPUT"
            }
            
            val userReferralCode = generateReferralCode()

            val newUser = DbUser(
                emailOrPhone = emailOrPhone,
                name = name,
                email = if (emailOrPhone.contains("@")) emailOrPhone else "",
                passwordHash = passwordHash,
                isProfileCreated = true,
                referralCode = userReferralCode,
                lastCheckInDate = "",
                videosWatchedToday = 0,
                lastVideoResetDate = "",
                totalCoinsEarned = 0,
                totalCoinsRedeemed = 0
            )
            dao.insertUser(newUser)

            android.util.Log.d("ReferralAudit", "Final Firestore payload before set(). referredBy exists? Yes. Value is: '$referredByUid'")
            val data = mapOf(
                "uid" to uid,
                "name" to name,
                "email" to (if (emailOrPhone.contains("@")) emailOrPhone else ""),
                "phone" to (if (!emailOrPhone.contains("@")) emailOrPhone else ""),
                "walletBalance" to 0.0,
                "totalEarnings" to 0.0,
                "investedAmount" to 0.0,
                "coins" to 0,
                "activePlan" to "",
                "createdAt" to System.currentTimeMillis(),
                "isProfileCreated" to true,
                "referralCode" to userReferralCode,
                "referredBy" to referredByUid,
                "totalReferrals" to 0,
                "referralEarnings" to 0.0,
                "lastCheckInDate" to "",
                "videosWatchedToday" to 0,
                "lastVideoResetDate" to "",
                "totalCoinsEarned" to 0,
                "totalCoinsRedeemed" to 0
            )
            
            try {
                firestore.collection("users").document(uid).set(data).await()
                android.util.Log.d("ReferralAudit", "Firestore success: Payload saved for uid=$uid")
                auditLogs["savedReferredBy"] = data["referredBy"].toString()
            } catch (e: Exception) {
                android.util.Log.e("ReferralAudit", "Firestore failure: ${e.message}", e)
                auditLogs["savedReferredBy"] = "SAVE_FAILED"
            }
            
            referralAuditState.value = auditLogs

            if (referralCodeEntered.isNotBlank() && referredByUid.isNotBlank()) {
                android.util.Log.d("ReferralAudit", "Triggering processReferral")
                processReferral(uid, name, email, referralCodeEntered) // Will handle the rest (referrals collection, referrer total). If it fails on referrer doc, at least the new user has referredBy populated.
            }

            val notificationData = mapOf(
                "uid" to uid,
                "title" to "Welcome to MoneyMitra!",
                "message" to "Start investing on starter, growth, or premium plans to earn daily return ranges.",
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("notifications").add(notificationData).await()

            currentUidState.value = uid
            val plans = dao.getAllInvestmentPlansSync(uid)
            if (plans.isEmpty()) {
                initPlansAndTasks(uid)
            }
            startUserSnapshotListener(uid)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            val existing = dao.getUserSync(emailOrPhone)
            if (existing == null) {
                val newUser = DbUser(
                    emailOrPhone = emailOrPhone,
                    name = name,
                    email = if (emailOrPhone.contains("@")) emailOrPhone else "",
                    passwordHash = passwordHash,
                    isProfileCreated = true
                )
                dao.insertUser(newUser)
                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun signUpUserWithPhone(uid: String, name: String, phoneNumber: String, referralCodeEntered: String = ""): Boolean = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val email = ""
        android.util.Log.d("ReferralAudit", "Repository signUpUserWithPhone: referralCode received: '$referralCodeEntered'")
        
        val auditLogs = mutableMapOf<String, String>()
        auditLogs["signupReferralCode"] = referralCodeEntered
        
        try {
            var referredByUid = ""
            if (referralCodeEntered.isNotBlank()) {
                val searchCode = referralCodeEntered.trim().uppercase()
                android.util.Log.d("ReferralAudit", "Executing Firestore Query for referralCode: '$searchCode'")
                try {
                    val qSnapshot = firestore.collection("users")
                        .whereEqualTo("referralCode", searchCode)
                        .get().await()
                    android.util.Log.d("ReferralAudit", "Query result count: ${qSnapshot.size()}")
                    auditLogs["queryResultCount"] = qSnapshot.size().toString()
                    
                    if (!qSnapshot.isEmpty) {
                        referredByUid = qSnapshot.documents.first().id
                        android.util.Log.d("ReferralAudit", "referredByUid value set to: $referredByUid")
                        auditLogs["referredByUid"] = referredByUid
                    } else {
                        android.util.Log.d("ReferralAudit", "No user found with referralCode: $searchCode")
                        auditLogs["referredByUid"] = "NOT_FOUND"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReferralAudit", "Error querying referrer: ${e.message}", e)
                    e.printStackTrace()
                    auditLogs["queryError"] = e.message ?: "Unknown error"
                }
            } else {
                auditLogs["referredByUid"] = "BLANK_INPUT"
            }
            
            val userReferralCode = generateReferralCode()

            val newUser = DbUser(
                emailOrPhone = phoneNumber,
                name = name,
                email = "",
                passwordHash = "N/A",
                isProfileCreated = true,
                referralCode = userReferralCode,
                lastCheckInDate = "",
                videosWatchedToday = 0,
                lastVideoResetDate = "",
                totalCoinsEarned = 0,
                totalCoinsRedeemed = 0
            )
            dao.insertUser(newUser)

            android.util.Log.d("ReferralAudit", "Final Firestore payload before set(). referredBy exists? Yes. Value is: '$referredByUid'")
            val data = mapOf(
                "uid" to uid,
                "name" to name,
                "email" to "",
                "phone" to phoneNumber,
                "walletBalance" to 0.0,
                "totalEarnings" to 0.0,
                "investedAmount" to 0.0,
                "coins" to 0,
                "activePlan" to "",
                "createdAt" to System.currentTimeMillis(),
                "isProfileCreated" to true,
                "referralCode" to userReferralCode,
                "referredBy" to referredByUid,
                "totalReferrals" to 0,
                "referralEarnings" to 0.0,
                "lastCheckInDate" to "",
                "videosWatchedToday" to 0,
                "lastVideoResetDate" to "",
                "totalCoinsEarned" to 0,
                "totalCoinsRedeemed" to 0
            )
            
            try {
                firestore.collection("users").document(uid).set(data).await()
                android.util.Log.d("ReferralAudit", "Firestore success: Payload saved for uid=$uid")
                auditLogs["savedReferredBy"] = data["referredBy"].toString()
            } catch (e: Exception) {
                android.util.Log.e("ReferralAudit", "Firestore failure: ${e.message}", e)
                auditLogs["savedReferredBy"] = "SAVE_FAILED"
            }
            
            referralAuditState.value = auditLogs

            if (referralCodeEntered.isNotBlank() && referredByUid.isNotBlank()) {
                android.util.Log.d("ReferralAudit", "Triggering processReferral")
                processReferral(uid, name, email, referralCodeEntered)
            }

            val notificationData = mapOf(
                "uid" to uid,
                "title" to "Welcome to MoneyMitra!",
                "message" to "Start investing on starter, growth, or premium plans to earn daily return ranges.",
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("notifications").add(notificationData).await()

            val current = dao.getUserSessionSync() ?: return@withContext false
            val session = current.copy(
                phoneNumber = phoneNumber,
                name = name,
                email = "",
                balance = 0.0,
                investedAmount = 0.0,
                earnings = 0.0,
                coins = 0,
                isLoggedIn = true,
                isProfileCreated = true,
                referralCode = userReferralCode,
                referredBy = referredByUid,
                totalReferrals = 0,
                referralEarnings = 0.0
            )
            dao.updateUserSession(session)

            currentUidState.value = uid
            val plans = dao.getAllInvestmentPlansSync(uid)
            if (plans.isEmpty()) {
                initPlansAndTasks(uid)
            }
            startUserSnapshotListener(uid)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun loginWithPhoneUid(uid: String, phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val dbName = doc.getString("name") ?: ""
                val dbEmail = doc.getString("email") ?: ""
                val dbPhone = doc.getString("phone") ?: phoneNumber
                val dbBalance = doc.getDouble("walletBalance") ?: 0.0
                val dbEarnings = doc.getDouble("totalEarnings") ?: 0.0
                val dbInvested = doc.getDouble("investedAmount") ?: 0.0
                val dbCoins = doc.get("coins") as? Number ?: 0
                val dbLastCheckInDate = doc.getString("lastCheckInDate") ?: ""
                val dbVideosWatchedToday = (doc.get("videosWatchedToday") as? Number)?.toInt() ?: 0
                val dbLastVideoResetDate = doc.getString("lastVideoResetDate") ?: ""
                val dbTotalCoinsEarned = (doc.get("totalCoinsEarned") as? Number)?.toInt() ?: 0
                val dbTotalCoinsRedeemed = (doc.get("totalCoinsRedeemed") as? Number)?.toInt() ?: 0
                val dbBankName = (doc.get("bankDetails") as? Map<*, *>)?.get("bankName") as? String ?: doc.getString("bankName") ?: ""
                val dbAccNum = (doc.get("bankDetails") as? Map<*, *>)?.get("accountNumber") as? String ?: doc.getString("accountNumber") ?: ""
                val dbIfsc = (doc.get("bankDetails") as? Map<*, *>)?.get("ifscCode") as? String ?: doc.getString("ifscCode") ?: ""
                val dbAccHolder = (doc.get("bankDetails") as? Map<*, *>)?.get("accountHolderName") as? String ?: doc.getString("accountHolderName") ?: ""
                val dbUpiId = doc.getString("upiId") ?: ""
                val isProfileCreated = doc.getBoolean("isProfileCreated") ?: true
                val dbReferralCode = doc.getString("referralCode") ?: ""
                val dbReferredBy = doc.getString("referredBy") ?: ""
                val dbTotalReferrals = doc.getLong("totalReferrals")?.toInt() ?: 0
                val dbReferralEarnings = doc.getDouble("referralEarnings") ?: 0.0

                val current = dao.getUserSessionSync() ?: return@withContext false
                val session = current.copy(
                    phoneNumber = dbPhone,
                    name = dbName,
                    email = dbEmail,
                    balance = dbBalance,
                    investedAmount = dbInvested,
                    earnings = dbEarnings,
                    coins = dbCoins.toInt(),
                    isLoggedIn = true,
                    isProfileCreated = isProfileCreated,
                    accountHolderName = dbAccHolder,
                    bankName = dbBankName,
                    accountNumber = dbAccNum,
                    ifscCode = dbIfsc,
                    upiId = dbUpiId,
                    referralCode = dbReferralCode,
                    referredBy = dbReferredBy,
                    totalReferrals = dbTotalReferrals,
                    referralEarnings = dbReferralEarnings,
                    lastCheckInDate = dbLastCheckInDate,
                    videosWatchedToday = dbVideosWatchedToday,
                    lastVideoResetDate = dbLastVideoResetDate,
                    totalCoinsEarned = dbTotalCoinsEarned,
                    totalCoinsRedeemed = dbTotalCoinsRedeemed
                )
                dao.updateUserSession(session)

                val localUser = DbUser(
                    emailOrPhone = dbPhone,
                    name = dbName,
                    email = dbEmail,
                    passwordHash = "N/A",
                    balance = dbBalance,
                    investedAmount = dbInvested,
                    earnings = dbEarnings,
                    coins = dbCoins.toInt(),
                    isProfileCreated = isProfileCreated,
                    accountHolderName = dbAccHolder,
                    bankName = dbBankName,
                    accountNumber = dbAccNum,
                    ifscCode = dbIfsc,
                    upiId = dbUpiId,
                    lastCheckInDate = dbLastCheckInDate,
                    videosWatchedToday = dbVideosWatchedToday,
                    lastVideoResetDate = dbLastVideoResetDate,
                    totalCoinsEarned = dbTotalCoinsEarned,
                    totalCoinsRedeemed = dbTotalCoinsRedeemed
                )
                dao.insertUser(localUser)
                currentUidState.value = uid
                val plans = dao.getAllInvestmentPlansSync(uid)
                if (plans.isEmpty()) {
                    initPlansAndTasks(uid)
                }
                syncUserInvestmentsFromFirestore()
                startUserSnapshotListener(uid)
                return@withContext true
            } else {
                val userReferralCode = generateReferralCode()
                val data = mapOf(
                    "uid" to uid,
                    "name" to "User ${phoneNumber.takeLast(4)}",
                    "email" to "",
                    "phone" to phoneNumber,
                    "walletBalance" to 0.0,
                    "totalEarnings" to 0.0,
                    "investedAmount" to 0.0,
                    "coins" to 0,
                    "activePlan" to "",
                    "createdAt" to System.currentTimeMillis(),
                    "isProfileCreated" to true,
                    "referralCode" to userReferralCode,
                    "referredBy" to "",
                    "totalReferrals" to 0,
                    "referralEarnings" to 0.0,
                    "lastCheckInDate" to "",
                    "videosWatchedToday" to 0,
                    "lastVideoResetDate" to "",
                    "totalCoinsEarned" to 0,
                    "totalCoinsRedeemed" to 0
                )
                firestore.collection("users").document(uid).set(data).await()

                val current = dao.getUserSessionSync() ?: return@withContext false
                val session = current.copy(
                    phoneNumber = phoneNumber,
                    name = "User ${phoneNumber.takeLast(4)}",
                    email = "",
                    balance = 0.0,
                    investedAmount = 0.0,
                    earnings = 0.0,
                    coins = 0,
                    isLoggedIn = true,
                    isProfileCreated = true,
                    referralCode = userReferralCode,
                    referredBy = "",
                    totalReferrals = 0,
                    referralEarnings = 0.0
                )
                dao.updateUserSession(session)

                val localUser = DbUser(
                    emailOrPhone = phoneNumber,
                    name = "User ${phoneNumber.takeLast(4)}",
                    email = "",
                    passwordHash = "N/A",
                    isProfileCreated = true,
                    referralCode = userReferralCode,
                    lastCheckInDate = "",
                    videosWatchedToday = 0,
                    lastVideoResetDate = "",
                    totalCoinsEarned = 0,
                    totalCoinsRedeemed = 0
                )
                dao.insertUser(localUser)
                currentUidState.value = uid
                val plans = dao.getAllInvestmentPlansSync(uid)
                if (plans.isEmpty()) {
                    initPlansAndTasks(uid)
                }
                startUserSnapshotListener(uid)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun forgotPasswordReset(emailOrPhone: String, newPasswordHash: String): Boolean = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val email = if (emailOrPhone.contains("@")) emailOrPhone.trim() else "${emailOrPhone.trim()}@moneymitra.com"
        try {
            auth.sendPasswordResetEmail(email).await()
            val user = dao.getUserSync(emailOrPhone)
            if (user != null) {
                dao.updateUser(user.copy(passwordHash = newPasswordHash))
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            val user = dao.getUserSync(emailOrPhone)
            if (user != null) {
                dao.updateUser(user.copy(passwordHash = newPasswordHash))
                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        currentUserRole.value = "user"
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        
        saveActiveSessionToUser(current)

        try {
            auth.signOut()
            stopUserSnapshotListener()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Clear Room/SQLite user-specific tables on logout
        dao.deleteAllTransactions()
        dao.deleteAllInvestmentPlans()
        dao.deleteAllDailyTasks()

        val cleared = current.copy(
            phoneNumber = "",
            name = "",
            email = "",
            isLoggedIn = false,
            isProfileCreated = false,
            balance = 0.0,
            lockedBalance = 0.0,
            investedAmount = 0.0,
            earnings = 0.0,
            coins = 0,
            bankName = "",
            accountNumber = "",
            ifscCode = "",
            upiId = ""
        )
        dao.updateUserSession(cleared)

        currentUidState.value = ""
        // Re-initialize default available plans and daily tasks
        initPlansAndTasks("")
    }

    private suspend fun saveActiveSessionToUser(current: UserSession) {
        val user = dao.getUserSync(current.phoneNumber)
        if (user != null) {
            dao.updateUser(
                user.copy(
                    name = current.name,
                    email = current.email,
                    balance = current.balance,
                    lockedBalance = current.lockedBalance,
                    investedAmount = current.investedAmount,
                    earnings = current.earnings,
                    coins = current.coins,
                    accountHolderName = current.accountHolderName,
                    bankName = current.bankName,
                    accountNumber = current.accountNumber,
                    ifscCode = current.ifscCode,
                    upiId = current.upiId,
                    isProfileCreated = current.isProfileCreated,
                    referralCode = current.referralCode,
                    referredBy = current.referredBy,
                    totalReferrals = current.totalReferrals,
                    referralEarnings = current.referralEarnings,
                    lastCheckInDate = current.lastCheckInDate,
                    videosWatchedToday = current.videosWatchedToday,
                    lastVideoResetDate = current.lastVideoResetDate,
                    totalCoinsEarned = current.totalCoinsEarned,
                    totalCoinsRedeemed = current.totalCoinsRedeemed
                )
            )
        }

        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            try {
                android.util.Log.d("InvestFlow", "saveActiveSessionToUser to Firestore: balance=${current.balance}, investedAmount=${current.investedAmount}")
                val dbPlan = dao.getAllInvestmentPlansSync(firebaseUser.uid).find { it.status == "ACTIVE" }?.planId ?: ""
                val bankDetailsMap = mapOf(
                    "accountHolderName" to current.accountHolderName,
                    "bankName" to current.bankName,
                    "accountNumber" to current.accountNumber,
                    "ifscCode" to current.ifscCode
                )
                try {
                    firestore.collection("users").document(firebaseUser.uid)
                        .collection("bankDetails").document("details")
                        .set(bankDetailsMap, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val data = mapOf(
                    "uid" to firebaseUser.uid,
                    "name" to current.name,
                    "email" to current.email,
                    "phone" to current.phoneNumber,
                    "walletBalance" to current.balance,
                    "lockedBalance" to current.lockedBalance,
                    "totalEarnings" to current.earnings,
                    "investedAmount" to current.investedAmount,
                    "coins" to current.coins,
                    "activePlan" to dbPlan,
                    "accountHolderName" to current.accountHolderName,
                    "bankName" to current.bankName,
                    "accountNumber" to current.accountNumber,
                    "ifscCode" to current.ifscCode,
                    "upiId" to current.upiId,
                    "bankDetails" to bankDetailsMap,
                    "isProfileCreated" to current.isProfileCreated,
                    "referralCode" to current.referralCode,
                    "referredBy" to current.referredBy,
                    "totalReferrals" to current.totalReferrals,
                    "referralEarnings" to current.referralEarnings,
                    "lastCheckInDate" to current.lastCheckInDate,
                    "videosWatchedToday" to current.videosWatchedToday,
                    "lastVideoResetDate" to current.lastVideoResetDate,
                    "totalCoinsEarned" to current.totalCoinsEarned,
                    "totalCoinsRedeemed" to current.totalCoinsRedeemed
                )
                firestore.collection("users").document(firebaseUser.uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun createProfile(name: String, email: String, referralCode: String) = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        
        val firebaseUser = auth.currentUser
        var referredByUid = ""

        if (firebaseUser != null && referralCode.isNotBlank()) {
            try {
                val qSnapshot = firestore.collection("users")
                    .whereEqualTo("referralCode", referralCode.trim().uppercase())
                    .get().await()
                if (!qSnapshot.isEmpty) {
                    referredByUid = qSnapshot.documents.first().id
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val myReferralCode = if (current.referralCode.isEmpty() || current.referralCode == "MM123456") {
            generateReferralCode()
        } else {
            current.referralCode
        }
        
        val updated = current.copy(
            name = name,
            email = email,
            referralCode = myReferralCode,
            isProfileCreated = true,
            referredBy = if(referredByUid.isNotBlank()) referredByUid else current.referredBy
        )
        dao.updateUserSession(updated)
        saveActiveSessionToUser(updated)

        if (firebaseUser != null && referredByUid.isNotBlank()) {
            try {
                firestore.collection("users").document(firebaseUser.uid)
                    .set(mapOf("referredBy" to referredByUid), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            processReferral(firebaseUser.uid, name, email, referralCode)
        }
    }

    suspend fun saveUserProfileAndBank(
        name: String,
        email: String,
        phone: String,
        accountHolderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String,
        upiId: String
    ) = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        val updated = current.copy(
            name = name,
            email = email,
            phoneNumber = phone,
            accountHolderName = accountHolderName,
            bankName = bankName,
            accountNumber = accountNumber,
            ifscCode = ifscCode,
            upiId = upiId,
            isProfileCreated = true
        )
        dao.updateUserSession(updated)
        saveActiveSessionToUser(updated)
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        dao.deleteAllTransactions()
        dao.deleteAllUsers()
        dao.deleteAllInvestmentPlans()
        dao.updateUserSession(
            UserSession(
                id = 1,
                balance = 0.0,
                investedAmount = 0.0,
                earnings = 0.0,
                coins = 0,
                isOnboarded = false,
                isLoggedIn = false,
                isProfileCreated = false
            )
        )
        // Reset tasks too
        val currentUid = auth.currentUser?.uid ?: ""
        initPlansAndTasks(currentUid)
    }

    suspend fun checkIfBlocked(uid: String) {
        if (uid.isBlank()) return
        val doc = firestore.collection("users").document(uid).get().await()
        if (doc.exists()) {
            val blocked = doc.getBoolean("blocked") ?: false
            val status = doc.getString("status") ?: "ACTIVE"
            if (blocked || status.equals("BLOCKED", ignoreCase = true)) {
                throw Exception("Your account is BLOCKED. You cannot perform this operation.")
            }
        }
    }

    suspend fun addMoney(amount: Double) = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isBlank()) return@withContext
        checkIfBlocked(uid)
        
        saveTransactionToFirestore(
            uid = uid,
            type = "DEPOSIT",
            amount = amount,
            description = "Wallet Deposit via UPI / Card",
            status = "PENDING"
        )
    }

    suspend fun approveDeposit(transactionRefId: String) = withContext(Dispatchers.IO) {
        val docRef = firestore.collection("transactions").document(transactionRefId)
        val userRefIdHolder = ArrayList<String>()
        val approvedAmountHolder = ArrayList<Double>()

        firestore.runTransaction { transaction ->
            val doc = transaction.get(docRef)
            if (!doc.exists()) throw Exception("Deposit request not found")
            
            val status = doc.getString("status") ?: "PENDING"
            if (status != "PENDING") throw Exception("Deposit is already $status")
            
            val uid = doc.getString("uid") ?: ""
            val amount = doc.getDouble("amount") ?: 0.0
            
            userRefIdHolder.clear()
            userRefIdHolder.add(uid)
            approvedAmountHolder.clear()
            approvedAmountHolder.add(amount)

            val userRef = firestore.collection("users").document(uid)
            val userSnap = transaction.get(userRef)
            if (userSnap.exists()) {
                val currentWallet = userSnap.getDouble("walletBalance") ?: userSnap.getDouble("balance") ?: 0.0
                val nextWallet = currentWallet + amount
                transaction.update(
                    userRef,
                    mapOf(
                        "walletBalance" to nextWallet,
                        "balance" to com.google.firebase.firestore.FieldValue.delete()
                    )
                )
            }
            
            transaction.update(docRef, "status", "SUCCESS")
            null
        }.await()

        val uid = userRefIdHolder.firstOrNull() ?: ""
        val amount = approvedAmountHolder.firstOrNull() ?: 0.0

        if (uid.isNotEmpty()) {
            // Send notification
            sendNotification(
                uid = uid,
                title = "Deposit Approved",
                message = "Your deposit of ₹${String.format("%,.2f", amount)} was approved and wallet balance updated successfully.",
                type = "DEPOSIT_APPROVED"
            )
            
            // Process referral reward on successful deposit
            try {
                checkAndApplyReferralReward(amount, passedReferredUid = uid)
            } catch (e: Exception) {
                logReferralAudit("Failed to apply referral reward on deposit approval: ${e.message}", "WARN", e)
            }
        }
    }

    suspend fun rejectDeposit(transactionRefId: String) = withContext(Dispatchers.IO) {
        val docRef = firestore.collection("transactions").document(transactionRefId)
        val userRefIdHolder = ArrayList<String>()
        val approvedAmountHolder = ArrayList<Double>()

        firestore.runTransaction { transaction ->
            val doc = transaction.get(docRef)
            if (!doc.exists()) throw Exception("Deposit request not found")
            
            val status = doc.getString("status") ?: "PENDING"
            if (status != "PENDING") throw Exception("Deposit is already $status")
            
            val uid = doc.getString("uid") ?: ""
            val amount = doc.getDouble("amount") ?: 0.0
            
            userRefIdHolder.clear()
            userRefIdHolder.add(uid)
            approvedAmountHolder.clear()
            approvedAmountHolder.add(amount)

            transaction.update(docRef, "status", "REJECTED")
            null
        }.await()

        val uid = userRefIdHolder.firstOrNull() ?: ""
        val amount = approvedAmountHolder.firstOrNull() ?: 0.0

        if (uid.isNotEmpty()) {
            // Send notification
            sendNotification(
                uid = uid,
                title = "Deposit Rejected",
                message = "Your deposit request of ₹${String.format("%,.2f", amount)} was rejected. Please contact support.",
                type = "DEPOSIT_REJECTED"
            )
        }
    }

    suspend fun withdrawMoney(amount: Double, upiId: String = "", bankName: String = "Bank") = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: throw Exception("Session not found")
        
        // 1. Validate minimum withdrawal ₹500
        if (amount < 500) {
            throw Exception("Minimum withdrawal amount must be ₹500")
        }
        
        // 2. Validate amount <= walletBalance
        if (amount > current.balance) {
            throw Exception("Insufficient wallet balance for this withdrawal.")
        }

        // 3. Validate bank details exist (Account Holder Name, Bank Name, Account Number, IFSC Code)
        val hasBankDetails = !current.accountHolderName.isNullOrBlank() && 
                             !current.bankName.isNullOrBlank() && 
                             !current.accountNumber.isNullOrBlank() && 
                             !current.ifscCode.isNullOrBlank()
        if (!hasBankDetails) {
            throw Exception("Bank details are incomplete. Please update your profile first.")
        }

        val uid = auth.currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            throw Exception("User not logged in.")
        }
        checkIfBlocked(uid)

        // 4. Validate no existing PENDING withdrawal
        val pendingQuery = firestore.collection("withdrawals")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "PENDING")
            .get().await()
        if (!pendingQuery.isEmpty) {
            throw Exception("You already have a PENDING withdrawal request under process.")
        }

        // 5. Move amount: walletBalance -= amount, lockedBalance += amount
        val newBalance = current.balance - amount
        val newLocked = current.lockedBalance + amount
        val updated = current.copy(balance = newBalance, lockedBalance = newLocked)
        dao.updateUserSession(updated)
        saveActiveSessionToUser(updated)

        // 6. Create withdrawal document (under /withdrawals with uid, amount, status = PENDING, createdAt)
        val createdAt = System.currentTimeMillis()
        val withdrawalData = mapOf(
            "uid" to uid,
            "amount" to amount,
            "status" to "PENDING",
            "createdAt" to createdAt,
            "timestamp" to createdAt,
            "upiId" to upiId,
            "bankName" to bankName,
            "accountHolderName" to current.accountHolderName,
            "accountNumber" to current.accountNumber,
            "ifscCode" to current.ifscCode
        )
        firestore.collection("withdrawals").add(withdrawalData).await()

        // 7. Create transaction: type = WITHDRAWAL, amount, status = PENDING
        saveTransactionToFirestore(
            uid = uid,
            type = "WITHDRAWAL",
            amount = amount,
            description = "Requested ₹$amount withdrawal to ${current.bankName}",
            status = "PENDING"
        )

        // Send notification
        sendNotification(
            uid = uid,
            title = "Withdrawal Submitted",
            message = "Your request to withdraw ₹${String.format("%,.2f", amount)} is successfully submitted and under process.",
            type = "WITHDRAWAL_SUBMITTED"
        )
    }

    suspend fun approveWithdrawal(withdrawalId: String) = withContext(Dispatchers.IO) {
        val wDocRef = firestore.collection("withdrawals").document(withdrawalId)
        val userRefIdHolder = ArrayList<String>()
        val approvedAmountHolder = ArrayList<Double>()

        firestore.runTransaction { transaction ->
            val wDoc = transaction.get(wDocRef)
            if (!wDoc.exists()) throw Exception("Withdrawal request not found")
            
            val status = wDoc.getString("status") ?: "PENDING"
            if (status != "PENDING") throw Exception("Withdrawal is already $status")
            
            val uid = wDoc.getString("uid") ?: throw Exception("User ID not found")
            val amount = wDoc.getDouble("amount") ?: 0.0

            userRefIdHolder.clear()
            userRefIdHolder.add(uid)
            approvedAmountHolder.clear()
            approvedAmountHolder.add(amount)
            
            // Adjust target user's balances in Firestore: lockedBalance -= amount
            val userRef = firestore.collection("users").document(uid)
            val userSnap = transaction.get(userRef)
            
            // 1. Update withdrawal status to APPROVED
            transaction.update(wDocRef, "status", "APPROVED")
            
            if (userSnap.exists()) {
                val currentLocked = userSnap.getDouble("lockedBalance") ?: 0.0
                val nextLocked = (currentLocked - amount).coerceAtLeast(0.0)
                transaction.update(userRef, "lockedBalance", nextLocked)
            }
            null
        }.await()

        val uid = userRefIdHolder.firstOrNull() ?: ""
        val amount = approvedAmountHolder.firstOrNull() ?: 0.0

        if (uid.isNotEmpty()) {
            // Send notification
            sendNotification(
                uid = uid,
                title = "Withdrawal Approved",
                message = "Your withdrawal request of ₹${String.format("%,.2f", amount)} was approved and transferred successfully.",
                type = "WITHDRAWAL_APPROVED"
            )

            // 3. Mark matching transaction as APPROVED
            try {
                val transactionsQuery = firestore.collection("transactions")
                    .whereEqualTo("uid", uid)
                    .whereEqualTo("type", "WITHDRAWAL")
                    .whereEqualTo("amount", amount)
                    .whereEqualTo("status", "PENDING")
                    .get().await()
                if (!transactionsQuery.isEmpty) {
                    val tId = transactionsQuery.documents.first().id
                    firestore.collection("transactions").document(tId).update("status", "APPROVED").await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun rejectWithdrawal(withdrawalId: String) = withContext(Dispatchers.IO) {
        val wDocRef = firestore.collection("withdrawals").document(withdrawalId)
        val userRefIdHolder = ArrayList<String>()
        val approvedAmountHolder = ArrayList<Double>()

        firestore.runTransaction { transaction ->
            val wDoc = transaction.get(wDocRef)
            if (!wDoc.exists()) throw Exception("Withdrawal request not found")
            
            val status = wDoc.getString("status") ?: "PENDING"
            if (status != "PENDING") throw Exception("Withdrawal is already $status")
            
            val uid = wDoc.getString("uid") ?: throw Exception("User ID not found")
            val amount = wDoc.getDouble("amount") ?: 0.0

            userRefIdHolder.clear()
            userRefIdHolder.add(uid)
            approvedAmountHolder.clear()
            approvedAmountHolder.add(amount)
            
            // Adjust target user's balances in Firestore: walletBalance += amount, lockedBalance -= amount
            val userRef = firestore.collection("users").document(uid)
            val userSnap = transaction.get(userRef)
            
            // 1. Update withdrawal status to REJECTED
            transaction.update(wDocRef, "status", "REJECTED")
            
            if (userSnap.exists()) {
                val currentLocked = userSnap.getDouble("lockedBalance") ?: 0.0
                val currentWallet = userSnap.getDouble("walletBalance") ?: userSnap.getDouble("balance") ?: 0.0
                
                val nextLocked = (currentLocked - amount).coerceAtLeast(0.0)
                val nextWallet = currentWallet + amount
                
                transaction.update(userRef, "walletBalance", nextWallet)
                transaction.update(userRef, "balance", nextWallet)
                transaction.update(userRef, "lockedBalance", nextLocked)
            }
            null
        }.await()

        val uid = userRefIdHolder.firstOrNull() ?: ""
        val amount = approvedAmountHolder.firstOrNull() ?: 0.0

        if (uid.isNotEmpty()) {
            // Send notification
            sendNotification(
                uid = uid,
                title = "Withdrawal Rejected",
                message = "Your withdrawal request of ₹${String.format("%,.2f", amount)} was rejected. Please contact support. Funds have been returned to your wallet.",
                type = "WITHDRAWAL_REJECTED"
            )

            // 3. Mark matching transaction as REJECTED
            try {
                val transactionsQuery = firestore.collection("transactions")
                    .whereEqualTo("uid", uid)
                    .whereEqualTo("type", "WITHDRAWAL")
                    .whereEqualTo("amount", amount)
                    .whereEqualTo("status", "PENDING")
                    .get().await()
                if (!transactionsQuery.isEmpty) {
                    val tId = transactionsQuery.documents.first().id
                    firestore.collection("transactions").document(tId).update("status", "REJECTED").await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun investInPlan(planId: String, amount: Double) = withContext(Dispatchers.IO) {
        android.util.Log.d("InvestFlow", "investInPlan called with planId=$planId, amount=$amount")
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        android.util.Log.d("InvestFlow", "current session found. balance=${current.balance}")
        
        val firebaseUid = auth.currentUser?.uid ?: ""
        if (firebaseUid.isNotEmpty()) {
            checkIfBlocked(firebaseUid)
        }
        // Ensure only single active plan is allowed
        val plans = dao.getAllInvestmentPlansSync(firebaseUid)
        val hasLocalActive = plans.any { it.status == "ACTIVE" }
        val remoteActiveQuery = firestore.collection("investments")
            .whereEqualTo("uid", firebaseUid)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .await()
        val hasRemoteActive = !remoteActiveQuery.isEmpty
        android.util.Log.d("InvestFlow", "hasLocalActive=$hasLocalActive, hasRemoteActive=$hasRemoteActive")
        if (hasLocalActive || hasRemoteActive) {
            android.util.Log.d("InvestFlow", "User already has an active plan. Throwing exception.")
            throw Exception("You already have an active investment. Wait until your current plan matures before starting a new investment.")
        }

        if (current.balance >= amount) {
            val newBalance = current.balance - amount
            val newInvested = current.investedAmount + amount
            val updated = current.copy(balance = newBalance, investedAmount = newInvested)
            android.util.Log.d("InvestFlow", "Updating user session: newBalance=$newBalance, newInvested=$newInvested")
            dao.updateUserSession(updated)
            saveActiveSessionToUser(updated)
            try {
                checkAndApplyReferralReward(amount)
            } catch (e: Exception) {
                logReferralAudit("Failed to apply referral reward in investInPlan: ${e.message}", "WARN", e)
            }

            // Update plan details
            val plan = plans.find { it.planId == planId }
            if (plan != null) {
                val validityDays = when (planId) {
                    "starter" -> 30
                    "growth" -> 45
                    else -> 60
                }
                val rate = when (planId) {
                    "starter" -> 0.01
                    "growth" -> 0.0175
                    else -> 0.025
                }
                val dailyGain = amount * rate
                
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                val today = java.util.Date()
                val activationDateStr = sdf.format(today) + " IST"
                
                val sdfExpiry = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                sdfExpiry.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
                calendar.time = today
                calendar.add(java.util.Calendar.DAY_OF_YEAR, validityDays)
                val expiryDateStr = sdfExpiry.format(calendar.time)

                val updatedPlanDetails = plan.copy(
                    investedAmount = amount,
                    status = "ACTIVE",
                    activationDate = activationDateStr,
                    expiryDate = expiryDateStr,
                    dailyEarnings = dailyGain,
                    totalEarnings = 0.0,
                    daysCompleted = 0,
                    daysRemaining = validityDays
                )
                updateLocalAndRemoteInvestmentPlan(updatedPlanDetails)
                uploadNotificationToFirestore("Plan Activated", "Successfully invested ₹$amount in ${plan.name}!")
            }

            saveTransactionToFirestore(
                uid = firebaseUid,
                type = "INVESTMENT_PURCHASE",
                amount = amount,
                description = "Invested ₹$amount in ${plan?.name ?: "Investment Plan"}",
                status = "SUCCESS"
            )
        }
    }

    suspend fun simulatePassageOfDay(planId: String) = withContext(Dispatchers.IO) {
        val firebaseUid = auth.currentUser?.uid ?: ""
        if (firebaseUid.isBlank()) return@withContext

        // 1. Fetch active plan details locally from Room for immediate updates
        val localPlans = dao.getAllInvestmentPlansSync(firebaseUid)
        val plan = localPlans.find { it.planId == planId }
        val currentSession = dao.getUserSessionSync()

        if (plan == null || plan.status != "ACTIVE" || plan.daysRemaining <= 0 || currentSession == null) {
            android.util.Log.e("SimulateDay", "Simulation cannot proceed: active plan not found, or has 0 days left.")
            return@withContext
        }

        val planName = plan.name
        val dailyEarnings = plan.dailyEarnings
        val totalEarnings = plan.totalEarnings
        val daysCompleted = plan.daysCompleted
        val daysRemaining = plan.daysRemaining
        val investedAmount = plan.investedAmount

        // 2. Perform local state transitions instantly
        val nextCompleted = daysCompleted + 1
        val nextRemaining = daysRemaining - 1
        val nextTotalEarnings = totalEarnings + dailyEarnings
        val earnedAmt = dailyEarnings

        var finalStatus = "ACTIVE"
        var finalInvestedAmount = investedAmount
        var isMatured = false

        var walletBalance = currentSession.balance
        val currentEarnings = currentSession.earnings
        val currentCoins = currentSession.coins

        var nextUserEarnings = currentEarnings + dailyEarnings
        val nextUserCoins = currentCoins + 10 // +10 Coins on forward

        var nextBalance = walletBalance + dailyEarnings
        var nextUserInvested = currentSession.investedAmount

        if (nextRemaining == 0) {
            finalStatus = "COMPLETED"
            finalInvestedAmount = 0.0
            isMatured = true
            nextBalance += investedAmount
            nextUserInvested = (nextUserInvested - investedAmount).coerceAtLeast(0.0)
        }

        val updatedPlanToSave = plan.copy(
            investedAmount = finalInvestedAmount,
            status = finalStatus,
            totalEarnings = nextTotalEarnings,
            daysCompleted = nextCompleted,
            daysRemaining = nextRemaining
        )

        val updatedSession = currentSession.copy(
            balance = nextBalance,
            investedAmount = nextUserInvested,
            earnings = nextUserEarnings,
            coins = nextUserCoins
        )

        // 3. Persist local Room updates instantly so flow triggers UI update immediately
        dao.updateInvestmentPlan(updatedPlanToSave)
        dao.updateUserSession(updatedSession)

        // 4. Create Wallet History entry
        // Type: INVESTMENT_EARNING, Amount: Daily Profit, Status: SUCCESS
        saveTransactionToFirestore(
            uid = firebaseUid,
            type = "INVESTMENT_EARNING",
            amount = earnedAmt,
            description = "Daily profit credited for $planName",
            status = "SUCCESS"
        )

        if (isMatured) {
            saveTransactionToFirestore(
                uid = firebaseUid,
                type = "INVESTMENT_RETURN",
                amount = investedAmount,
                description = "Matured principal refund for $planName",
                status = "SUCCESS"
            )
            uploadNotificationToFirestore("Investment Matured", "$planName has completed and principal of ₹${String.format("%,.2f", investedAmount)} has been returned to your wallet.")
        } else {
            uploadNotificationToFirestore("Daily Return Credited", "Received ₹${String.format("%,.2f", earnedAmt)} return from $planName plus 10 bonus coins.")
        }

        // 5. Fire-and-forget remote Firestore update to sync state with graceful try-catch
        try {
            val userRef = firestore.collection("users").document(firebaseUid)
            var investRef = firestore.collection("investments").document("${firebaseUid}_$planId")

            // Query to find the precise document for custom or dynamic global investments
            try {
                val querySnapshot = firestore.collection("investments")
                    .whereEqualTo("uid", firebaseUid)
                    .whereEqualTo("planId", planId)
                    .whereEqualTo("status", "ACTIVE")
                    .get()
                    .await()
                if (!querySnapshot.isEmpty) {
                    investRef = querySnapshot.documents.first().reference
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            firestore.runTransaction { transaction ->
                val investData = mapOf(
                    "uid" to firebaseUid,
                    "planId" to planId,
                    "name" to planName,
                    "returnsRange" to plan.returnsRange,
                    "minInvestment" to plan.minInvestment,
                    "investedAmount" to finalInvestedAmount,
                    "riskLevel" to plan.riskLevel,
                    "status" to finalStatus,
                    "activationDate" to plan.activationDate,
                    "expiryDate" to plan.expiryDate,
                    "dailyEarnings" to dailyEarnings,
                    "totalEarnings" to nextTotalEarnings,
                    "daysCompleted" to nextCompleted,
                    "daysRemaining" to nextRemaining,
                    "updatedAt" to System.currentTimeMillis()
                )
                transaction.set(investRef, investData)

                transaction.update(userRef, mapOf(
                    "walletBalance" to nextBalance,
                    "balance" to com.google.firebase.firestore.FieldValue.delete(),
                    "investedAmount" to nextUserInvested,
                    "earnings" to nextUserEarnings,
                    "coins" to nextUserCoins
                ))
                null
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("SimulateDay", "Remote Firestore sync failed (gracefully caught to prevent blocking): ${e.message}", e)
        }
    }

    suspend fun completeTask(taskId: String) = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val tasks = dao.getAllDailyTasksSync()
        val task = tasks.find { it.taskId == taskId } ?: return@withContext
        if (!task.isCompleted) {
            dao.updateDailyTask(task.copy(isCompleted = true))

            val current = dao.getUserSessionSync() ?: return@withContext
            val newCoins = current.coins + task.coinReward
            val updated = current.copy(coins = newCoins)
            dao.updateUserSession(updated)
            saveActiveSessionToUser(updated)

            insertLocalAndRemoteTransaction(
                DbTransaction(
                    title = "Task: ${task.title}",
                    dateText = "Just now",
                    amount = task.coinReward.toDouble(),
                    type = "COIN_EARN"
                )
            )
        }
    }

    suspend fun claimDailyCheckIn() = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        }
        val todayDate = sdf.format(java.util.Date())
        if (current.lastCheckInDate == todayDate) {
            throw java.lang.IllegalStateException("Daily Check-In already claimed today")
        }

        val newCoins = current.coins + 20
        val newTotalCoinsEarned = current.totalCoinsEarned + 20
        val updated = current.copy(
            coins = newCoins,
            totalCoinsEarned = newTotalCoinsEarned,
            lastCheckInDate = todayDate
        )
        dao.updateUserSession(updated)
        saveActiveSessionToUser(updated)

        val uid = auth.currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            saveTransactionToFirestore(
                uid = uid,
                type = "COIN_CHECKIN",
                amount = 20.0,
                description = "Daily Check-In Reward (+20 Coins)",
                status = "SUCCESS"
            )
            uploadNotificationToFirestore("Daily Check-In Reward", "Claimed 20 Coins successfully!")
        }
    }

    suspend fun watchVideoAndEarn() = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        }
        val todayDate = sdf.format(java.util.Date())
        
        var watchedToday = current.videosWatchedToday
        if (current.lastVideoResetDate != todayDate) {
            watchedToday = 0
        }

        if (watchedToday >= 10) {
            throw java.lang.IllegalStateException("Maximum of 10 rewarded videos reached for today")
        }

        val newWatchedCount = watchedToday + 1
        val newCoins = current.coins + 30
        val newTotalCoinsEarned = current.totalCoinsEarned + 30
        val updated = current.copy(
            coins = newCoins,
            totalCoinsEarned = newTotalCoinsEarned,
            videosWatchedToday = newWatchedCount,
            lastVideoResetDate = todayDate
        )
        dao.updateUserSession(updated)
        saveActiveSessionToUser(updated)

        val uid = auth.currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            saveTransactionToFirestore(
                uid = uid,
                type = "COIN_VIDEO",
                amount = 30.0,
                description = "Watch & Earn Video Reward (+30 Coins)",
                status = "SUCCESS"
            )
            uploadNotificationToFirestore("Watch & Earn", "Completed video watch bonus! 30 Coins credited.")
        }
    }

    suspend fun redeemCoins(coinsAmount: Int, upiId: String = "") = withContext(Dispatchers.IO) {
        ensureSessionInitialized()
        val current = dao.getUserSessionSync() ?: return@withContext
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isNotEmpty()) {
            checkIfBlocked(uid)
        }
        if (coinsAmount < 1000) {
            throw java.lang.IllegalArgumentException("Minimum redemption is 1000 Coins")
        }
        if (current.coins < coinsAmount) {
            throw java.lang.IllegalStateException("Insufficient Coins balance")
        }

        val newCoins = current.coins - coinsAmount
        val newTotalRedeemed = current.totalCoinsRedeemed + coinsAmount
        val addedCash = coinsAmount / 100.0
        val newBalance = current.balance + addedCash
        val updated = current.copy(
            coins = newCoins,
            totalCoinsRedeemed = newTotalRedeemed,
            balance = newBalance
        )
        dao.updateUserSession(updated)
        saveActiveSessionToUser(updated)

        if (uid.isNotEmpty()) {
            saveTransactionToFirestore(
                uid = uid,
                type = "COIN_REDEMPTION",
                amount = addedCash,
                description = "Redeemed $coinsAmount Coins for ₹${String.format("%.2f", addedCash)} cash credit",
                status = "SUCCESS"
            )
            uploadNotificationToFirestore("Coins Redeemed", "Successfully redeemed $coinsAmount Coins for ₹${String.format("%.2f", addedCash)}!")
        }
    }

    private fun generateReferralCode(): String {
        return "MM" + (100000..999999).random().toString()
    }

    private fun logReferralAudit(message: String, level: String = "DEBUG", throwable: Throwable? = null) {
        val resolvedLevel = when (level.uppercase()) {
            "ERROR" -> "ERROR"
            "WARN" -> "WARN"
            "INFO" -> "INFO"
            else -> "DEBUG"
        }
        val platformLevel = when (resolvedLevel) {
            "ERROR" -> android.util.Log.ERROR
            "WARN" -> android.util.Log.WARN
            "INFO" -> android.util.Log.INFO
            else -> android.util.Log.DEBUG
        }
        val suffix = throwable?.let { "\n" + android.util.Log.getStackTraceString(it) } ?: ""
        android.util.Log.println(platformLevel, "ReferralAudit", message + suffix)

        try {
            val logData = mapOf(
                "message" to (message + if (throwable != null) " | Exception: ${throwable.localizedMessage}" else ""),
                "timestamp" to System.currentTimeMillis(),
                "level" to resolvedLevel
            )
            firestore.collection("debugLogs").add(logData)
                .addOnFailureListener { e ->
                    android.util.Log.e("ReferralAudit", "Failed to write debugLog to Firestore asynchronously", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("ReferralAudit", "Failed to write debugLog to Firestore synchronously", e)
        }
    }

    private suspend fun processReferral(referredUid: String, referredName: String, referredEmailOrPhone: String, codeEntered: String) {
        if (codeEntered.isBlank()) return
        try {
            logReferralAudit("processReferral started: referredUid=$referredUid, codeEntered=$codeEntered")
            val qSnapshot = firestore.collection("users")
                .whereEqualTo("referralCode", codeEntered.trim().uppercase())
                .get().await()
            if (!qSnapshot.isEmpty) {
                val referrerDoc = qSnapshot.documents.first()
                val referrerUid = referrerDoc.id
                logReferralAudit("processReferral referrer found: referrerUid=$referrerUid for referredUid=$referredUid")
                
                if (referrerUid != referredUid) {
                    val refDocId = "${referrerUid}_${referredUid}"
                    val existingRef = firestore.collection("referrals")
                        .document(refDocId)
                        .get().await()
                    val refExists = existingRef.exists()
                    logReferralAudit("processReferral check: logDocId=$refDocId, exists=$refExists")
                    
                    if (!refExists) {
                        val refData = mapOf(
                            "referrerUid" to referrerUid,
                            "referrerName" to (referrerDoc.getString("name") ?: ""),
                            "referredUid" to referredUid,
                            "referredName" to referredName,
                            "referredEmailOrPhone" to referredEmailOrPhone,
                            "timestamp" to System.currentTimeMillis(),
                            "status" to "PENDING",
                            "amount" to 50.0,
                            "referredRewardAmount" to 20.0
                        )
                        firestore.collection("referrals").document(refDocId).set(refData).await()
                    }
                } else {
                    logReferralAudit("processReferral: referrerUid cannot be the same as referredUid")
                }
            } else {
                logReferralAudit("processReferral: No referrer user found with code $codeEntered")
            }
        } catch (e: Exception) {
            logReferralAudit("Referral failure inside processReferral", "ERROR", e)
        }
    }

    private suspend fun checkAndApplyReferralReward(
        transactionAmount: Double,
        passedReferredUid: String? = null,
        passedReferrerUid: String? = null
    ) {
        if (transactionAmount < 500.0) {
            logReferralAudit("checkAndApplyReferralReward: transactionAmount ($transactionAmount) is less than ₹500.0. No reward applied.")
            return
        }
        val currentUid = passedReferredUid ?: auth.currentUser?.uid ?: return
        logReferralAudit("checkAndApplyReferralReward called for currentUid: $currentUid, passedReferrerUid: $passedReferrerUid")
        
        try {
            // Determine the referrerUid first
            var referrerUid = passedReferrerUid
            if (referrerUid.isNullOrBlank()) {
                val userSnap = firestore.collection("users").document(currentUid).get().await()
                if (userSnap.exists()) {
                    referrerUid = userSnap.getString("referredBy")
                }
            }
            
            logReferralAudit("checkAndApplyReferralReward debug: resolved referrerUid: $referrerUid")
            
            // Resolve direct document ID
            val refDocId = if (!referrerUid.isNullOrBlank()) {
                "${referrerUid}_$currentUid"
            } else {
                null
            }
            
            // Let's retrieve referral doc either directly or via query fallback
            val finalReferralDoc = if (refDocId != null) {
                val directSnap = firestore.collection("referrals").document(refDocId).get().await()
                logReferralAudit("checkAndApplyReferralReward direct lookup: document id: $refDocId, exists: ${directSnap.exists()}")
                if (directSnap.exists()) {
                    val status = directSnap.getString("status")
                    if (status == "COMPLETED") {
                        logReferralAudit("checkAndApplyReferralReward: Referral is already COMPLETED. Doing nothing to prevent duplicate rewards.")
                        return
                    }
                    if (status == "PENDING") {
                        directSnap
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            } ?: run {
                logReferralAudit("Direct lookup of $refDocId unsuccessful or not pending, falling back to query for $currentUid")
                val qSnapshot = firestore.collection("referrals")
                    .whereEqualTo("referredUid", currentUid)
                    .get().await()
                if (!qSnapshot.isEmpty) {
                    val firstDoc = qSnapshot.documents.first()
                    val status = firstDoc.getString("status")
                    if (status == "COMPLETED") {
                        logReferralAudit("checkAndApplyReferralReward query: Referral is already COMPLETED. Doing nothing to prevent duplicate rewards.")
                        return
                    }
                    if (status == "PENDING") {
                        firstDoc
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            
            if (finalReferralDoc == null) {
                logReferralAudit("No pending referral document found/resolved for $currentUid")
                return
            }
            
            val finalReferrerUid = finalReferralDoc.getString("referrerUid")
            if (finalReferrerUid.isNullOrBlank()) {
                logReferralAudit("Referrer UID is null/empty in referral document")
                return
            }
            
            val finalRefDocId = finalReferralDoc.id
            val finalRefDocExists = finalReferralDoc.exists()
            logReferralAudit("Pending referral found: DocumentID = $finalRefDocId, ReferrerUID = $finalReferrerUid, Document Exists = $finalRefDocExists")
            
            val finalRefDocRef = firestore.collection("referrals").document(finalRefDocId)
            val referrerRef = firestore.collection("users").document(finalReferrerUid)
            val referredRef = firestore.collection("users").document(currentUid)

            logReferralAudit("Transaction start: applying rewards...")
            try {
                firestore.runTransaction { transaction ->
                    val referralSnap = transaction.get(finalRefDocRef)
                    val referrerSnap = transaction.get(referrerRef)
                    val referredSnap = transaction.get(referredRef)
                    
                    val status = referralSnap.getString("status") ?: "PENDING"
                    if (status != "PENDING") {
                        throw IllegalStateException("Referral already processed (Status: $status)")
                    }
                    
                    if (!referrerSnap.exists()) {
                        throw IllegalStateException("Referrer profile document not found: $finalReferrerUid")
                    }
                    if (!referredSnap.exists()) {
                        throw IllegalStateException("Referred user profile document not found: $currentUid")
                    }
                    
                    val alreadyRewarded = referredSnap.getBoolean("hasReceivedReferralReward") ?: false
                    if (alreadyRewarded) {
                        throw IllegalStateException("User has already received referral reward")
                    }
                    
                    // Get current user balances from Firestore
                    val referrerBalance = (referrerSnap.get("walletBalance") as? Number)?.toDouble()
                        ?: (referrerSnap.get("balance") as? Number)?.toDouble()
                        ?: 0.0
                    val referrerTotalRefs = (referrerSnap.get("totalReferrals") as? Number)?.toInt() ?: 0
                    val referrerEarnings = (referrerSnap.get("referralEarnings") as? Number)?.toDouble() ?: 0.0
                    
                    val referredBalance = (referredSnap.get("walletBalance") as? Number)?.toDouble()
                        ?: (referredSnap.get("balance") as? Number)?.toDouble()
                        ?: 0.0
                    val referredTotalEarnings = (referredSnap.get("totalEarnings") as? Number)?.toDouble() ?: 0.0
                    
                    // Calculate new states with added rewards (Referrer: +₹50, Referred: +₹20)
                    val newReferrerBalance = referrerBalance + 50.0
                    val newReferrerEarnings = referrerEarnings + 50.0
                    val newReferrerTotalRefs = referrerTotalRefs + 1
                    
                    val newReferredBalance = referredBalance + 20.0
                    val newReferredTotalEarnings = referredTotalEarnings + 20.0
                    
                    // Perform the atomic transaction updates individual calls (avoiding vararg conversion issues)
                    transaction.update(finalRefDocRef, "status", "COMPLETED")
                    transaction.update(finalRefDocRef, "processedAt", System.currentTimeMillis())
                    
                    transaction.update(referrerRef, "walletBalance", newReferrerBalance)
                    transaction.update(referrerRef, "referralEarnings", newReferrerEarnings)
                    transaction.update(referrerRef, "totalReferrals", newReferrerTotalRefs)
                    
                    transaction.update(referredRef, "walletBalance", newReferredBalance)
                    transaction.update(referredRef, "totalEarnings", newReferredTotalEarnings)
                    transaction.update(referredRef, "hasReceivedReferralReward", true)

                    // Write Referral Bonus (₹50) transaction document
                    val referrerTxDocRef = firestore.collection("transactions").document()
                    val referrerTxData = mapOf(
                        "transactionId" to referrerTxDocRef.id,
                        "uid" to finalReferrerUid,
                        "type" to "REFERRAL_BONUS",
                        "amount" to 50.0,
                        "description" to "Referral Bonus for inviting " + (referredSnap.getString("name") ?: "a friend"),
                        "status" to "SUCCESS",
                        "createdAt" to System.currentTimeMillis()
                    )
                    transaction.set(referrerTxDocRef, referrerTxData)

                    // Write Referral Reward (₹20) transaction document
                    val referredTxDocRef = firestore.collection("transactions").document()
                    val referredTxData = mapOf(
                        "transactionId" to referredTxDocRef.id,
                        "uid" to currentUid,
                        "type" to "REFERRAL_REWARD",
                        "amount" to 20.0,
                        "description" to "Referral Reward for joining via code",
                        "status" to "SUCCESS",
                        "createdAt" to System.currentTimeMillis()
                    )
                    transaction.set(referredTxDocRef, referredTxData)
                    
                    null
                }.await()
                logReferralAudit("Full transaction commit client-side completed successfully!")
                
                // Send real-time notifications to both referrer and referee
                sendNotification(
                    uid = finalReferrerUid,
                    title = "Referral Reward Earned",
                    message = "Congratulations! You have earned ₹50.00 referral bonus for inviting a friend.",
                    type = "REFERRAL_REWARD"
                )
                
                sendNotification(
                    uid = currentUid,
                    title = "Referral Bonus Earned",
                    message = "Congratulations! You have earned ₹20.00 welcome bonus for joining via referral code.",
                    type = "REFERRAL_REWARD"
                )
            } catch (txEx: Exception) {
                logReferralAudit(
                    "Transaction failed inside checkAndApplyReferralReward",
                    "ERROR",
                    txEx
                )
                throw txEx
            }
        } catch (e: Exception) {
            logReferralAudit(
                "Referral failure inside checkAndApplyReferralReward",
                "ERROR",
                e
            )
            throw e
        }
    }
}
