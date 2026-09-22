package com.bexmarket.ng.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferences = ThemePreferences(this)
        
        setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                OnboardingScreen(
                    onFinished = {
                        scope.launch {
                            themePreferences.setOnboardingComplete(true)
                            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val slides = listOf(
        OnboardingSlide(
            title = "Buy Safely",
            description = "Browse thousands of products from verified local sellers with peace of mind.",
            backgroundColor = Color(0xFFE8F5E9)
        ),
        OnboardingSlide(
            title = "Sell Quickly",
            description = "Post your items in less than a minute and reach thousands of buyers in your area.",
            backgroundColor = Color(0xFFF1F8E9)
        ),
        OnboardingSlide(
            title = "Connect Locally",
            description = "Chat directly with sellers and buyers to arrange easy pickups and payments.",
            backgroundColor = Color(0xFFE0F2F1)
        )
    )

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(slide.backgroundColor)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = slide.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF006400),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = slide.description,
                        fontSize = 18.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                }
            }

            // Indicators
            Row(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFF006400) else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(10.dp)
                    )
                }
            }

            // Bottom Buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                if (pagerState.currentPage < 2) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text("Skip", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400))
                    ) {
                        Text("Get Started", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val backgroundColor: Color
)
