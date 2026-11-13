package org.dsiders.nextstep.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.data.UserRole
import org.dsiders.nextstep.ui.components.Avatar
import org.dsiders.nextstep.ui.components.BackRow
import org.dsiders.nextstep.ui.components.ChoiceChip
import org.dsiders.nextstep.ui.components.Dot
import org.dsiders.nextstep.ui.components.EmptyState
import org.dsiders.nextstep.ui.components.FieldInput
import org.dsiders.nextstep.ui.components.KeyValueRow
import org.dsiders.nextstep.ui.components.NsCard
import org.dsiders.nextstep.ui.components.Pill
import org.dsiders.nextstep.ui.components.PrimaryButton
import org.dsiders.nextstep.ui.components.RowDivider
import org.dsiders.nextstep.ui.components.SecondaryButton
import org.dsiders.nextstep.ui.components.SectionLabel
import org.dsiders.nextstep.ui.components.timeAgo
import org.dsiders.nextstep.ui.student.ChoiceDialog
import org.dsiders.nextstep.ui.student.SettingRow
import org.dsiders.nextstep.ui.theme.Palette

// ---------------------------------------------------------------------------
// Lists
// ---------------------------------------------------------------------------

@Composable
fun AdminCaseworkersScreen(container: AppContainer, onCaseworker: (Long) -> Unit, onAdd: () -> Unit) {
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = null)
    val students by remember { container.repo.observeStudents() }.collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ListHeader("Caseworkers", searching, { searching = !searching; if (!searching) query = "" })
        if (searching) FieldInput(query, { query = it }, "Search by name", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        val all = caseworkers
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when {
                all == null -> {}
                all.isEmpty() -> EmptyState("No caseworkers yet", "Add one so students have someone to turn to.")
                else -> all.filter { query.isBlank() || it.fullName.contains(query, true) }.forEach { cw ->
                    val count = students.count { it.mentorId == cw.id }
                    PersonCard(
                        cw.fullName,
                        listOf(cw.centre, "@${cw.username}").filter { it.isNotBlank() }.joinToString(" · "),
                        cw.skillList().joinToString(", ").ifBlank { "No kinds of work set" },
                        "$count student${if (count == 1) "" else "s"}",
                        dot = if (cw.available) Palette.Green else Palette.Grey,
                    ) { onCaseworker(cw.id) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.padding(16.dp)) { PrimaryButton("Add a caseworker", onAdd, icon = Icons.Filled.Add) }
    }
}

@Composable
fun AdminStudentsScreen(container: AppContainer, onStudent: (Long) -> Unit) {
    val students by remember { container.repo.observeStudents() }.collectAsState(initial = null)
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())
    val goals by remember { container.repo.observeAllGoals() }.collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ListHeader("Students", searching, { searching = !searching; if (!searching) query = "" })
        if (searching) FieldInput(query, { query = it }, "Search by name", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        val all = students
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when {
                all == null -> {}
                all.isEmpty() -> EmptyState("No students yet", "Students create their own accounts, or a caseworker adds them.")
                else -> all.filter { query.isBlank() || it.fullName.contains(query, true) }.forEach { s ->
                    val mentor = caseworkers.firstOrNull { it.id == s.mentorId }
                    PersonCard(
                        s.fullName,
                        listOf(s.college, "@${s.username}").filter { it.isNotBlank() }.joinToString(" · "),
                        goals.firstOrNull { it.studentId == s.id }?.goalText?.ifBlank { null } ?: "No goal yet",
                        mentor?.let { "Mentor: ${it.fullName}" } ?: "No mentor",
                    ) { onStudent(s.id) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ListHeader(title: String, searching: Boolean, onToggleSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        IconButton(onToggleSearch) { Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, "Search", tint = Palette.Navy) }
    }
}

@Composable
private fun PersonCard(name: String, subtitle: String, line: String, trailing: String, dot: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    Box(Modifier.padding(vertical = 5.dp)) {
        NsCard(onClick = onClick, padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name, size = 40.dp, background = Palette.Lavender)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        dot?.let { Spacer(Modifier.width(6.dp)); Dot(it, 7.dp) }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(trailing, style = MaterialTheme.typography.bodySmall, color = Palette.NavyMuted)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Palette.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Details
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCaseworkerScreen(container: AppContainer, caseworkerId: Long, onStudent: (Long) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val cw by remember(caseworkerId) { container.repo.observeUser(caseworkerId) }.collectAsState(initial = null)
    val students by remember { container.repo.observeStudents() }.collectAsState(initial = emptyList())
    var confirmRemove by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val me = cw ?: return
    val theirs = students.filter { it.mentorId == me.id }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack, title = me.fullName)
        NsCard(padding = PaddingValues(0.dp)) {
            KeyValueRow("Username", me.username)
            RowDivider(); KeyValueRow("Phone", me.phone.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("Centre", me.centre.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("Languages", me.languages)
            RowDivider(); KeyValueRow("Working hours", me.workingHours)
            RowDivider(); KeyValueRow("Available to help", if (me.available) "Yes" else "Away", valueColor = if (me.available) Palette.Green else Palette.Grey)
            RowDivider(); KeyValueRow("Joined", timeAgo(me.createdAt))
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Kinds of work they mentor")
        Spacer(Modifier.height(8.dp))
        TagRow(me.skillList(), "None set yet, so no students are matched to them.")
        Spacer(Modifier.height(16.dp))
        SectionLabel("What they help with")
        Spacer(Modifier.height(8.dp))
        TagRow(me.topics.split(',').map { it.trim() }.filter { it.isNotEmpty() }, "None set yet.")
        Spacer(Modifier.height(16.dp))
        SectionLabel("Their students (${theirs.size})")
        Spacer(Modifier.height(8.dp))
        if (theirs.isEmpty()) Text("No students are matched to them yet.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
        else NsCard(padding = PaddingValues(0.dp)) {
            theirs.forEachIndexed { i, s ->
                if (i > 0) RowDivider()
                KeyValueRow(s.fullName, "@${s.username}", onClick = { onStudent(s.id) }, trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight)
            }
        }
        Spacer(Modifier.height(24.dp))
        SecondaryButton("Remove caseworker", { confirmRemove = true }, icon = Icons.Outlined.Delete, enabled = !removing)
        Spacer(Modifier.height(20.dp))
    }

    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text("Remove ${me.fullName}?") },
        text = {
            Text(
                "They will no longer be able to log in. Their ${theirs.size} student${if (theirs.size == 1) "" else "s"} will be matched to " +
                    "another mentor, and their open help requests go back to the shared queue. Notes and messages they wrote are kept.",
            )
        },
        confirmButton = {
            TextButton({
                confirmRemove = false; removing = true
                scope.launch { container.repo.deleteCaseworker(me.id); onBack() }
            }) { Text("Remove", color = Palette.AmberText) }
        },
        dismissButton = { TextButton({ confirmRemove = false }) { Text("Cancel") } },
    )
}

@Composable
fun AdminStudentScreen(container: AppContainer, studentId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val student by remember(studentId) { container.repo.observeUser(studentId) }.collectAsState(initial = null)
    val goal by remember(studentId) { container.repo.observeGoal(studentId) }.collectAsState(initial = null)
    val facts by remember(studentId) { container.repo.observeFacts(studentId) }.collectAsState(initial = emptyList())
    val requests by remember(studentId) { container.repo.observeRequestsFor(studentId) }.collectAsState(initial = emptyList())
    val notes by remember(studentId) { container.repo.observeNotes(studentId) }.collectAsState(initial = emptyList())
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())
    var pickMentor by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val me = student ?: return
    val role = roles.firstOrNull { it.id == goal?.targetRoleId }
    val addedBy = caseworkers.firstOrNull { it.id == me.addedByCaseworkerId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack, title = me.fullName)
        NsCard(padding = PaddingValues(0.dp)) {
            KeyValueRow("Username", me.username)
            RowDivider(); KeyValueRow("Phone", me.phone.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("College", me.college.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("Course", me.course.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("Year", me.year.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("District", me.district.ifBlank { "Not given" })
            RowDivider(); KeyValueRow("Language", me.language)
            RowDivider(); KeyValueRow("Joined", timeAgo(me.createdAt))
            RowDivider(); KeyValueRow("Account made by", addedBy?.fullName ?: if (me.addedByCaseworkerId != null) "A removed caseworker" else "Themselves")
        }
        Spacer(Modifier.height(16.dp))
        NsCard(padding = PaddingValues(0.dp)) {
            SettingRow(Icons.Outlined.PersonPin, "Mentor", caseworkers.firstOrNull { it.id == me.mentorId }?.fullName ?: "None") { pickMentor = true }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Goal")
        Spacer(Modifier.height(8.dp))
        NsCard(padding = PaddingValues(0.dp)) {
            KeyValueRow("In their words", goal?.goalText?.ifBlank { null } ?: "No goal yet")
            RowDivider(); KeyValueRow("Target role", role?.title ?: "Not decided")
            RowDivider(); KeyValueRow("Kind of work", goal?.field?.ifBlank { null } ?: "Not decided")
            RowDivider(); KeyValueRow("Questions finished", if (goal?.intakeDone == true) "Yes" else "No")
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("What the app knows about them")
        Spacer(Modifier.height(8.dp))
        if (facts.isEmpty()) Text("Nothing yet.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
        else NsCard(padding = PaddingValues(0.dp)) {
            facts.forEachIndexed { i, f -> if (i > 0) RowDivider(); KeyValueRow(f.label, f.value) }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Activity")
        Spacer(Modifier.height(8.dp))
        NsCard(padding = PaddingValues(0.dp)) {
            KeyValueRow("Help requests", "${requests.size}")
            RowDivider(); KeyValueRow("Caseworker notes", "${notes.size}")
        }
        Spacer(Modifier.height(24.dp))
        SecondaryButton("Remove student", { confirmRemove = true }, icon = Icons.Outlined.Delete, enabled = !removing)
        Spacer(Modifier.height(20.dp))
    }

    if (pickMentor) ChoiceDialog(
        "Mentor",
        caseworkers.map { "${it.fullName}${if (it.available) "" else " (away)"}" to it.id },
        me.mentorId, { pickMentor = false },
    ) { id -> scope.launch { container.repo.updateUser(me.copy(mentorId = id)) }; pickMentor = false }

    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text("Remove ${me.fullName}?") },
        text = { Text("This deletes their account and everything held about them: goal, answers, steps, help requests, messages and notes. It can't be undone.") },
        confirmButton = {
            TextButton({
                confirmRemove = false; removing = true
                scope.launch {
                    container.repo.deleteStudent(me.id)
                    container.engine.invalidate(me.id)
                    onBack()
                }
            }) { Text("Remove", color = Palette.AmberText) }
        },
        dismissButton = { TextButton({ confirmRemove = false }) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(tags: List<String>, empty: String) {
    if (tags.isEmpty()) Text(empty, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
    else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { Pill(it) }
    }
}

// ---------------------------------------------------------------------------
// Add a caseworker
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCaseworkerScreen(container: AppContainer, onCreated: (Long) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val roles by remember { container.repo.observeRoles() }.collectAsState(initial = emptyList())
    val fields = roles.filter { it.reviewed }.map { it.field.trim() }.filter { it.isNotEmpty() }.distinct()
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var usernameEdited by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("barabari" + (1000..9999).random()) }
    var phone by rememberSaveable { mutableStateOf("") }
    var centre by rememberSaveable { mutableStateOf("") }
    var languages by rememberSaveable { mutableStateOf("English") }
    var skills by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        BackRow(onBack, title = "Add a caseworker")
        Text("They can change their own details after they log in.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldInput(name, {
                name = it
                if (!usernameEdited) username = it.trim().lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
            }, "Full name")
            FieldInput(username, { username = it.trim(); usernameEdited = true; error = null }, "Username")
            FieldInput(password, { password = it }, "Starting password", supportingText = "Give this to the caseworker. At least 8 characters.")
            FieldInput(phone, { phone = it }, "Phone", keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
            FieldInput(centre, { centre = it }, "Centre, e.g. Kukatpally centre")
            FieldInput(languages, { languages = it }, "Languages", supportingText = "Comma separated, e.g. Telugu, Hindi, English")
        }
        if (fields.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Kinds of work they can mentor", style = MaterialTheme.typography.titleSmall, color = Palette.Navy)
            Text("Students whose goal is in one of these are matched to them.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.forEach { f -> ChoiceChip(f, f in skills, { skills = if (f in skills) skills - f else skills + f }) }
            }
        }
        error?.let { Text(it, color = Palette.AmberText, modifier = Modifier.padding(vertical = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Add caseworker", {
            busy = true
            scope.launch {
                container.repo.register(UserRole.CASEWORKER, name, username, password, phone = phone, centre = centre, skills = fields.filter { it in skills })
                    .onSuccess { user ->
                        container.repo.updateUser(user.copy(languages = languages.trim().ifBlank { "English" }))
                        onCreated(user.id)
                    }
                    .onFailure { error = it.message }
                busy = false
            }
        }, enabled = name.isNotBlank() && username.isNotBlank() && !username.contains(' ') && password.length >= 8 &&
            (skills.isNotEmpty() || fields.isEmpty()), loading = busy)
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------------------
// Admin profile
// ---------------------------------------------------------------------------

@Composable
fun AdminProfileScreen(container: AppContainer, me: UserEntity) {
    val students by remember { container.repo.observeStudents() }.collectAsState(initial = emptyList())
    val caseworkers by remember { container.repo.observeCaseworkers() }.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("My profile", style = MaterialTheme.typography.titleLarge, color = Palette.Navy)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(me.fullName, size = 64.dp, background = Palette.Lavender)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(me.fullName, style = MaterialTheme.typography.headlineSmall)
                Text("Admin, Barabari Collective · @${me.username}", style = MaterialTheme.typography.bodyMedium, color = Palette.Text)
            }
        }
        Spacer(Modifier.height(18.dp))
        NsCard(padding = PaddingValues(0.dp)) {
            KeyValueRow("Caseworkers", "${caseworkers.size}")
            RowDivider(); KeyValueRow("Available now", "${caseworkers.count { it.available }}")
            RowDivider(); KeyValueRow("Students", "${students.size}")
            RowDivider(); KeyValueRow("Students without a mentor", "${students.count { s -> caseworkers.none { it.id == s.mentorId } }}")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "The admin password is set in the app's build settings, not here.",
            style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted,
        )
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Sign out", { container.session.signOut() }, icon = Icons.AutoMirrored.Filled.Logout)
    }
}
