package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.MoneyMitraNotification
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: MoneyMitraViewModel
) {
    val notifications by viewModel.userNotifications.collectAsState()
    
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Unread", "Completed", "Pending", "Announcements")
    
    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Unread" -> notifications.filter { !it.isRead }
            "Completed" -> notifications.filter { 
                it.type in listOf("DEPOSIT_APPROVED", "WITHDRAWAL_APPROVED", "INVESTMENT_MATURED", "REFERRAL_REWARD") 
            }
            "Pending" -> notifications.filter { 
                it.type in listOf("WITHDRAWAL_SUBMITTED") 
            }
            "Announcements" -> notifications.filter { 
                it.type == "GENERAL" || it.type == "ANNOUNCEMENT" || it.uid == "broadcast"
            }
            else -> notifications
        }
    }

    var pageSizeLimit by remember { mutableStateOf(20) }
    val paginatedNotifications = remember(filteredNotifications, pageSizeLimit) {
        filteredNotifications.take(pageSizeLimit)
    }
    
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notifications",
                            fontWeight = FontWeight.Bold,
                            color = MitraTextMain,
                            fontSize = 20.sp
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .background(MitraErrorRed, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount New",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MitraTextMain
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Text(
                                text = "Mark All Read",
                                color = MitraPrimaryGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MitraSurface,
                    titleContentColor = MitraTextMain
                )
            )
        },
        containerColor = MitraBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Pills Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MitraPrimaryGreen else MitraSurface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MitraPrimaryGreen else MitraBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_pill_$filter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else MitraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MitraLightGreenBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "No Notifications",
                                tint = MitraPrimaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Caught Up!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MitraTextMain
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedFilter == "All") {
                                "You don't have any notifications right now. Activity and administrative announcements will appear here."
                            } else {
                                "No notifications found matching filter '$selectedFilter'."
                            },
                            fontSize = 13.sp,
                            color = MitraTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(paginatedNotifications, key = { it.notificationId }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onMarkRead = { viewModel.markNotificationAsRead(notification.notificationId) }
                        )
                    }
                    if (filteredNotifications.size > pageSizeLimit) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { pageSizeLimit += 20 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                    modifier = Modifier.testTag("load_more_notifications")
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
fun NotificationItem(
    notification: MoneyMitraNotification,
    onMarkRead: () -> Unit
) {
    val detail = getNotificationDetails(notification.type)
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MitraSurface else MitraLightGreenBg
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (notification.isRead) MitraBorder else MitraPrimaryGreen.copy(0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                if (!notification.isRead) {
                    onMarkRead()
                }
            }
            .testTag("notification_item_${notification.notificationId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon space matching style of category
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(detail.bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = detail.icon,
                    contentDescription = notification.type,
                    tint = detail.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = MitraTextMain,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatTimeElapsed(notification.createdAt),
                        fontSize = 11.sp,
                        color = MitraTextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = if (notification.isRead) MitraTextSecondary else MitraTextMain,
                    lineHeight = 18.sp
                )

                if (!notification.isRead) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MitraPrimaryGreen, CircleShape)
                        )
                        Text(
                            text = "Tap to mark read",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MitraPrimaryGreen
                        )
                    }
                }
            }
        }
    }
}

data class NotificationUIConfig(
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color
)

fun getNotificationDetails(type: String): NotificationUIConfig {
    return when (type) {
        "DEPOSIT_APPROVED" -> NotificationUIConfig(
            icon = Icons.Default.CheckCircle,
            color = MitraSuccessGreen,
            bgColor = Color(0xFFE8F8F0)
        )
        "DEPOSIT_REJECTED" -> NotificationUIConfig(
            icon = Icons.Default.Cancel,
            color = MitraErrorRed,
            bgColor = Color(0xFFFDE8E8)
        )
        "WITHDRAWAL_SUBMITTED" -> NotificationUIConfig(
            icon = Icons.Default.Schedule,
            color = Color(0xFF3B82F6),
            bgColor = Color(0xFFEFF6FF)
        )
        "WITHDRAWAL_APPROVED" -> NotificationUIConfig(
            icon = Icons.Default.DoubleArrow,
            color = MitraSuccessGreen,
            bgColor = Color(0xFFE8F8F0)
        )
        "WITHDRAWAL_REJECTED" -> NotificationUIConfig(
            icon = Icons.Default.ErrorOutline,
            color = MitraErrorRed,
            bgColor = Color(0xFFFDE8E8)
        )
        "INVESTMENT_PURCHASED" -> NotificationUIConfig(
            icon = Icons.Default.Stars,
            color = MitraAccentGold,
            bgColor = Color(0xFFFFFBEB)
        )
        "INVESTMENT_MATURED" -> NotificationUIConfig(
            icon = Icons.Default.EmojiEvents,
            color = MitraAccentGold,
            bgColor = Color(0xFFFFFBEB)
        )
        "REFERRAL_REWARD" -> NotificationUIConfig(
            icon = Icons.Default.Redeem,
            color = Color(0xFFEC4899),
            bgColor = Color(0xFFFDF2F8)
        )
        "ANNOUNCEMENT" -> NotificationUIConfig(
            icon = Icons.Default.Campaign,
            color = MitraAccentGold,
            bgColor = Color(0xFFFFFBEB)
        )
        else -> NotificationUIConfig(
            icon = Icons.Default.Campaign,
            color = MitraPrimaryGreen,
            bgColor = MitraLightGreenBg
        )
    }
}

fun formatTimeElapsed(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 7) return "${days}d ago"
    
    val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
