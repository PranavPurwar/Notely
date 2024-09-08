package dev.pranav.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.gson.Gson
import dev.pranav.notes.ui.theme.Fonts
import dev.pranav.notes.ui.theme.NotesTheme
import dev.pranav.notes.ui.theme.Typography
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.navigationBarColor = Color.Transparent.toArgb()

        setContent {
            Screen()
        }
    }
}

private val gson = Gson()

private val destinations = arrayOf(
    listOf("Home", Icons.Filled.Home, Screens.Home),
    listOf("Favorites", Icons.Filled.Favorite, Screens.Favourite),
)

@Composable
fun Screen() {
    val drawerState = remember { DrawerState(DrawerValue.Closed) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val openDrawer = {
        scope.launch {
            focusManager.clearFocus()
            drawerState.open()
        }
    }
    val navController = rememberNavController()

    val data = remember { NotesLiveData() }

    val notesFile = LocalContext.current.filesDir.resolve("notes")
    if (!notesFile.exists()) {
        notesFile.writeText(SecureAES.encrypt("[]"))
    }
    val notes = gson.fromJson(SecureAES.decrypt(notesFile.readText()), Array<Note>::class.java)
    notes.forEach { data.add(it) }

    data.observeForever {
        notesFile.writeText(SecureAES.encrypt(gson.toJson(it)))
    }

    NotesTheme {
        ModalNavigationDrawer(
            modifier = Modifier.statusBarsPadding(),
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerShape = MaterialTheme.shapes.extraLarge.copy(
                        topStart = ZeroCornerSize,
                        bottomStart = ZeroCornerSize
                    ),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                    ) {
                        Text("Notes", style = MaterialTheme.typography.titleLarge)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                    destinations.forEach { destination ->
                        val (label, icon, route) = destination
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            selected = navController.currentDestination == route,
                            onClick = {
                                navController.navigate(route)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(icon as ImageVector, label.toString()) },
                            label = { Text(label as String) },
                        )
                    }
                }
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = Screens.Home,
            ) {
                composable<Screens.Home> {
                    HomeScreen(openDrawer = { openDrawer() }, openNote = {
                        navController.navigate(Screens.Editor(it))
                    }, data, navController)
                }
                composable<Screens.Favourite> {
                    FavouriteScreen(openDrawer = { openDrawer() }, openNote = {
                        navController.navigate(Screens.Editor(it))
                    }, data, navController)
                }
                composable<Screens.Editor> { backStackEntry ->
                    val editor: Screens.Editor = backStackEntry.toRoute()
                    NoteEditor(data, editor.pos, navController)
                }
            }
        }
    }
}

class Screens {
    @Serializable
    data object Home

    @Serializable
    data class Editor(val pos: Int = -1)

    @Serializable
    data object Favourite
}

