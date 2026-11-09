package com.almanac.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almanac.app.alarm.AlarmScheduler
import com.almanac.app.data.Store
import com.almanac.app.ui.components.Rule
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.tap
import com.almanac.app.ui.screens.CalendarScreen
import com.almanac.app.ui.screens.ClockScreen
import com.almanac.app.ui.screens.HabitsScreen
import com.almanac.app.ui.screens.TasksScreen
import com.almanac.app.ui.screens.TodayScreen
import com.almanac.app.ui.theme.AlmanacTheme
import com.almanac.app.ui.theme.Type

enum class Tab(val label: String) {
    Today("Today"),
    Calendar("Calendar"),
    Clock("Clock"),
    Tasks("Tasks"),
    Habits("Rituals"),
}

class MainActivity : ComponentActivity() {

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        AlarmScheduler.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 && savedInstanceState == null) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by Store.data.collectAsState()
            AlmanacTheme(mode = themeMode.themeMode) {
                val palette = p
                LaunchedEffect(palette.isDark) {
                    val bar = palette.paper.toArgb()
                    val style = if (palette.isDark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, bar)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                AlmanacApp()
            }
        }
    }
}

@Composable
private fun AlmanacApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(p.paper)) {
        Crossfade(
            targetState = Tab.entries[tab],
            animationSpec = tween(180),
            label = "tab",
            modifier = Modifier.weight(1f),
        ) { t ->
            when (t) {
                Tab.Today -> TodayScreen(onOpen = { tab = it.ordinal })
                Tab.Calendar -> CalendarScreen()
                Tab.Clock -> ClockScreen()
                Tab.Tasks -> TasksScreen()
                Tab.Habits -> HabitsScreen()
            }
        }
        IndexBar(Tab.entries[tab], onSelect = { tab = it.ordinal })
    }
}

/**
 * A printed index along the foot of the page: numbered entries, the current one
 * marked with a signal bar along the top rule.
 */
@Composable
private fun IndexBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(p.paper).navigationBarsPadding()) {
        Rule(color = p.ink)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Tab.entries.forEach { t ->
                val on = t == selected
                Column(Modifier.weight(1f).tap { onSelect(t) }) {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) p.signal else Color.Transparent))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "%02d".format(t.ordinal + 1),
                        style = Type.tag,
                        color = if (on) p.signal else p.muted,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    Text(
                        t.label.uppercase(),
                        style = Type.tag.copy(fontSize = 12.sp),
                        color = if (on) p.ink else p.muted,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp, end = 2.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }
}
