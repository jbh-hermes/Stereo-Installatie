package nl.hoefvinyl.stereoinstallatie.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.hoefvinyl.stereoinstallatie.MarantzClient
import nl.hoefvinyl.stereoinstallatie.MarantzDiscovery
import nl.hoefvinyl.stereoinstallatie.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

private enum class Tab(val label:String, val icon:ImageVector) {
    START("Start", Icons.Filled.Home),
    RADIO("Radio", Icons.Filled.Radio),
    FAVORIETEN("Favorieten", Icons.Filled.Favorite),
    VERSTERKER("Versterker", Icons.Filled.Speaker),
    BRONNEN("Bronnen", Icons.Filled.Apps)
}

@Composable
fun StereoInstallatieApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("stereo_installatie_clean", 0) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(Tab.START) }
    var ip by remember { mutableStateOf(prefs.getString("na8005_ip", "192.168.68.73") ?: "192.168.68.73") }
    var nowPlaying by remember { mutableStateOf(MarantzClient.NowPlaying("Nog geen station", "", "")) }
    var favorites by remember { mutableStateOf<List<MarantzClient.Favorite>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    fun message(text:String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    fun ensureMarantz(after:(String)->Unit) {
        scope.launch {
            busy = true
            val found = withContext(Dispatchers.IO) {
                when {
                    MarantzClient.isReachable(ip) -> ip
                    else -> MarantzDiscovery.findNa8005(context)
                        ?: "192.168.68.73".takeIf { MarantzClient.isReachable(it) }
                }
            }
            busy = false
            if(found == null) message("NA8005 niet gevonden")
            else {
                ip = found
                prefs.edit().putString("na8005_ip", found).apply()
                after(found)
            }
        }
    }

    fun refreshNowPlaying() {
        ensureMarantz { host ->
            scope.launch { nowPlaying = withContext(Dispatchers.IO) { MarantzClient.fetchNowPlaying(host) } }
        }
    }

    fun refreshFavorites(showMessage:Boolean = false) {
        ensureMarantz { host ->
            scope.launch {
                busy = true
                favorites = withContext(Dispatchers.IO) { MarantzClient.fetchFavorites(host) }
                busy = false
                if(showMessage) message("Favorieten bijgewerkt")
            }
        }
    }

    fun naCommand(label:String, command:(String)->Boolean) {
        ensureMarantz { host ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) { command(host) }
                message(if(ok) label else "Opdracht mislukt")
                if(ok) refreshNowPlaying()
            }
        }
    }

    fun ampCommand(label:String, action:String) {
        ensureMarantz { host ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) { MarantzClient.sendAmplifierCommand(host, action) }
                message(if(ok) label else "PM8003 reageerde niet")
            }
        }
    }

    fun saveCurrentFavorite() {
        ensureMarantz { host ->
            scope.launch {
                val current = withContext(Dispatchers.IO) { MarantzClient.fetchFavorites(host) }
                val used = current.mapNotNull { it.position.toIntOrNull() }.toSet()
                val slot = (1..50).firstOrNull { it !in used }
                if(slot == null) message("Alle 50 favorietenslots zijn bezet")
                else {
                    val pos = "%02d".format(slot)
                    val ok = withContext(Dispatchers.IO) { MarantzClient.addCurrentToFavorite(host, pos) }
                    message(if(ok) "Zender bewaard in slot $pos" else "Bewaren mislukt")
                    if(ok) refreshFavorites(false)
                }
            }
        }
    }

    fun playAdjacent(direction:Int) {
        ensureMarantz { host ->
            scope.launch {
                val list = if(favorites.isNotEmpty()) favorites else withContext(Dispatchers.IO) { MarantzClient.fetchFavorites(host) }
                if(list.isEmpty()) {
                    message("Er zijn nog geen favorieten")
                    return@launch
                }
                val currentName = nowPlaying.station.trim()
                val currentIndex = list.indexOfFirst { it.name.equals(currentName, ignoreCase = true) }
                val base = if(currentIndex >= 0) currentIndex else 0
                val next = (base + direction + list.size) % list.size
                val fav = list[next]
                val ok = withContext(Dispatchers.IO) { MarantzClient.callFavorite(host, fav.position) }
                message(if(ok) fav.name else "Zender kon niet worden gestart")
                if(ok) refreshNowPlaying()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshNowPlaying()
        refreshFavorites(false)
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(
                containerColor = NavigationBackground,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            tab = item
                            if(item == Tab.START || item == Tab.RADIO) refreshNowPlaying()
                            if(item == Tab.FAVORIETEN) refreshFavorites(false)
                        },
                        icon = { Icon(item.icon, item.label, modifier = Modifier.size(27.dp)) },
                        label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryText,
                            selectedTextColor = PrimaryText,
                            indicatorColor = ActivePurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground)
        ) {
            when(tab) {
                Tab.START -> StartScreen(
                    nowPlaying = nowPlaying,
                    onOpen = { tab = it },
                    onSource = { cmd, label -> naCommand("$label gekozen") { MarantzClient.selectSource(it, cmd) } }
                )
                Tab.RADIO -> RadioScreen(
                    nowPlaying = nowPlaying,
                    onPowerOn = { naCommand("NA8005 aan") { MarantzClient.powerOn(it) } },
                    onPowerOff = { naCommand("NA8005 stand-by") { MarantzClient.standby(it) } },
                    onPrevious = { playAdjacent(-1) },
                    onNext = { playAdjacent(1) },
                    onSave = { saveCurrentFavorite() },
                    onSearch = { query ->
                        message(if(query.isBlank()) "Vul een zoekterm in" else "Zoeken naar '$query' wordt als volgende radiofunctie gekoppeld")
                    }
                )
                Tab.FAVORIETEN -> FavoritesScreen(
                    favorites = favorites,
                    onRefresh = { refreshFavorites(true) },
                    onPlay = { fav ->
                        ensureMarantz { host ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.callFavorite(host, fav.position) }
                                message(if(ok) fav.name else "Favoriet kon niet worden gestart")
                                if(ok) refreshNowPlaying()
                            }
                        }
                    },
                    onDelete = { fav ->
                        ensureMarantz { host ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.deleteFavorite(host, fav.position) }
                                message(if(ok) "Favoriet " + fav.position + " verwijderd" else "Verwijderen mislukt")
                                if(ok) refreshFavorites(false)
                            }
                        }
                    },
                    onAdd = { saveCurrentFavorite() }
                )
                Tab.VERSTERKER -> AmplifierScreen(
                    onAllOn = {
                        naCommand("NA8005 aan") { MarantzClient.powerOn(it) }
                        ampCommand("PM8003 aan-opdracht verzonden", "POWER_ON")
                    },
                    onAllOff = {
                        ampCommand("PM8003 stand-by-opdracht verzonden", "POWER_OFF")
                        naCommand("NA8005 stand-by") { MarantzClient.standby(it) }
                    },
                    onAmp = { action, label -> ampCommand(label, action) }
                )
                Tab.BRONNEN -> SourcesScreen(
                    onOpen = { tab = it },
                    onSource = { cmd, label -> naCommand("$label gekozen") { MarantzClient.selectSource(it, cmd) } }
                )
            }

            if(busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Gold,
                    trackColor = CardBackground
                )
            }
        }
    }
}

