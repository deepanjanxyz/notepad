package com.deepanjanxyz.notepad

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.deepanjanxyz.notepad.core.designsystem.EliteMemoTheme
import com.deepanjanxyz.notepad.core.model.ThemeMode
import com.deepanjanxyz.notepad.navigation.AppNavHost

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as EliteMemoApplication
        val preferenceRepository = app.appContainer.preferenceRepository

        setContent {
            val themeMode by preferenceRepository.observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            EliteMemoTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        appContainer = app.appContainer
                    )
                }
            }
        }
    }
}
