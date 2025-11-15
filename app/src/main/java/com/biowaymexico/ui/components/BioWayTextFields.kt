package com.biowaymexico.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Campo de texto personalizado de BioWay
 */
@Composable
fun BioWayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 14.sp) },
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = BioWayColors.PrimaryGreen
                    )
                }
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = BioWayColors.TextGrey
                        )
                    }
                }
            },
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BioWayColors.PrimaryGreen,
                unfocusedBorderColor = BioWayColors.LightGrey,
                focusedLabelColor = BioWayColors.PrimaryGreen,
                unfocusedLabelColor = BioWayColors.TextGrey,
                cursorColor = BioWayColors.PrimaryGreen,
                errorBorderColor = BioWayColors.Error,
                errorLabelColor = BioWayColors.Error,
                errorCursorColor = BioWayColors.Error,
                disabledBorderColor = BioWayColors.BackgroundGrey,
                disabledTextColor = BioWayColors.TextGrey
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = BioWayColors.Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Campo de texto para contraseñas con botón de mostrar/ocultar
 */
@Composable
fun BioWayPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    BioWayTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        leadingIcon = Icons.Default.Lock,
        trailingIcon = if (passwordVisible) {
            Icons.Default.Visibility
        } else {
            Icons.Default.VisibilityOff
        },
        onTrailingIconClick = { passwordVisible = !passwordVisible },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        modifier = modifier
    )
}
