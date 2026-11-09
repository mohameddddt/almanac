package com.almanac.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.almanac.app.data.Priority
import com.almanac.app.data.Store
import com.almanac.app.data.Task
import com.almanac.app.ui.components.Chip
import com.almanac.app.ui.components.DatePickDialog
import com.almanac.app.ui.components.EmptyNote
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.GhostButton
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.PaperCard
import com.almanac.app.ui.components.Ring
import com.almanac.app.ui.components.RoundIcon
import com.almanac.app.ui.components.ScreenTitle
import com.almanac.app.ui.components.SectionLabel
import com.almanac.app.ui.components.p
import java.time.LocalDate
import java.time.ZoneId

private enum class Filter(val label: String) { Open("Open"), Today("Today"), Upcoming("Upcoming"), Done("Done") }

@Composable
private fun priorityColor(pr: Priority) = when (pr) {
    Priority.High -> p.terracotta
    Priority.Normal -> p.brass
    Priority.Low -> p.sage
}

@Composable
fun TasksScreen() {
    val data by Store.data.collectAsState()
    val today = LocalDate.now()
    var filter by rememberSaveable { mutableStateOf(Filter.Open) }
    var editing by remember { mutableStateOf<Task?>(null) }

    val open = data.tasks.filter { !it.done }
    val doneToday = data.tasks.count {
        it.done && it.doneAt != null &&
            java.time.Instant.ofEpochMilli(it.doneAt).atZone(ZoneId.systemDefault()).toLocalDate() == today
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        ScreenTitle("${open.size} open · $doneToday done today", "Tasks") {
            val dueToday = data.tasks.filter { it.due == today }
            if (dueToday.isNotEmpty()) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    val prog by animateFloatAsState(dueToday.count { it.done } / dueToday.size.toFloat(), label = "ring")
                    Ring(prog, p.sage, Modifier.size(46.dp), stroke = 4.dp)
                    Text("${dueToday.count { it.done }}/${dueToday.size}", style = MaterialTheme.typography.labelSmall, color = p.ink)
                }
            }
        }

        Composer(today)
        Spacer(Modifier.height(22.dp))

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Filter.entries.forEach { Chip(it.label, filter == it, { filter = it }) }
        }
        Spacer(Modifier.height(16.dp))

        val sortKey = compareByDescending<Task> { it.priority.ordinal }.thenBy { it.createdAt }
        when (filter) {
            Filter.Open -> {
                val groups = listOf(
                    "Overdue" to open.filter { it.due != null && it.due.isBefore(today) },
                    "Today" to open.filter { it.due == today },
                    "Upcoming" to open.filter { it.due != null && it.due.isAfter(today) }.sortedBy { it.due },
                    "Someday" to open.filter { it.due == null },
                ).filter { it.second.isNotEmpty() }
                if (groups.isEmpty()) EmptyNote("All clear. Enjoy the quiet.")
                groups.forEach { (name, list) ->
                    Spacer(Modifier.height(12.dp))
                    SectionLabel(name, trailing = { Text("${list.size}", style = MaterialTheme.typography.labelMedium) })
                    Spacer(Modifier.height(4.dp))
                    val sorted = if (name == "Upcoming") list else list.sortedWith(sortKey)
                    sorted.forEach { t -> key(t.id) { TaskRow(t, today) { editing = t } } }
                }
            }
            Filter.Today -> {
                val list = data.tasks.filter { it.due != null && !it.due.isAfter(today) && (!it.done || it.due == today) }
                if (list.isEmpty()) EmptyNote("Nothing due today.")
                list.sortedWith(compareBy<Task> { it.done }.then(sortKey)).forEach { t -> key(t.id) { TaskRow(t, today) { editing = t } } }
            }
            Filter.Upcoming -> {
                val list = open.filter { it.due != null && it.due.isAfter(today) }.sortedBy { it.due }
                if (list.isEmpty()) EmptyNote("Nothing on the horizon.")
                list.forEach { t -> key(t.id) { TaskRow(t, today) { editing = t } } }
            }
            Filter.Done -> {
                val list = data.tasks.filter { it.done }.sortedByDescending { it.doneAt }
                if (list.isEmpty()) EmptyNote("Finished tasks will gather here.")
                list.forEach { t -> key(t.id) { TaskRow(t, today) { editing = t } } }
                if (list.isNotEmpty()) GhostButton("Clear all finished", { Store.clearDone() }, color = p.terracotta)
            }
        }
    }

    editing?.let { TaskSheet(it) { editing = null } }
}

