  package com.nishanth.learnapplication

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/*
dependencies {
    implementation(platform(
        "androidx.compose:compose-bom:2025.12.00"
    ))

    implementation(
        "androidx.activity:activity-compose:1.12.0"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )
}
 */


private data class PaymentNotice(
    val id: String,
    val title: String,
    val detail: String
)

private val sampleNotices = listOf(
    PaymentNotice("n1", "Payment received", "₹2,500 received"),
    PaymentNotice("n2", "Transfer completed", "₹1,200 sent"),
    PaymentNotice("n3", "Bill payment", "₹850 paid"),
    PaymentNotice("n4", "Reward credited", "₹100 received")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotificationArchiveScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationArchiveScreen() {
    val notices = remember {
        mutableStateListOf<PaymentNotice>().apply {
            addAll(sampleNotices)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingUndo by remember { mutableStateOf(false) }

    fun archive(notice: PaymentNotice) {
        if (pendingUndo) return
        val index = notices.indexOfFirst { it.id == notice.id }
        if (index < 0) return

        notices.removeAt(index)
        pendingUndo = true

        scope.launch {
            try {
                val result = snackbarHostState.showSnackbar(
                    message = "${notice.title} archived",
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                if (
                    result == SnackbarResult.ActionPerformed &&
                    notices.none { it.id == notice.id }
                ) {
                    notices.add(index.coerceAtMost(notices.size), notice)
                }
            } finally {
                pendingUndo = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Payment notifications") })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            Text(
                "Swipe left to archive, or use the Archive button. " +
                        "Undo is available in the snackbar.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notices, key = { it.id }) { notice ->
                    SwipeNoticeRow(
                        notice = notice,
                        gesturesEnabled = !pendingUndo,
                        onArchive = { archive(notice) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeNoticeRow(
    notice: PaymentNotice,
    gesturesEnabled: Boolean,
    onArchive: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { newValue ->
            newValue == SwipeToDismissBoxValue.EndToStart &&
                    gesturesEnabled
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (
            dismissState.currentValue ==
            SwipeToDismissBoxValue.EndToStart
        ) {
            onArchive()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = gesturesEnabled,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "Archive",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        ListItem(
            headlineContent = { Text(notice.title) },
            supportingContent = { Text(notice.detail) },
            trailingContent = {
                TextButton(
                    onClick = onArchive,
                    enabled = gesturesEnabled
                ) {
                    Text("Archive")
                }
            }
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun LightPreview() {
    MaterialTheme { NotificationArchiveScreen() }
}

@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DarkPreview() {
    MaterialTheme { NotificationArchiveScreen() }
}
