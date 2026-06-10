package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class DbUser(
    @PrimaryKey val emailOrPhone: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val balance: Double = 0.0,
    val lockedBalance: Double = 0.0,
    val investedAmount: Double = 0.0,
    val earnings: Double = 0.0,
    val coins: Int = 0,
    val isProfileCreated: Boolean = false,
    val accountHolderName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val upiId: String = "",
    val referralCode: String = "",
    val referredBy: String = "",
    val totalReferrals: Int = 0,
    val referralEarnings: Double = 0.0,
    val lastCheckInDate: String = "",
    val videosWatchedToday: Int = 0,
    val lastVideoResetDate: String = "",
    val totalCoinsEarned: Int = 0,
    val totalCoinsRedeemed: Int = 0
)

@Entity(tableName = "user_session")
data class UserSession(
    @PrimaryKey val id: Int = 1,
    val phoneNumber: String = "",
    val name: String = "",
    val email: String = "",
    val referralCode: String = "",
    val balance: Double = 0.0,
    val lockedBalance: Double = 0.0,
    val investedAmount: Double = 0.0,
    val earnings: Double = 0.0,
    val coins: Int = 0,
    val isOnboarded: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isProfileCreated: Boolean = false,
    val accountHolderName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val upiId: String = "",
    val referredBy: String = "",
    val totalReferrals: Int = 0,
    val referralEarnings: Double = 0.0,
    val lastCheckInDate: String = "",
    val videosWatchedToday: Int = 0,
    val lastVideoResetDate: String = "",
    val totalCoinsEarned: Int = 0,
    val totalCoinsRedeemed: Int = 0
)

@Entity(tableName = "transactions")
data class DbTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateText: String, // e.g. "Today, 09:30 AM" or "Yesterday, 06:30 PM"
    val amount: Double,
    val type: String, // "CREDIT", "DEBIT", "COIN_EARN", "COIN_REDEEM"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "investment_plans", primaryKeys = ["uid", "planId"])
data class DbInvestmentPlan(
    val uid: String,
    val planId: String, // starter, growth, premium
    val name: String,
    val returnsRange: String, // "8% - 10% Returns (p.a.)", etc.
    val minInvestment: Double,
    val investedAmount: Double = 0.0,
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
    val status: String = "AVAILABLE", // AVAILABLE, ACTIVE, COMPLETED
    val activationDate: String = "",
    val expiryDate: String = "",
    val dailyEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val daysCompleted: Int = 0,
    val daysRemaining: Int = 0
)

@Entity(tableName = "daily_tasks")
data class DbDailyTask(
    @PrimaryKey val taskId: String, // checkin, watch_ad, invite, article, quiz
    val title: String,
    val description: String,
    val coinReward: Int,
    val isCompleted: Boolean = false,
    val buttonText: String = "Claim"
)

data class MoneyMitraTransaction(
    val transactionId: String = "",
    val uid: String = "",
    val type: String = "", // DEPOSIT, WITHDRAWAL, REFERRAL_BONUS, REFERRAL_REWARD, INVESTMENT_PURCHASE, INVESTMENT_RETURN, ADMIN_CREDIT, ADMIN_DEBIT
    val amount: Double = 0.0,
    val description: String = "",
    val status: String = "SUCCESS", // SUCCESS, PENDING, APPROVED, REJECTED, COMPLETED
    val createdAt: Long = System.currentTimeMillis()
)

data class MoneyMitraWithdrawal(
    val id: String = "",
    val uid: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val upiId: String = "",
    val bankName: String = "",
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = ""
)

data class MoneyMitraGlobalPlan(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val durationDays: Int = 0,
    val dailyReturn: Double = 0.0, // e.g. 2.0 (%)
    val totalReturn: Double = 0.0, // e.g. 60.0 (%)
    val isActive: Boolean = true,
    val minAmount: Double = 0.0,
    val maxAmount: Double = 0.0
)

data class MoneyMitraActiveInvestment(
    val id: String = "",
    val uid: String = "",
    val planId: String = "",
    val name: String = "",
    val investedAmount: Double = 0.0,
    val activationDate: String = "",
    val expiryDate: String = "",
    val status: String = "ACTIVE", // ACTIVE, MATURED
    val dailyEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val daysCompleted: Int = 0,
    val daysRemaining: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class MoneyMitraReferral(
    val id: String = "",
    val referrerUid: String = "",
    val referrerName: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referredEmailOrPhone: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, COMPLETED
    val amount: Double = 50.0,
    val referredRewardAmount: Double = 20.0
)

data class MoneyMitraNotification(
    val notificationId: String = "",
    val uid: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // e.g., DEPOSIT_APPROVED, DEPOSIT_REJECTED, WITHDRAWAL_SUBMITTED, WITHDRAWAL_APPROVED, WITHDRAWAL_REJECTED, INVESTMENT_PURCHASED, INVESTMENT_MATURED, REFERRAL_REWARD, ANNOUNCEMENT
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


