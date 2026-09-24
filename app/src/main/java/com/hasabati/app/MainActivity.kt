package com.hasabati.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import com.hasabati.app.ui.navigation.HasabatiNavGraph
import com.hasabati.app.ui.settings.PinLockScreen
import com.hasabati.app.ui.settings.PinPrefs
import com.hasabati.app.ui.theme.HasabatiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // فرض اتجاه RTL بالكامل للتطبيق (القسم 28)
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HasabatiTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppRoot()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val pinEnabled by PinPrefs.isEnabled(context).collectAsState(initial = false)
    val savedPin by PinPrefs.pinCode(context).collectAsState(initial = "")
    var unlocked by remember { mutableStateOf(false) }

    if (pinEnabled && !unlocked) {
        PinLockScreen(correctPin = savedPin, onUnlocked = { unlocked = true })
    } else {
        HasabatiNavGraph()
    }
}
