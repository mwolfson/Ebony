package com.androiddev.social.auth.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.OnNewIntentProvider
import androidx.core.util.Consumer

@Composable
fun SignInWebView(
    url: String,
    onWebError: (message: String) -> Unit,
    onCancel: () -> Unit,
    shouldCancelLoadingUrl: (url: String) -> Boolean,
    modifier: Modifier,
) {
    val webIntent = webBrowserIntent(
        url = url,
        primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
        secondaryColor =  androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = .5f)
    )

    val handler = Handler(Looper.getMainLooper())

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                // post to a handler to wait for a redirect intent as that should supersede this
                handler.post { onCancel() }
            }
        }

    OnNewIntent { intent ->
        val redirectUrl = intent?.data?.toString()
        if (redirectUrl != null) {
            if (shouldCancelLoadingUrl(redirectUrl)) {
                handler.removeCallbacksAndMessages(null)
            } else {
                onCancel()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            Modifier
                .align(Alignment.Center)
                .size(84.dp)
        )
    }
    DisposableEffect(url) {
        launcher.launch(webIntent)
        onDispose {
            handler.removeCallbacksAndMessages(null)
        }
    }
}

private fun webBrowserIntent(url: String, primaryColor: Color, secondaryColor: Color): Intent {
    val intent = CustomTabsIntent.Builder()
        .setToolbarColor(primaryColor.toArgb())
        .setSecondaryToolbarColor(secondaryColor.toArgb())
        .build()
        .intent
    intent.data = Uri.parse(url)
    return intent
}

@Composable
private fun OnNewIntent(callback: (Intent?) -> Unit) {
    val context = LocalContext.current
    val newIntentProvider = context as OnNewIntentProvider

    val listener = remember(newIntentProvider) { Consumer<Intent?> { callback(it) } }

    DisposableEffect(listener) {
        newIntentProvider.addOnNewIntentListener(listener)
        onDispose {
            newIntentProvider.removeOnNewIntentListener(listener)
        }
    }
}
