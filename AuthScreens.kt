package com.example.authentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.ConnectCoral
import com.example.ui.theme.ConnectIndigo
import com.google.firebase.auth.FirebaseUser

private enum class AuthRoute {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    PHONE,
}

@Composable
fun AuthScreen(
    onAuthSuccess: (FirebaseUser) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentRoute by remember { mutableStateOf(AuthRoute.LOGIN) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val cancelledMsg = stringResource(R.string.auth_google_cancelled)

    val triggerGoogleSignIn = {
        isGoogleLoading = true
        statusMessage = null
        isError = false
        GoogleAuthHelper.launchInteractiveGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = { user ->
                isGoogleLoading = false
                onAuthSuccess(user)
            },
            onAuthCancelled = {
                isGoogleLoading = false
                isError = false
                statusMessage = cancelledMsg
            },
            onAuthError = { err ->
                isGoogleLoading = false
                isError = true
                statusMessage = err
            },
        )
    }

    if (currentRoute != AuthRoute.LOGIN) {
        BackHandler {
            statusMessage = null
            currentRoute = AuthRoute.LOGIN
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (currentRoute) {
                AuthRoute.LOGIN -> LoginContent(
                    isGoogleLoading = isGoogleLoading,
                    statusMessage = statusMessage,
                    isError = isError,
                    onGoogleSignIn = triggerGoogleSignIn,
                    onSubmitEmailNotice = { msg, err ->
                        statusMessage = msg
                        isError = err
                    },
                    onNavigateRegister = {
                        statusMessage = null
                        currentRoute = AuthRoute.REGISTER
                    },
                    onNavigateForgot = {
                        statusMessage = null
                        currentRoute = AuthRoute.FORGOT_PASSWORD
                    },
                    onNavigatePhone = {
                        statusMessage = null
                        currentRoute = AuthRoute.PHONE
                    },
                )

                AuthRoute.REGISTER -> RegisterContent(
                    isGoogleLoading = isGoogleLoading,
                    statusMessage = statusMessage,
                    isError = isError,
                    onGoogleSignIn = triggerGoogleSignIn,
                    onSubmitRegisterNotice = { msg, err ->
                        statusMessage = msg
                        isError = err
                    },
                    onBackToLogin = {
                        statusMessage = null
                        currentRoute = AuthRoute.LOGIN
                    },
                )

                AuthRoute.FORGOT_PASSWORD -> ForgotPasswordContent(
                    statusMessage = statusMessage,
                    isError = isError,
                    onSubmitResetNotice = { msg, err ->
                        statusMessage = msg
                        isError = err
                    },
                    onBackToLogin = {
                        statusMessage = null
                        currentRoute = AuthRoute.LOGIN
                    },
                )

                AuthRoute.PHONE -> PhoneAuthContent(
                    isGoogleLoading = isGoogleLoading,
                    statusMessage = statusMessage,
                    isError = isError,
                    onGoogleSignIn = triggerGoogleSignIn,
                    onSubmitPhoneNotice = { msg, err ->
                        statusMessage = msg
                        isError = err
                    },
                    onBackToLogin = {
                        statusMessage = null
                        currentRoute = AuthRoute.LOGIN
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_connect_hero),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color(0xFF090B14).copy(alpha = 0.82f),
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
private fun GooglePrimaryButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ConnectIndigo,
            contentColor = Color.White,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.auth_loading))
        } else {
            Text(
                text = stringResource(R.string.auth_continue_google),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatusNoticeBanner(
    message: String?,
    isError: Boolean,
) {
    if (message.isNullOrBlank()) return
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun LoginContent(
    isGoogleLoading: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onGoogleSignIn: () -> Unit,
    onSubmitEmailNotice: (String, Boolean) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    onNavigatePhone: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val emptyFieldsError = stringResource(R.string.auth_error_empty_fields)
    val providerNotice = stringResource(R.string.auth_provider_notice_email)

    AuthHeroBanner()
    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.auth_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.auth_welcome_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(20.dp))

    GooglePrimaryButton(isLoading = isGoogleLoading, onClick = onGoogleSignIn)

    Spacer(modifier = Modifier.height(16.dp))
    StatusNoticeBanner(message = statusMessage, isError = isError)
    if (!statusMessage.isNullOrBlank()) Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_or_divider),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(14.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email_label)) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password_label)) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = onNavigateForgot,
                    modifier = Modifier.testTag("forgot_password_button"),
                ) {
                    Text(stringResource(R.string.auth_forgot_password))
                }
            }
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        onSubmitEmailNotice(emptyFieldsError, true)
                    } else {
                        onSubmitEmailNotice(providerNotice, false)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("email_login_button"),
            ) {
                Text(stringResource(R.string.auth_login_button))
            }

            OutlinedButton(
                onClick = onNavigatePhone,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("phone_login_button"),
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auth_phone_login))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.auth_no_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onNavigateRegister,
            modifier = Modifier.testTag("go_to_register_button"),
        ) {
            Text(
                text = stringResource(R.string.auth_sign_up_link),
                fontWeight = FontWeight.Bold,
                color = ConnectCoral,
            )
        }
    }
}

