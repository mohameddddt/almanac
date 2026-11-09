package com.almanac.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.almanac.app.data.Priority
import com.almanac.app.data.Store
import com.almanac.app.data.Task
import com.almanac.app.ui.components.BallotBox
import com.almanac.app.ui.components.Blank
import com.almanac.app.ui.components.DatePickDialog
import com.almanac.app.ui.components.Fmt
import com.almanac.app.ui.components.InkButton
import com.almanac.app.ui.components.LineField
import com.almanac.app.ui.components.Masthead
import com.almanac.app.ui.components.Rule
import com.almanac.app.ui.components.SectionHead
import com.almanac.app.ui.components.Segmented
import com.almanac.app.ui.components.Sheet
import com.almanac.app.ui.components.TextAction
import com.almanac.app.ui.components.p
import com.almanac.app.ui.components.rememberToday
import com.almanac.app.ui.components.tap
import com.almanac.app.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Filters = listOf("Open", "Today", "Next", "Done")
private val PriorityMarks = mapOf(Priority.High to "!!!", Priority.Normal to "!!", Priority.Low to "!")

@Composable
private fun priorityColor(pr: Priority) = when (pr) {
    Priority.High -> p.signal
    Priority.Normal -> p.ink
    Priority.Low -> p.muted
}

@Composable
fun TasksScreen() {
    val data by Store.data.collectAsState()
    val today = rememberToday()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Task?>(null) }

    val open = remember(data.tasks) { data.tasks.filter { !it.done } }
    val doneToday = remember(data.tasks, today) {
        data.tasks.count {
            it.done && it.doneAt != null &&
                Instant.ofEpochMilli(it.doneAt).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(ScreenPadding),
    ) {
        Masthead("Tasks", "${open.size} open · $doneToday done today")
        Spacer(Modifier.height(18.dp))
        Composer(today)
        Spacer(Modifier.height(26.dp))
        Segmented(Filters, filter, { filter = it })
        Spacer(Modifier.height(10.dp))

        val sortKey = compareByDescending<Task> { it.priority.ordinal }.thenBy { it.createdAt }
        val row: @Composable (Task) -> Unit = { t -> key(t.id) { TaskRow(t, today) { editing = t } } }
        when (filter) {
            0 -> {
                val groups = listOf(
                    "Overdue" to open.filter { it.due != null && it.due.isBefore(today) }.sortedWith(sortKey),
                    "Today" to open.filter { it.due == today }.sortedWith(sortKey),
                    "Upcoming" to open.filter { it.due != null && it.due.isAfter(today) }.sortedBy { it.due },
                    "Someday" to open.filter { it.due == null }.sortedWith(sortKey),
                ).filter { it.second.isNotEmpty() }
                if (groups.isEmpty()) Blank("Nothing open.")
                groups.forEachIndexed { i, (name, list) ->
                    Spacer(Modifier.height(18.dp))
                    SectionHead("${'A' + i}", name, trailing = "${list.size}")
                    list.forEach { row(it) }
                }
            }
            1 -> {
                val list = data.tasks.filter { it.due != null && !it.due.isAfter(today) && (!it.done || it.due == today) }
                if (list.isEmpty()) Blank("Nothing due today.")
                list.sortedWith(compareBy<Task> { it.done }.then(sortKey)).forEach { row(it) }
            }
            2 -> {
                val list = open.filter { it.due != null && it.due.isAfter(today) }.sortedBy { it.due }
                if (list.isEmpty()) Blank("Nothing scheduled ahead.")
                list.forEach { row(it) }
            }
            else -> {
                val list = data.tasks.filter { it.done }.sortedByDescending { it.doneAt }
                if (list.isEmpty()) Blank("No finished tasks.")
                list.forEach { row(it) }
                if (list.isNotEmpty()) TextAction("Clear finished", { Store.clearDone() }, color = p.signal)
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

    Row(verticalAlignment = Alignment.Bottom) {
        LineField(text, { text = it }, "New task", modifier = Modifier.weight(1f), onDone = add)
        Spacer(Modifier.width(10.dp))
        InkButton("Add", add, enabled = text.isNotBlank(), color = p.signal)
    }
    Spacer(Modifier.height(12.dp))
    DueAndPriority(today, due, { due = it }, { picking = true }, priority, { priority = it })
    if (picking) DatePickDialog(due ?: today, { picking = false }) { due = it; picking = false }
}

@Composable
private fun DueAndPriority(
    today: LocalDate,
    due: LocalDate?,
    onDue: (LocalDate?) -> Unit,
    onPickDate: () -> Unit,
    priority: Priority,
    onPriority: (Priority) -> Unit,
) {
    val custom = due != null && due != today && due != today.plusDays(1)
    Segmented(
        listOf("Today", "Tmrw", if (custom) due!!.format(Fmt.shortDate) else "Date", "None"),
        selected = when { due == null -> 3; due == today -> 0; due == today.plusDays(1) -> 1; else -> 2 },
        onSelect = {
            when (it) {
                0 -> onDue(today)
                1 -> onDue(today.plusDays(1))
                2 -> onPickDate()
                else -> onDue(null)
            }
        },
    )
    Spacer(Modifier.height(6.dp))
    val order = listOf(Priority.Low, Priority.Normal, Priority.High)
    Segmented(
        order.map { "${PriorityMarks[it]} ${it.name}" },
        selected = order.indexOf(priority),
        onSelect = { onPriority(order[it]) },
    )
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
                Modifier.fillMaxSize().background(p.signal).padding(horizontal = 20.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) { Text("DELETE", style = Type.tag, color = p.paper) }
        },
    ) {
        Column(Modifier.fillMaxWidth().background(p.paper).tap(onClick = onEdit)) {
            Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                BallotBox(t.done, { Store.toggleTask(t.id) }, color = priorityColor(t.priority))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                        color = if (t.done) p.muted else p.ink,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    t.due?.let {
                        Text(
                            Fmt.relative(it, today).uppercase(),
                            style = Type.tag,
                            color = if (!t.done && it.isBefore(today)) p.signal else p.muted,
                        )
                    }
                    if (t.priority != Priority.Normal && !t.done) {
                        Text(PriorityMarks.getValue(t.priority), style = Type.tag, color = priorityColor(t.priority))
                    }
                }
            }
            Rule()
        }
    }
}

@Composable
private fun TaskSheet(initial: Task, onDismiss: () -> Unit) {
    val today = rememberToday()
    var title by remember { mutableStateOf(initial.title) }
    var priority by remember { mutableStateOf(initial.priority) }
    var due by remember { mutableStateOf(initial.due) }
    var picking by remember { mutableStateOf(false) }

    Sheet("Edit task", onDismiss) {
        LineField(title, { title = it }, "Task", large = true, singleLine = false)
        Spacer(Modifier.height(18.dp))
        DueAndPriority(today, due, { due = it }, { picking = true }, priority, { priority = it })
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction("Delete", { Store.deleteTask(initial.id); onDismiss() }, color = p.signal)
            Spacer(Modifier.weight(1f))
            TextAction("Cancel", onDismiss)
            InkButton("Save", {
                Store.updateTask(initial.copy(title = title.trim(), priority = priority, due = due))
                onDismiss()
            }, enabled = title.isNotBlank())
        }
    }
    if (picking) DatePickDialog(due ?: today, { picking = false }) { due = it; picking = false }
}
