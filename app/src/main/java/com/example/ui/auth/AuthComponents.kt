package com.example.ui.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * AuthTextField
 * ─────────────
 * Reusable OutlinedTextField for auth screens with:
 *  • leading icon
 *  • optional password toggle (eye icon)
 *  • inline error text below the field
 *  • error-coloured border on validation failure
 */
@Composable
fun AuthTextField(
    value:           String,
    onValueChange:   (String) -> Unit,
    label:           String,
    leadingIcon:     ImageVector,
    modifier:        Modifier           = Modifier,
    isPassword:      Boolean            = false,
    passwordVisible: Boolean            = false,
    onPasswordToggle: (() -> Unit)?     = null,
    errorMessage:    String?            = null,
    enabled:         Boolean            = true,
    keyboardOptions: KeyboardOptions    = KeyboardOptions.Default,
    keyboardActions: KeyboardActions    = KeyboardActions.Default
) {
    Column(modifier = modifier.animateContentSize()) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            label           = { Text(label) },
            leadingIcon     = {
                Icon(
                    imageVector        = leadingIcon,
                    contentDescription = null,
                    tint               = if (errorMessage != null)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon    = if (isPassword) {
                {
                    IconButton(onClick = { onPasswordToggle?.invoke() }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
            isError         = errorMessage != null,
            enabled         = enabled,
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier        = Modifier.fillMaxWidth(),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor     = MaterialTheme.colorScheme.error,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Inline field-level error
        if (errorMessage != null) {
            Text(
                text     = errorMessage,
                color    = MaterialTheme.colorScheme.error,
                style    = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * ErrorBanner
 * ───────────
 * Red surface banner shown below the form fields for global auth errors
 * (e.g. "Incorrect email or password"). Dismissible via X button.
 */
@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text     = message,
                color    = MaterialTheme.colorScheme.onErrorContainer,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * AmbientBackground
 * ──────────────────
 * Decorative radial-gradient blobs that give auth screens their premium feel.
 * Renders two blurred circles in opposite corners using the theme's primary
 * and secondary colours.
 */
@Composable
fun AmbientBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-120).dp)
                .blur(100.dp)
                .let { m ->
                    m // background applied via Surface-level gradient below
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
            )
        }
        // Top-right glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .blur(90.dp)
        )
        // Bottom-left glow — rendered as a tinted box via Brush
    }
}