@Composable
private fun RegisterContent(
    isGoogleLoading: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onGoogleSignIn: () -> Unit,
    onSubmitRegisterNotice: (String, Boolean) -> Unit,
    onBackToLogin: () -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val emptyErr = stringResource(R.string.auth_error_empty_fields)
    val mismatchErr = stringResource(R.string.auth_error_password_mismatch)
    val providerNotice = stringResource(R.string.auth_provider_notice_email)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackToLogin, modifier = Modifier.testTag("register_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.auth_register_button),
            style = MaterialTheme.typography.headlineMedium,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    GooglePrimaryButton(isLoading = isGoogleLoading, onClick = onGoogleSignIn)
    Spacer(modifier = Modifier.height(12.dp))
    StatusNoticeBanner(message = statusMessage, isError = isError)
    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.auth_full_name_label)) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.auth_username_label)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_username_input"),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email_label)) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_email_input"),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password_label)) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_password_input"),
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input"),
            )
            Button(
                onClick = {
                    when {
                        fullName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank() ->
                            onSubmitRegisterNotice(emptyErr, true)
                        password != confirmPassword ->
                            onSubmitRegisterNotice(mismatchErr, true)
                        else ->
                            onSubmitRegisterNotice(providerNotice, false)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("register_submit_button"),
            ) {
                Text(stringResource(R.string.auth_register_button))
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.auth_have_account),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onBackToLogin) {
            Text(stringResource(R.string.auth_sign_in_link), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ForgotPasswordContent(
    statusMessage: String?,
    isError: Boolean,
    onSubmitResetNotice: (String, Boolean) -> Unit,
    onBackToLogin: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    val emptyErr = stringResource(R.string.auth_error_empty_fields)
    val resetNotice = stringResource(R.string.auth_provider_notice_reset)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackToLogin, modifier = Modifier.testTag("forgot_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.auth_forgot_title),
            style = MaterialTheme.typography.headlineMedium,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.auth_forgot_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    StatusNoticeBanner(message = statusMessage, isError = isError)
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.auth_email_label)) },
        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("forgot_email_input"),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = {
            if (email.isBlank()) {
                onSubmitResetNotice(emptyErr, true)
            } else {
                onSubmitResetNotice(resetNotice, false)
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("send_reset_button"),
    ) {
        Text(stringResource(R.string.auth_send_reset_link))
    }
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onBackToLogin) {
        Text(stringResource(R.string.auth_back_to_login))
    }
}

@Composable
private fun PhoneAuthContent(
    isGoogleLoading: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onGoogleSignIn: () -> Unit,
    onSubmitPhoneNotice: (String, Boolean) -> Unit,
    onBackToLogin: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpRequested by remember { mutableStateOf(false) }

    val invalidPhone = stringResource(R.string.auth_error_invalid_phone)
    val phoneNotice = stringResource(R.string.auth_provider_notice_phone)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackToLogin, modifier = Modifier.testTag("phone_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.auth_phone_title),
            style = MaterialTheme.typography.headlineMedium,
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.auth_phone_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    StatusNoticeBanner(message = statusMessage, isError = isError)
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text(stringResource(R.string.auth_phone_label)) },
        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("phone_number_input"),
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (otpRequested) {
        OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) otpCode = it },
            label = { Text(stringResource(R.string.auth_otp_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("phone_otp_input"),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    Button(
        onClick = {
            if (!phone.trim().startsWith("+") || phone.trim().length < 8) {
                onSubmitPhoneNotice(invalidPhone, true)
            } else {
                otpRequested = true
                onSubmitPhoneNotice(phoneNotice, false)
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("phone_submit_button"),
    ) {
        Text(stringResource(if (otpRequested) R.string.auth_verify_otp else R.string.auth_send_otp))
    }

    Spacer(modifier = Modifier.height(20.dp))
    GooglePrimaryButton(isLoading = isGoogleLoading, onClick = onGoogleSignIn)
}
