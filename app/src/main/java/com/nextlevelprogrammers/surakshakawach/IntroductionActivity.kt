package com.nextlevelprogrammers.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nextlevelprogrammers.surakshakawach.ui.IntroductionScreen
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme

class IntroductionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SurakshaKawachTheme {
                IntroductionScreen()
            }
        }
    }
}