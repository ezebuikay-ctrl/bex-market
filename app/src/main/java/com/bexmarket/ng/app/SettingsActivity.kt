package com.bexmarket.ng.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onBack = { finish() },
                    onLogout = {
                        authViewModel.logout()
                        val intent = android.content.Intent(this, LoginActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val notificationsEnabled by viewModel.isNotificationsEnabled.collectAsState(initial = false)
    val saveStatus by viewModel.saveStatus.collectAsState()

    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }
    var whatsapp by remember(user) { mutableStateOf(user?.whatsapp ?: "") }

    // Location State
    var stateExpanded by remember { mutableStateOf(false) }
    val nigeriaStates = NigeriaData.statesAndAreas.keys.toList().sorted()
    var selectedState by remember(user) { mutableStateOf(user?.locationState ?: "Lagos") }

    var areaExpanded by remember { mutableStateOf(false) }
    val areas = NigeriaData.statesAndAreas[selectedState] ?: listOf("General")
    var selectedArea by remember(user) { mutableStateOf(user?.locationAxis ?: "General") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Profile Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("User Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("Default Location", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // State Dropdown
                ExposedDropdownMenuBox(
                    expanded = stateExpanded,
                    onExpandedChange = { stateExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedState,
                        onValueChange = {},
                        label = { Text("State") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = stateExpanded,
                        onDismissRequest = { stateExpanded = false }
                    ) {
                        nigeriaStates.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    selectedState = st
                                    selectedArea = NigeriaData.statesAndAreas[st]?.firstOrNull() ?: "General"
                                    stateExpanded = false
                                }
                            )
                        }
                    }
                }

                // Area Dropdown
                ExposedDropdownMenuBox(
                    expanded = areaExpanded,
                    onExpandedChange = { areaExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedArea,
                        onValueChange = {},
                        label = { Text("Area / Axis") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = areaExpanded,
                        onDismissRequest = { areaExpanded = false }
                    ) {
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area) },
                                onClick = {
                                    selectedArea = area
                                    areaExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("App Preferences", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            // Notification Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text("Push Notifications")
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            }

            // Community Feedback
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://t.me/bexmarketngfeedback")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/bexmarketngfeedback"))
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Feedback, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Join Feedback Community", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveProfileChanges(name, email, phone, whatsapp, selectedState, selectedArea) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = saveStatus !is SettingsViewModel.SaveStatus.Loading
            ) {
                if (saveStatus is SettingsViewModel.SaveStatus.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Changes")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Red),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logout Account", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Handle Save Status
            LaunchedEffect(saveStatus) {
                when (saveStatus) {
                    is SettingsViewModel.SaveStatus.Success -> {
                        Toast.makeText(onBack as? android.content.Context ?: return@LaunchedEffect, "Changes saved!", Toast.LENGTH_SHORT).show()
                        viewModel.resetSaveStatus()
                    }
                    is SettingsViewModel.SaveStatus.Error -> {
                        Toast.makeText(onBack as? android.content.Context ?: return@LaunchedEffect, (saveStatus as SettingsViewModel.SaveStatus.Error).message, Toast.LENGTH_LONG).show()
                        viewModel.resetSaveStatus()
                    }
                    else -> {}
                }
            }
        }
    }
}
