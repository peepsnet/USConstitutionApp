package com.example.usconstitution

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The documents are parsed off the main thread, once per process, so the app opens
            // straight away (and does not reload when the screen rotates or changes display).
            val library by produceState(initialValue = cachedLibrary) {
                if (value == null) {
                    value = withContext(Dispatchers.Default) { loadConstitution(applicationContext) }
                        .also { cachedLibrary = it }
                }
            }
            var appearance by remember { mutableStateOf(loadAppearance(this)) }
            val dark = isDarkTheme(appearance.mode)
            val colors = remember(appearance, dark) { colorSchemeFor(this@MainActivity, appearance, dark) }
            // Keep the status and navigation bar icons readable in the chosen light or dark mode.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                this@MainActivity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            val base = LocalDensity.current
            val config = LocalConfiguration.current
            // Each display remembers its own zoom, so a size that suits a 4K screen is not applied
            // to a phone screen (or the other way round). A display with no saved choice starts at
            // a size picked from its screen size.
            val displayKey = "${config.densityDpi}_${config.smallestScreenWidthDp}"
            var chosen by remember(displayKey) {
                mutableFloatStateOf(savedZoom(this@MainActivity, displayKey, config.smallestScreenWidthDp))
            }
            // "Zoom" scales the whole interface (text and spacing together), on top of the
            // system font size, so the app stays readable on very large or very dense screens.
            // It is capped so the screen is never narrower than about 200dp after zooming,
            // which keeps the controls reachable on a phone however high it was set.
            val maxZoom = (config.screenWidthDp / MIN_WIDTH_DP).coerceAtLeast(1f)
            val zoom = chosen.coerceIn(ZOOM_STEPS.first(), maxOf(ZOOM_STEPS.first(), maxZoom))
            CompositionLocalProvider(LocalDensity provides Density(base.density * zoom, base.fontScale)) {
                androidx.compose.material3.MaterialTheme(colorScheme = colors) {
                    val groups = library
                    if (groups == null) {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    } else {
                        App(
                            groups = groups,
                            zoom = zoom,
                            onZoom = { direction ->
                                chosen = stepZoom(zoom, direction, maxZoom)
                                saveZoom(this@MainActivity, displayKey, chosen)
                            },
                            appearance = appearance,
                            onAppearance = {
                                appearance = it
                                saveAppearance(this@MainActivity, it)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The parsed documents, kept for the life of the process. */
@Volatile
private var cachedLibrary: List<Group>? = null

private const val PREFS = "reader"
private val ZOOM_STEPS = listOf(
    0.75f, 0.9f, 1f, 1.1f, 1.25f, 1.4f, 1.6f, 1.8f, 2f, 2.25f, 2.5f, 3f, 3.5f, 4f, 5f, 6f,
)

/** Starting zoom for a display that has no saved choice: bigger for wider screens. */
private fun autoZoom(smallestWidthDp: Int): Float {
    val wanted = (smallestWidthDp / 480f).coerceIn(1f, 4f)
    return ZOOM_STEPS.last { it <= wanted + 0.001f }
}

private fun savedZoom(context: Context, displayKey: String, smallestWidthDp: Int): Float =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getFloat("zoom_$displayKey", autoZoom(smallestWidthDp))
        .coerceIn(ZOOM_STEPS.first(), ZOOM_STEPS.last())

private fun saveZoom(context: Context, displayKey: String, zoom: Float) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("zoom_$displayKey", zoom).apply()
}

private const val MIN_WIDTH_DP = 200f

/** The next larger (direction > 0) or smaller (direction < 0) zoom step; 0 resets to 100%. */
private fun stepZoom(current: Float, direction: Int, maxZoom: Float): Float {
    if (direction == 0) return 1f
    val steps = ZOOM_STEPS.filter { it <= maxZoom + 0.001f }.ifEmpty { listOf(ZOOM_STEPS.first()) }
    val at = steps.indexOfFirst { it >= current - 0.001f }.let { if (it < 0) steps.lastIndex else it }
    val target = if (direction > 0) at + 1 else at - 1
    return steps[target.coerceIn(0, steps.lastIndex)]
}

private const val KEY_BOOKMARKS = "bookmarks"
const val BOOKMARKS_GROUP = "Bookmarks"

private fun loadBookmarks(context: Context): Set<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_BOOKMARKS, emptySet())?.toSet() ?: emptySet()

private fun saveBookmarks(context: Context, titles: Set<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putStringSet(KEY_BOOKMARKS, titles.toSet()).apply()
}

@Composable
fun App(
    groups: List<Group>,
    zoom: Float,
    onZoom: (Int) -> Unit,
    appearance: Appearance,
    onAppearance: (Appearance) -> Unit,
) {
    val context = LocalContext.current
    val all = remember(groups) { groups.flatMap { it.parts } }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    // The list's state lives here so it survives opening an entry and coming back.
    var query by rememberSaveable { mutableStateOf("") }
    // Every group starts collapsed (a Bookmarks group, if there is one, starts open).
    var collapsed by rememberSaveable { mutableStateOf(groups.map { it.name }) }
    val listState = rememberLazyListState()
    val part = all.firstOrNull { it.title == selected }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(loadBookmarks(context)) }

    fun toggleBookmark(title: String) {
        bookmarks = if (title in bookmarks) bookmarks - title else bookmarks + title
        saveBookmarks(context, bookmarks)
    }

    // Swiping and the Previous/Next buttons move within the group the entry belongs to.
    val group = groups.firstOrNull { g -> g.parts.any { it.title == selected } }
    val siblings = group?.parts.orEmpty()
    val index = siblings.indexOfFirst { it.title == selected }
    val previous = if (index > 0) siblings[index - 1] else null
    val next = if (index in 0 until siblings.size - 1) siblings[index + 1] else null

    BackHandler(enabled = showAbout) { showAbout = false }
    BackHandler(enabled = part != null && !showAbout) { selected = null }

    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
    } else if (part == null) {
        HomeScreen(
            groups = groups,
            bookmarked = all.filter { it.title in bookmarks },
            query = query,
            onQuery = { query = it },
            collapsed = collapsed,
            onCollapsed = { collapsed = it },
            listState = listState,
            zoom = zoom,
            onZoom = onZoom,
            onOpen = { selected = it.title },
            onAppearance = { showAppearance = true },
            onAbout = { showAbout = true },
        )
    } else {
        ReaderScreen(
            part = part,
            groupName = group?.name,
            bookmarked = part.title in bookmarks,
            onToggleBookmark = { toggleBookmark(part.title) },
            previous = previous,
            next = next,
            onNavigate = { selected = it.title },
            onZoom = onZoom,
            onBack = { selected = null },
        )
    }

    if (showAppearance) {
        AppearanceDialog(appearance, onAppearance, onDismiss = { showAppearance = false })
    }
}

// ---- Shared pieces -------------------------------------------------------------------------

@Composable
private fun barColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

/** A round, colored badge holding an icon. */
@Composable
private fun IconBadge(icon: ImageVector, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
private fun ZoomControls(zoom: Float, onZoom: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onZoom(-1) }) { Icon(Icons.Filled.ZoomOut, contentDescription = "Smaller text and display") }
        Text(
            "${(zoom * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .clickable(onClickLabel = "Reset zoom to 100%") { onZoom(0) }
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
        IconButton(onClick = { onZoom(1) }) { Icon(Icons.Filled.ZoomIn, contentDescription = "Larger text and display") }
    }
}