@Composable
private fun Page(title:String, content:@Composable ColumnScope.()->Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(title, color = PrimaryText, fontSize = 30.sp, fontWeight = FontWeight.Normal)
        content()
    }
}

@Composable
private fun StartScreen(nowPlaying:MarantzClient.NowPlaying, onOpen:(Tab)->Unit, onSource:(String,String)->Unit) {
    Page("Stereo-Installatie") {
        AppCard {
            Icon(Icons.Filled.GraphicEq, null, tint = Gold, modifier = Modifier.size(50.dp).align(Alignment.CenterHorizontally))
            Text("Nu afspelen", color = SecondaryText, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(nowPlaying.station.ifBlank { "Nog geen station" }, color = PrimaryText, fontSize = 26.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(nowPlaying.artist.ifBlank { "Artiest" }, color = Gold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(nowPlaying.title.ifBlank { "Titel" }, color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            VintageVuMeters(active = nowPlaying.station.isNotBlank() && nowPlaying.station != "Nog geen station")
        }
        Text("Snel kiezen", color = PrimaryText, fontSize = 26.sp)
        QuickGrid(listOf(
            Triple(Icons.Filled.Radio,"Internet Radio"){ onOpen(Tab.RADIO) },
            Triple(Icons.Filled.Storage,"Muziekserver"){ onSource("SISERVER","Muziekserver") },
            Triple(Icons.Filled.Usb,"USB"){ onSource("SIUSB","USB") },
            Triple(Icons.Filled.Star,"Favorieten"){ onOpen(Tab.FAVORIETEN) }
        ))
    }
}

@Composable
private fun RadioScreen(
    nowPlaying:MarantzClient.NowPlaying,
    onPowerOn:()->Unit,
    onPowerOff:()->Unit,
    onPrevious:()->Unit,
    onNext:()->Unit,
    onSave:()->Unit,
    onSearch:(String)->Unit
) {
    var query by remember { mutableStateOf("") }
    Page("Radio") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoldAction("NA8005 aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f), onPowerOn)
            GoldAction("NA8005 uit", Icons.Filled.PowerOff, Modifier.weight(1f), onPowerOff)
        }
        AppCard {
            Text("Nu op de radio", color = SecondaryText, fontSize = 18.sp)
            Text(nowPlaying.station.ifBlank { "Nog geen station" }, color = PrimaryText, fontSize = 25.sp)
            Text(nowPlaying.artist.ifBlank { "Artiest" }, color = Gold, fontSize = 20.sp)
            Text(nowPlaying.title.ifBlank { "Titel" }, color = PrimaryText, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineAction("Vorige", Icons.Filled.SkipPrevious, Modifier.weight(1f), onPrevious)
                OutlineAction("Volgende", Icons.Filled.SkipNext, Modifier.weight(1f), onNext)
            }
            GoldAction("Zender bewaren", Icons.Outlined.FavoriteBorder, Modifier.fillMaxWidth(), onSave)
        }
        AppCard {
            Text("Zoek radiostation", color = PrimaryText, fontSize = 24.sp)
            OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Naam of zoekwoord") }, modifier = Modifier.fillMaxWidth())
            GoldAction("Zoeken", Icons.Filled.Search, Modifier.fillMaxWidth()) { onSearch(query) }
        }
    }
}

@Composable
private fun FavoritesScreen(
    favorites:List<MarantzClient.Favorite>,
    onRefresh:()->Unit,
    onPlay:(MarantzClient.Favorite)->Unit,
    onDelete:(MarantzClient.Favorite)->Unit,
    onAdd:()->Unit
) {
    val bySlot = remember(favorites) { favorites.associateBy { it.position.toIntOrNull() ?: -1 } }
    Page("Marantz-favorieten") {
        Text("50 slots • tik op een station om af te spelen", color = SecondaryText, fontSize = 16.sp)
        AppCard {
            repeat(50) { index ->
                val slot = index + 1
                val fav = bySlot[slot]
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = fav != null) { fav?.let(onPlay) }.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("%02d".format(slot), color = Gold, fontSize = 17.sp, modifier = Modifier.width(44.dp))
                    Icon(Icons.Filled.Radio, null, tint = Gold)
                    Spacer(Modifier.width(12.dp))
                    Text(fav?.name ?: "Vrij slot", color = if(fav == null) SecondaryText else PrimaryText, modifier = Modifier.weight(1f))
                    if(fav != null) IconButton(onClick = { onDelete(fav) }) { Icon(Icons.Filled.DeleteOutline, null, tint = SecondaryText) }
                }
                if(index < 49) HorizontalDivider(color = SecondaryText.copy(alpha = 0.10f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldAction("Toevoegen", Icons.Filled.Add, Modifier.weight(1f), onAdd)
            OutlineAction("Vernieuwen", Icons.Filled.Refresh, Modifier.weight(1f), onRefresh)
        }
    }
}

@Composable
private fun AmplifierScreen(onAllOn:()->Unit, onAllOff:()->Unit, onAmp:(String,String)->Unit) {
    Page("PM8003 versterker") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoldAction("Alles aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f), onAllOn)
            OutlineAction("Alles uit", Icons.Filled.PowerOff, Modifier.weight(1f), onAllOff)
        }
        Text("Alleen PM8003", color = PrimaryText, fontSize = 25.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoldAction("Aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f)) { onAmp("POWER_ON","PM8003 aan") }
            GoldAction("Stand-by", Icons.Filled.PowerOff, Modifier.weight(1f)) { onAmp("POWER_OFF","PM8003 stand-by") }
        }
        AppCard {
            Text("Volume", color = PrimaryText, fontSize = 26.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldAction("Zachter", Icons.Filled.VolumeDown, Modifier.weight(1f)) { onAmp("VOLUME_DOWN","Volume zachter") }
                GoldAction("Harder", Icons.Filled.VolumeUp, Modifier.weight(1f)) { onAmp("VOLUME_UP","Volume harder") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldAction("Dempen", Icons.Filled.VolumeOff, Modifier.weight(1f)) { onAmp("MUTE_ON","Dempen") }
                GoldAction("Geluid aan", Icons.Filled.VolumeUp, Modifier.weight(1f)) { onAmp("MUTE_OFF","Geluid aan") }
            }
        }
        Text("Ingang kiezen", color = PrimaryText, fontSize = 25.sp)
        QuickGrid(listOf(
            Triple(Icons.Filled.Album,"CD"){ onAmp("INPUT_CD","CD gekozen") },
            Triple(Icons.Filled.Radio,"Tuner"){ onAmp("INPUT_TUNER","Tuner gekozen") },
            Triple(Icons.Filled.Cable,"AUX"){ onAmp("INPUT_AUX","AUX gekozen") },
            Triple(Icons.Filled.Album,"Phono"){ onAmp("INPUT_PHONO","Phono gekozen") }
        ))
    }
}

@Composable
private fun SourcesScreen(onOpen:(Tab)->Unit, onSource:(String,String)->Unit) {
    Page("Bronnen") {
        QuickGrid(listOf(
            Triple(Icons.Filled.Radio,"Internet Radio"){ onOpen(Tab.RADIO) },
            Triple(Icons.Filled.Storage,"Muziekserver / NAS"){ onSource("SISERVER","Muziekserver / NAS") },
            Triple(Icons.Filled.Usb,"USB"){ onSource("SIUSB","USB") },
            Triple(Icons.Filled.Star,"Favorieten"){ onOpen(Tab.FAVORIETEN) },
            Triple(Icons.Filled.MusicNote,"Spotify"){ onSource("SISPOTIFY","Spotify") },
            Triple(Icons.Filled.Cable,"Digitaal coax"){ onSource("SICOAXIAL","Digitaal coax") },
            Triple(Icons.Filled.GraphicEq,"Digitaal optisch"){ onSource("SIOPTICAL","Digitaal optisch") },
            Triple(Icons.Filled.Computer,"USB-DAC"){ onSource("SIUSBDAC","USB-DAC") }
        ))
    }
}

@Composable
private fun AppCard(content:@Composable ColumnScope.()->Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = CardBackground, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun QuickGrid(items:List<Triple<ImageVector,String,()->Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (icon,label,onClick) ->
                    Surface(
                        modifier = Modifier.weight(1f).height(132.dp),
                        color = CardBackground,
                        shape = RoundedCornerShape(26.dp),
                        onClick = onClick
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(icon, null, tint = Gold, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(label, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
                if(row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoldAction(text:String, icon:ImageVector, modifier:Modifier = Modifier, onClick:()->Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = PurpleText)
    ) {
        Icon(icon, null, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun OutlineAction(text:String, icon:ImageVector, modifier:Modifier = Modifier, onClick:()->Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
    ) {
        Icon(icon, null, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun VintageVuMeters(active:Boolean) {
    val transition = rememberInfiniteTransition(label = "vu")
    val left by transition.animateFloat(
        initialValue = 0.32f,
        targetValue = if(active) 0.86f else 0.38f,
        animationSpec = infiniteRepeatable(tween(if(active) 520 else 1500), RepeatMode.Reverse),
        label = "left"
    )
    val right by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = if(active) 0.76f else 0.45f,
        animationSpec = infiniteRepeatable(tween(if(active) 690 else 1700), RepeatMode.Reverse),
        label = "right"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AnalogMeter(left, "L", Modifier.weight(1f))
        AnalogMeter(right, "R", Modifier.weight(1f))
    }
}

@Composable
private fun AnalogMeter(level:Float, label:String, modifier:Modifier = Modifier) {
    Surface(modifier = modifier.height(92.dp), color = MeterFace, shape = RoundedCornerShape(12.dp)) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height
                val pivot = Offset(w/2f, h*0.88f)
                val radius = w*0.40f
                for(i in 0..10) {
                    val angle = Math.toRadians(205.0 + (130.0*i/10.0))
                    val inner = Offset(pivot.x + cos(angle).toFloat()*radius*0.78f, pivot.y + sin(angle).toFloat()*radius*0.78f)
                    val outer = Offset(pivot.x + cos(angle).toFloat()*radius, pivot.y + sin(angle).toFloat()*radius)
                    drawLine(Color(0xFF3B2A15), inner, outer, strokeWidth = if(i%2==0) 2.2f else 1.2f)
                }
                val needleAngle = Math.toRadians(205.0 + 130.0*level.coerceIn(0f,1f))
                val needleEnd = Offset(pivot.x + cos(needleAngle).toFloat()*radius*0.73f, pivot.y + sin(needleAngle).toFloat()*radius*0.73f)
                drawLine(Color(0xFF8A241C), pivot, needleEnd, strokeWidth = 3.4f, cap = StrokeCap.Round)
                drawCircle(Color(0xFF332515), radius = 5f, center = pivot)
            }
            Text("−20      −10      −3   0   +3", color = MeterInk, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
            Text(label, color = MeterInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(7.dp))
        }
    }
}
