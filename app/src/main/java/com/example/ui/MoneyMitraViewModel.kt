package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DbDailyTask
import com.example.data.DbInvestmentPlan
import com.example.data.DbTransaction
import com.example.data.MoneyMitraGlobalPlan
import com.example.data.MoneyMitraActiveInvestment
import com.example.data.MoneyMitraReferral
import com.example.data.MoneyMitraNotification
import com.example.data.MoneyMitraTransaction
import com.example.data.MoneyMitraWithdrawal
import com.example.data.MoneyMitraDatabase
import com.example.data.MoneyMitraRepository
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import android.app.Activity
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit

class MoneyMitraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MoneyMitraDatabase.getDatabase(application, viewModelScope)
    private val repository = MoneyMitraRepository(db.moneyMitraDao())

    // Real Firebase Phone Auth States
    val verificationId = MutableStateFlow("")
    var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    val authError = MutableStateFlow<String?>(null)
    val isVerificationInProgress = MutableStateFlow(false)
    val isCodeSent = MutableStateFlow(false)

    val currentUserRole: StateFlow<String> = repository.currentUserRole
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "user"
        )

    val apiLoading = MutableStateFlow(false)
    val apiError = MutableStateFlow<String?>(null)
    val dashboardSelectedTab = MutableStateFlow(0)

    private var rewardedAd: RewardedAd? = null
    val isAdLoading = MutableStateFlow(false)
    val isAdReady = MutableStateFlow(false)

    fun loadRewardedAd() {
        if (isAdLoading.value || rewardedAd != null) return
        isAdLoading.value = true
        val adRequest = AdRequest.Builder().build()
        // Standard Test Ad Unit ID for Rewarded Ads
        val adUnitId = "ca-app-pub-3940256099942544/5224354917"
        
        RewardedAd.load(
            getApplication(),
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isAdLoading.value = false
                    isAdReady.value = false
                    android.util.Log.e("AdMob", "Rewarded ad failed to load: ${loadAdError.message}")
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading.value = false
                    isAdReady.value = true
                    android.util.Log.d("AdMob", "Rewarded ad loaded successfully.")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
                android.util.Log.d("AdMob", "User earned AdMob reward of ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            })
            rewardedAd = null
            isAdReady.value = false
            // Preload next
            loadRewardedAd()
        } else {
            loadRewardedAd()
        }
    }

    fun clearApiError() {
        apiError.value = null
    }

    private inline fun launchNetworkAction(
        crossinline action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            apiLoading.value = true
            apiError.value = null
            try {
                action()
            } catch (e: Exception) {
                e.printStackTrace()
                apiError.value = mapExceptionToMessage(e)
            } finally {
                apiLoading.value = false
            }
        }
    }

    private fun mapExceptionToMessage(e: Exception): String {
        val msg = e.localizedMessage ?: ""
        return when {
            e is java.net.UnknownHostException || e is java.io.IOException || msg.contains("network", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true) -> {
                "Network unavailable. Please check your internet connection."
            }
            msg.contains("permission-denied", ignoreCase = true) || msg.contains("permission denied", ignoreCase = true) -> {
                "Permission denied. You do not have sufficient authority to perform this action."
            }
            msg.contains("deadline-exceeded", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) -> {
                "Request timed out. Please try again."
            }
            else -> {
                msg.ifBlank { "Operation failed. Please try again." }
            }
        }
    }

    val userSession: StateFlow<UserSession?> = repository.userSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val transactions: StateFlow<List<MoneyMitraTransaction>> = repository.realTimeTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val investmentPlans: StateFlow<List<DbInvestmentPlan>> = repository.allInvestmentPlans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val globalInvestmentPlans: StateFlow<List<MoneyMitraGlobalPlan>> = repository.globalInvestmentPlans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userActiveInvestments: StateFlow<List<MoneyMitraActiveInvestment>> = repository.realTimeActiveInvestments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val adminActiveInvestments: StateFlow<List<MoneyMitraActiveInvestment>> = repository.adminRealTimeActiveInvestments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyTasks: StateFlow<List<DbDailyTask>> = repository.allDailyTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userWithdrawals: StateFlow<List<MoneyMitraWithdrawal>> = repository.realTimeWithdrawals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val adminWithdrawals: StateFlow<List<MoneyMitraWithdrawal>> = repository.adminRealTimeWithdrawals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userReferrals: StateFlow<List<MoneyMitraReferral>> = repository.realTimeReferrals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userNotifications: StateFlow<List<MoneyMitraNotification>> = repository.realTimeNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val withdrawError = MutableStateFlow<String?>(null)
    val withdrawSuccess = MutableStateFlow(false)

    val referralAuditState = repository.referralAuditState.asStateFlow()

    // Form states
    var loginPhoneInput = MutableStateFlow("")
    var otpInput = MutableStateFlow("")
    var profileNameInput = MutableStateFlow("")
    var profileEmailInput = MutableStateFlow("")
    var profileReferralInput = MutableStateFlow("")

    // Temporary signup credentials cache and flow indicator
    var signupName = ""
    var signupEmailOrPhone = ""
    var signupPassword = ""
    var signupReferralCode = ""
    var otpContext = "login"

    var addMoneyAmountInput = MutableStateFlow("")
    var withdrawMoneyAmountInput = MutableStateFlow("")
    var withdrawBankAccount = MutableStateFlow("")
    var withdrawUpiId = MutableStateFlow("")
    var redeemCoinsInput = MutableStateFlow("1000")
    var redeemUpiId = MutableStateFlow("")

    // Navigation states
    private val _selectedPlanId = MutableStateFlow<String?>("growth")
    val selectedPlanId = _selectedPlanId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSessionInitialized()
        }
        loadRewardedAd()
    }

    fun selectPlan(planId: String) {
        _selectedPlanId.value = planId
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
        }
    }

    fun login(phone: String) {
        viewModelScope.launch {
            repository.login(phone)
        }
    }

    fun startPhoneVerification(phoneNumber: String, activity: Activity) {
        authError.value = null
        isVerificationInProgress.value = true
        isCodeSent.value = false
        
        val cleaned = phoneNumber.trim().replace(" ", "").replace("-", "")
        val formattedPhone = if (cleaned.startsWith("+")) cleaned else "+91$cleaned"
        android.util.Log.d("PhoneAuthAudit", "Starting verification for: $formattedPhone")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                android.util.Log.d("PhoneAuthAudit", "onVerificationCompleted: credential received")
                viewModelScope.launch {
                    try {
                        signInWithPhoneCredential(credential)
                    } catch (e: Exception) {
                        authError.value = "Auto-verification failed: ${e.message}"
                        isVerificationInProgress.value = false
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                android.util.Log.e("PhoneAuthAudit", "onVerificationFailed: ${e.message}", e)
                authError.value = "Verification failed: ${e.localizedMessage ?: e.message}"
                isVerificationInProgress.value = false
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                android.util.Log.d("PhoneAuthAudit", "onCodeSent: verificationId = $id")
                verificationId.value = id
                forceResendingToken = token
                isCodeSent.value = true
                isVerificationInProgress.value = false
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            android.util.Log.e("PhoneAuthAudit", "Error building/starting verification", e)
            authError.value = "Error: ${e.localizedMessage ?: e.message}"
            isVerificationInProgress.value = false
        }
    }

    fun resendVerificationCode(phoneNumber: String, activity: Activity) {
        val token = forceResendingToken
        if (token == null) {
            authError.value = "Resend token is not available. Please restart verification."
            return
        }
        authError.value = null
        isVerificationInProgress.value = true
        
        val cleaned = phoneNumber.trim().replace(" ", "").replace("-", "")
        val formattedPhone = if (cleaned.startsWith("+")) cleaned else "+91$cleaned"
        android.util.Log.d("PhoneAuthAudit", "Resending verification for: $formattedPhone")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    try {
                        signInWithPhoneCredential(credential)
                    } catch (e: Exception) {
                        authError.value = "Auto-verification failed: ${e.message}"
                        isVerificationInProgress.value = false
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                authError.value = "Resend failed: ${e.localizedMessage ?: e.message}"
                isVerificationInProgress.value = false
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId.value = id
                forceResendingToken = token
                isCodeSent.value = true
                isVerificationInProgress.value = false
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .setForceResendingToken(token)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            authError.value = "Resend error: ${e.localizedMessage ?: e.message}"
            isVerificationInProgress.value = false
        }
    }

    suspend fun verifyAndSignIn(otpCode: String): Boolean {
        val verId = verificationId.value
        if (verId.isEmpty()) {
            authError.value = "SMS session invalid. Send a new code."
            return false
        }
        
        try {
            isVerificationInProgress.value = true
            val credential = PhoneAuthProvider.getCredential(verId, otpCode)
            val success = signInWithPhoneCredential(credential)
            isVerificationInProgress.value = false
            return success
        } catch (e: Exception) {
            authError.value = "Incorrect verification code. Please try again."
            isVerificationInProgress.value = false
            return false
        }
    }

    private suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Boolean {
        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val authResult = com.google.android.gms.tasks.Tasks.await(firebaseAuth.signInWithCredential(credential))
        val firebaseUser = authResult.user ?: return false
        val uid = firebaseUser.uid
        val phone = firebaseUser.phoneNumber ?: ""
        
        if (otpContext == "signup") {
            val signupSuccess = repository.signUpUserWithPhone(
                uid = uid,
                name = signupName,
                phoneNumber = phone,
                referralCodeEntered = signupReferralCode
            )
            return signupSuccess
        } else {
            val loginSuccess = repository.loginWithPhoneUid(uid, phone)
            return loginSuccess
        }
    }

    suspend fun loginWithPassword(emailOrPhone: String, password: String): Boolean {
        return repository.loginWithPassword(emailOrPhone, password)
    }

    suspend fun signUpUser(name: String, emailOrPhone: String, passwordHash: String, referralCode: String = ""): Boolean {
        android.util.Log.d("ReferralAudit", "ViewModel signUpUser: referralCode received: $referralCode")
        return repository.signUpUser(name, emailOrPhone, passwordHash, referralCode)
    }

    suspend fun forgotPasswordReset(emailOrPhone: String, newPasswordHash: String): Boolean {
        return repository.forgotPasswordReset(emailOrPhone, newPasswordHash)
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun createProfile(name: String, email: String, referralCode: String) {
        viewModelScope.launch {
            repository.createProfile(name, email, referralCode)
        }
    }

    fun saveUserProfileAndBank(
        name: String,
        email: String,
        phone: String,
        accountHolderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String,
        upiId: String
    ) {
        viewModelScope.launch {
            repository.saveUserProfileAndBank(name, email, phone, accountHolderName, bankName, accountNumber, ifscCode, upiId)
        }
    }

    fun addMoney(amount: Double) {
        launchNetworkAction {
            repository.addMoney(amount)
        }
    }

    fun approveDeposit(id: String, onSuccess: () -> Unit = {}) {
        launchNetworkAction {
            repository.approveDeposit(id)
            onSuccess()
        }
    }

    fun rejectDeposit(id: String, onSuccess: () -> Unit = {}) {
        launchNetworkAction {
            repository.rejectDeposit(id)
            onSuccess()
        }
    }

    fun withdrawMoney(amount: Double, upiId: String = "", bank: String = "Bank", onSuccess: () -> Unit = {}) {
        launchNetworkAction {
            withdrawError.value = null
            withdrawSuccess.value = false
            repository.withdrawMoney(amount, upiId, bank)
            withdrawSuccess.value = true
            onSuccess()
        }
    }

    fun approveWithdrawal(id: String) {
        launchNetworkAction {
            repository.approveWithdrawal(id)
        }
    }

    fun rejectWithdrawal(id: String) {
        launchNetworkAction {
            repository.rejectWithdrawal(id)
        }
    }

    fun investSelectedPlan(amount: Double) {
        val planId = _selectedPlanId.value ?: return
        launchNetworkAction {
            repository.investInPlan(planId, amount)
        }
    }

    fun completeTask(taskId: String) {
        launchNetworkAction {
            repository.completeTask(taskId)
        }
    }

    fun claimDailyCheckIn() {
        launchNetworkAction {
            repository.claimDailyCheckIn()
        }
    }

    fun watchVideoAndEarn() {
        launchNetworkAction {
            repository.watchVideoAndEarn()
        }
    }

    fun redeemCoins(amount: Int, upiId: String = "") {
        launchNetworkAction {
            repository.redeemCoins(amount, upiId)
        }
    }

    fun resetAllData() {
        launchNetworkAction {
            repository.resetAllData()
        }
    }

    fun simulatePassageOfDay(planId: String) {
        launchNetworkAction {
            repository.simulatePassageOfDay(planId)
        }
    }

    fun addGlobalPlan(plan: MoneyMitraGlobalPlan, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            apiLoading.value = true
            apiError.value = null
            try {
                repository.addGlobalPlan(plan)
                onSuccess()
            } catch (e: Exception) {
                apiError.value = mapExceptionToMessage(e)
                onFailure(e)
            } finally {
                apiLoading.value = false
            }
        }
    }

    fun updateGlobalPlan(plan: MoneyMitraGlobalPlan, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            apiLoading.value = true
            apiError.value = null
            try {
                repository.updateGlobalPlan(plan)
                onSuccess()
            } catch (e: Exception) {
                apiError.value = mapExceptionToMessage(e)
                onFailure(e)
            } finally {
                apiLoading.value = false
            }
        }
    }

    fun toggleGlobalPlanStatus(planId: String, isActive: Boolean, onSuccess: () -> Unit = {}) {
        launchNetworkAction {
            repository.toggleGlobalPlanStatus(planId, isActive)
            onSuccess()
        }
    }

    fun deleteGlobalPlan(planId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            apiLoading.value = true
            apiError.value = null
            try {
                repository.deleteGlobalPlan(planId)
                onSuccess()
            } catch (e: Exception) {
                apiError.value = mapExceptionToMessage(e)
                onFailure(e)
            } finally {
                apiLoading.value = false
            }
        }
    }

    fun buyGlobalPlan(planId: String, planName: String, planAmount: Double, durationDays: Int, dailyReturn: Double, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        if (apiLoading.value) return
        apiLoading.value = true
        viewModelScope.launch {
            apiError.value = null
            try {
                repository.buyGlobalPlan(planId, planName, planAmount, durationDays, dailyReturn)
                onSuccess()
            } catch (e: Exception) {
                apiError.value = mapExceptionToMessage(e)
                onFailure(e)
            } finally {
                apiLoading.value = false
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }
}