// ---- Home ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    groups: List<Group>,
    bookmarked: List<Part>,
    query: String,
    onQuery: (String) -> Unit,
    collapsed: List<String>,
    onCollapsed: (List<String>) -> Unit,
    listState: LazyListState,
    zoom: Float,
    onZoom: (Int) -> Unit,
    onOpen: (Part) -> Unit,
    onAppearance: () -> Unit,
    onAbout: () -> Unit,
) {
    val q = query.trim()
    // `collapsed` holds the names of collapsed groups. Searching always shows every match.
    val searching = q.isNotEmpty()
    // The user's bookmarks appear as a group at the top of the list (not while searching).
    val shown = if (!searching && bookmarked.isNotEmpty()) {
        listOf(Group(BOOKMARKS_GROUP, bookmarked)) + groups
    } else {
        groups
    }
    val allCollapsed = shown.isNotEmpty() && shown.all { it.name in collapsed }
    val bookmarkedTitles = remember(bookmarked) { bookmarked.map { it.title }.toSet() }
    val totalDocs = remember(groups) { groups.sumOf { it.parts.size } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = barColors(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("US Constitution", maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = onAppearance) { Icon(Icons.Filled.Palette, contentDescription = "Appearance: colors and theme") }
                    IconButton(onClick = onAbout) { Icon(Icons.Filled.Info, contentDescription = "About") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("Search all documents", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (!searching) {
                    TextButton(
                        onClick = { onCollapsed(if (allCollapsed) emptyList() else shown.map { it.name }) },
                    ) {
                        Icon(
                            if (allCollapsed) Icons.Filled.UnfoldMore else Icons.Filled.UnfoldLess,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (allCollapsed) "Expand all" else "Collapse all", maxLines = 1, softWrap = false)
                    }
                }
                // Zoom the whole interface, text included. Tap the percentage to reset to 100%.
                ZoomControls(zoom, onZoom)
            }
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                if (!searching) {
                    item(key = "hero") { Hero(totalDocs, groups.size) }
                }
                var anyResult = false
                for (group in shown) {
                    val matches = if (q.isEmpty()) group.parts else group.parts.filter {
                        it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true)
                    }
                    if (matches.isEmpty()) continue
                    anyResult = true
                    val isCollapsed = !searching && group.name in collapsed
                    item(key = "group-" + group.name) {
                        GroupHeader(
                            name = group.name,
                            count = matches.size,
                            collapsed = isCollapsed,
                            showChevron = !searching,
                            onClick = {
                                if (!searching) {
                                    onCollapsed(if (isCollapsed) collapsed - group.name else collapsed + group.name)
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (isCollapsed) continue
                    items(matches, key = { group.name + "|" + it.title }) { part ->
                        EntryRow(part, part.title in bookmarkedTitles, { onOpen(part) }, Modifier.animateItem())
                    }
                }
                if (!anyResult) {
                    item {
                        Text(
                            "No matches",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(documents: Int, collections: Int) {
    val scheme = MaterialTheme.colorScheme
    // Bold colors in light mode; the deeper container colors in dark mode so the card is not glaring.
    val darkMode = scheme.background.luminance() < 0.5f
    val from = if (darkMode) scheme.primaryContainer else scheme.primary
    val to = if (darkMode) scheme.secondaryContainer else scheme.secondary
    val ink = if (darkMode) scheme.onPrimaryContainer else scheme.onPrimary
    Box(
        Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(from, to)))
            .padding(20.dp),
    ) {
        Icon(
            Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = ink.copy(alpha = 0.18f),
            modifier = Modifier.size(104.dp).align(Alignment.CenterEnd),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "We the People",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ink,
            )
            Text(
                "The Constitution and the documents behind it",
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
            )
            Text(
                "$documents documents in $collections collections",
                style = MaterialTheme.typography.labelLarge,
                color = ink.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun GroupHeader(
    name: String,
    count: Int,
    collapsed: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")
    Row(
        modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (collapsed) scheme.surfaceVariant else scheme.primaryContainer)
            .clickable(onClickLabel = if (collapsed) "Expand" else "Collapse", onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(groupIcon(name))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (collapsed) scheme.onSurface else scheme.onPrimaryContainer,
            )
            Text(
                if (count == 1) "1 document" else "$count documents",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@Composable
private fun EntryRow(part: Part, bookmarked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val main = part.title.substringBefore(" – ")
    val sub = part.title.substringAfter(" – ", "")
    Row(
        modifier
            .padding(start = 20.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(scheme.primary))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(main, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        if (bookmarked) {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = "Bookmarked",
                tint = scheme.primary,
                modifier = Modifier.padding(end = 12.dp).size(20.dp),
            )
        }
    }
}

// ---- Appearance dialog ---------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceDialog(
    appearance: Appearance,
    onChange: (Appearance) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        icon = { Icon(Icons.Filled.Palette, contentDescription = null) },
        title = { Text("Appearance") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (mode in ThemeMode.entries) {
                        val icon = when (mode) {
                            ThemeMode.System -> Icons.Filled.SettingsBrightness
                            ThemeMode.Light -> Icons.Filled.LightMode
                            ThemeMode.Dark -> Icons.Filled.DarkMode
                        }
                        FilterChip(
                            selected = appearance.mode == mode,
                            onClick = { onChange(appearance.copy(mode = mode)) },
                            label = { Text(mode.label) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Color", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                for (accent in Accent.entries) {
                    if (accent == Accent.Dynamic && !dynamicColorSupported) continue
                    val selected = appearance.accent == accent
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onChange(appearance.copy(accent = accent)) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (accent == Accent.Dynamic) {
                                        Brush.sweepGradient(
                                            listOf(Color(0xFF1D4E89), Color(0xFFB22234), Color(0xFF2E6B3F), Color(0xFFE0A800), Color(0xFF1D4E89)),
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(accent.swatch, accent.swatch))
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            accent.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
    )
}

// ---- Reader --------------------------------------------------------------------------------

private val sectionHeading = Regex("^(Section \\d+|Article (\\d+|[IVXLC]+))$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    part: Part,
    groupName: String?,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    previous: Part?,
    next: Part?,
    onNavigate: (Part) -> Unit,
    onZoom: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val size = 18f // body text size in sp; the Zoom setting scales it (and everything else)
    val scroll = rememberScrollState()
    val swipeDistance = with(LocalDensity.current) { 96.dp.toPx() }
    // Start every entry at the top, including when moving to the previous or next one.
    LaunchedEffect(part.title) { scroll.scrollTo(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = barColors(),
                title = { Text(part.title.substringBefore(" – "), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.semantics {
                            contentDescription = if (bookmarked) "Remove bookmark" else "Add bookmark"
                        },
                    ) {
                        Icon(if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null)
                    }
                    IconButton(onClick = { onZoom(-1) }) { Icon(Icons.Filled.ZoomOut, contentDescription = "Smaller text and display") }
                    IconButton(onClick = { onZoom(1) }) { Icon(Icons.Filled.ZoomIn, contentDescription = "Larger text and display") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                // Swipe left for the next entry in the group, right for the previous one.
                .pointerInput(part.title, previous?.title, next?.title) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragCancel = { total = 0f },
                        onDragEnd = {
                            if (total > swipeDistance) previous?.let(onNavigate)
                            else if (total < -swipeDistance) next?.let(onNavigate)
                            total = 0f
                        },
                        onHorizontalDrag = { _, amount -> total += amount },
                    )
                }
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (groupName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(groupIcon(groupName), size = 28)
                    Spacer(Modifier.width(10.dp))
                    Text(groupName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            // Touch and hold to select text; the selection can be copied or shared.
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // The top bar is short, so a dated title ("Name – March 6, 1819") is shown in full here.
                    val subtitle = part.title.substringAfter(" – ", "")
                    if (subtitle.isNotEmpty()) {
                        Text(
                            if (subtitle.any { it.isDigit() }) part.title else subtitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    for (paragraph in part.body.split(Regex("\\n\\s*\\n"))) {
                        val text = paragraph.trim()
                        if (text.isEmpty()) continue
                        val lines = text.lines()
                        // A "Section N" line may stand alone or lead a paragraph.
                        if (sectionHeading.matches(lines.first().trim())) {
                            Text(
                                lines.first().trim(),
                                fontSize = (size - 2).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            val rest = lines.drop(1).joinToString("\n").trim()
                            if (rest.isNotEmpty()) Body(rest, size)
                        } else {
                            Body(text, size)
                        }
                    }
                }
            }
            if (previous != null || next != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (previous != null) {
                        FilledTonalButton(onClick = { onNavigate(previous) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                            Text(previous.title.substringBefore(" – "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (next != null) {
                        FilledTonalButton(onClick = { onNavigate(next) }, modifier = Modifier.weight(1f)) {
                            Text(next.title.substringBefore(" – "), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ---- About ---------------------------------------------------------------------------------

private val aboutSections = listOf(
    "About this app" to
        "US Constitution, version 3.0.0. A searchable, easy-to-navigate reference to the " +
        "Constitution of the United States, broken down by article and amendment.",
    "Credit to the original app" to
        "This app is a ground-up rebuild, for current versions of Android, of the app " +
        "\"United States Constitution!\" (version 2.0.3) by Ken Hunt, first released in 2009. " +
        "Its table of contents, article-by-article layout and collection of related documents " +
        "are the model for this one.\n\n" +
        "The original author wrote: \"This application is for all you constitution geeks. " +
        "I wanted something that was easier to navigate then other applications I've seen, " +
        "had a real table of contents broken down by articles, sections and amendments, " +
        "didn't treat the first ten amendments like one giant inseparable document, was " +
        "searchable, and contained other related documents, ideas, and trivia.\"\n\n" +
        "He also noted that he did not write the material himself, but gathered the text, " +
        "notes, and annotations from a variety of sources, mostly the now defunct website " +
        "www.usconstitution.net.",
    "Original sources" to
        "The original app credited these reference sources:\n" +
        "• www.usconstitution.net, © 1995–2011 by Steve Mount. All rights reserved.\n" +
        "• BBC\n" +
        "• FindLaw: For Legal Professionals\n" +
        "• The National Archives\n" +
        "• Wikipedia",
    "Sources for the added documents" to
        "The texts of the Articles of Confederation, the Virginia Declaration of Rights, the " +
        "Treaty of Paris, the Declaration of the Rights of Man and the thirteen state " +
        "ratification documents are from the Avalon Project " +
        "at Yale Law School. The Great Compromise is taken from James Madison's notes of the " +
        "Federal Convention (July 5 and 16, 1787), also from the Avalon Project. The " +
        "Proclamation by the King is from Wikisource. All of these " +
        "are public-domain works; the source sites are credited for making them available. " +
        "Each document ends with a \"Text source\" line naming where its text came from.",
    "Presidential speeches" to
        "Jefferson's, Roosevelt's first inaugural and Kennedy's inaugural addresses are from the " +
        "Avalon Project at Yale Law School. Wilson's war message, Roosevelt's \"Day of Infamy\" " +
        "address, Eisenhower's farewell, Kennedy's civil rights address, Johnson's " +
        "\"We Shall Overcome\" address, Truman's address on Greece and Turkey and Nixon's " +
        "resignation address are from the American Presidency Project at the " +
        "University of California, Santa Barbara. Reagan's speeches are from the Ronald Reagan " +
        "Presidential Library, and President Bush's September 2001 addresses and President " +
        "Obama's first inaugural address are from the archived White House websites of those " +
        "administrations. Official speeches of a " +
        "President are works of the U.S. government. Audience reactions such as \"(Applause.)\" " +
        "are left out.",
    "Supreme Court cases" to
        "The Supreme Court excerpts come from the Legal Information Institute at Cornell Law " +
        "School. Each is only a selected passage of the Court's opinion (and, in some cases, of " +
        "a concurrence or dissent), edited for length: citations and footnote numbers are " +
        "removed, and an ellipsis marks where a passage begins mid-paragraph. The full opinions are available " +
        "from the Supreme Court and from the Legal Information Institute. Opinions of the " +
        "Court are public-domain government works.",
    "More sources" to
        "The Federalist Papers are from the Library of Congress. Washington's addresses, " +
        "Lincoln's First Inaugural, the Monroe Doctrine, the Virginia Plan and the Magna Carta " +
        "clauses are from the Avalon Project at Yale Law School. The Four Freedoms and Brown v. " +
        "Board of Education are from the National Archives, the Declaration of Sentiments is " +
        "from the National Park Service, and Marbury v. Madison is from the Cornell Legal " +
        "Information Institute. Each of these documents names its source under its title.",
    "About the text in this app" to
        "The text of the Constitution, its amendments and the documents in the American DNA " +
        "group is public-domain primary material. It was entered independently for this app " +
        "and has not been copied from usconstitution.net. That site's notes and annotations " +
        "are protected by copyright and are not included. Spelling and capitalization follow " +
        "the original documents. Please check these texts against an official copy, such as " +
        "those at the National Archives, before relying on them.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = barColors(),
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((heading, text) in aboutSections) {
                Text(
                    heading,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Body(text: String, size: Float) {
    Text(
        text,
        fontSize = size.sp,
        lineHeight = (size * 1.5f).sp,
        fontFamily = FontFamily.Serif,
    )
}
