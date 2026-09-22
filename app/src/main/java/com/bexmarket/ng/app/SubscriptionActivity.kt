package com.bexmarket.ng.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.paystack.android.Paystack
import co.paystack.android.PaystackSdk
import co.paystack.android.Transaction
import co.paystack.android.model.Charge

class SubscriptionActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Paystack SDK
        if (!PaystackSdk.isSdkInitialized()) {
            PaystackSdk.initialize(applicationContext)
            PaystackSdk.setPublicKey("pk_test_ec1c10df9c310a2dfdedf4")
        }

        setContent {
            AppTheme {
                SubscriptionScreen(authViewModel) {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Subscription", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CardMembership,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Activate Premium Store",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Gain full unlimited access to publish your products on BEX Market.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            currentUser?.subscriptionExpiresAt?.let { expiry ->
                Text(
                    text = "Current Expiry: ${expiry.take(10)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = {
                    val email = currentUser?.email
                    if (email.isNullOrEmpty()) {
                        Toast.makeText(context, "User email not found. Please log in again.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    
                    isProcessing = true
                    
                    val charge = Charge()
                    charge.amount = 200000 // 200000 kobo = N2,000
                    charge.email = email
                    
                    PaystackSdk.chargeCard(context as android.app.Activity, charge, object : Paystack.TransactionCallback {
                        override fun onSuccess(transaction: Transaction) {
                            Log.d("PaystackPayment", "Transaction successful: ${transaction.reference}")
                            authViewModel.addSubscriptionDays(30) { success, error ->
                                isProcessing = false
                                if (success) {
                                    Toast.makeText(context, "Subscription updated! 30 days added.", Toast.LENGTH_LONG).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Payment succeeded but failed to sync profile: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        override fun beforeValidate(transaction: Transaction) {
                            Log.d("PaystackPayment", "Before validate: ${transaction.reference}")
                        }

                        override fun onError(error: Throwable, transaction: Transaction?) {
                            isProcessing = false
                            Log.e("PaystackPayment", "Payment Error: ${error.message}", error)
                            Toast.makeText(context, "Payment Failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }


                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Pay ₦2,000 / Month", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
