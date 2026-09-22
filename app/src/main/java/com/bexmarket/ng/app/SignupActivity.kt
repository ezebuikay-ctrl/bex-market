package com.bexmarket.ng.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SignupMethod { EMAIL, USERNAME }

class SignupActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var signupMethod by remember { mutableStateOf<SignupMethod?>(null) }
                val authStatus by authViewModel.authStatus.collectAsState()

                LaunchedEffect(authStatus) {
                    if (authStatus is AuthStatus.Authenticated) {
                        startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        finish()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (signupMethod) {
                        null -> MethodSelectionScreen(onMethodSelected = { signupMethod = it })
                        SignupMethod.EMAIL -> EmailSignupScreen(
                            viewModel = authViewModel,
                            onBack = { signupMethod = null }
                        )
                        SignupMethod.USERNAME -> UsernameSignupScreen(
                            viewModel = authViewModel,
                            onBack = { signupMethod = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MethodSelectionScreen(onMethodSelected: (SignupMethod) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Join BEX Market", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B6B2E))
        Text("Choose your preferred registration method", modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        Button(
            onClick = { onMethodSelected(SignupMethod.EMAIL) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B6B2E))
        ) {
            Icon(Icons.Default.Email, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign up with Email")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onMethodSelected(SignupMethod.USERNAME) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign up with Username")
        }
    }
}

@Composable
fun EmailSignupScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var isAcknowledged by remember { mutableStateOf(false) }
    val authStatus by viewModel.authStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        
        Text("Email Registration", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Username / Business Name") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(Modifier.height(32.dp))

        // Security Warning
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            val context = LocalContext.current
            val annotatedString = buildAnnotatedString {
                append("If you forget your password or username, you will permanently lose access to your account unless you contact support via Telegram: ")
                pushStringAnnotation(tag = "telegram", annotation = "https://t.me/BMngsupport")
                withStyle(style = SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                    append("t.me/BMngsupport")
                }
                pop()
                append(". Please write them down in a secure place.")
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "WARNING: Account recovery is NOT supported.",
                    color = Color.Red,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "telegram", start = offset, end = offset).firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(annotation.item)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAcknowledged,
                onCheckedChange = { isAcknowledged = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0B6B2E))
            )
            Text(
                "I have written down my password and username and understand that I cannot recover my account if I lose them.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.clickable { isAcknowledged = !isAcknowledged }
            )
        }

        Spacer(Modifier.height(24.dp))

        if (authStatus is AuthStatus.Error) {
            Text((authStatus as AuthStatus.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = { viewModel.signUp(email, password, name, name) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B6B2E)),
            enabled = authStatus !is AuthStatus.Loading && isAcknowledged
        ) {
            if (authStatus is AuthStatus.Loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            else Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UsernameSignupScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var businessName by remember { mutableStateOf("") }
    var isAcknowledged by remember { mutableStateOf(false) }
    val authStatus by viewModel.authStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        
        Text("Username Registration", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Business Name") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(Modifier.height(32.dp))

        // Security Warning
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            val context = LocalContext.current
            val annotatedString = buildAnnotatedString {
                append("If you forget your password or username, you will permanently lose access to your account unless you contact support via Telegram: ")
                pushStringAnnotation(tag = "telegram", annotation = "https://t.me/BMngsupport")
                withStyle(style = SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                    append("t.me/BMngsupport")
                }
                pop()
                append(". Please write them down in a secure place.")
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "WARNING: Account recovery is NOT supported.",
                    color = Color.Red,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "telegram", start = offset, end = offset).firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(annotation.item)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAcknowledged,
                onCheckedChange = { isAcknowledged = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0B6B2E))
            )
            Text(
                "I have written down my password and username and understand that I cannot recover my account if I lose them.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.clickable { isAcknowledged = !isAcknowledged }
            )
        }

        Spacer(Modifier.height(24.dp))

        if (authStatus is AuthStatus.Error) {
            Text((authStatus as AuthStatus.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        Button(
            onClick = { viewModel.signUpWithUsername(username, password, businessName) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B6B2E)),
            enabled = authStatus !is AuthStatus.Loading && isAcknowledged
        ) {
            if (authStatus is AuthStatus.Loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            else Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold)
        }
    }
}
