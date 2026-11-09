package com.almanac.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.almanac.app.alarm.AlarmScheduler
import com.almanac.app.data.Store
import com.almanac.app.ui.components.p
import com.almanac.app.ui.screens.CalendarScreen
import com.almanac.app.ui.screens.ClockScreen
import com.almanac.app.ui.screens.HabitsScreen
import com.almanac.app.ui.screens.TasksScreen
import com.almanac.app.ui.screens.TodayScreen
import com.almanac.app.ui.theme.AlmanacTheme
import com.almanac.app.ui.theme.DarkPalette
import com.almanac.app.ui.theme.LightPalette

enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Outlined.WbTwilight),
    Calendar("Calendar", Icons.Outlined.CalendarMonth),
    Clock("Clock", Icons.Outlined.Schedule),
    Tasks("Tasks", Icons.Outlined.TaskAlt),
    Habits("Rituals", Icons.Outlined.Spa),
}

class MainActivity : ComponentActivity() {

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        AlarmScheduler.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            val data by Store.data.collectAsState()
            AlmanacTheme(mode = data.themeMode) {
                val dark = p.isDark
                androidx.compose.runtime.LaunchedEffect(dark) {
                    val bar = if (dark) DarkPalette.paper.toArgb() else LightPalette.paper.toArgb()
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar),
                    )
                }
                AlmanacApp()
            }
        }
    }
}

@Composable
private fun AlmanacApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(p.paper)) {
        AnimatedContent(
            targetState = Tab.entries[tab],
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 40 }) togetherWith fadeOut(tween(160))
            },
            label = "tab",
        ) { t ->
            when (t) {
                Tab.Today -> TodayScreen(onOpen = { tab = it.ordinal })
                Tab.Calendar -> CalendarScreen()
                Tab.Clock -> ClockScreen()
                Tab.Tasks -> TasksScreen()
                Tab.Habits -> HabitsScreen()
            }
        }
        // paper fades in under the bar so content sinks away rather than colliding with it
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(96.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, p.paper.copy(alpha = 0.92f)))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(p.paper.copy(alpha = 0.92f)),
        )
        NavBar(
            selected = Tab.entries[tab],
            onSelect = { tab = it.ordinal },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
        )
    }
}

/** A floating ink capsule; the selected tab unfurls into a paper pill with its name. */
@Composable
private fun NavBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .shadow(18.dp, CircleShape, ambientColor = p.ink, spotColor = p.ink)
            .clip(CircleShape)
            .background(if (p.isDark) p.card else p.ink)
            .border(1.dp, p.paper.copy(alpha = 0.14f), CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            val active = t == selected
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) (if (p.isDark) p.ink else p.paper) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(t) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    t.icon, t.label,
                    tint = if (active) p.terracotta else (if (p.isDark) p.muted else p.paper.copy(alpha = 0.62f)),
                    modifier = Modifier.size(21.dp),
                )
                AnimatedVisibility(active, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                    Row {
                        Spacer(Modifier.width(8.dp))
                        Text(t.label, style = MaterialTheme.typography.labelLarge, color = if (p.isDark) p.paper else p.ink)
                    }
                }
            }
        }
    }
}
