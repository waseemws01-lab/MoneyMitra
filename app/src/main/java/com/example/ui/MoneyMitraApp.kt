package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.DbDailyTask
import com.example.data.DbInvestmentPlan
import com.example.data.DbTransaction
import com.example.data.MoneyMitraGlobalPlan
import com.example.data.MoneyMitraActiveInvestment
import com.example.data.MoneyMitraTransaction
import com.example.data.UserSession
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyMitraApp(
    viewModel: MoneyMitraViewModel = viewModel()
) {
    val navController = rememberNavController()
    val session by viewModel.userSession.collectAsState()
    val apiLoading by viewModel.apiLoading.collectAsState()
    val apiError by viewModel.apiError.collectAsState()

    // Determine initial startup route
    val startRoute = when {
        session == null -> "splash"
        !session!!.isOnboarded -> "onboarding"
        !session!!.isLoggedIn -> "login"
        !session!!.isProfileCreated -> "create_profile"
        else -> "dashboard_container"
    }

    // Key container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            composable("splash") {
                SplashScreen(navController, session)
            }
            composable("onboarding") {
                OnboardingScreen(navController, viewModel)
            }
            composable("login") {
                LoginScreen(navController, viewModel)
            }
            composable("signup") {
                SignupScreen(navController, viewModel)
            }
            composable("otp") {
                OtpScreen(navController, viewModel)
            }
            composable("create_profile") {
                CreateProfileScreen(navController, viewModel)
            }
            composable("dashboard_container") {
                DashboardContainer(navController, viewModel)
            }
            composable("add_money") {
                AddMoneyScreen(navController, viewModel)
            }
            composable("upi_payment") {
                UpiPaymentScreen(navController, viewModel)
            }
            composable("payment_success") {
                PaymentSuccessScreen(navController)
            }
            composable("withdrawal_success") {
                WithdrawalSuccessScreen(navController, viewModel)
            }
            composable("coin_redemption_success/{coins}/{amount}") { backStackEntry ->
                val coins = backStackEntry.arguments?.getString("coins")?.toIntOrNull() ?: 0
                val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                CoinRedemptionSuccessScreen(navController, viewModel, coins, amount)
            }
            composable("withdraw_money") {
                WithdrawMoneyScreen(navController, viewModel)
            }
            composable("transaction_history") {
                TransactionHistoryScreen(navController, viewModel)
            }
            composable("daily_tasks") {
                TasksHomeView(navController, viewModel)
            }
            composable("redeem_coins") {
                RedeemCoinsScreen(navController, viewModel)
            }
            composable("reward_history") {
                RewardHistoryScreen(navController, viewModel)
            }
            composable("refer_earn") {
                ReferEarnScreen(navController, viewModel)
            }
            composable("about_us") {
                AboutUsScreen(navController)
            }
            composable("edit_profile") {
                EditProfileScreen(navController, viewModel)
            }
            composable("bank_account") {
                BankAccountScreen(navController, viewModel)
            }
            composable("forgot_password") {
                ForgotPasswordScreen(navController, viewModel)
            }
            composable("admin_dashboard") {
                AdminDashboardScreen(navController, viewModel)
            }
            composable("notifications") {
                NotificationsScreen(navController, viewModel)
            }
        }

        // Global Loading Progress Overlay
        if (apiLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = true, onClick = {}) // block clicks during operations
                    .testTag("api_loading_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MitraPrimaryGreen)
                        Text(
                            text = "Please wait...",
                            fontWeight = FontWeight.Medium,
                            color = MitraTextMain,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Global Error Dismissible Toast/Banner
        apiError?.let { errorText ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .testTag("api_error_overlay"),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)), // Red
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 60.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.clearApiError() }
                        ) {
                            Text("DISMISS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// 01. Splash Screen
@Composable
fun MoneyMitraLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 180.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            
            val walletTop = h * 0.35f
            val walletBottom = h * 0.95f
            val walletLeft = w * 0.15f
            val walletRight = w * 0.85f
            val walletCenterY = (walletTop + walletBottom) / 2
            
            // --- DRAW SYSTEM: LEAVES ---
            val stemX = w * 0.5f
            val stemY = walletTop + 5f
            
            // 1. LEFT LEAF
            val leftLeafPath = Path().apply {
                moveTo(stemX, stemY)
                cubicTo(
                    stemX - w * 0.15f, stemY - h * 0.05f,
                    w * 0.22f, h * 0.22f,
                    w * 0.22f, h * 0.10f
                )
                cubicTo(
                    w * 0.25f, h * 0.05f,
                    stemX - w * 0.05f, stemY - h * 0.25f,
                    stemX, stemY
                )
            }
            drawPath(
                path = leftLeafPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8CE23B), Color(0xFF137C3C)),
                    start = Offset(w * 0.22f, h * 0.10f),
                    end = Offset(stemX, stemY)
                )
            )
            
            // 2. RIGHT LEAF
            val rightLeafPath = Path().apply {
                moveTo(stemX, stemY)
                cubicTo(
                    stemX + w * 0.15f, stemY - h * 0.05f,
                    w * 0.78f, h * 0.22f,
                    w * 0.78f, h * 0.10f
                )
                cubicTo(
                    w * 0.75f, h * 0.05f,
                    stemX + w * 0.05f, stemY - h * 0.25f,
                    stemX, stemY
                )
            }
            drawPath(
                path = rightLeafPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8CE23B), Color(0xFF137C3C)),
                    start = Offset(w * 0.78f, h * 0.10f),
                    end = Offset(stemX, stemY)
                )
            )
            
            // Leaf veins
            val leftVein = Path().apply {
                moveTo(stemX, stemY)
                quadraticTo(w * 0.35f, h * 0.20f, w * 0.22f, h * 0.10f)
            }
            drawPath(
                path = leftVein,
                color = Color(0xFFA6EE5C),
                style = Stroke(width = 3f)
            )
            
            val rightVein = Path().apply {
                moveTo(stemX, stemY)
                quadraticTo(w * 0.65f, h * 0.20f, w * 0.78f, h * 0.10f)
            }
            drawPath(
                path = rightVein,
                color = Color(0xFFA6EE5C),
                style = Stroke(width = 3f)
            )

            // --- DRAW SYSTEM: WALLET HIGHLIGHTS & BODY ---
            // Background shadows/folded money pages
            val card1 = Path().apply {
                val c1Left = walletLeft + w * 0.05f
                val c1Right = walletRight - w * 0.05f
                val c1Top = walletTop - h * 0.06f
                moveTo(c1Left, walletTop)
                lineTo(c1Left, c1Top + h * 0.02f)
                quadraticTo(c1Left, c1Top, c1Left + w * 0.04f, c1Top)
                lineTo(c1Right - w * 0.04f, c1Top)
                quadraticTo(c1Right, c1Top, c1Right, c1Top + h * 0.02f)
                lineTo(c1Right, walletTop)
                close()
            }
            drawPath(
                path = card1,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F5A2B), Color(0xFF1C9E4F)),
                    start = Offset(w * 0.5f, walletTop - h * 0.06f),
                    end = Offset(w * 0.5f, walletTop)
                )
            )
            
            val card2 = Path().apply {
                val c2Left = walletLeft + w * 0.08f
                val c2Right = walletRight - w * 0.08f
                val c2Top = walletTop - h * 0.03f
                moveTo(c2Left, walletTop)
                lineTo(c2Left, c2Top + h * 0.01f)
                quadraticTo(c2Left, c2Top, c2Left + w * 0.03f, c2Top)
                lineTo(c2Right - w * 0.03f, c2Top)
                quadraticTo(c2Right, c2Top, c2Right, c2Top + h * 0.01f)
                lineTo(c2Right, walletTop)
                close()
            }
            drawPath(
                path = card2,
                color = Color(0xFFE4F5EB)
            )
            
            // Primary Wallet Rounded Rect
            val walletRect = Rect(
                left = walletLeft,
                top = walletTop,
                right = walletRight,
                bottom = walletBottom
            )
            val walletPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = walletRect,
                        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
                    )
                )
            }
            drawPath(
                path = walletPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2ECC71), Color(0xFF137C3C)),
                    startY = walletTop,
                    endY = walletBottom
                )
            )
            
            // Side clasp background (Navy skin overlay)
            val claspLeft = walletRight - w * 0.15f
            val claspRight = walletRight + w * 0.05f
            val claspTop = walletCenterY - h * 0.1f
            val claspBottom = walletCenterY + h * 0.1f
            val claspH = claspBottom - claspTop
            val claspRect = Rect(
                left = claspLeft,
                top = claspTop,
                right = claspRight,
                bottom = claspBottom
            )
            val claspPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = claspRect,
                        cornerRadius = CornerRadius(claspH * 0.3f, claspH * 0.3f)
                    )
                )
            }
            drawPath(
                path = claspPath,
                color = Color(0xFF0D2545)
            )
            
            // Clasp snap highlights
            drawCircle(
                color = Color.White,
                radius = claspH * 0.20f,
                center = Offset(claspLeft + (claspRight - claspLeft) * 0.35f, walletCenterY)
            )
            drawCircle(
                color = Color(0xFF2ECC71),
                radius = claspH * 0.08f,
                center = Offset(claspLeft + (claspRight - claspLeft) * 0.35f, walletCenterY)
            )
            
            // Beautiful Stitching Dashed Outline
            val inset = w * 0.04f
            val stitchingRect = Rect(
                left = walletLeft + inset,
                top = walletTop + inset,
                right = walletRight - inset,
                bottom = walletBottom - inset
            )
            val stitchingPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = stitchingRect,
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                )
            }
            drawPath(
                path = stitchingPath,
                color = Color(0xFFC5EAD0),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(8f, 8f),
                        phase = 0f
                    )
                )
            )
        }
        
        // India Rupee symbol beautifully drawn inside the wallet card area
        Text(
            text = "₹",
            color = Color.White,
            fontSize = (size.value * 0.32f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = size * 0.11f)
        )
    }
}