@Composable
private fun Composer(today: LocalDate) {
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.Normal) }
    var due by remember { mutableStateOf<LocalDate?>(today) }
    var picking by remember { mutableStateOf(false) }
    val add = {
        if (text.isNotBlank()) {
            Store.addTask(text, priority, due)
            text = ""
        }
    }

    PaperCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineField(text, { text = it }, "Add a task…", modifier = Modifier.weight(1f), onDone = add)
            Spacer(Modifier.width(12.dp))
            RoundIcon(
                Icons.Outlined.ArrowUpward, "Add", add,
                container = if (text.isBlank()) p.line else p.terracotta,
                tint = if (text.isBlank()) p.muted else p.onAccent, size = 40.dp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Priority.entries.reversed().forEach { pr ->
                val c = priorityColor(pr)
                val on = priority == pr
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, if (on) c else Color.Transparent, CircleShape)
                        .clickable { priority = pr }
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(c.copy(alpha = if (on) 1f else 0.45f)),
                )
            }
            Text(priority.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 2.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip("Today", due == today, { due = today })
            Chip("Tomorrow", due == today.plusDays(1), { due = today.plusDays(1) })
            val custom = due != null && due != today && due != today.plusDays(1)
            Chip(if (custom) due!!.format(Fmt.shortDay) else "Pick date", custom, { picking = true })
            Chip("Someday", due == null, { due = null })
        }
    }
    if (picking) DatePickDialog(due ?: today, { picking = false }) { due = it; picking = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(t: Task, today: LocalDate, onEdit: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) { Store.deleteTask(t.id); true } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(p.terracotta.copy(alpha = 0.9f))
                    .padding(horizontal = 22.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.DeleteOutline, "Delete", tint = p.onAccent)
            }
        },
    ) {
        val c = priorityColor(t.priority)
        val fill by animateColorAsState(if (t.done) c else Color.Transparent, label = "check")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(p.paper)
                .clickable(onClick = onEdit)
                .padding(vertical = 13.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(1.8.dp, c, CircleShape)
                    .clickable { Store.toggleTask(t.id) },
                contentAlignment = Alignment.Center,
            ) {
                if (t.done) Icon(Icons.Outlined.Check, "Done", tint = p.onAccent, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).alpha(if (t.done) 0.55f else 1f)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                t.due?.let {
                    Text(
                        Fmt.relative(it, today),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!t.done && it.isBefore(today)) p.terracotta else p.muted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskSheet(initial: Task, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    var title by remember { mutableStateOf(initial.title) }
    var priority by remember { mutableStateOf(initial.priority) }
    var due by remember { mutableStateOf(initial.due) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp)) {
            Text("EDIT TASK", style = MaterialTheme.typography.labelMedium, color = p.terracotta)
            LineField(title, { title = it }, "Task", large = true, singleLine = false)
            Spacer(Modifier.height(18.dp))
            Text("PRIORITY", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.reversed().forEach { pr ->
                    Chip(pr.name, priority == pr, { priority = pr }, accent = priorityColor(pr))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("DUE", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Today", due == today, { due = today })
                Chip("Tomorrow", due == today.plusDays(1), { due = today.plusDays(1) })
                val custom = due != null && due != today && due != today.plusDays(1)
                Chip(if (custom) due!!.format(Fmt.shortDay) else "Pick date", custom, { picking = true })
                Chip("Someday", due == null, { due = null })
            }
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("Delete", { Store.deleteTask(initial.id); onDismiss() }, color = p.terracotta)
                Spacer(Modifier.weight(1f))
                GhostButton("Cancel", onDismiss)
                InkButton("Save", {
                    Store.updateTask(initial.copy(title = title.trim(), priority = priority, due = due))
                    onDismiss()
                }, enabled = title.isNotBlank())
            }
        }
    }
    if (picking) DatePickDialog(due ?: today, { picking = false }) { due = it; picking = false }
}
