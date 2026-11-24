package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GoogleSignInButton
import com.example.ui.components.AsyncDrawableImage
import com.example.ui.components.PartnerStatusBadge
import com.example.ui.components.StatusNoticeBanner
import com.google.firebase.auth.FirebaseUser

@Composable
fun AuthAndOnboardingScreen(
    isAlreadySignedIn: Boolean = false,
    onAuthenticated: (user: FirebaseUser, consentAccepted: Boolean) -> Unit = { _, _ -> },
    onCloseReplay: () -> Unit = {},
) {
    var consentChecked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isAlreadySignedIn) {
                        IconButton(
                            onClick = onCloseReplay,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to profile",
                            )
                        }
                    }
                    Text(
                        text = "PaiseJar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                PartnerStatusBadge()
            }

            AsyncDrawableImage(
                resId = R.drawable.img_onboarding_jar,
                contentDescription = "PaiseJar round-up savings illustration",
                contentScale = ContentScale.Crop,
                inSampleSize = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(MaterialTheme.shapes.large),
            )

            Text(
                text = "Everyday spending can help you build a saving habit.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = "Choose a round-up target (₹10, ₹50, ₹100, or custom ₹10 increments) and cap rules. PaiseJar calculates the spare change from eligible purchases so you stay in control.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Examples card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "How round-up math works (₹100 target)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ExampleCalculationRow(
                        label = "Coffee purchase",
                        spend = "₹180",
                        target = "₹200",
                        roundUp = "+₹20",
                    )
                    ExampleCalculationRow(
                        label = "Food delivery order",
                        spend = "₹470",
                        target = "₹500",
                        roundUp = "+₹30",
                    )
                    ExampleCalculationRow(
                        label = "Exact multiple purchase",
                        spend = "₹500",
                        target = "₹500",
                        roundUp = "₹0",
                    )
                }
            }

            // Honest financial & security disclosures
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Fees, Partner Status & Data Safety",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    DisclosureItem(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Real-money transfers are currently disabled",
                        body = "No RBI Account Aggregator (FIU) or UPI Autopay partner is connected in this release. Your confirmed rupee balance is ₹0; optional sandbox scenarios show calculation previews only.",
                    )
                    DisclosureItem(
                        icon = Icons.Rounded.VerifiedUser,
                        title = "Transparent fees & no lock-in",
                        body = "PaiseJar app fee is ₹0. Because no funds are held here right now, there are no withdrawal charges or balance lock-ins.",
                    )
                    DisclosureItem(
                        icon = Icons.Rounded.Lock,
                        title = "Never enter bank passwords or PINs",
                        body = "We never ask for bank passwords, UPI PINs, card PINs, or full card numbers. Your account recovery works through your Google Sign-In.",
                    )
                    DisclosureItem(
                        icon = Icons.Rounded.Shield,
                        title = "What is stored in your cloud account",
                        body = "Only your round-up rules, goal targets, optional sandbox preview entries, audit history, and support tickets — protected by authenticated Firestore security rules.",
                    )
                }
            }

            if (!isAlreadySignedIn) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { consentChecked = !consentChecked },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (consentChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = consentChecked,
                            onCheckedChange = { consentChecked = it },
                            modifier = Modifier.testTag("consent_checkbox"),
                        )
                        Text(
                            text = "I understand PaiseJar does not link a bank account or move real money yet, and I consent to storing my rules, goals, and sandbox calculations under my Google account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                authError?.let { msg ->
                    StatusNoticeBanner(
                        title = "Sign-in notice",
                        subtitle = msg,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                GoogleSignInButton(
                    enabled = consentChecked,
                    buttonText = "Sign in with Google",
                    onSignInSuccess = { user ->
                        authError = null
                        onAuthenticated(user, consentChecked)
                    },
                    onSignInError = { err ->
                        authError = err
                    },
                )

                if (!consentChecked) {
                    Text(
                        text = "Check the consent disclosure above to enable Google Sign-In.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Button(
                    onClick = onCloseReplay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                ) {
                    Text("Back to Profile")
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExampleCalculationRow(
    label: String,
    spend: String,
    target: String,
    roundUp: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Purchase $spend → rounded to $target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = roundUp,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun DisclosureItem(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