@Serializable
data class Note(
    val title: String,
    val body: String,
    val isFavorite: Boolean,
    val date: String,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    openDrawer: () -> Unit = {},
    openNote: (Int) -> Unit = {},
    data: NotesLiveData,
    navController: NavController? = null
) {
    var text by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf(data.value) }

    data.observeForever {
        notes = it
    }

    Scaffold(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController?.navigate(Screens.Editor(-1)) },
                modifier = Modifier.semantics { traversalIndex = 2f },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                SearchBar(
                    modifier = Modifier
                        .semantics { traversalIndex = 0f }
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(if (expanded) 1f else 0.94f),
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier.wrapContentWidth(),
                            query = text,
                            onQueryChange = { text = it },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            placeholder = { Text("Search your notes") },
                            leadingIcon = {
                                if (expanded) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.clickable {
                                            expanded = false
                                        })
                                } else {
                                    Icon(Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        modifier = Modifier.clickable {
                                            expanded = false
                                            openDrawer()
                                        })
                                }
                            },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.background)
                            .wrapContentHeight()
                            .imePadding(),
                    ) {
                        data.value!!.forEachIndexed { index, note ->
                            if (note.title.contains(text, ignoreCase = true) || note.body.contains(
                                    text, ignoreCase = true
                                )
                            ) ListItem(headlineContent = { Text(note.title) },
                                supportingContent = {
                                    Text(
                                        note.body, maxLines = 4, overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (note.isFavorite) Color.Red
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        openNote.invoke(index)
                                    }
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                }
            }

        }) { innerPadding ->
        if (notes!!.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No notes found",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = Fonts.Oxygen,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            modifier = Modifier
                .semantics { traversalIndex = 1f }
                .padding(PaddingValues(16.dp, 16.dp, 16.dp, 0.dp)),
            contentPadding = PaddingValues(0.dp, innerPadding.calculateTopPadding(), 0.dp, 24.dp),
        ) {
            items(count = notes!!.size) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                    onClick = { openNote(it) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim),
                    content = {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                notes!![it].title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = Fonts.Oxygen,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                notes!![it].body,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = Fonts.Karla,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 16,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                notes!![it].date,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = Fonts.Karla,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    })
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FavouriteScreen(
    openDrawer: () -> Unit = {},
    openNote: (Int) -> Unit = {},
    data: NotesLiveData,
    navController: NavController? = null
) {
    var text by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var favNotes = rememberSaveable { data.value!!.filter { it.isFavorite } }

    data.observeForever { notes ->
        favNotes = notes.filter { it.isFavorite }
    }

    Scaffold(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController?.navigate(Screens.Editor(-1)) },
                modifier = Modifier.semantics { traversalIndex = 2f },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                SearchBar(
                    modifier = Modifier
                        .semantics { traversalIndex = 0f }
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(if (expanded) 1f else 0.94f),
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier.wrapContentWidth(),
                            query = text,
                            onQueryChange = { text = it },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            placeholder = { Text("Search your favourite notes") },
                            leadingIcon = {
                                if (expanded) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.clickable {
                                            expanded = false
                                        })
                                } else {
                                    Icon(Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        modifier = Modifier.clickable {
                                            expanded = false
                                            openDrawer()
                                        })
                                }
                            },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.background)
                            .wrapContentHeight()
                            .imePadding()
                    ) {
                        favNotes.forEachIndexed { index, note ->
                            if (note.title.contains(text, ignoreCase = true) || note.body.contains(
                                    text, ignoreCase = true
                                )
                            ) ListItem(headlineContent = { Text(note.title) },
                                supportingContent = {
                                    Text(
                                        note.body, maxLines = 4, overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (note.isFavorite) Color.Red
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        openNote.invoke(index)
                                    }
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                }
            }

        }) { innerPadding ->
        if (favNotes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No favourite notes found",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = Fonts.Oxygen,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            modifier = Modifier
                .semantics { traversalIndex = 1f }
                .padding(PaddingValues(16.dp, 16.dp, 16.dp, 0.dp)),
            contentPadding = PaddingValues(0.dp, innerPadding.calculateTopPadding(), 0.dp, 24.dp),
        ) {
            items(count = favNotes.size) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                    onClick = { openNote(data.value!!.indexOf(favNotes[it])) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim),
                    content = {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                favNotes[it].title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = Fonts.Oxygen,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                favNotes[it].body,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = Fonts.Karla,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 16,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                favNotes[it].date,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = Fonts.Karla,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(data = NotesLiveData().apply {
        repeat(10) {
            add(
                Note(
                    "Text $it",
                    if (it % 2 == 0) "Lorem Ipsun Jores Odor" else "Google Fonts takes several months to make new fonts available on Android. There's a gap in time between when a font is added in fonts.google.com and when it's available through the downloadable fonts API (either in the View system or in Compose). Newly added fonts might fail to load in your app with an IllegalStateException.",
                    true,
                    "12 July"
                )
            )
        }
    })
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NoteEditor(notes: NotesLiveData, pos: Int = -1, navController: NavController? = null) {
    val note = if (pos == -1) Note("", "", false, "") else notes.value!![pos]
    val isFavorite = remember { mutableStateOf(note.isFavorite) }
    val title = remember { mutableStateOf(note.title) }
    val body = remember { mutableStateOf(note.body) }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(
            title = { },
            actions = {
                IconButton(onClick = {
                    notes.remove(note)
                    navController?.popBackStack()
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear",
                    )
                }
                IconButton(onClick = { isFavorite.value = !isFavorite.value }) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = "Favorite",
                        tint = if (isFavorite.value) Color.Red
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    if (body.value.isBlank()) {
                        navController?.popBackStack()
                        return@IconButton
                    }
                    if (pos == -1) {
                        notes.add(
                            Note(
                                title.value,
                                body.value,
                                isFavorite.value,
                                LocalDate.now().toString()
                            )
                        )
                    } else {
                        notes.update(
                            pos, Note(
                                title.value,
                                body.value,
                                isFavorite.value,
                                LocalDate.now().toString()
                            )
                        )
                    }
                    navController?.popBackStack()
                }) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Clear",
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (pos == -1) {
                        notes.add(
                            Note(
                                title.value,
                                body.value,
                                isFavorite.value,
                                LocalDate.now().toString()
                            )
                        )
                    } else {
                        notes.update(
                            pos, Note(
                                title.value,
                                body.value,
                                isFavorite.value,
                                LocalDate.now().toString()
                            )
                        )
                    }
                    navController?.popBackStack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
        )
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                value = title.value,
                onValueChange = { title.value = it },
                textStyle = Typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                placeholder = {
                    Text(
                        "Title", style = Typography.bodyLarge.copy(
                            color = Color.Gray, fontWeight = FontWeight.SemiBold
                        )
                    )
                },
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp),
            )

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 8.dp),
                value = body.value,
                onValueChange = { body.value = it },
                textStyle = Typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                placeholder = {
                    Text(
                        "Body", maxLines = 1, style = Typography.bodyLarge.copy(
                            color = Color.Gray,
                        )
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoteEditorPreview() {
    NoteEditor(
        NotesLiveData().apply {
            add(
                Note(
                    "Joe Biden",
                    "Lorem Ipsun Jores nvm i forgor thr rest skretchware por codirang!!!",
                    true,
                    "7 Sept"
                )
            )
        }, 0
    )
}

class NotesLiveData : LiveData<List<Note>>() {
    private val notes = mutableListOf<Note>()

    init {
        value = notes
    }

    fun add(note: Note) {
        notes.add(note)
        value = notes
    }

    fun remove(note: Note) {
        notes.remove(note)
        value = notes
    }

    fun update(pos: Int, note: Note) {
        notes[pos] = note
        value = notes
    }
}