@Composable
fun SplashScreen(navController: NavHostController, session: UserSession?) {
    LaunchedEffect(session) {
        if (session != null) {
            kotlinx.coroutines.delay(2000)
            val dest = when {
                !session.isOnboarded -> "onboarding"
                !session.isLoggedIn -> "login"
                !session.isProfileCreated -> "create_profile"
                else -> "dashboard_container"
            }
            navController.navigate(dest) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Dynamic scale and fade animatables
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                )
            )
        }
    }

    val splashGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFEBF5EE), Color(0xFFF0FDF4))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(splashGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animate graphic elements using the graphicsLayer modifier
        Column(
            modifier = Modifier.graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                alpha = alpha.value
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Logo Frame with subtle dynamic glow shadow
            Box(
                modifier = Modifier
                    .size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .offset(y = 12.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF137C3C).copy(0.12f), Color.Transparent)
                            )
                        )
                )
                MoneyMitraLogo(size = 180.dp)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Premium wordmark recreating file branding
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Money",
                    color = Color(0xFF0D2545), // Navy Blue matching brand logo
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Mitra",
                    color = Color(0xFF137C3C), // Accent Green matching brand logo
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            // Tagline flanked by elegant dynamic horizontal line accents
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.5.dp)
                        .background(Color(0xFF137C3C).copy(0.35f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Helping your money grow.",
                    color = Color(0xFF556B82),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.5.dp)
                        .background(Color(0xFF137C3C).copy(0.35f))
                )
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        // Professional sleek dot indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(6.dp)
                        .background(
                            color = Color(0xFF137C3C).copy(alpha = if (index == 0) 0.8f else 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// 02. Onboarding Screen Core & Visual components

@Composable
fun OnboardingVisualScreen1() {
    val infiniteTransition = rememberInfiniteTransition(label = "screen1")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "graph_progress"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val particles = remember {
        List(5) { i ->
            val initialX = 0.2f + (0.15f * i)
            val velocity = 0.6f + (0.2f * i)
            val size = 6f + (2f * i)
            Triple(initialX, velocity, size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingX = 40f
            val paddingY = 40f
            val chartWidth = width - 2 * paddingX
            val chartHeight = height - 2 * paddingY

            // Draw grid background (subtle thin lines)
            val numGridLines = 4
            for (i in 0..numGridLines) {
                val y = paddingY + (chartHeight / numGridLines) * i
                drawLine(
                    color = MitraBorder.copy(alpha = 0.4f),
                    start = Offset(paddingX, y),
                    end = Offset(width - paddingX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Upward growth graph coordinate mapping
            val points = listOf(
                Offset(paddingX, paddingY + chartHeight * 0.85f),
                Offset(paddingX + chartWidth * 0.25f, paddingY + chartHeight * 0.70f),
                Offset(paddingX + chartWidth * 0.5f, paddingY + chartHeight * 0.55f),
                Offset(paddingX + chartWidth * 0.75f, paddingY + chartHeight * 0.30f),
                Offset(width - paddingX, paddingY + chartHeight * 0.15f)
            )

            // Dynamic growth path
            val linePath = Path()
            val fillPath = Path()
            
            if (points.isNotEmpty()) {
                val p0 = points.first()
                linePath.moveTo(p0.x, p0.y)
                fillPath.moveTo(p0.x, p0.y)

                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cp1 = Offset(prev.x + (curr.x - prev.x) / 2f, prev.y)
                    val cp2 = Offset(prev.x + (curr.x - prev.x) / 2f, curr.y)
                    
                    // Partial path calculation based on progress
                    val currentSectionProgress = ((progress - (i - 1) / 4f) * 4f).coerceIn(0f, 1f)
                    if (currentSectionProgress > 0f) {
                        val midX = prev.x + (curr.x - prev.x) * currentSectionProgress
                        val midY = prev.y + (curr.y - prev.y) * currentSectionProgress
                        val midCp1 = Offset(prev.x + (midX - prev.x) / 2f, prev.y)
                        val midCp2 = Offset(prev.x + (midX - prev.x) / 2f, midY)
                        linePath.cubicTo(midCp1.x, midCp1.y, midCp2.x, midCp2.y, midX, midY)
                        fillPath.cubicTo(midCp1.x, midCp1.y, midCp2.x, midCp2.y, midX, midY)
                    }
                }
                
                // Close for fill
                val lastVisibleX = paddingX + chartWidth * progress
                val lastVisibleScaleY = 0.85f - (0.85f - 0.15f) * progress
                val lastVisibleY = paddingY + chartHeight * lastVisibleScaleY
                fillPath.lineTo(lastVisibleX, height - paddingY)
                fillPath.lineTo(paddingX, height - paddingY)
                fillPath.close()
            }

            // Fill background gradient (premium emerald look)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MitraPrimaryGreen.copy(alpha = 0.25f),
                        MitraPrimaryGreen.copy(alpha = 0.0f)
                    ),
                    startY = paddingY,
                    endY = height - paddingY
                )
            )

            // Draw line
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(MitraPrimaryGreen.copy(0.7f), MitraPrimaryGreen)
                ),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.cornerPathEffect(40f)
                )
            )

            // Pulsing node at current peak
            if (progress > 0.05f) {
                val currentX = paddingX + chartWidth * progress
                val currentY = (paddingY + chartHeight * 0.85f) - (chartHeight * 0.70f * progress)

                // Pulse ring
                drawCircle(
                    color = MitraPrimaryGreen.copy(alpha = 0.25f * (2f - pulseScale)),
                    radius = 18.dp.toPx() * pulseScale,
                    center = Offset(currentX, currentY)
                )

                // Highlight core dot
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
                drawCircle(
                    color = MitraPrimaryGreen,
                    radius = 5.dp.toPx(),
                    center = Offset(currentX, currentY)
                )
            }

            // Rising particles inside canvas
            particles.forEachIndexed { idx, part ->
                val (initialX, velocity, size) = part
                // Calculate y coordinate based on individual progress speed
                val partProgress = (progress * velocity + (idx * 0.15f)) % 1f
                val py = (height - paddingY) - (chartHeight * partProgress)
                val px = paddingX + (chartWidth * initialX) + (20f * kotlin.math.sin(partProgress * Math.PI.toFloat() * 3f))
                
                if (py > paddingY && py < height - paddingY) {
                    drawCircle(
                        color = MitraPrimaryGreen.copy(alpha = 0.35f * (1f - partProgress)),
                        radius = size,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingVisualScreen2() {
    val infiniteTransition = rememberInfiniteTransition(label = "screen2")
    
    // Plant growth progress
    val plantGrowth by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plant_growth"
    )

    // Gold coins transition path
    val coinTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coin_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f

            // Draw clean fintech floor base
            drawLine(
                color = MitraBorder.copy(0.6f),
                start = Offset(40f, height - 50f),
                end = Offset(width - 40f, height - 50f),
                strokeWidth = 2.dp.toPx()
            )

            // DRAW WALLET (Left position)
            val walletLeft = centerX - 180f
            val walletTop = height - 120f
            val walletWidth = 100f
            val walletHeight = 70f
            
            // Outer wallet back
            drawRoundRect(
                color = MitraPrimaryGreen.copy(0.12f),
                topLeft = Offset(walletLeft, walletTop),
                size = androidx.compose.ui.geometry.Size(walletWidth, walletHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = MitraPrimaryGreen,
                topLeft = Offset(walletLeft, walletTop),
                size = androidx.compose.ui.geometry.Size(walletWidth, walletHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
            // Wallet flap
            val flapPath = Path().apply {
                moveTo(walletLeft + walletWidth * 0.4f, walletTop + walletHeight * 0.1f)
                lineTo(walletLeft + walletWidth * 0.85f, walletTop + walletHeight * 0.35f)
                lineTo(walletLeft + walletWidth * 0.4f, walletTop + walletHeight * 0.6f)
                close()
            }
            drawPath(path = flapPath, color = MitraBackground)
            drawPath(path = flapPath, color = MitraPrimaryGreen, style = Stroke(width = 2.dp.toPx()))
            // Gold clasp button
            drawCircle(
                color = MitraAccentGold,
                radius = 4.dp.toPx(),
                center = Offset(walletLeft + walletWidth * 0.65f, walletTop + walletHeight * 0.35f)
            )

            // DRAW GROWING PLANT POT & STEM (Right position)
            val potCenterX = centerX + 120f
            val potTop = height - 110f
            
            // Flowerpot
            val potPath = Path().apply {
                moveTo(potCenterX - 35f, potTop)
                lineTo(potCenterX + 35f, potTop)
                lineTo(potCenterX + 25f, potTop + 50f)
                lineTo(potCenterX - 25f, potTop + 50f)
                close()
            }
            drawPath(path = potPath, color = MitraPrimaryGreen.copy(0.11f))
            drawPath(path = potPath, color = MitraPrimaryGreen, style = Stroke(width = 2.dp.toPx()))

            // Plant stem
            val stemBaseY = potTop
            val currentPlantHeight = 110f * plantGrowth
            val stemTopY = stemBaseY - currentPlantHeight
            
            drawLine(
                color = MitraPrimaryGreen,
                start = Offset(potCenterX, stemBaseY),
                end = Offset(potCenterX, stemTopY),
                strokeWidth = 3.dp.toPx()
            )

            // Branches and Leaves
            if (plantGrowth > 0.5f) {
                // First leaf pair (Left)
                val leaf1Y = stemBaseY - (currentPlantHeight * 0.4f)
                drawOval(
                    color = MitraPrimaryGreen,
                    topLeft = Offset(potCenterX - 32f, leaf1Y - 12f),
                    size = androidx.compose.ui.geometry.Size(30f * plantGrowth, 16f * plantGrowth)
                )
                // Second leaf pair (Right)
                val leaf2Y = stemBaseY - (currentPlantHeight * 0.7f)
                drawOval(
                    color = MitraPrimaryGreen,
                    topLeft = Offset(potCenterX + 2f, leaf2Y - 12f),
                    size = androidx.compose.ui.geometry.Size(30f * plantGrowth, 16f * plantGrowth)
                )
                // Top growing bud
                drawCircle(
                    color = MitraSuccessGreen,
                    radius = 9f * plantGrowth,
                    center = Offset(potCenterX, stemTopY)
                )
            }

            // COINS ARCH ANIMATION (From wallet back opening into the plant top)
            // Coin 1 path (Arc)
            val startPoint = Offset(walletLeft + walletWidth / 2f, walletTop + 10f)
            val endPoint = Offset(potCenterX, stemTopY - 10f)
            
            // Let's draw 3 consecutive gold coins traveling on delay
            val coinDelays = listOf(0.0f, 0.35f, 0.7f)
            coinDelays.forEach { delayValue ->
                val rawVal = (coinTime + delayValue) % 1.0f
                if (rawVal in 0.01f..0.99f) {
                    // Parabolic arc path
                    val tx = startPoint.x + (endPoint.x - startPoint.x) * rawVal
                    // peak heights
                    val curveHeight = 120f
                    val ty = startPoint.y + (endPoint.y - startPoint.y) * rawVal - (curveHeight * kotlin.math.sin(rawVal * Math.PI.toFloat()))

                    // Draw golden coin visual with a subtle inner border
                    drawCircle(
                        color = MitraAccentGold,
                        radius = 8.dp.toPx(),
                        center = Offset(tx, ty)
                    )
                    drawCircle(
                        color = Color.White.copy(0.7f),
                        radius = 4.dp.toPx(),
                        center = Offset(tx, ty),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Soft floating particles around the plant
            for (i in 0..3) {
                val pRawX = potCenterX - 60f + (120f * (i / 3f))
                val pOscillate = (coinTime + (i * 0.25f)) % 1f
                val pY = potTop - 30f - (80f * pOscillate)
                drawCircle(
                    color = MitraPrimaryGreen.copy(0.25f * (1f - pOscillate)),
                    radius = 3.dp.toPx(),
                    center = Offset(pRawX + 15f * kotlin.math.sin(pOscillate * 6f), pY)
                )
            }
        }
    }
}

@Composable
fun OnboardingVisualScreen3() {
    val infiniteTransition = rememberInfiniteTransition(label = "screen3")
    
    // Cards sliding offsets
    val slideAnim by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cards_slide"
    )

    // Shield circular glow pulse
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // BACKGROUND SECURE GLOW RINGS
            drawCircle(
                color = MitraPrimaryGreen.copy(alpha = 0.06f * (2f - glowScale)),
                radius = 95.dp.toPx() * glowScale,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = MitraPrimaryGreen.copy(alpha = 0.09f),
                radius = 65.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // DRAW CARD 1 (Behind, sliding right, top-left styled)
            val cardHeight = 65.dp.toPx()
            val cardWidth = 140.dp.toPx()
            val card1X = centerX - 190f + slideAnim
            val card1Y = centerY - 100f
            
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(card1X, card1Y),
                size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = MitraBorder,
                topLeft = Offset(card1X, card1Y),
                size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Decorative line details
            drawLine(
                color = MitraPrimaryGreen,
                start = Offset(card1X + 20f, card1Y + 25f),
                end = Offset(card1X + 60f, card1Y + 25f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = MitraTextSecondary.copy(0.4f),
                start = Offset(card1X + 20f, card1Y + 45f),
                end = Offset(card1X + 110f, card1Y + 45f),
                strokeWidth = 2.dp.toPx()
            )

            // DRAW CARD 2 (In front, sliding left, bottom-right styled)
            val card2X = centerX + 60f - slideAnim
            val card2Y = centerY + 30f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(card2X, card2Y),
                size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawRoundRect(
                color = MitraBorder,
                topLeft = Offset(card2X, card2Y),
                size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Decorative growth mini bar-chart
            for (idx in 0..2) {
                val barH = (15f + (idx * 16f))
                drawRect(
                    color = MitraPrimaryGreen,
                    topLeft = Offset(card2X + 25f + (idx * 20f), card2Y + 45f - barH),
                    size = androidx.compose.ui.geometry.Size(12f, barH)
                )
            }
            drawLine(
                color = MitraTextMain,
                start = Offset(card2X + 85f, card2Y + 25f),
                end = Offset(card2X + 125f, card2Y + 25f),
                strokeWidth = 3.dp.toPx()
            )

            // DRAW GREEN SHIELD (Center)
            val shieldWidth = 60.dp.toPx()
            val shieldHeight = 70.dp.toPx()
            val shieldX = centerX - shieldWidth / 2f
            val shieldY = centerY - shieldHeight / 2f

            // Advanced Shield Path
            val shieldPath = Path().apply {
                moveTo(centerX, shieldY) // top-center
                quadraticTo(centerX + shieldWidth * 0.45f, shieldY, centerX + shieldWidth * 0.5f, shieldY + shieldHeight * 0.2f)
                lineTo(centerX + shieldWidth * 0.5f, shieldY + shieldHeight * 0.55f)
                quadraticTo(centerX + shieldWidth * 0.45f, shieldY + shieldHeight * 0.85f, centerX, shieldY + shieldHeight) // bottom-point
                quadraticTo(centerX - shieldWidth * 0.45f, shieldY + shieldHeight * 0.85f, centerX - shieldWidth * 0.5f, shieldY + shieldHeight * 0.55f)
                lineTo(centerX - shieldWidth * 0.5f, shieldY + shieldHeight * 0.2f)
                quadraticTo(centerX - shieldWidth * 0.45f, shieldY, centerX, shieldY)
                close()
            }

            drawPath(path = shieldPath, color = MitraLightGreenBg)
            drawPath(path = shieldPath, color = MitraPrimaryGreen, style = Stroke(width = 3.5.dp.toPx()))

            // Elegant checkmark inside shield (pops on pulsating beat)
            val checkmarkPath = Path().apply {
                moveTo(centerX - 12f, centerY + 2f)
                lineTo(centerX - 3f, centerY + 11f)
                lineTo(centerX + 14f, centerY - 8f)
            }
            drawPath(
                path = checkmarkPath,
                color = MitraPrimaryGreen,
                style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(2f))
            )
        }
    }
}

@Composable
fun OnboardingVisualScreen4() {
    val infiniteTransition = rememberInfiniteTransition(label = "screen4")
    
    // Rotation of floating coin
    val coinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coin_spin"
    )

    // Pulse effect on details card
    val cardPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallet_card_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
            scaleX = cardPulse
            scaleY = cardPulse
        }) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // Central Smart Wallet Card
            val centralW = 150.dp.toPx()
            val centralH = 100.dp.toPx()
            val cx = centerX - centralW / 2f
            val cy = centerY - centralH / 2f - 20f

            // Premium gradient card backdrop
            drawRoundRect(
                brush = Brush.linearGradient(listOf(MitraPrimaryGreen, MitraDeepGreen)),
                topLeft = Offset(cx, cy),
                size = androidx.compose.ui.geometry.Size(centralW, centralH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )

            // Inner styling card chip
            drawRoundRect(
                color = MitraAccentGold.copy(0.7f),
                topLeft = Offset(cx + 25f, cy + 25f),
                size = androidx.compose.ui.geometry.Size(40f, 30f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Simulated balance text line
            drawLine(
                color = Color.White.copy(0.5f),
                start = Offset(cx + 25f, cy + 70f),
                end = Offset(cx + 140f, cy + 70f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = Color.White,
                start = Offset(cx + 25f, cy + 85f),
                end = Offset(cx + 120f, cy + 85f),
                strokeWidth = 4.dp.toPx()
            )

            // Draw a spinning gold coin nearby
            val coinRad = 15.dp.toPx()
            val coinCX = centerX + 110f
            val coinCY = centerY - 80f

            // Simulate 3D rotation using standard ellipse width calculation
            val aspectScale = kotlin.math.cos(Math.toRadians(coinRotation.toDouble())).toFloat()
            
            drawCircle(
                color = MitraAccentGold,
                radius = coinRad,
                center = Offset(coinCX, coinCY)
            )
            drawCircle(
                color = Color.White.copy(0.4f),
                radius = coinRad * 0.6f,
                center = Offset(coinCX, coinCY),
                style = Stroke(width = 2.dp.toPx())
            )

            // Connected network indicators for refer-earn bonuses
            val node1 = Offset(centerX - 110f, centerY + 80f)
            val node2 = Offset(centerX + 110f, centerY + 80f)

            // Dashed connection lines to central wallet card
            val walletAnchor = Offset(centerX, cy + centralH)
            
            drawLine(
                color = MitraPrimaryGreen,
                start = walletAnchor,
                end = node1,
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            drawLine(
                color = MitraPrimaryGreen,
                start = walletAnchor,
                end = node2,
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Left connected node avatar
            drawCircle(
                color = MitraPrimaryGreen.copy(0.12f),
                radius = 16.dp.toPx(),
                center = node1
            )
            drawCircle(
                color = MitraPrimaryGreen,
                radius = 16.dp.toPx(),
                center = node1,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Tiny internal dot icon representing task checkmark / tick
            drawCircle(
                color = MitraPrimaryGreen,
                radius = 4.dp.toPx(),
                center = node1
            )

            // Right connected node avatar
            drawCircle(
                color = MitraLightGreenBg,
                radius = 16.dp.toPx(),
                center = node2
            )
            drawCircle(
                color = MitraPrimaryGreen,
                radius = 16.dp.toPx(),
                center = node2,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Plus sign
            drawLine(color = MitraPrimaryGreen, start = Offset(node2.x - 6f, node2.y), end = Offset(node2.x + 6f, node2.y), strokeWidth = 2.dp.toPx())
            drawLine(color = MitraPrimaryGreen, start = Offset(node2.x, node2.y - 6f), end = Offset(node2.x, node2.y + 6f), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
fun OnboardingPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val widthAnimate by animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "width"
            )
            val colorAnimate by animateColorAsState(
                targetValue = if (isActive) MitraPrimaryGreen else MitraBorder,
                animationSpec = tween(300),
                label = "color"
            )

            Box(
                modifier = Modifier
                    .size(width = widthAnimate, height = 8.dp)
                    .clip(CircleShape)
                    .background(colorAnimate)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    var currentPage by remember { mutableIntStateOf(0) }
    var touchXStart by remember { mutableFloatStateOf(0f) }

    val screenTitle = when (currentPage) {
        0 -> "Grow Your Money\nThe Smart Way"
        1 -> "Don't Let Your Money\nSit Idle"
        2 -> "Invest With\nConfidence"
        else -> "More Ways\nTo Earn"
    }

    val screenDescription = when (currentPage) {
        0 -> "Choose investment plans, track your earnings, and manage your finances from one place."
        1 -> "Money can do more than just sit in your account.\n\nPut your money to work and build a better future."
        2 -> "Simple plans.\n\nEasy tracking.\n\nTransparent earnings.\n\nAll in one place."
        else -> "✓ Daily Check-In Rewards\n\n✓ Watch & Earn Tasks\n\n✓ Refer & Earn Bonuses\n\n✓ One Wallet For Everything"
    }

    val lightGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchXStart = offset.x
                    },
                    onDragEnd = {
                        // Gesture completed
                    },
                    onDragCancel = {},
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val diffX = change.position.x - touchXStart
                        if (diffX.absoluteValue > 120f) {
                            if (diffX < 0 && currentPage < 3) {
                                currentPage++
                                touchXStart = change.position.x
                            } else if (diffX > 0 && currentPage > 0) {
                                currentPage--
                                touchXStart = change.position.x
                            }
                        }
                    }
                )
            }
            .padding(24.dp)
    ) {
        // Logo / Title Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Logo",
                    tint = MitraPrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MoneyMitra",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MitraPrimaryGreen
                )
            }

            // Simple active/total page label
            Text(
                text = "${currentPage + 1}/4",
                fontSize = 13.sp,
                color = MitraPrimaryGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(MitraLightGreenBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated illustrations container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 2 } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally { width -> -width / 2 } + fadeOut(tween(300))
                        )
                    } else {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally { width -> width / 2 } + fadeOut(tween(300))
                        )
                    }.using(SizeTransform(clip = false))
                },
                label = "visuals_animation"
            ) { page ->
                when (page) {
                    0 -> OnboardingVisualScreen1()
                    1 -> OnboardingVisualScreen2()
                    2 -> OnboardingVisualScreen3()
                    else -> OnboardingVisualScreen4()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Description text contents with animation wrapper
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(tween(300)).togetherWith(fadeOut(tween(250)))
                },
                label = "text_animation"
            ) { page ->
                Column {
                    Text(
                        text = screenTitle,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MitraTextMain,
                        lineHeight = 34.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = screenDescription,
                        fontSize = 15.sp,
                        color = MitraTextSecondary,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Nav controls (Skip, Page Index Dots, Next/Get Started)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SKIP Button is hidden on last screen, keeps exact screen proportion
            if (currentPage < 3) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        navController.navigate("login") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "Skip",
                        color = MitraTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(50.dp)) // Maintain horizontal alignment layout balance
            }

            // Indicator Dots
            OnboardingPageIndicator(
                pageCount = 4,
                currentPage = currentPage,
                modifier = Modifier.testTag("onboarding_indicators")
            )

            // NEXT / GET STARTED Button
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "button_scale"
            )

            Button(
                onClick = {
                    if (currentPage < 3) {
                        currentPage++
                    } else {
                        viewModel.completeOnboarding()
                        navController.navigate("login") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag(if (currentPage == 3) "get_started_button" else "onboarding_next_button")
            ) {
                Text(
                    text = if (currentPage == 3) "Get Started" else "Next",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// 03. Login Screen
@Composable
fun LoginScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val lightGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = "Logo",
                tint = MitraPrimaryGreen,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MoneyMitra",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MitraPrimaryGreen
            )
        }

        Text(
            text = "Welcome Back 👋",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Text(
            text = "Login to access your high-yield secure investments.",
            fontSize = 15.sp,
            color = MitraTextSecondary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MitraBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val isPhoneNumber = emailOrPhone.isNotBlank() && !emailOrPhone.contains("@") && emailOrPhone.trim().all { it.isDigit() || it == '+' || it == ' ' || it == '-' }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEA4335),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                FintechTextField(
                    value = emailOrPhone,
                    onValueChange = { 
                        emailOrPhone = it
                        errorMessage = ""
                    },
                    label = "Email or Phone Number",
                    placeholder = "e.g. admin@mitra.com or 9876543210",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "User", tint = MitraPrimaryGreen)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (!isPhoneNumber) {
                    FintechTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = ""
                        },
                        label = "Password",
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = MitraPrimaryGreen)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MitraTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MitraPrimaryGreen.copy(0.06f), RoundedCornerShape(12.dp))
                            .border(1.dp, MitraPrimaryGreen.copy(0.15f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "OTP Login", tint = MitraPrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Secure OTP Login", color = MitraPrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "An official SMS OTP code will be dispatched to your phone for verification. Passwords are not used.",
                                color = MitraTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MitraPrimaryGreen)
                        )
                        Text(
                            text = "Remember Me",
                            fontSize = 14.sp,
                            color = MitraTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isPhoneNumber) {
                        Text(
                            text = "Forgot Password?",
                            color = MitraPrimaryGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.navigate("forgot_password") }
                        )
                    }
                }

                val context = LocalContext.current
                Button(
                    onClick = {
                        if (isPhoneNumber) {
                            val activity = context.findActivity()
                            if (activity != null) {
                                viewModel.signupEmailOrPhone = ""
                                viewModel.loginPhoneInput.value = emailOrPhone.trim()
                                viewModel.otpContext = "login"
                                viewModel.otpInput.value = ""
                                viewModel.startPhoneVerification(emailOrPhone.trim(), activity)
                                navController.navigate("otp")
                            } else {
                                errorMessage = "Activity context is not found."
                            }
                        } else {
                            if (emailOrPhone.isNotBlank() && password.isNotBlank()) {
                                coroutineScope.launch {
                                    val success = viewModel.loginWithPassword(emailOrPhone.trim(), password)
                                    if (success) {
                                        navController.navigate("dashboard_container") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        errorMessage = "Incorrect email/phone or password."
                                    }
                                }
                            }
                        }
                    },
                    enabled = if (isPhoneNumber) emailOrPhone.isNotBlank() else (emailOrPhone.isNotBlank() && password.isNotBlank()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MitraPrimaryGreen,
                        disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (isPhoneNumber) "Get Verification SMS OTP" else "Secure Login",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { navController.navigate("signup") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MitraPrimaryGreen
                    ),
                    border = BorderStroke(1.5.dp, MitraPrimaryGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Create New Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraPrimaryGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                color = MitraTextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = "Sign Up",
                color = MitraPrimaryGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { navController.navigate("signup") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(1.dp, MitraPrimaryGreen.copy(0.3f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MitraPrimaryGreen.copy(0.08f), RoundedCornerShape(24.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secure",
                tint = MitraPrimaryGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "100% Secure & Encrypted",
                fontSize = 12.sp,
                color = MitraPrimaryGreen,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 03B. Signup Screen
@Composable
fun SignupScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    var name by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var referralCodeEntered by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val lightGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )
        }

        Text(
            text = "Join MoneyMitra to multiply your savings.",
            fontSize = 15.sp,
            color = MitraTextSecondary,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MitraBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEA4335),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                FintechTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMessage = ""
                    },
                    label = "Full Name",
                    placeholder = "e.g. Rohan Mitra",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = "Name", tint = MitraPrimaryGreen)
                    }
                )

                FintechTextField(
                    value = emailOrPhone,
                    onValueChange = { 
                        emailOrPhone = it
                        errorMessage = ""
                    },
                    label = "Email Address or Phone Number",
                    placeholder = "e.g. rohan@gmail.com or 9876543210",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = "Email", tint = MitraPrimaryGreen)
                    }
                )

                FintechTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = ""
                    },
                    label = "Password",
                    placeholder = "Create strong password",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = MitraPrimaryGreen)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = MitraTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                FintechTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        errorMessage = ""
                    },
                    label = "Confirm Password",
                    placeholder = "Repeat strong password",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LockClock, contentDescription = "Confirm password", tint = MitraPrimaryGreen)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = MitraTextSecondary
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                FintechTextField(
                    value = referralCodeEntered,
                    onValueChange = { 
                        referralCodeEntered = it
                        errorMessage = ""
                    },
                    label = "Referral Code (Optional)",
                    placeholder = "e.g. MM123456",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "Referral Code", tint = MitraPrimaryGreen)
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { agreeToTerms = !agreeToTerms }
                        .padding(vertical = 12.dp)
                ) {
                    Checkbox(
                        checked = agreeToTerms,
                        onCheckedChange = { agreeToTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = MitraPrimaryGreen)
                    )
                    Text(
                        text = "I clarify and agree to Terms & Conditions",
                        fontSize = 13.sp,
                        color = MitraTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                val context = LocalContext.current
                Button(
                    onClick = {
                        when {
                            name.isBlank() || emailOrPhone.isBlank() || password.isBlank() -> {
                                errorMessage = "Please fill all fields to proceed."
                            }
                            password != confirmPassword -> {
                                errorMessage = "Passwords do not match."
                            }
                            password.length < 6 -> {
                                errorMessage = "Password must be at least 6 characters."
                            }
                            !emailOrPhone.trim().all { it.isDigit() || it == '+' || it == '-' || it == ' ' } || emailOrPhone.trim().length < 10 -> {
                                errorMessage = "Please enter a valid 10-digit Indian mobile number."
                            }
                            !agreeToTerms -> {
                                errorMessage = "Please accept the terms to sign up."
                            }
                            else -> {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    // Cache credentials and redirect to OTP Verification screen
                                    viewModel.signupName = name.trim()
                                    viewModel.signupEmailOrPhone = emailOrPhone.trim()
                                    viewModel.signupPassword = password
                                    viewModel.signupReferralCode = referralCodeEntered.trim().uppercase()
                                    android.util.Log.d("ReferralAudit", "SignupScreen: referralCodeEntered received from input: ${viewModel.signupReferralCode}")
                                    viewModel.otpContext = "signup"
                                    viewModel.loginPhoneInput.value = emailOrPhone.trim()
                                    viewModel.otpInput.value = "" // clear prior verification
                                    viewModel.startPhoneVerification(emailOrPhone.trim(), activity)
                                    navController.navigate("otp")
                                } else {
                                    errorMessage = "Activity Context not found."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MitraPrimaryGreen,
                        disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Register & Verify Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Already registered? ", color = MitraTextSecondary, fontSize = 14.sp)
            Text(
                text = "Sign In",
                color = MitraPrimaryGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { navController.navigate("login") { popUpTo("signup") { inclusive = true } } }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// 04. OTP Verification Screen
@Composable
fun OtpScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val phone = viewModel.signupEmailOrPhone.ifEmpty { viewModel.loginPhoneInput.collectAsState().value }
    val otp by viewModel.otpInput.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isProgressing by viewModel.isVerificationInProgress.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var secondsLeft by remember { mutableStateOf(30) }
    LaunchedEffect(key1 = secondsLeft) {
        if (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            secondsLeft--
        }
    }

    val lightGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGradient)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter Verification Code",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        Text(
            text = "Enter the 6-Digit security OTP dispatch sent for verification identifier of:\n$phone",
            fontSize = 15.sp,
            color = MitraTextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        if (!authError.isNullOrEmpty()) {
            Text(
                text = authError ?: "",
                color = Color(0xFFEA4335),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 0 until 6) {
                val char = otp.getOrNull(i)?.toString() ?: ""
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, if (char.isNotEmpty()) MitraPrimaryGreen else MitraBorder, RoundedCornerShape(12.dp))
                        .background(if (char.isNotEmpty()) MitraPrimaryGreen.copy(0.12f) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraTextMain
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (secondsLeft > 0) {
            Text(
                text = "Resend OTP in 00:${String.format("%02d", secondsLeft)}",
                fontSize = 14.sp,
                color = MitraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val context = LocalContext.current
            Text(
                text = "Resend OTP",
                fontSize = 14.sp,
                color = MitraPrimaryGreen,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val activity = context.findActivity()
                        if (activity != null) {
                            viewModel.resendVerificationCode(phone, activity)
                            secondsLeft = 30
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.weight(0.2f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "Back")
            )

            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowKeys.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (key.isNotEmpty()) Color(0xFFF1F5F9) else Color.Transparent)
                                .clickable(enabled = key.isNotEmpty()) {
                                    if (key == "Back") {
                                        if (otp.isNotEmpty()) {
                                            viewModel.otpInput.value = otp.dropLast(1)
                                        }
                                    } else {
                                        if (otp.length < 6) {
                                            viewModel.otpInput.value = otp + key
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "Back") {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = MitraTextMain)
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MitraTextMain
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (otp.length == 6) {
                    coroutineScope.launch {
                        val verified = viewModel.verifyAndSignIn(otp)
                        if (verified) {
                            viewModel.otpInput.value = ""
                            navController.navigate("dashboard_container") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                }
            },
            enabled = otp.length == 6 && !isProgressing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MitraPrimaryGreen,
                disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isProgressing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Verify & Continue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 05. Create Profile Screen
@Composable
fun CreateProfileScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val name by viewModel.profileNameInput.collectAsState()
    val email by viewModel.profileEmailInput.collectAsState()
    val referral by viewModel.profileReferralInput.collectAsState()
    
    val lightGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGradient)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Let's Create Your Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        Text(
            text = "Please provide some basic details to set up your investment workspace.",
            fontSize = 15.sp,
            color = MitraTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        FintechTextField(
            value = name,
            onValueChange = { viewModel.profileNameInput.value = it },
            label = "Full Name",
            placeholder = "As per PAN card"
        )

        FintechTextField(
            value = email,
            onValueChange = { viewModel.profileEmailInput.value = it },
            label = "Email Address",
            placeholder = "example@gmail.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        FintechTextField(
            value = referral,
            onValueChange = { viewModel.profileReferralInput.value = it },
            label = "Referral Code (Optional)",
            placeholder = "Enter referral code"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && email.isNotBlank()) {
                    viewModel.createProfile(name, email, referral)
                    navController.navigate("dashboard_container") {
                        popUpTo("create_profile") { inclusive = true }
                    }
                }
            },
            enabled = name.isNotBlank() && email.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MitraPrimaryGreen,
                disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Create Account", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 06. Dashboard Container (Housing bottom tabs)
@Composable
fun DashboardContainer(parentNavController: NavHostController, viewModel: MoneyMitraViewModel) {
    val selectedTab by viewModel.dashboardSelectedTab.collectAsState()
    val session by viewModel.userSession.collectAsState()
    val auditState by viewModel.referralAuditState.collectAsState()

    val lightGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(lightGradient),
        bottomBar = {
            Column {
                Divider(color = MitraBorder)
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.dashboardSelectedTab.value = 0 },
                        icon = { Icon(if (selectedTab == 0) Icons.Default.Home else Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MitraPrimaryGreen,
                            selectedTextColor = MitraPrimaryGreen,
                            indicatorColor = MitraPrimaryGreen.copy(0.12f),
                            unselectedIconColor = MitraTextSecondary,
                            unselectedTextColor = MitraTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.dashboardSelectedTab.value = 1 },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Invest") },
                        label = { Text("Invest") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MitraPrimaryGreen,
                            selectedTextColor = MitraPrimaryGreen,
                            indicatorColor = MitraPrimaryGreen.copy(0.12f),
                            unselectedIconColor = MitraTextSecondary,
                            unselectedTextColor = MitraTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.dashboardSelectedTab.value = 2 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                        label = { Text("Wallet") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MitraPrimaryGreen,
                            selectedTextColor = MitraPrimaryGreen,
                            indicatorColor = MitraPrimaryGreen.copy(0.12f),
                            unselectedIconColor = MitraTextSecondary,
                            unselectedTextColor = MitraTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.dashboardSelectedTab.value = 3 },
                        icon = { Icon(Icons.Default.Task, contentDescription = "Tasks") },
                        label = { Text("Tasks") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MitraPrimaryGreen,
                            selectedTextColor = MitraPrimaryGreen,
                            indicatorColor = MitraPrimaryGreen.copy(0.12f),
                            unselectedIconColor = MitraTextSecondary,
                            unselectedTextColor = MitraTextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { viewModel.dashboardSelectedTab.value = 4 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MitraPrimaryGreen,
                            selectedTextColor = MitraPrimaryGreen,
                            indicatorColor = MitraPrimaryGreen.copy(0.12f),
                            unselectedIconColor = MitraTextSecondary,
                            unselectedTextColor = MitraTextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (auditState.isNotEmpty()) {
                androidx.compose.material3.Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    contentColor = Color.Green,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("DEBUG AUDIT LOGS", fontWeight = FontWeight.Bold)
                        auditState.forEach { (key, value) ->
                            Text("$key: $value", fontSize = 12.sp)
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> HomeDashboardView(parentNavController, viewModel, onNavigateToTab = { viewModel.dashboardSelectedTab.value = it })
                    1 -> InvestmentPlansView(parentNavController, viewModel)
                    2 -> WalletBalanceView(parentNavController, viewModel)
                    3 -> TasksHomeView(parentNavController, viewModel)
                    4 -> ProfileView(parentNavController, viewModel)
                }
            }
        }
    }
}

data class UnifiedActivePlan(
    val planId: String,
    val name: String,
    val investedAmount: Double,
    val dailyEarnings: Double,
    val totalEarnings: Double,
    val daysCompleted: Int,
    val daysRemaining: Int,
    val activationDate: String,
    val expiryDate: String
)

// 06. Home Dashboard View
@Composable
fun HomeDashboardView(
    parentNavController: NavHostController,
    viewModel: MoneyMitraViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val session by viewModel.userSession.collectAsState()
    val txs by viewModel.transactions.collectAsState()
    val plans by viewModel.investmentPlans.collectAsState()
    val activeInvestments by viewModel.userActiveInvestments.collectAsState()
    val notifications by viewModel.userNotifications.collectAsState()
    val hasUnread = remember(notifications) { notifications.any { !it.isRead } }

    val activePlan: UnifiedActivePlan? = remember(plans, activeInvestments) {
        val remoteActive = activeInvestments.find { it.status == "ACTIVE" }
        if (remoteActive != null) {
            UnifiedActivePlan(
                planId = remoteActive.planId,
                name = remoteActive.name,
                investedAmount = remoteActive.investedAmount,
                dailyEarnings = remoteActive.dailyEarnings,
                totalEarnings = remoteActive.totalEarnings,
                daysCompleted = remoteActive.daysCompleted,
                daysRemaining = remoteActive.daysRemaining,
                activationDate = remoteActive.activationDate,
                expiryDate = remoteActive.expiryDate
            )
        } else {
            val localActive = plans.find { it.status == "ACTIVE" }
            if (localActive != null) {
                UnifiedActivePlan(
                    planId = localActive.planId,
                    name = localActive.name,
                    investedAmount = localActive.investedAmount,
                    dailyEarnings = localActive.dailyEarnings,
                    totalEarnings = localActive.totalEarnings,
                    daysCompleted = localActive.daysCompleted,
                    daysRemaining = localActive.daysRemaining,
                    activationDate = localActive.activationDate,
                    expiryDate = localActive.expiryDate
                )
            } else {
                null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (session?.name?.take(2) ?: "MM").uppercase(),
                            color = MitraPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hello, ${session?.name ?: "Rohan"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MitraTextMain
                        )
                        Text(
                            text = "Wealth Partner",
                            fontSize = 12.sp,
                            color = MitraTextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                        .clickable { parentNavController.navigate("notifications") }
                        .testTag("notifications_icon_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MitraTextMain
                    )
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MitraErrorRed, CircleShape)
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                                .testTag("unread_indicator_dot")
                        )
                    }
                }
            }
        }

        // Total Balance Card (Emerald green card matching Screen 07 look)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MitraPrimaryGreen),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(text = "Total Balance", color = Color.White.copy(0.85f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${String.format("%,.2f", session?.balance ?: 0.00)}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Earnings", color = Color.White.copy(0.85f), fontSize = 11.sp)
                            Text(
                                text = "+₹${String.format("%,.2f", session?.earnings ?: 0.00)}",
                                color = MitraAccentGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { parentNavController.navigate("add_money") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MitraPrimaryGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Money", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { parentNavController.navigate("withdraw_money") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = "Withdraw", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Investment Dashboard Section (Rule 5 & Dashboard Widgets Rule 7)
        if (activePlan != null) {
            val plan = activePlan
            val totalPlanDays = plan.daysCompleted + plan.daysRemaining
            val progressPercent = if (totalPlanDays > 0) plan.daysCompleted.toFloat() / totalPlanDays.toFloat() else 0f

            item {
                Text(
                    text = "Active Wealth Engine",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MitraTextMain,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.8.dp, Brush.linearGradient(listOf(MitraPrimaryGreen.copy(0.3f), Color.White)), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF22C55E), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = plan.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MitraTextMain
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("ACTIVE", color = Color(0xFF15803D), fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Indicator with custom label
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Engine Progress", fontWeight = FontWeight.SemiBold, color = MitraTextSecondary, fontSize = 12.sp)
                            Text("${(progressPercent * 100).toInt()}% Completed", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progressPercent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MitraPrimaryGreen,
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid of modern 2x2 dashboard widgets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("Active Investment", fontSize = 10.sp, color = MitraTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${String.format("%,.0f", plan.investedAmount)}", fontWeight = FontWeight.ExtraBold, color = MitraTextMain, fontSize = 14.sp)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("Total Earnings", fontSize = 10.sp, color = MitraTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("+₹${String.format("%,.1f", plan.totalEarnings)}", fontWeight = FontWeight.ExtraBold, color = MitraPrimaryGreen, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("Current Value", fontSize = 10.sp, color = MitraTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${String.format("%,.1f", plan.investedAmount + plan.totalEarnings)}", fontWeight = FontWeight.ExtraBold, color = MitraPrimaryGreen, fontSize = 14.sp)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("Days Remaining", fontSize = 10.sp, color = MitraTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${plan.daysRemaining} Days left", fontWeight = FontWeight.ExtraBold, color = MitraTextMain, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dates and Status rows
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Activation Date", fontSize = 12.sp, color = MitraTextSecondary)
                            Text(plan.activationDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Expiry Date", fontSize = 12.sp, color = MitraTextSecondary)
                            Text(plan.expiryDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fast forward action
                        Button(
                            onClick = { viewModel.simulatePassageOfDay(plan.planId) },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Forward Only", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1-Click Day Fast-Forward Simulation", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Active Wealth Engine",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MitraTextMain,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MitraBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No Active Plan",
                            tint = MitraTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Investment Plan",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MitraTextMain
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start investing by selecting a plan",
                            fontSize = 14.sp,
                            color = MitraTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToTab(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Select an investment plan to get started", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Actions Title
        item {
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Quick Actions Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionItem(
                    icon = Icons.Default.TrendingUp,
                    label = "Invest",
                    color = MitraPrimaryGreen,
                    onClick = { onNavigateToTab(1) }
                )
                QuickActionItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Wallet",
                    color = MitraPrimaryGreen,
                    onClick = { onNavigateToTab(2) }
                )
                QuickActionItem(
                    icon = Icons.Default.History,
                    label = "History",
                    color = MitraPrimaryGreen,
                    onClick = { parentNavController.navigate("transaction_history") }
                )
                QuickActionItem(
                    icon = Icons.Default.Share,
                    label = "Refer",
                    color = MitraPrimaryGreen,
                    onClick = { parentNavController.navigate("refer_earn") }
                )
            }
        }

        // Banner Promo Ad (Deep Premium Forest card with rocket)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF032616)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(1) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Small, Grow Big!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invest in high performing plans and get up to 18% p.a. returns.",
                            color = Color.White.copy(0.85f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Invest Now", color = Color(0xFF032616), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Promo",
                        tint = MitraAccentGold,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        // Recent Transactions Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MitraTextMain
                )
                TextButton(onClick = { parentNavController.navigate("transaction_history") }) {
                    Text("View All", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Transactions List
        items(txs.take(4)) { transaction ->
            MoneyMitraTransactionRow(transaction)
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MitraTextMain)
    }
}

@Composable
fun MoneyMitraTransactionRow(transaction: MoneyMitraTransaction) {
    val dateString = remember(transaction.createdAt) {
        try {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(transaction.createdAt))
        } catch (e: Exception) {
            "Just now"
        }
    }

    val isCoinTransaction = when (transaction.type) {
        "COIN_CHECKIN", "COIN_VIDEO" -> true
        else -> false
    }

    val isCredit = when (transaction.type) {
        "DEPOSIT", "REFERRAL_BONUS", "REFERRAL_REWARD", "INVESTMENT_RETURN", "INVESTMENT_EARNING", "ADMIN_CREDIT", "CREDIT", "COIN_CHECKIN", "COIN_VIDEO", "COIN_REDEEM", "COIN_REDEMPTION", "DAILY_RETURN" -> true
        else -> false
    }

    val typeLabel = when (transaction.type) {
        "DEPOSIT" -> "Capital Added"
        "WITHDRAWAL" -> "Payout Request"
        "REFERRAL_BONUS" -> "Ref Bonus Credited"
        "REFERRAL_REWARD" -> "Ref Reward Credited"
        "INVESTMENT_PURCHASE" -> "Capital Invested"
        "INVESTMENT_RETURN", "INVESTMENT_EARNING" -> "Investment Returns"
        "ADMIN_CREDIT" -> "Admin Credit"
        "ADMIN_DEBIT" -> "Admin Debit"
        "COIN_CHECKIN" -> "Daily Check-In Reward"
        "COIN_VIDEO" -> "Watch & Earn Reward"
        "COIN_REDEEM", "COIN_REDEMPTION" -> "Coin Redemption Credit"
        "DAILY_RETURN" -> "Daily Return"
        else -> if (transaction.type.isNotBlank()) {
            transaction.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        } else {
            "Transaction"
        }
    }

    val icon = when (transaction.type) {
        "DEPOSIT", "ADMIN_CREDIT" -> Icons.Default.Add
        "WITHDRAWAL", "ADMIN_DEBIT" -> Icons.Default.ArrowUpward
        "REFERRAL_BONUS", "REFERRAL_REWARD" -> Icons.Default.Star
        "INVESTMENT_PURCHASE" -> Icons.Default.ArrowUpward
        "INVESTMENT_RETURN", "INVESTMENT_EARNING", "DAILY_RETURN" -> Icons.Default.ArrowDownward
        "COIN_CHECKIN" -> Icons.Default.CheckCircle
        "COIN_VIDEO" -> Icons.Default.PlayCircle
        "COIN_REDEEM", "COIN_REDEMPTION" -> Icons.Default.Stars
        else -> Icons.Default.ArrowDownward
    }

    val color = if (isCredit) MitraPrimaryGreen else Color(0xFFEA4335)
    val statusColor = when (transaction.status) {
        "SUCCESS", "COMPLETED", "APPROVED" -> MitraPrimaryGreen
        "PENDING" -> MitraAccentGold
        "REJECTED" -> Color(0xFFEA4335)
        else -> MitraTextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(color.copy(0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = typeLabel,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = typeLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MitraTextMain
                        )
                        Text(
                            text = dateString,
                            fontSize = 11.sp,
                            color = MitraTextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isCoinTransaction) {
                            val sign = if (isCredit) "+" else "-"
                            "$sign${transaction.amount.toInt()} Coins"
                        } else {
                            if (isCredit) "+₹${String.format("%,.2f", transaction.amount)}" else "-₹${String.format("%,.2f", transaction.amount)}"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = color
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(statusColor.copy(0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            if (transaction.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transaction.description,
                    fontSize = 12.sp,
                    color = MitraTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// 07. Wallet Balance View
@Composable
fun WalletBalanceView(
    parentNavController: NavHostController,
    viewModel: MoneyMitraViewModel
) {
    val session by viewModel.userSession.collectAsState()
    val txs by viewModel.transactions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Wallet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )
        }

        // Wallet Card (Emerald green card matching Screen 07 look perfectly)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MitraPrimaryGreen),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(text = "Total Balance", color = Color.White.copy(0.85f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%,.2f", session?.balance ?: 0.00)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Invested Amount", color = Color.White.copy(0.85f), fontSize = 11.sp)
                            Text(
                                text = "₹${String.format("%,.2f", session?.investedAmount ?: 0.00)}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Earnings", color = Color.White.copy(0.85f), fontSize = 11.sp)
                            Text(
                                text = "+₹${String.format("%,.2f", session?.earnings ?: 0.00)}",
                                color = MitraAccentGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { parentNavController.navigate("add_money") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MitraPrimaryGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Money", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { parentNavController.navigate("withdraw_money") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = "Withdraw", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(txs) { transaction ->
            MoneyMitraTransactionRow(transaction)
        }
    }
}

// 10. Investment Plans View
@Composable
fun InvestmentPlansView(
    parentNavController: NavHostController,
    viewModel: MoneyMitraViewModel
) {
    DynamicInvestmentHubView(parentNavController, viewModel)
}

@Composable
fun GlobalPlanCard(
    plan: MoneyMitraGlobalPlan,
    userBalance: Double,
    enabled: Boolean = true,
    onInvestClick: (MoneyMitraGlobalPlan, String) -> Unit
) {
    var calcAmountStr by remember { mutableStateOf(plan.minAmount.toInt().toString()) }
    val calcAmount = calcAmountStr.toDoubleOrNull() ?: 0.0
    val estDailyEarning = calcAmount * (plan.dailyReturn / 100.0)
    val totalRoiPercent = plan.dailyReturn * plan.durationDays
    val estTotalEarning = calcAmount * (totalRoiPercent / 100.0)

    val planId = plan.id.lowercase()
    val isStarter = planId.contains("starter")
    val isGrowth = planId.contains("growth")

    // Dynamic Visual Palette
    val primaryColor = when {
        isStarter -> Color(0xFF00A35C) // Vibrant Emerald Green
        isGrowth -> Color(0xFF1565C0)  // Premium Cobalt Blue
        else -> Color(0xFF7B1FA2)      // Elegant Royal Purple
    }

    val lightBgColor = when {
        isStarter -> Color(0xFFE6F6ED) // Mint Tint
        isGrowth -> Color(0xFFE3F2FD)  // Ice Blue Tint
        else -> Color(0xFFF3E5F5)      // Lavender Tint
    }

    val badgeText = when {
        isStarter -> "BEST FOR BEGINNERS"
        isGrowth -> "MOST POPULAR"
        else -> "HIGH VALUE"
    }

    val planIcon = when {
        isStarter -> Icons.Default.TrendingUp
        isGrowth -> Icons.Default.ElectricBolt
        else -> Icons.Default.Verified
    }

    val borderGlow = Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.5f)))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderGlow), RoundedCornerShape(16.dp))
            .testTag("explore_plan_card_${plan.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MitraTextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .background(lightBgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = planIcon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = badgeText,
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%,.0f", plan.minAmount)} - ₹${String.format("%,.0f", plan.maxAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = primaryColor
                    )
                    Text(
                        "Investment Range",
                        fontSize = 11.sp,
                        color = MitraTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(lightBgColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DURATION", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                    Text("${plan.durationDays} Days", fontSize = 14.sp, color = MitraTextMain, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("DAILY RETURN", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                    Text("${plan.dailyReturn}%", fontSize = 14.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("TOTAL ROI", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                    Text("+${String.format("%.2f", totalRoiPercent)}%", fontSize = 14.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                }
            }

            // Expected Daily Earnings Calculator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9).copy(0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Earnings Calculator & Estimator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MitraTextMain
                )
                
                OutlinedTextField(
                    value = calcAmountStr,
                    onValueChange = { calcAmountStr = it },
                    label = { Text("Calculate Investment (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("plan_calc_input_${plan.id}"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MitraBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("EST. DAILY EARNING", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%,.2f", estDailyEarning)}", fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL PROFIT", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%,.2f", estTotalEarning)}", fontSize = 13.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Guaranteed automated payout",
                    fontSize = 11.sp,
                    color = MitraTextSecondary
                )
                Box(
                    modifier = Modifier
                        .background(if (enabled) primaryColor else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .then(if (enabled) Modifier.clickable { onInvestClick(plan, calcAmountStr) } else Modifier)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Invest Now", color = if (enabled) Color.White else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DynamicInvestmentHubView(
    parentNavController: NavHostController,
    viewModel: MoneyMitraViewModel
) {
    val globalPlans by viewModel.globalInvestmentPlans.collectAsState()
    val activeInvestments by viewModel.userActiveInvestments.collectAsState()
    val session by viewModel.userSession.collectAsState()

    var userSelectedSubTab by remember { mutableStateOf("Explore Plans") }
    var investPlanToPay by remember { mutableStateOf<MoneyMitraGlobalPlan?>(null) }
    var investAmountStr by remember(investPlanToPay?.id) { 
        mutableStateOf(investPlanToPay?.minAmount?.toInt()?.toString() ?: "") 
    }
    var lastInvestedAmount by remember { mutableStateOf(0.0) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var activeInvestmentsPageSize by remember { mutableStateOf(20) }
    val paginatedActiveInvestments = remember(activeInvestments, activeInvestmentsPageSize) {
        activeInvestments.take(activeInvestmentsPageSize)
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Investment Successful!", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen) },
            text = { Text("Your investment of ₹${String.format("%,.2f", lastInvestedAmount)} was submitted successfully. Your money is now working for you!", color = MitraTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error", fontWeight = FontWeight.Bold, color = Color(0xFFEA4335)) },
            text = { Text(errorMessage ?: "", color = MitraTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335))
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    if (investPlanToPay != null) {
        val plan = investPlanToPay!!
        val userBalance = session?.balance ?: 0.0
        val amountToInvest = investAmountStr.toDoubleOrNull() ?: 0.0
        val isWithinRange = amountToInvest >= plan.minAmount && amountToInvest <= plan.maxAmount
        val isSufficient = userBalance >= amountToInvest && amountToInvest > 0.0
        val isValidInput = isWithinRange && isSufficient

        AlertDialog(
            onDismissRequest = { investPlanToPay = null },
            modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Confirm Investment",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MitraTextMain
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "You are about to invest in:",
                        fontWeight = FontWeight.Medium,
                        color = MitraTextSecondary,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = investAmountStr,
                        onValueChange = { investAmountStr = it },
                        label = { Text("Enter Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("user_invest_amount_input"),
                        singleLine = true
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Plan Name:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextSecondary)
                            Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextMain)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Limits/Range:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextSecondary)
                            Text("₹${String.format("%,.0f", plan.minAmount)} - ₹${String.format("%,.0f", plan.maxAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextMain)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Duration:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextSecondary)
                            Text("${plan.durationDays} Days", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextMain)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Daily Growth:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextSecondary)
                            Text("${plan.dailyReturn}% Daily", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraPrimaryGreen)
                        }
                        Divider(color = MitraBorder, modifier = Modifier.padding(vertical = 4.dp))
                        
                        val totalProfitPercent = plan.dailyReturn * plan.durationDays
                        val totalReturnAmt = amountToInvest * (1.0 + (totalProfitPercent / 100.0))
                        
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Total ROI %:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextSecondary)
                            Text("${String.format("%.2f", totalProfitPercent)}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraPrimaryGreen)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Est. Return at Maturity:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MitraTextSecondary)
                            Text("₹${String.format("%,.2f", totalReturnAmt)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MitraPrimaryGreen)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Wallet Balance:",
                            fontSize = 13.sp,
                            color = MitraTextSecondary
                        )
                        Text(
                            text = "₹${String.format("%,.2f", userBalance)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (userBalance >= amountToInvest) MitraTextMain else Color(0xFFEA4335)
                        )
                    }

                    if (amountToInvest < plan.minAmount) {
                        Text(
                            text = "Minimum investment is ₹${String.format("%,.0f", plan.minAmount)}.",
                            color = Color(0xFFEA4335),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (amountToInvest > plan.maxAmount) {
                        Text(
                            text = "Maximum investment is ₹${String.format("%,.0f", plan.maxAmount)}.",
                            color = Color(0xFFEA4335),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (userBalance < amountToInvest) {
                        Text(
                            text = "Insufficient balance. Please add money or decrease investment.",
                            color = Color(0xFFEA4335),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                if (userBalance >= amountToInvest && isValidInput) {
                    Button(
                        onClick = {
                            lastInvestedAmount = amountToInvest
                            viewModel.buyGlobalPlan(
                                planId = plan.id,
                                planName = plan.name,
                                planAmount = amountToInvest,
                                durationDays = plan.durationDays,
                                dailyReturn = plan.dailyReturn,
                                onSuccess = {
                                    investPlanToPay = null
                                    showSuccessDialog = true
                                },
                                onFailure = { err ->
                                    investPlanToPay = null
                                    errorMessage = err.message ?: "Failed to process investment"
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("invest_confirm_button")
                    ) {
                        Text("Invest Now", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            investPlanToPay = null
                            parentNavController.navigate("add_money")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MitraAccentGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Money", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text("Add Money", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { investPlanToPay = null }) {
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
        Text(
            text = "Investment Plans",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Choose an investment plan and earn daily returns.",
            fontSize = 13.sp,
            color = MitraTextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sub tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Explore Plans", "My Investments").forEach { tab ->
                val isSelected = userSelectedSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MitraPrimaryGreen else Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { userSelectedSubTab = tab }
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

        if (userSelectedSubTab == "Explore Plans") {
            val activeGlobalPlans = globalPlans.filter { it.isActive }
            val staticPlans by viewModel.investmentPlans.collectAsState()
            val hasActiveInvestment = activeInvestments.any { it.status == "ACTIVE" } || staticPlans.any { it.status == "ACTIVE" }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (hasActiveInvestment) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You already have an active investment. Wait until your current plan matures before starting a new investment.",
                                    color = Color(0xFF991B1B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                items(activeGlobalPlans) { plan ->
                    GlobalPlanCard(
                        plan = plan,
                        userBalance = session?.balance ?: 0.0,
                        enabled = !hasActiveInvestment,
                        onInvestClick = { p, initialAmount ->
                            if (!hasActiveInvestment) {
                                investPlanToPay = p
                                investAmountStr = initialAmount
                            }
                        }
                    )
                }
            }
        } else {
            // My Investments
            if (activeInvestments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "No Investments",
                            tint = MitraTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "You have no active investments",
                            fontWeight = FontWeight.Bold,
                            color = MitraTextMain
                        )
                        Text(
                            "Explore available wealth plans and start earning daily returns!",
                            fontSize = 12.sp,
                            color = MitraTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(paginatedActiveInvestments) { investment ->
                        val isMatured = investment.status == "MATURED"
                        val isCompleted = investment.status == "COMPLETED"
                        val tagColor = when {
                            isCompleted -> Color(0xFF64748B)  // Slate gray
                            isMatured -> MitraPrimaryGreen
                            else -> MitraAccentGold
                        }
                        val tagBgColor = tagColor.copy(0.12f)
                        val returnPercentage = if (investment.investedAmount > 0) {
                            (investment.dailyEarnings / investment.investedAmount) * 100.0
                        } else 0.0
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, MitraBorder), RoundedCornerShape(12.dp))
                                .testTag("active_investment_card_${investment.id}")
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
                                    Text(
                                        text = investment.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MitraTextMain
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(tagBgColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = investment.status.uppercase(),
                                            color = tagColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("INVESTED AMOUNT", fontSize = 10.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("₹${String.format("%,.2f", investment.investedAmount)}", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 14.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("DAILY RETURN %", fontSize = 10.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${String.format("%.2f", returnPercentage)}%", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen, fontSize = 14.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("DAILY PROFIT", fontSize = 10.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("₹${String.format("%,.2f", investment.dailyEarnings)}", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen, fontSize = 14.sp)
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
                                        Text("START DATE", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text(investment.activationDate, fontSize = 12.sp, color = MitraTextMain, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("MATURITY DATE", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text(investment.expiryDate, fontSize = 12.sp, color = MitraTextMain, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("DAYS ACTIVE", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${investment.daysCompleted} Active (${investment.daysRemaining} Left)", fontSize = 12.sp, color = MitraTextMain, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("TOTAL EARNINGS", fontSize = 9.sp, color = MitraTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("₹${String.format("%,.2f", investment.totalEarnings)}", fontSize = 13.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    if (activeInvestments.size > activeInvestmentsPageSize) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { activeInvestmentsPageSize += 20 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                    modifier = Modifier.testTag("load_more_active_investments")
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

val dynamicOldInvestmentPlansPlaceholderFuse = true

@Composable
fun InvestmentPlansViewOldPlaceholderDef(
    parentNavController: NavHostController,
    viewModel: MoneyMitraViewModel
) {
    val plans by viewModel.investmentPlans.collectAsState()
    val activeInvestments by viewModel.userActiveInvestments.collectAsState()
    val session by viewModel.userSession.collectAsState()
    var selectedTab by remember { mutableStateOf("All Plans") }
    var investPlanToPay by remember { mutableStateOf<DbInvestmentPlan?>(null) }
    var investAmount by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    if (investPlanToPay != null) {
        val minInvestment = investPlanToPay!!.minInvestment
        val maxInvestment = when (investPlanToPay!!.planId) {
            "starter" -> 5000.0
            "growth" -> 50000.0
            else -> 100000.0
        }

        val amt = investAmount.toDoubleOrNull() ?: 0.0
        val currentError = when {
            investAmount.isBlank() -> null
            amt < minInvestment -> "Minimum investment is ₹${String.format("%,.0f", minInvestment)}"
            amt > maxInvestment -> "Maximum investment is ₹${String.format("%,.0f", maxInvestment)}"
            amt > (session?.balance ?: 0.0) -> "Insufficient wallet balance!"
            else -> null
        }
        val isInputValid = investAmount.isNotBlank() && currentError == null

        AlertDialog(
            onDismissRequest = { investPlanToPay = null },
            modifier = Modifier
                .border(1.dp, MitraBorder, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Invest in ${investPlanToPay!!.name}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MitraTextMain
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Current wallet balance: ₹${String.format("%,.2f", session?.balance ?: 0.0)}",
                        fontWeight = FontWeight.Medium,
                        color = MitraTextSecondary,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = investAmount,
                        onValueChange = { investAmount = it },
                        label = { Text("Investment Amount", color = MitraTextSecondary) },
                        prefix = { Text("₹ ", color = MitraTextMain) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = currentError != null,
                        supportingText = {
                            if (currentError != null) {
                                Text(currentError, color = Color(0xFFEA4335), fontSize = 12.sp)
                            } else {
                                Text("Allowed: ₹${String.format("%,.0f", minInvestment)} to ₹${String.format("%,.0f", maxInvestment)}", color = MitraTextSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MitraTextMain,
                            unfocusedTextColor = MitraTextMain,
                            focusedBorderColor = MitraPrimaryGreen,
                            unfocusedBorderColor = MitraBorder,
                            cursorColor = MitraPrimaryGreen,
                            focusedLabelColor = MitraPrimaryGreen,
                            unfocusedLabelColor = MitraTextSecondary,
                            errorBorderColor = Color(0xFFEA4335),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isInputValid) {
                        val days = when (investPlanToPay!!.planId) {
                            "starter" -> 30
                            "growth" -> 45
                            else -> 60
                        }
                        val rate = when (investPlanToPay!!.planId) {
                            "starter" -> 0.01
                            "growth" -> 0.0175
                            else -> 0.025
                        }
                        val dailyEst = amt * rate
                        val totalEst = amt + (dailyEst * days)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PROJECTION DETAILS", fontSize = 11.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Daily Growth (Up to ${(rate*100).toInt()}%):", color = MitraTextMain, fontSize = 13.sp)
                                    Text("₹${String.format("%,.2f", dailyEst)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Total return after $days Days:", color = MitraTextMain, fontSize = 13.sp)
                                    Text("₹${String.format("%,.2f", totalEst)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isInputValid) {
                            viewModel.selectPlan(investPlanToPay!!.planId)
                            viewModel.investSelectedPlan(amt)
                            investPlanToPay = null
                            parentNavController.navigate("payment_success")
                        }
                    },
                    enabled = isInputValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MitraPrimaryGreen,
                        disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Invest Now", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { investPlanToPay = null }) {
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
        Text(
            text = "Investment Plans",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Explore secure, technological AI-fueled financial investment plans tailored for your growth.",
            fontSize = 14.sp,
            color = MitraTextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val tabs = listOf("All Plans", "Low/Med Risk", "High Risk")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MitraPrimaryGreen else Color(0xFFF1F5F9),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, if (isSelected) MitraPrimaryGreen else MitraBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
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

        val filteredPlans = plans.filter {
            when (selectedTab) {
                "Low/Med Risk" -> it.riskLevel == "LOW" || it.riskLevel == "MEDIUM"
                "High Risk" -> it.riskLevel == "HIGH" || it.riskLevel == "VERY_HIGH"
                else -> true
            }
        }.sortedBy { it.minInvestment }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredPlans) { dbPlan ->
                val planId = dbPlan.planId
                val name = dbPlan.name
                val validity = when(planId) {
                    "starter" -> "30 Days"
                    "growth" -> "45 Days"
                    else -> "60 Days"
                }
                val dailyGrowth = when(planId) {
                    "starter" -> "Up to 1%"
                    "growth" -> "Up to 1.75%"
                    else -> "Up to 2.5%"
                }
                val risk = when(planId) {
                    "starter" -> "Moderate"
                    "growth" -> "High"
                    else -> "Very High"
                }
                val range = when(planId) {
                    "starter" -> "₹500 - ₹5,000"
                    "growth" -> "₹5,001 - ₹50,000"
                    else -> "₹50,001 - ₹100,000"
                }
                val minInvestVal = when(planId) {
                    "starter" -> 500.0
                    "growth" -> 5001.0
                    else -> 50001.0
                }
                val days = when(planId) {
                    "starter" -> 30
                    "growth" -> 45
                    else -> 60
                }
                val rate = when(planId) {
                    "starter" -> 0.01
                    "growth" -> 0.0175
                    else -> 0.025
                }
                val starterGain = minInvestVal * rate
                val starterTotal = minInvestVal + (starterGain * days)

                val tag = when(planId) {
                    "starter" -> "Beginner Friendly"
                    "growth" -> "Most Popular 🔥"
                    else -> "AI Advisor & Priority Support 🌟"
                }

                val borderGlow = if (dbPlan.status == "ACTIVE") {
                    Brush.linearGradient(listOf(MitraPrimaryGreen, MitraPrimaryGreen))
                } else if (planId == "premium") {
                    Brush.linearGradient(listOf(MitraAccentGold, MitraAccentGold))
                } else {
                    Brush.linearGradient(listOf(MitraBorder, MitraBorder))
                }

                val hasActiveOverall = plans.any { it.status == "ACTIVE" } || activeInvestments.any { it.status == "ACTIVE" }
                val isThisActive = dbPlan.status == "ACTIVE"
                val isThisCompleted = dbPlan.status == "COMPLETED"

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderGlow, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (tag != null) {
                                        Text(
                                            text = tag,
                                            color = if (planId == "premium") MitraAccentGold else MitraPrimaryGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .background(
                                                    if (planId == "premium") MitraAccentGold.copy(0.12f) else MitraPrimaryGreen.copy(0.12f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (isThisActive) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFF22C55E), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("ACTIVE", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    } else if (isThisCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("COMPLETED", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = MitraTextMain
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = dailyGrowth,
                                    color = MitraPrimaryGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Text("Daily Growth", color = MitraTextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Amount Range", fontSize = 11.sp, color = MitraTextSecondary)
                                Text(range, fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 14.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Validity", fontSize = 11.sp, color = MitraTextSecondary)
                                Text(validity, fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 14.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Risk Level", fontSize = 11.sp, color = MitraTextSecondary)
                                Text(risk, fontWeight = FontWeight.Bold, color = if (planId == "starter") MitraPrimaryGreen else if (planId == "growth") MitraAccentGold else Color(0xFFEA4335), fontSize = 14.sp)
                            }
                        }

                        if (isThisActive) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Countdown details with custom progress bar
                            val totalPlanDays = dbPlan.daysCompleted + dbPlan.daysRemaining
                            val progressPercent = if (totalPlanDays > 0) dbPlan.daysCompleted.toFloat() / totalPlanDays.toFloat() else 0f
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDFBF4)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFC6F6D5), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Plan Progress", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen, fontSize = 12.sp)
                                        Text("${(progressPercent * 100).toInt()}% Done", fontWeight = FontWeight.SemiBold, color = MitraPrimaryGreen, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = progressPercent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MitraPrimaryGreen,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Completed", fontSize = 11.sp, color = MitraTextSecondary)
                                            Text("${dbPlan.daysCompleted} Days", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Remaining", fontSize = 11.sp, color = MitraTextSecondary)
                                            Text("${dbPlan.daysRemaining} Days", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Duration", fontSize = 11.sp, color = MitraTextSecondary)
                                            Text("$totalPlanDays Days", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Activation Date:", fontSize = 12.sp, color = MitraTextSecondary)
                                        Text(dbPlan.activationDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Expiry Date:", fontSize = 12.sp, color = MitraTextSecondary)
                                        Text(dbPlan.expiryDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Daily earnings tracker for active plan
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MitraBorder, RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("DAILY EARNINGS TRACKER", fontSize = 11.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.ExtraBold)
                                    Divider(color = MitraBorder)
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Investment Amount:", color = MitraTextSecondary, fontSize = 13.sp)
                                        Text("₹${String.format("%,.2f", dbPlan.investedAmount)}", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Daily Earnings:", color = MitraTextSecondary, fontSize = 13.sp)
                                        Text("₹${String.format("%,.2f", dbPlan.dailyEarnings)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Today's Earnings:", color = MitraTextSecondary, fontSize = 13.sp)
                                        Text("₹${String.format("%,.2f", if (dbPlan.daysCompleted == 0) 0.0 else dbPlan.dailyEarnings)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Total Earnings Earned:", color = MitraTextSecondary, fontSize = 13.sp)
                                        Text("₹${String.format("%,.2f", dbPlan.totalEarnings)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Divider(color = MitraBorder)
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Current Value:", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("₹${String.format("%,.2f", dbPlan.investedAmount + dbPlan.totalEarnings)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulation interactive controls!
                            Button(
                                onClick = { viewModel.simulatePassageOfDay(planId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Simulate", tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simulate 1 Day Forward", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        } else {
                            // Standard Projections card
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                    .border(0.5.dp, MitraBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "PROJECTIONS AT MIN INVESTMENT (₹${String.format("%,.0f", minInvestVal)}):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MitraTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Proj. Daily Earnings:", fontSize = 12.sp, color = MitraTextMain)
                                        Text("₹${String.format("%,.2f", starterGain)}/day", fontSize = 12.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Est. Return after $validity:", fontSize = 12.sp, color = MitraTextMain)
                                        Text("₹${String.format("%,.2f", starterTotal)}", fontSize = 12.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (hasActiveOverall) {
                                // Locked state warning because another plan is active
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "You already have an active investment. Wait until your current plan matures before starting a new investment.",
                                            color = Color(0xFF991B1B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {},
                                    enabled = false,
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Locked", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                }
                            } else {
                                // standard invest now button
                                Button(
                                    onClick = {
                                        investPlanToPay = dbPlan
                                        investAmount = minInvestVal.toInt().toString()
                                        inputError = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (planId == "premium") MitraAccentGold else MitraPrimaryGreen
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Invest Now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (planId == "premium") Color(0xFF1E293B) else Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Disclaimer", tint = Color(0xFFE11D48), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Investments are subject to market risks.\nProjected returns are not guaranteed.",
                            fontSize = 12.sp,
                            color = Color(0xFF9F1239),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 11. Tasks Home View
@Composable
fun TasksHomeView(
    parentNavController: NavHostController,
    viewModel: MoneyMitraViewModel
) {
    val session by viewModel.userSession.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiLoading by viewModel.apiLoading.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val isAdReady by viewModel.isAdReady.collectAsState()
    val isAdLoading by viewModel.isAdLoading.collectAsState()

    // Preload Rewarded Ad on screen enter
    LaunchedEffect(Unit) {
        viewModel.loadRewardedAd()
    }
    
    val todayDate = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        }
        sdf.format(java.util.Date())
    }

    // Determine watch count reset state
    val watchedToday = remember(session, todayDate) {
        val s = session ?: return@remember 0
        if (s.lastVideoResetDate == todayDate) s.videosWatchedToday else 0
    }

    var showRedeemDialogInTasks by remember { mutableStateOf(false) }
    var showMinRedemptionPromoDialog by remember { mutableStateOf(false) }
    var inputRedeemAmount by remember { mutableStateOf("100") }
    val upiIdForRedeem = session?.upiId ?: ""
    var localUpiInput by remember { mutableStateOf(upiIdForRedeem) }

    // Display Toast or Handle ViewModel errors
    LaunchedEffect(apiError) {
        apiError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearApiError()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9F8))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mitra Tasks & Rewards",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MitraTextMain
                )
                
                IconButton(
                    onClick = { parentNavController.navigate("reward_history") }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Rewards History",
                        tint = MitraPrimaryGreen
                    )
                }
            }
        }

        // 3. Coin Wallet
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COIN WALLET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraPrimaryGreen,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Coins Balance",
                                    tint = MitraAccentGold,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${session?.coins ?: 0}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MitraTextMain
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Redeemable Value",
                                fontSize = 11.sp,
                                color = MitraTextSecondary
                            )
                            Text(
                                text = "₹${String.format("%,.2f", (session?.coins ?: 0) / 100.0)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MitraPrimaryGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Earned", fontSize = 11.sp, color = MitraTextSecondary)
                            Text(
                                text = "${session?.totalCoinsEarned ?: 0} Coins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraTextMain
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Redeemed", fontSize = 11.sp, color = MitraTextSecondary)
                            Text(
                                text = "${session?.totalCoinsRedeemed ?: 0} Coins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraTextMain
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rate Limit", fontSize = 11.sp, color = MitraTextSecondary)
                            Text(
                                text = "100 Coins = ₹1",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraAccentGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val currentCoins = session?.coins ?: 0
                            if (currentCoins < 1000) {
                                showMinRedemptionPromoDialog = true
                            } else {
                                localUpiInput = session?.upiId ?: ""
                                inputRedeemAmount = "$currentCoins"
                                showRedeemDialogInTasks = true
                            }
                        },
                        enabled = !apiLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MitraPrimaryGreen,
                            disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("redeem_coins_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Redeem Coins", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // 3b. Coin Wallet Info & Requirements
        item {
            val currentCoins = session?.coins ?: 0
            val remainingCoinsNeeded = (1000 - currentCoins).coerceAtLeast(0)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MitraBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("coin_redemption_info_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "REDEMPTION DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraTextSecondary,
                        letterSpacing = 1.0.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MitraPrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conversion Rate", fontSize = 13.sp, color = MitraTextSecondary)
                        }
                        Text("100 Coins = ₹1", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MitraAccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Minimum Redemption", fontSize = 13.sp, color = MitraTextSecondary)
                        }
                        Text("1000 Coins (₹10)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = MitraAccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Current Coin Balance", fontSize = 13.sp, color = MitraTextSecondary)
                        }
                        Text("$currentCoins Coins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MitraTextMain)
                    }

                    if (currentCoins < 1000) {
                        HorizontalDivider(color = MitraBorder.copy(alpha = 0.2f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remaining Coins Needed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Text(
                                text = "$remainingCoinsNeeded Coins",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    } else {
                        HorizontalDivider(color = MitraBorder.copy(alpha = 0.2f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MitraPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Redemption Eligible!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MitraPrimaryGreen
                                )
                            }
                            Text(
                                text = "Ready to Convert",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MitraPrimaryGreen
                            )
                        }
                    }
                }
            }
        }

        // 1. Daily Check-In
        item {
            val isClaimedToday = session?.lastCheckInDate == todayDate
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isClaimedToday) MitraBorder.copy(alpha = 0.3f) else MitraPrimaryGreen.copy(0.1f), 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isClaimedToday) Icons.Default.CheckCircle else Icons.Default.Celebration,
                                    contentDescription = "Daily Checkin Logo",
                                    tint = if (isClaimedToday) MitraTextSecondary else MitraPrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Calendar Check-In",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MitraTextMain
                                )
                                Text(
                                    text = "Reward: 20 Coins",
                                    fontSize = 12.sp,
                                    color = MitraAccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isClaimedToday) {
                            Box(
                                modifier = Modifier
                                    .background(MitraPrimaryGreen.copy(0.12f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Claimed Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MitraPrimaryGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isClaimedToday) "You have claimed today's reward. Come back tomorrow!" else "Earn 20 coins absolutely free by just claiming today's reward.",
                        fontSize = 12.sp,
                        color = MitraTextSecondary
                    )
                    
                    if (isClaimedToday) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Next Claim Availability: After midnight (UTC)",
                            fontSize = 11.sp,
                            color = MitraTextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.claimDailyCheckIn() },
                        enabled = !isClaimedToday && !apiLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MitraPrimaryGreen,
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledContentColor = MitraTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isClaimedToday) "Claimed Today" else "Claim Reward (+20 Coins)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Watch & Earn
        item {
            val limitReached = watchedToday >= 10
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (limitReached) MitraBorder.copy(alpha = 0.3f) else Color(0xFFFFECEB), 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch & Earn Logo",
                                    tint = if (limitReached) MitraTextSecondary else Color(0xFFEA4335),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Watch & Earn Videos",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MitraTextMain
                                )
                                Text(
                                    text = "Reward: 30 Coins each",
                                    fontSize = 12.sp,
                                    color = MitraPrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Watched Today", fontSize = 11.sp, color = MitraTextSecondary)
                            Text(
                                text = "$watchedToday / 10",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraTextMain
                            )
                        }

                        Column {
                            Text("Remaining", fontSize = 11.sp, color = MitraTextSecondary)
                            Text(
                                text = "${10 - watchedToday} Videos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (limitReached) Color(0xFFEA4335) else MitraPrimaryGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Earned Today", fontSize = 11.sp, color = MitraTextSecondary)
                            Text(
                                text = "${watchedToday * 30} Coins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraAccentGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                if (isAdReady) {
                                    viewModel.showRewardedAd(activity) {
                                        viewModel.watchVideoAndEarn()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Ad is loading, please try again in a few seconds...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.loadRewardedAd()
                                }
                            }
                        },
                        enabled = !limitReached && !apiLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEA4335),
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledContentColor = MitraTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (limitReached) {
                                "Limit Reached (10/10)"
                            } else if (isAdLoading) {
                                "Loading Ad..."
                            } else if (!isAdReady) {
                                "Preparing Ad..."
                            } else {
                                "Watch Video Ad (+30 Coins)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (limitReached) MitraTextSecondary else Color.White
                        )
                    }
                }
            }
        }
    }

    // REDEEM CONFIRMATION DIALOG DIRECTLY IN TASKS SCREEN
    if (showRedeemDialogInTasks) {
        var redeemConfirmError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showRedeemDialogInTasks = false },
            title = {
                Text(
                    text = "Convert Coins to Wallet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MitraTextMain
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Convert your earned coins directly into your withdrawable wallet balance.",
                        fontSize = 13.sp,
                        color = MitraTextSecondary
                    )
                    
                    OutlinedTextField(
                        value = inputRedeemAmount,
                        onValueChange = { inputRedeemAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Coins to Convert") },
                        placeholder = { Text("Enter number of coins") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MitraPrimaryGreen,
                            focusedLabelColor = MitraPrimaryGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .border(1.dp, MitraBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Your Coins Balance", fontSize = 12.sp, color = MitraTextSecondary)
                            Text("${session?.coins ?: 0} Coins", fontSize = 12.sp, color = MitraTextMain, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Conversion Rate", fontSize = 12.sp, color = MitraTextSecondary)
                            Text("100 Coins = ₹1.00", fontSize = 12.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                        }
                        val enterCoins = inputRedeemAmount.toIntOrNull() ?: 0
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Conversion Value", fontSize = 12.sp, color = MitraTextSecondary)
                            Text("₹${String.format("%.2f", enterCoins / 100.0)} Credit", fontSize = 12.sp, color = MitraPrimaryGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    redeemConfirmError?.let {
                        Text(it, color = Color(0xFFEA4335), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountToRedeem = inputRedeemAmount.toIntOrNull() ?: 0
                        if (amountToRedeem < 1000) {
                            redeemConfirmError = "Minimum redemption is 1000 Coins"
                        } else if (amountToRedeem > (session?.coins ?: 0)) {
                            redeemConfirmError = "Insufficient coins balance"
                        } else {
                            viewModel.redeemCoins(amountToRedeem)
                            showRedeemDialogInTasks = false
                            val amountStr = String.format(java.util.Locale.US, "%.2f", amountToRedeem / 100.0)
                            parentNavController.navigate("coin_redemption_success/$amountToRedeem/$amountStr")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen)
                ) {
                    Text("Convert Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRedeemDialogInTasks = false }) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // MINIMUM REDEMPTION NOT REACHED DIALOG
    if (showMinRedemptionPromoDialog) {
        val currentCoins = session?.coins ?: 0
        val remainingCoinsNeeded = (1000 - currentCoins).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = { showMinRedemptionPromoDialog = false },
            title = {
                Text(
                    text = "Minimum Redemption Not Reached",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MitraTextMain
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You currently have $currentCoins coins.",
                        fontSize = 14.sp,
                        color = MitraTextMain
                    )
                    Text(
                        text = "Minimum redemption required:\n1000 Coins (₹10)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraPrimaryGreen
                    )
                    Text(
                        text = "You need $remainingCoinsNeeded more coins before redemption is available.",
                        fontSize = 14.sp,
                        color = MitraTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMinRedemptionPromoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    modifier = Modifier.testTag("ok_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// 12. Profile View
@Composable
fun ProfileView(parentNavController: NavHostController, viewModel: MoneyMitraViewModel) {
    val session by viewModel.userSession.collectAsState()

    var versionClicks by remember { mutableStateOf(0) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isVerifyingAdmin by remember { mutableStateOf(false) }
    var adminError by remember { mutableStateOf<String?>(null) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9F8))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MitraTextMain,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        // 1. Profile Header: User avatar, User name, Registered mobile number
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Avatar", tint = MitraPrimaryGreen, modifier = Modifier.size(52.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = session?.name ?: "Rohan",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        val displayPhone = session?.phoneNumber?.ifBlank { "Not Connected" } ?: "Not Connected"
        Text(
            text = "Mobile: $displayPhone",
            fontSize = 14.sp,
            color = MitraTextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // FINAL PROFILE MENU
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // 1. Edit Profile
                ProfileMenuItem(icon = Icons.Default.Edit, title = "Edit Profile") { 
                    parentNavController.navigate("edit_profile") 
                }
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                
                // 2. Bank Account
                ProfileMenuItem(icon = Icons.Default.AccountBalance, title = "Bank Account") { 
                    parentNavController.navigate("bank_account") 
                }
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                
                // 3. Refer & Earn
                ProfileMenuItem(icon = Icons.Default.People, title = "Refer & Earn") { 
                    parentNavController.navigate("refer_earn") 
                }
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                
                // 4. About Us
                ProfileMenuItem(icon = Icons.Default.Info, title = "About Us") { 
                    parentNavController.navigate("about_us") 
                }
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                
                // 5. Help & Support
                ProfileMenuItem(icon = Icons.Default.HelpOutline, title = "Help & Support") { 
                    showHelpSupportDialog = true 
                }
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                
                // 6. Reset App State (Debug/Admin Only)
                val isDebug = try { com.example.BuildConfig.DEBUG } catch (e: Exception) { false }
                val isAdminUser = session?.email?.contains("admin", ignoreCase = true) == true
                if (isDebug || isAdminUser) {
                    ProfileMenuItem(icon = Icons.Default.Refresh, title = "Reset App State", color = Color(0xFFEA4335)) {
                        viewModel.resetAllData()
                        parentNavController.navigate("splash") {
                            popUpTo("dashboard_container") { inclusive = true }
                        }
                    }
                    HorizontalDivider(color = MitraBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
                }

                // 7. Logout
                ProfileMenuItem(icon = Icons.Default.Logout, title = "Logout", color = Color(0xFFEA4335)) {
                    viewModel.logout()
                    parentNavController.navigate("login") {
                        popUpTo("dashboard_container") { inclusive = true }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "App Version 1.2.0-mitra",
            color = MitraTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    versionClicks++
                    if (versionClicks >= 5) {
                        versionClicks = 0
                        showAdminDialog = true
                    }
                }
                .testTag("app_version_text")
        )
    }

    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifyingAdmin) {
                    showAdminDialog = false
                    passwordInput = ""
                    adminError = null
                }
            },
            title = {
                Text(
                    text = "Admin Identity Validation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MitraTextMain
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = "Verify your authority by entering the super_admin credentials.",
                        fontSize = 13.sp,
                        color = MitraTextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Admin Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                    )
                    
                    if (adminError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = adminError!!,
                            color = MitraErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isVerifyingAdmin = true
                            adminError = null
                            try {
                                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (currentUser == null) {
                                    adminError = "Please register or log in as a user first"
                                    return@launch
                                }
                                val uid = currentUser.uid
                                val firestoreRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val doc = firestoreRef.collection("admins").document(uid).get().await()
                                
                                if (!doc.exists()) {
                                    adminError = "Access Denied: UID not found in admins collection"
                                    return@launch
                                }
                                
                                val role = doc.getString("role")
                                if (role != "super_admin") {
                                    adminError = "Access Denied: Role is unauthorized. Required: 'super_admin'"
                                    return@launch
                                }
                                
                                val expectedPassword = doc.getString("password") 
                                    ?: doc.getString("admin_password") 
                                    ?: "admin123"
                                
                                if (passwordInput != expectedPassword) {
                                    adminError = "Invalid admin password. Please try again."
                                    return@launch
                                }
                                
                                showAdminDialog = false
                                passwordInput = ""
                                parentNavController.navigate("admin_dashboard")
                            } catch (e: Exception) {
                                adminError = "Error: " + (e.localizedMessage ?: "Unknown failure")
                            } finally {
                                isVerifyingAdmin = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    enabled = !isVerifyingAdmin && passwordInput.isNotEmpty(),
                    modifier = Modifier.testTag("admin_login_submit_button")
                ) {
                    if (isVerifyingAdmin) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", color = Color.White)
                    } else {
                        Text("Verify & Continue", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdminDialog = false
                        passwordInput = ""
                        adminError = null
                    },
                    enabled = !isVerifyingAdmin
                ) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (showHelpSupportDialog) {
        AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Support", tint = MitraPrimaryGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Help & Support", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MitraTextMain)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Welcome to MoneyMitra Support Hub! Our team is dedicated to providing you the best financial experience.", color = MitraTextSecondary, fontSize = 14.sp)
                    
                    HorizontalDivider(color = MitraBorder.copy(alpha = 0.5f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📧 Email Support:", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                        Text("support@moneymitra.com", color = MitraPrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📞 Phone/WhatsApp Support:", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                        Text("+91 98765 43210", color = MitraPrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🕒 Operating Hours:", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                        Text("Monday - Saturday, 9 AM - 6 PM IST", color = MitraTextSecondary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpSupportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    color: Color = MitraTextMain,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MitraTextSecondary)
    }
}

// 13. Redeem Coins Screen
@Composable
fun RedeemCoinsScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val session by viewModel.userSession.collectAsState()
    var redeemAmount by remember { mutableStateOf("1000") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MitraTextMain
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Convert Coins to Wallet Balance",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MitraPrimaryGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Coins Balance", color = Color.White.copy(0.85f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${session?.coins ?: 0}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                }
                Text(
                    text = "≈ ₹${String.format("%.2f", (session?.coins ?: 0) / 100.0)}",
                    color = MitraAccentGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Quick Coins Amount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1000", "2000", "5000", "10000").forEach { valCoins ->
                val isSelected = redeemAmount == valCoins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MitraPrimaryGreen.copy(0.12f) else Color(0xFFF1F5F9),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, if (isSelected) MitraPrimaryGreen else MitraBorder, RoundedCornerShape(8.dp))
                        .clickable { redeemAmount = valCoins }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = valCoins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSelected) MitraPrimaryGreen else MitraTextMain
                        )
                        Text(
                            text = "₹${(valCoins.toInt() / 100.0).toInt()}",
                            fontSize = 11.sp,
                            color = if (isSelected) MitraPrimaryGreen else MitraTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = redeemAmount,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                redeemAmount = filtered
            },
            label = { Text("Or Enter Coins to Redeem", color = MitraTextSecondary) },
            placeholder = { Text("e.g. 350", color = MitraTextSecondary.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MitraTextMain,
                unfocusedTextColor = MitraTextMain,
                focusedBorderColor = MitraPrimaryGreen,
                unfocusedBorderColor = MitraBorder,
                cursorColor = MitraPrimaryGreen,
                focusedLabelColor = MitraPrimaryGreen,
                unfocusedLabelColor = MitraTextSecondary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF1F5F9)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Redemption Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)

        Spacer(modifier = Modifier.height(8.dp))

        // Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Conversion Rate", color = MitraTextSecondary, fontSize = 13.sp)
                    Text("100 Coins = ₹1.00 Wallet Credit", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                
                HorizontalDivider(color = MitraBorder.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Coins to Convert", color = MitraTextSecondary, fontSize = 13.sp)
                    val coinsToRedeemVal = redeemAmount.toIntOrNull() ?: 0
                    Text("$coinsToRedeemVal Coins", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                HorizontalDivider(color = MitraBorder.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estimated Wallet Credit", color = MitraTextSecondary, fontSize = 13.sp)
                    val creditVal = (redeemAmount.toIntOrNull() ?: 0) / 100.0
                    Text("₹${String.format("%.2f", creditVal)}", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val qtyToRedeem = redeemAmount.toIntOrNull() ?: 0
        val isRedeemEnabled = qtyToRedeem >= 1000 && (session?.coins ?: 0) >= qtyToRedeem

        Button(
            onClick = {
                if (isRedeemEnabled) {
                    showConfirmDialog = true
                }
            },
            enabled = isRedeemEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MitraPrimaryGreen,
                disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (qtyToRedeem < 1000) "Minimum 1000 Coins required" else "Convert to Wallet Balance",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showConfirmDialog) {
        val qty = redeemAmount.toIntOrNull() ?: 0
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Coin Convert", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to convert $qty coins to ₹${String.format("%.2f", qty / 100.0)} Wallet Credit? This amount will be credited directly to your withdrawable wallet balance.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.redeemCoins(qty)
                        val amountStr = String.format(java.util.Locale.US, "%.2f", qty / 100.0)
                        navController.navigate("coin_redemption_success/$qty/$amountStr")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen)
                ) {
                    Text("Confirm Convert", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = MitraTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// 14. Reward History View
@Composable
fun RewardHistoryScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val txs by viewModel.transactions.collectAsState()
    val rewardTxs = txs.filter { it.type == "COIN_EARN" || it.type == "COIN_REDEEM" || it.type == "COIN_REDEMPTION" || it.type == "COIN_CHECKIN" || it.type == "COIN_VIDEO" }

    var rewardPageSize by remember { mutableStateOf(20) }
    val paginatedRewardTxs = remember(rewardTxs, rewardPageSize) {
        rewardTxs.take(rewardPageSize)
    }

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MitraTextMain
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Reward History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (rewardTxs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No reward transactions found", color = MitraTextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(paginatedRewardTxs) { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (tx.type == "COIN_EARN") MitraAccentGold.copy(0.12f) else Color(0xFFFCE8E6),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "COIN_EARN") Icons.Default.Stars else Icons.Default.CallMade,
                                        contentDescription = "Reward Icon",
                                        tint = if (tx.type == "COIN_EARN") MitraAccentGold else Color(0xFFEA4335),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val titleVal = if (tx.description.isNotBlank()) tx.description else if (tx.type == "COIN_EARN") "Coins Earned" else "Coins Redeemed"
                                    val dateTextVal = try {
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(tx.createdAt))
                                    } catch (e: Exception) {
                                        "Just now"
                                    }
                                    Text(text = titleVal, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)
                                    Text(text = dateTextVal, fontSize = 11.sp, color = MitraTextSecondary)
                                }
                            }

                            Text(
                                text = if (tx.type == "COIN_EARN") "+${tx.amount.toInt()} coins" else "-${tx.amount.toInt()} coins",
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "COIN_EARN") MitraPrimaryGreen else Color(0xFFEA4335),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                if (rewardTxs.size > rewardPageSize) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { rewardPageSize += 20 },
                                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                modifier = Modifier.testTag("load_more_rewards")
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

// 15. Refer & Earn Screen
@Composable
fun ReferEarnScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val session by viewModel.userSession.collectAsState()
    val code = session?.referralCode?.ifEmpty { "MM123456" } ?: "MM123456"
    
    // Live referrals state from ViewModel which listens to Firestore /referrals
    val liveReferrals by viewModel.userReferrals.collectAsState()

    var referralsPageSize by remember { mutableStateOf(20) }
    val paginatedReferrals = remember(liveReferrals, referralsPageSize) {
        liveReferrals.take(referralsPageSize)
    }
    
    // Compute dynamic stats based on live referrals
    val totalReferralsCount = liveReferrals.size
    val pendingReferralsCount = liveReferrals.count { it.status.uppercase() == "PENDING" }
    val completedReferralsCount = liveReferrals.count { it.status.uppercase() == "COMPLETED" }
    val computedEarnings = liveReferrals.filter { it.status.uppercase() == "COMPLETED" }.sumOf { it.amount }
    
    // Default / fallback to session stats if Firestore is loading or has different values
    val displayTotalReferrals = maxOf(session?.totalReferrals ?: 0, totalReferralsCount)
    val displayReferralEarnings = maxOf(session?.referralEarnings ?: 0.0, computedEarnings)

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MitraTextMain
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Referral Dashboard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info & Design Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MitraPrimaryGreen.copy(0.06f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Refer Icons",
                                tint = MitraPrimaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Invite Friends & Multiply Rewards",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraTextMain
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share your unique code below to unlock passive earning pathways for both of you.",
                                fontSize = 12.sp,
                                color = MitraTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Stat Cards Layout (2x2 Grid using Rows)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Total Referrals
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                .testTag("stat_total_referrals")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("Total Referrals", color = MitraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$displayTotalReferrals",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = MitraTextMain
                                )
                            }
                        }

                        // Pending Referrals
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                .testTag("stat_pending_referrals")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("Pending Referrals", color = MitraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$pendingReferralsCount",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = Color(0xFFD97706) // amber dark
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFFBBF24), CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Referral Earnings
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                                .testTag("stat_referral_earnings")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("Referral Earnings", color = MitraTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "₹${String.format("%,.2f", displayReferralEarnings)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = MitraPrimaryGreen
                                )
                            }
                        }

                        // Your Code Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MitraPrimaryGreen.copy(0.25f), RoundedCornerShape(12.dp))
                                .testTag("stat_referral_code")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("Your Code", color = MitraPrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = code,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MitraTextMain,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: Copy Code & Share Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy Action
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            try {
                                val systemClipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Referral Code", code)
                                systemClipboard?.setPrimaryClip(clip)
                            } catch (e: Exception) {
                                // Ignore exceptions safely
                            }
                            android.widget.Toast.makeText(context, "Code Copied: $code", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                            .testTag("referral_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Referral Code",
                            tint = MitraPrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Code", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Share Action
                    Button(
                        onClick = {
                            val shareText = "Hey! Join MoneyMitra to multiply your savings and get ₹20 welcome bonus on your first deposit/investment! Use my referral code: $code\nDownload now at: https://aistudio.build/moneymitra"
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("referral_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Referral Code",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Referral Reward Rules Card (Beautifully laid out)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Referral Program Reward Rules",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MitraTextMain
                        )
                        
                        Divider(color = MitraBorder.copy(0.5f))

                        // Rule 1
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                                    .background(MitraPrimaryGreen.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", color = MitraPrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Qualified Actions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MitraTextMain)
                                Text("Friend must register with your unique code and complete their first qualifying deposit or investment of at least ₹500.", fontSize = 12.sp, color = MitraTextSecondary)
                            }
                        }

                        // Rule 2
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                                    .background(MitraPrimaryGreen.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", color = MitraPrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Referrer Bonus: ₹50", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MitraPrimaryGreen)
                                Text("You will instantly receive ₹50 credited to your wallet balance once their first qualified activity is processed.", fontSize = 12.sp, color = MitraTextSecondary)
                            }
                        }

                        // Rule 3
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                                    .background(MitraPrimaryGreen.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("3", color = MitraPrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Referred Friend Bonus: ₹20", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MitraTextMain)
                                Text("Your friend gets ₹20 credited to their wallet balance upon processing of their welcome reward.", fontSize = 12.sp, color = MitraTextSecondary)
                            }
                        }
                    }
                }
            }

            // My Invited Friends: The Referral List
            item {
                Text(
                    text = "Invited Friends (${liveReferrals.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MitraTextMain,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (liveReferrals.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Empty Referrals",
                                tint = MitraTextSecondary.copy(0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No friends invited yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MitraTextMain
                            )
                            Text(
                                text = "When friends register using your link and make deposits, they'll appear here.",
                                fontSize = 12.sp,
                                color = MitraTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(paginatedReferrals, key = { it.id }) { referral ->
                    val isCompleted = referral.status.uppercase() == "COMPLETED"
                    val badgeBg = if (isCompleted) MitraPrimaryGreen.copy(0.12f) else Color(0xFFFFF3CD)
                    val badgeText = if (isCompleted) MitraPrimaryGreen else Color(0xFF856404)
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                            .testTag("referral_item_card_${referral.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = referral.referredName.ifBlank { "User ${referral.referredUid.takeLast(4)}" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MitraTextMain
                                )
                                Text(
                                    text = "Joined: ${formatReferralDate(referral.timestamp)}",
                                    fontSize = 11.sp,
                                    color = MitraTextSecondary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = referral.status.uppercase(),
                                    color = badgeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                if (liveReferrals.size > referralsPageSize) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { referralsPageSize += 20 },
                                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                modifier = Modifier.testTag("load_more_referrals")
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

private fun formatReferralDate(timestamp: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(timestamp))
    } catch (e: Exception) {
        "N/A"
    }
}

// 16. Add Money Screen
@Composable
fun AddMoneyScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val amountInput by viewModel.addMoneyAmountInput.collectAsState()

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MitraTextMain
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add Money",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        Text(
            text = "Refill your simulated wallet easily with digital fast-tracks.",
            fontSize = 14.sp,
            color = MitraTextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = amountInput,
            onValueChange = { viewModel.addMoneyAmountInput.value = it },
            label = { Text("Enter Amount", color = MitraTextSecondary) },
            prefix = { Text("₹ ", color = MitraTextMain) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MitraTextMain,
                unfocusedTextColor = MitraTextMain,
                focusedBorderColor = MitraPrimaryGreen,
                unfocusedBorderColor = MitraBorder,
                cursorColor = MitraPrimaryGreen,
                focusedLabelColor = MitraPrimaryGreen,
                unfocusedLabelColor = MitraTextSecondary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF1F5F9)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick selects
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("500", "1000", "2000", "5000").forEach { amt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .border(1.dp, MitraBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.addMoneyAmountInput.value = amt }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+₹$amt", fontWeight = FontWeight.Bold, color = MitraPrimaryGreen, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Recommended Methods", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 15.sp)

        Spacer(modifier = Modifier.height(12.dp))

        PaymentMethodRow(
            icon = Icons.Default.ElectricBolt,
            title = "UPI (Instant)",
            subtitle = "Pay using any UPI App"
        ) {
            if (amountInput.isNotBlank()) {
                navController.navigate("upi_payment")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PaymentMethodRow(
            icon = Icons.Default.AccountBalance,
            title = "Net Banking",
            subtitle = "All major Indian banks supported"
        ) {
            val d = amountInput.toDoubleOrNull() ?: 0.0
            if (d > 0) {
                viewModel.addMoney(d)
                navController.navigate("payment_success")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val d = amountInput.toDoubleOrNull() ?: 0.0
                if (d > 0) {
                    viewModel.addMoney(d)
                    navController.navigate("payment_success")
                }
            },
            enabled = amountInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MitraPrimaryGreen,
                disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PaymentMethodRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MitraPrimaryGreen)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MitraTextMain)
                Text(text = subtitle, fontSize = 11.sp, color = MitraTextSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = MitraTextSecondary)
    }
}

// 17. UPI Payment Screen
@Composable
fun UpiPaymentScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val amount by viewModel.addMoneyAmountInput.collectAsState()

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MitraTextMain
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Paying to", color = MitraTextSecondary, fontSize = 14.sp)
        Text("MoneyMitra", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MitraPrimaryGreen)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "₹${amount}",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Visual simulation of QR Code
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, MitraPrimaryGreen, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Code",
                tint = MitraTextMain,
                modifier = Modifier.size(160.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Scan & Pay Using Any UPI App", fontSize = 13.sp, color = MitraTextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Fake PSP provider badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("GPay", "PhonePe", "Paytm", "BHIM").forEach { psp ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .border(1.dp, MitraBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = psp,
                        fontSize = 12.sp,
                        color = MitraTextMain,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val d = amount.toDoubleOrNull() ?: 0.0
                if (d > 0) {
                    viewModel.addMoney(d)
                    navController.navigate("payment_success") {
                        popUpTo("add_money") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Simulate Payment Success", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// 18. Payment Success Screen
@Composable
fun PaymentSuccessScreen(navController: NavHostController) {
    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MitraPrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Deposit Request Sent!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your deposit request has been submitted successfully.\n\nYour wallet balance will update as soon as an administrator verifies the details.",
            fontSize = 14.sp,
            color = MitraTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                navController.navigate("dashboard_container") {
                    popUpTo("dashboard_container") { inclusive = false }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Go to Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun CoinRedemptionSuccessScreen(
    navController: NavHostController,
    viewModel: MoneyMitraViewModel,
    coins: Int,
    amount: Double
) {
    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MitraPrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Coins Redeemed Successfully!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$coins Coins converted to ₹${String.format("%.2f", amount)}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MitraPrimaryGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "₹${String.format("%.2f", amount)} has been credited to your Wallet Balance instantly.\n\nYou can view this transaction in Wallet History.",
            fontSize = 14.sp,
            color = MitraTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.dashboardSelectedTab.value = 2 // Wallet
                navController.navigate("dashboard_container") {
                    popUpTo("dashboard_container") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Go to Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.dashboardSelectedTab.value = 3 // Tasks
                navController.navigate("dashboard_container") {
                    popUpTo("dashboard_container") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MitraPrimaryGreen),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MitraPrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// 18.5. Withdrawal Success Screen
@Composable
fun WithdrawalSuccessScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MitraPrimaryGreen.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MitraPrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Withdrawal Request Submitted",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your withdrawal request has been received successfully.\n\nThe requested amount has been moved to Locked Balance and is awaiting admin approval.",
            fontSize = 14.sp,
            color = MitraTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = "Processing Time",
                        tint = MitraAccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Processing Time:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraTextMain
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "24–48 Hours",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MitraPrimaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Funds will be transferred to your linked bank account after approval.",
                    fontSize = 12.sp,
                    color = MitraTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                viewModel.dashboardSelectedTab.value = 2 // Wallet
                navController.navigate("dashboard_container") {
                    popUpTo("dashboard_container") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("go_to_wallet_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Go to Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.dashboardSelectedTab.value = 0 // Home
                navController.navigate("dashboard_container") {
                    popUpTo("dashboard_container") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("back_to_dashboard_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MitraPrimaryGreen),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MitraPrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// 19. Withdraw Money Screen
@Composable
fun WithdrawMoneyScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val session by viewModel.userSession.collectAsState()
    val amountInput by viewModel.withdrawMoneyAmountInput.collectAsState()
    val bankAccount by viewModel.withdrawBankAccount.collectAsState()
    val userWithdrawals by viewModel.userWithdrawals.collectAsState()
    val errorMsg by viewModel.withdrawError.collectAsState()

    var userWithdrawalsPageSize by remember { mutableStateOf(20) }
    val displayedWithdrawals = remember(userWithdrawals, userWithdrawalsPageSize) {
        userWithdrawals.take(userWithdrawalsPageSize)
    }

    val hasBankDetails = !session?.accountHolderName.isNullOrBlank() && 
                         !session?.bankName.isNullOrBlank() && 
                         !session?.accountNumber.isNullOrBlank() && 
                         !session?.ifscCode.isNullOrBlank()

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(horizontal = 24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MitraTextMain
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Withdraw Money",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Available: ₹${String.format("%,.2f", session?.balance ?: 0.0)}",
                fontSize = 14.sp,
                color = MitraPrimaryGreen,
                fontWeight = FontWeight.Medium
            )
            
            if ((session?.lockedBalance ?: 0.0) > 0.0) {
                Text(
                    text = "Locked (In-process): ₹${String.format("%,.2f", session?.lockedBalance ?: 0.0)}",
                    fontSize = 13.sp,
                    color = Color(0xFFE11D48),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Error Message banner
            if (!errorMsg.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = Color(0xFF9F1239),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. 24-48 hours processing notice card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Notice",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notice: Processing takes 24–48 hours. Funds are placed in locked balance until approved.",
                        color = Color(0xFF1E40AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedTextField(
                value = amountInput,
                onValueChange = { viewModel.withdrawMoneyAmountInput.value = it },
                label = { Text("Enter Amount (Min ₹500)", color = MitraTextSecondary) },
                prefix = { Text("₹ ", color = MitraTextMain) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MitraTextMain,
                    unfocusedTextColor = MitraTextMain,
                    focusedBorderColor = MitraPrimaryGreen,
                    unfocusedBorderColor = MitraBorder,
                    cursorColor = MitraPrimaryGreen,
                    focusedLabelColor = MitraPrimaryGreen,
                    unfocusedLabelColor = MitraTextSecondary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF1F5F9)
                ),
                modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("500", "1000", "2000", "5000").forEach { amt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .border(1.dp, MitraBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.withdrawMoneyAmountInput.value = amt }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("₹$amt", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Receiving Bank Account", fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(12.dp))

            if (hasBankDetails) {
                Card(
                     colors = CardDefaults.cardColors(containerColor = Color.White),
                     shape = RoundedCornerShape(12.dp),
                     modifier = Modifier
                         .fillMaxWidth()
                         .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Row(
                             modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             Text("Holder Name", color = MitraTextSecondary, fontSize = 12.sp)
                             Text(session?.accountHolderName ?: "", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                         }
                         Row(
                             modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             Text("Bank Name", color = MitraTextSecondary, fontSize = 12.sp)
                             Text(session?.bankName ?: "", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                         }
                         Row(
                             modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             Text("Account Number", color = MitraTextSecondary, fontSize = 12.sp)
                             Text(session?.accountNumber ?: "", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                         }
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             Text("IFSC Code", color = MitraTextSecondary, fontSize = 12.sp)
                             Text(session?.ifscCode ?: "", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                         }
                     }
                }
            } else {
                Card(
                     colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                     shape = RoundedCornerShape(12.dp),
                     modifier = Modifier
                         .fillMaxWidth()
                         .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text(
                             text = "Bank Details Required",
                             color = Color(0xFF991B1B),
                             fontWeight = FontWeight.Bold,
                             fontSize = 14.sp
                         )
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(
                             text = "No bank settlement details are configured. Please set up your bank account details under Profile → Bank Account Details first to enable withdrawals.",
                             color = Color(0xFF7F1D1D),
                             fontSize = 12.sp
                         )
                     }
                }
            }

            // 3. User withdrawals history list
            if (userWithdrawals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Withdrawal Requests",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MitraTextMain,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                displayedWithdrawals.forEach { w ->
                    val statusColor = when (w.status) {
                        "PENDING" -> Color(0xFFD97706)
                        "APPROVED" -> MitraPrimaryGreen
                        else -> Color(0xFFDC2626)
                    }
                    val statusBg = when (w.status) {
                        "PENDING" -> Color(0xFFFEF3C7)
                        "APPROVED" -> Color(0xFFDCFCE7)
                        else -> Color(0xFFFEE2E2)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "₹${String.format("%,.2f", w.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MitraTextMain,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", w.createdAt).toString(),
                                    color = MitraTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${w.bankName} - ${w.accountNumber}",
                                    color = MitraTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(statusBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = w.status,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                if (userWithdrawals.size > userWithdrawalsPageSize) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { userWithdrawalsPageSize += 20 },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                            modifier = Modifier.testTag("load_more_user_withdrawals")
                        ) {
                            Text("Load More", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        val d = amountInput.toDoubleOrNull() ?: 0.0
        val isEnabled = amountInput.isNotBlank() && d <= (session?.balance ?: 0.0) && d >= 500 && hasBankDetails

        Button(
            onClick = {
                if (isEnabled) {
                    val bankDetailStr = "${session?.bankName ?: "Bank"} - ${session?.accountNumber ?: ""}"
                    viewModel.withdrawMoney(d, bank = bankDetailStr) {
                        navController.navigate("withdrawal_success")
                    }
                }
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp).testTag("withdraw_now_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MitraPrimaryGreen,
                disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Withdraw Now",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// 20. Transaction History Screen
@Composable
fun TransactionHistoryScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val txs by viewModel.transactions.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MitraTextMain
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transaction History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MitraTextMain,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Credit", "Debit").forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MitraPrimaryGreen.copy(0.12f) else Color(0xFFF1F5F9),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, if (isSelected) MitraPrimaryGreen else MitraBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MitraPrimaryGreen else MitraTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val filteredTxs = txs.filter {
            val isCreditType = when (it.type) {
                "DEPOSIT", "REFERRAL_BONUS", "REFERRAL_REWARD", "INVESTMENT_RETURN", "INVESTMENT_EARNING", "ADMIN_CREDIT", "CREDIT" -> true
                else -> false
            }
            when (selectedFilter) {
                "Credit" -> isCreditType
                "Debit" -> !isCreditType
                else -> true
            }
        }

        var walletPageSize by remember { mutableStateOf(20) }
        val paginatedTxs = remember(filteredTxs, walletPageSize) {
            filteredTxs.take(walletPageSize)
        }

        if (filteredTxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MitraLightGreenBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "No Transactions",
                            tint = MitraPrimaryGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transactions logged yet",
                        fontWeight = FontWeight.Bold,
                        color = MitraTextMain,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Your deposits, withdrawals, and investment returns will be detailed right here.",
                        fontSize = 12.sp,
                        color = MitraTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(paginatedTxs) { tx ->
                    MoneyMitraTransactionRow(tx)
                }
                if (filteredTxs.size > walletPageSize) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { walletPageSize += 20 },
                                colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                                modifier = Modifier.testTag("load_more_transactions")
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

// 21. Fintech custom input component for ultra-high contrast & premium styling
@Composable
fun FintechTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderBrush = Brush.linearGradient(
        if (isFocused) {
            listOf(MitraPrimaryGreen, MitraPrimaryGreen)
        } else {
            listOf(MitraBorder, MitraBorder)
        }
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label, 
                    color = if (isFocused) MitraPrimaryGreen else MitraTextSecondary,
                    fontWeight = FontWeight.Medium
                ) 
            },
            placeholder = { 
                Text(
                    text = placeholder, 
                    color = MitraTextSecondary.copy(alpha = 0.5f)
                ) 
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MitraTextMain,
                unfocusedTextColor = MitraTextMain,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF1F5F9),
                focusedBorderColor = MitraPrimaryGreen,
                unfocusedBorderColor = MitraBorder,
                cursorColor = MitraPrimaryGreen,
                focusedLabelColor = MitraPrimaryGreen,
                unfocusedLabelColor = MitraTextSecondary,
                focusedPlaceholderColor = MitraTextSecondary.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = MitraTextSecondary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(16.dp)
                )
        )
    }
}

// 22. About Us Screen
@Composable
fun AboutUsScreen(navController: NavHostController) {
    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "About Us",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MitraTextMain
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Welcome to Our Platform",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MitraPrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A modern digital ecosystem designed to help users explore smarter financial growth opportunities through technology-driven investment strategies.\n\nOur platform combines advanced analytics, AI-powered insights, and diversified market exposure to create a seamless and user-friendly investment experience. We focus on helping users access modern investment opportunities across multiple growing sectors including Gold, Real Estate, Cryptocurrency, and Stock Market assets.",
                            color = MitraTextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Section 1: What We Offer
            item {
                Text("What We Offer", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val offers = listOf(
                            "Smart investment opportunities",
                            "AI-powered market analysis",
                            "Diversified asset allocation",
                            "Real-time portfolio tracking",
                            "Daily investment monitoring",
                            "Secure and modern user experience"
                        )
                        offers.forEach { offer ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = MitraPrimaryGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(offer, color = MitraTextMain, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Section 2: How Our Platform Works
            item {
                Text("How Our Platform Works", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val steps = listOf(
                            "Step 1" to "Users deposit investment funds in their secure wallet",
                            "Step 2" to "Funds are strategically allocated across multiple high-growth sectors",
                            "Step 3" to "AI engine monitors complex market trends & opportunities 24/7",
                            "Step 4" to "Users track clear portfolio yields and gains in real-time"
                        )
                        steps.forEach { (step, desc) ->
                            Column {
                                Text(step, fontWeight = FontWeight.Black, color = MitraAccentGold, fontSize = 12.sp)
                                Text(desc, color = MitraTextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Section 3: Investment Sectors
            item {
                Text("Smart Investment Sectors", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sectors = listOf(
                        Triple("Gold Investments", "Secure gold commodity trends", Color(0xFFD4AF37)),
                        Triple("Real Estate & Property Assets", "High yield infrastructural opportunities", Color(0xFFE67E22)),
                        Triple("Cryptocurrency Market", "Advanced high frequency digital liquidity", Color(0xFF3498DB)),
                        Triple("Stock Market Opportunities", "Proven blue chip value parameters", Color(0xFF2ECC71)),
                        Triple("Digital Financial Assets", "Dynamic tokenized assets configuration", Color(0xFF9B59B6))
                    )
                    sectors.forEach { (name, details, tint) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp).background(tint, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(name, fontWeight = FontWeight.Bold, color = MitraTextMain, fontSize = 14.sp)
                                    Text(details, color = MitraTextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Bank Savings vs Our Platform
            item {
                Text("Savings vs Our Platform", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Traditional Bank Savings:", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Lower traditional annual growth\n• Primarily focused on fund storage\n• Limited growth potential over time", color = MitraTextSecondary, fontSize = 13.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MitraBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Our Investment Platform:", color = MitraPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Market-based growth opportunities\n• Diversified investment sectors\n• AI-powered financial insights\n• Real-time growth tracking\n• Smart portfolio management", color = MitraTextSecondary, fontSize = 13.sp)
                    }
                }
            }

            // Section 5: Why Choose Us
            item {
                Text("Why Choose Us", color = MitraTextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, MitraBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val choices = listOf(
                            "Modern fintech investment experience",
                            "Growth-focused investment strategies",
                            "Diversified portfolio management",
                            "AI-powered financial insights",
                            "Daily portfolio tracking",
                            "User-friendly investment dashboard"
                        )
                        choices.forEach { choice ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = "Choice", tint = MitraAccentGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(choice, color = MitraTextMain, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Section 6: Disclaimer
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, Color(0xFFFCE8E6), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Disclaimer", tint = Color(0xFFEA4335), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Investments are subject to market risks and volatility. Returns, earnings, and growth percentages displayed within the app are estimated projections and are not guaranteed.",
                            fontSize = 12.sp,
                            color = Color(0xFF4A151B),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// 23. Forgot Password Screen
@Composable
fun ForgotPasswordScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    var phone by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val lightBgGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBgGradient)
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color(0xFFEA4335),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (step == 1) {
            Text(
                text = "Reset Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )

            Text(
                text = "Enter your registered Email or Phone number to receive a verification OTP.",
                fontSize = 15.sp,
                color = MitraTextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            FintechTextField(
                value = phone,
                onValueChange = { 
                    phone = it
                    errorMessage = ""
                },
                label = "Email or Phone Number",
                placeholder = "e.g. admin@mitra.com or 9876543210",
                leadingIcon = {
                    Icon(Icons.Default.AlternateEmail, contentDescription = "Identifier", tint = MitraPrimaryGreen)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (phone.isNotBlank()) {
                        step = 2
                    }
                },
                enabled = phone.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MitraPrimaryGreen,
                    disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Get OTP", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else if (step == 2) {
            Text(
                text = "Verification Code",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )

            Text(
                text = "An OTP code has been dispatched to $phone.",
                fontSize = 15.sp,
                color = MitraTextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            FintechTextField(
                value = otpCode,
                onValueChange = { 
                    otpCode = it
                    errorMessage = ""
                },
                label = "6-Digit OTP",
                placeholder = "123456",
                leadingIcon = {
                    Icon(Icons.Default.LockOpen, contentDescription = "OTP", tint = MitraPrimaryGreen)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (otpCode.length >= 4) {
                        step = 3
                    }
                },
                enabled = otpCode.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MitraPrimaryGreen,
                    disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Verify Code", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "Set New Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MitraTextMain
            )

            Text(
                text = "Choose a strong, alphanumeric key containing customized characters.",
                fontSize = 15.sp,
                color = MitraTextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            var passwordVisible by remember { mutableStateOf(false) }

            FintechTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    errorMessage = ""
                },
                label = "New Alphanumeric Password",
                placeholder = "••••••••",
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = MitraPrimaryGreen)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password view",
                            tint = MitraTextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (newPassword.isNotBlank()) {
                        coroutineScope.launch {
                            val success = viewModel.forgotPasswordReset(phone.trim(), newPassword)
                            if (success) {
                                navController.navigate("login") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                errorMessage = "User account not found. Please register."
                            }
                        }
                    }
                },
                enabled = newPassword.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MitraPrimaryGreen,
                    disabledContainerColor = MitraPrimaryGreen.copy(0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Update & Login", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 24. Fully Editable Edit Profile Screen (Personal Information Only)
@Composable
fun EditProfileScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val session by viewModel.userSession.collectAsState()
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf("") }
    
    val isPhoneAuth = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber?.isNotEmpty() == true
    }

    LaunchedEffect(session) {
        session?.let {
            name = it.name
            phone = it.phoneNumber
        }
    }

    val lightBackground = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBackground)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MitraTextMain
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            item {
                Text(
                    text = "Personal Information",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MitraPrimaryGreen,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FintechTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "User Full Name",
                            placeholder = "As on Pan card"
                        )

                        FintechTextField(
                            value = phone,
                            onValueChange = { if (!isPhoneAuth) phone = it },
                            label = if (isPhoneAuth) "Mobile Number (Verified - Read Only)" else "Mobile Number (+91)",
                            placeholder = "9876543210",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            trailingIcon = {
                                if (isPhoneAuth) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Phone",
                                        tint = MitraPrimaryGreen
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Optional Password Settings section if applicable (not Phone Auth)
            if (!isPhoneAuth) {
                item {
                    Text(
                        text = "Password Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraPrimaryGreen,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "To change your password, enter a new password below:",
                                fontSize = 12.sp,
                                color = MitraTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            FintechTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = "New Password",
                                placeholder = "Min 6 characters",
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password view",
                                            tint = MitraTextSecondary
                                        )
                                    }
                                }
                            )

                            if (passwordMessage.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = passwordMessage,
                                    fontSize = 12.sp,
                                    color = if (passwordMessage.contains("successfully", ignoreCase = true)) MitraPrimaryGreen else MitraErrorRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    scope.launch {
                        // Keep current bank details from session
                        val currentEmail = session?.email ?: ""
                        val currentBankHolder = session?.accountHolderName ?: ""
                        val currentBankName = session?.bankName ?: ""
                        val currentBankAcc = session?.accountNumber ?: ""
                        val currentBankIFSC = session?.ifscCode ?: ""
                        val currentBankUpi = session?.upiId ?: ""

                        if (newPassword.isNotBlank() && newPassword.length >= 6) {
                            try {
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)?.await()
                                passwordMessage = "Password updated successfully"
                            } catch (e: Exception) {
                                passwordMessage = "Password update failed: ${e.localizedMessage ?: "Unknown error"}"
                            }
                        }

                        viewModel.saveUserProfileAndBank(
                            name = name,
                            email = currentEmail,
                            phone = phone,
                            accountHolderName = currentBankHolder,
                            bankName = currentBankName,
                            accountNumber = currentBankAcc,
                            ifscCode = currentBankIFSC,
                            upiId = currentBankUpi
                        )
                        navController.popBackStack()
                    }
                }
            },
            enabled = name.isNotBlank() && phone.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MitraPrimaryGreen,
                disabledContainerColor = MitraPrimaryGreen.copy(0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 25. High-fidelity Bank Account Screen (Withdrawal Settlement Details)
@Composable
fun BankAccountScreen(navController: NavHostController, viewModel: MoneyMitraViewModel) {
    val session by viewModel.userSession.collectAsState()
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }

    var accountHolderName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }

    val hasBank = !session?.bankName.isNullOrBlank() && !session?.accountNumber.isNullOrBlank()

    LaunchedEffect(session, isEditing) {
        session?.let {
            if (!isEditing) {
                accountHolderName = it.accountHolderName
                bankName = it.bankName
                accountNumber = it.accountNumber
                confirmAccountNumber = it.accountNumber
                ifscCode = it.ifscCode
            }
        }
    }

    val lightBackground = Brush.verticalGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFF7F9F8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBackground)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MitraTextMain)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Bank Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MitraTextMain
            )
        }

        if (isEditing || !hasBank) {
            // Edit Bank Details Form View
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                item {
                    Text(
                        text = if (!hasBank) "Link Settlement Bank Account" else "Edit Bank Settlement Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MitraPrimaryGreen,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FintechTextField(
                                value = accountHolderName,
                                onValueChange = { accountHolderName = it },
                                label = "Account Holder Name",
                                placeholder = "As on bank account statement"
                            )

                            FintechTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = "Bank Name",
                                placeholder = "e.g. Axis Bank, HDFC Bank, SBI"
                            )

                            FintechTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = "Account Number",
                                placeholder = "e.g. 912010045612345",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            FintechTextField(
                                value = confirmAccountNumber,
                                onValueChange = { confirmAccountNumber = it },
                                label = "Confirm Account Number",
                                placeholder = "Re-enter Account Number",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            if (confirmAccountNumber.isNotEmpty() && accountNumber != confirmAccountNumber) {
                                Text(
                                    text = "Account numbers do not match",
                                    color = MitraErrorRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            FintechTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it.uppercase() },
                                label = "Bank IFSC Code",
                                placeholder = "e.g. UTIB0000123"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasBank) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MitraBorder)
                    ) {
                        Text("Cancel", color = MitraTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = {
                        if (accountHolderName.isNotBlank() && bankName.isNotBlank() && accountNumber.isNotBlank() && ifscCode.isNotBlank() && accountNumber == confirmAccountNumber) {
                            scope.launch {
                                // Save using ViewModel and persist current profile fields
                                val userName = session?.name ?: ""
                                val userEmail = session?.email ?: ""
                                val userPhone = session?.phoneNumber ?: ""
                                val currentUpi = session?.upiId ?: ""

                                viewModel.saveUserProfileAndBank(
                                    name = userName,
                                    email = userEmail,
                                    phone = userPhone,
                                    accountHolderName = accountHolderName,
                                    bankName = bankName,
                                    accountNumber = accountNumber,
                                    ifscCode = ifscCode,
                                    upiId = currentUpi
                                )
                                isEditing = false
                            }
                        }
                    },
                    enabled = accountHolderName.isNotBlank() && bankName.isNotBlank() && accountNumber.isNotBlank() && ifscCode.isNotBlank() && accountNumber == confirmAccountNumber,
                    modifier = Modifier.weight(if (hasBank) 1.5f else 1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MitraPrimaryGreen,
                        disabledContainerColor = MitraPrimaryGreen.copy(0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save Details", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // View Bank Details View Mode
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                // Display Linked Bank details beautifully using fintech card overlay
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .border(1.dp, MitraBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Settlement Bank Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MitraPrimaryGreen
                            )
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Bank",
                                tint = MitraPrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Render Bank Name & Masked Acc: Example: Axis Bank ••••1234
                        val maskedNo = if (accountNumber.length >= 4) accountNumber.takeLast(4) else accountNumber
                        Text(
                            text = "$bankName ••••$maskedNo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MitraTextMain
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("ACCOUNT HOLDER", fontSize = 10.sp, color = MitraTextSecondary, fontWeight = FontWeight.SemiBold)
                                Text(accountHolderName, fontSize = 14.sp, color = MitraTextMain, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("IFSC CODE", fontSize = 10.sp, color = MitraTextSecondary, fontWeight = FontWeight.SemiBold)
                                Text(ifscCode, fontSize = 14.sp, color = MitraTextMain, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MitraPrimaryGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Bank Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}