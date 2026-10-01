package nl.hoefvinyl.stereoinstallatie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF191A1D)
private val CardBg = Color(0xFF25272B)
private val Gold = Color(0xFFD7A75C)
private val Purple = Color(0xFF4B3182)
private val NavSelected = Color(0xFF554C69)
private val Text = Color(0xFFF5F2F6)
private val Muted = Color(0xFFBEBAC3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(25,26,29)
        window.navigationBarColor = android.graphics.Color.rgb(25,26,29)
        setContent { StereoApp() }
    }

    @Composable
    private fun StereoApp() {
        val prefs = remember { getSharedPreferences("stereo_installatie", MODE_PRIVATE) }
        var ip by remember { mutableStateOf(prefs.getString("marantz_ip", "192.168.68.73") ?: "192.168.68.73") }
        var tab by remember { mutableIntStateOf(0) }
        var busy by remember { mutableStateOf(false) }
        var favorites by remember { mutableStateOf<List<MarantzClient.Favorite>>(emptyList()) }
        var nowPlaying by remember { mutableStateOf(MarantzClient.NowPlaying("HoefVinyl Radio Live", "", "")) }
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        fun message(s: String) { scope.launch { snackbar.showSnackbar(s) } }

        fun resolve(force: Boolean = false) {
            scope.launch {
                busy = true
                val found = withContext(Dispatchers.IO) {
                    if (!force && MarantzClient.isReachable(ip)) ip
                    else MarantzDiscovery.findNa8005(applicationContext)
                        ?: if (MarantzClient.isReachable("192.168.68.73")) "192.168.68.73" else null
                }
                if (found != null) {
                    ip = found
                    prefs.edit().putString("marantz_ip", found).apply()
                } else message("Marantz niet gevonden op het lokale netwerk")
                busy = false
            }
        }

        fun refreshNowPlaying() {
            scope.launch {
                nowPlaying = withContext(Dispatchers.IO) { MarantzClient.fetchNowPlaying(ip) }
            }
        }

        fun refreshFavorites(showMessage: Boolean = false) {
            scope.launch {
                busy = true
                val list = withContext(Dispatchers.IO) { MarantzClient.fetchFavorites(ip) }
                favorites = list
                busy = false
                if (showMessage) message(if (list.isEmpty()) "Geen Marantz-favorieten ontvangen" else "Favorieten bijgewerkt")
            }
        }

        LaunchedEffect(Unit) {
            resolve(false)
            refreshNowPlaying()
        }
        LaunchedEffect(tab, ip) {
            if (tab == 2) refreshFavorites(false)
            if (tab == 0 || tab == 1) refreshNowPlaying()
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Bg, surface = Bg, surfaceVariant = CardBg,
                primary = Gold, secondary = Purple, onBackground = Text, onSurface = Text
            )
        ) {
            Scaffold(
                containerColor = Bg,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF242529), tonalElevation = 0.dp) {
                        val items = listOf(
                            Triple("Start", Icons.Filled.Home, 0),
                            Triple("Radio", Icons.Filled.Radio, 1),
                            Triple("Favorieten", Icons.Filled.Favorite, 2),
                            Triple("Versterker", Icons.Filled.Speaker, 3),
                            Triple("Bronnen", Icons.Filled.Apps, 4)
                        )
                        items.forEach { (label, icon, idx) ->
                            NavigationBarItem(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                icon = { Icon(icon, label, modifier = Modifier.size(30.dp)) },
                                label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Text,
                                    selectedTextColor = Text,
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted,
                                    indicatorColor = NavSelected
                                )
                            )
                        }
                    }
                }
            ) { inner ->
                Box(Modifier.fillMaxSize().padding(inner)) {
                    when(tab) {
                        0 -> HomeScreen(nowPlaying, { resolve(true) }, { tab = 1 }, { tab = 2 }) { cmd, label ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.selectSource(ip, cmd) }
                                message(if (ok) "${label} gekozen" else "Bron kon niet worden gekozen")
                            }
                        }
                        1 -> RadioScreen(nowPlaying, { on ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { if (on) MarantzClient.powerOn(ip) else MarantzClient.standby(ip) }
                                if (!ok) resolve(true)
                            }
                        }, { refreshNowPlaying() })
                        2 -> FavoritesScreen(favorites, { refreshFavorites(true) }, { fav ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.callFavorite(ip, fav.position) }
                                message(if (ok) "${fav.name} wordt afgespeeld" else "Favoriet kon niet worden gestart")
                                if (ok) refreshNowPlaying()
                            }
                        }, { fav ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.deleteFavorite(ip, fav.position) }
                                message(if (ok) "Favoriet verwijderd" else "Verwijderen mislukt")
                                if (ok) refreshFavorites(false)
                            }
                        })
                        3 -> AmplifierScreen({ cmd, msg ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.sendAmplifierCommand(ip, cmd) }
                                message(if (ok) msg else "PM8003 reageerde niet")
                            }
                        }, { on ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    if (on) {
                                        MarantzClient.powerOn(ip)
                                        MarantzClient.sendAmplifierCommand(ip, "POWER_ON")
                                    } else {
                                        MarantzClient.sendAmplifierCommand(ip, "POWER_OFF")
                                        MarantzClient.standby(ip)
                                    }
                                }
                            }
                        })
                        else -> SourcesScreen(ip) { cmd, label ->
                            if (cmd == "FAVORITES") tab = 2
                            else scope.launch {
                                val ok = withContext(Dispatchers.IO) { MarantzClient.selectSource(ip, cmd) }
                                message(if (ok) "${label} gekozen" else "Bron kon niet worden gekozen")
                            }
                        }
                    }
                    if (busy) LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = Gold, trackColor = CardBg
                    )
                }
            }
        }
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 32.dp, vertical = 24.dp), content = content)
}

@Composable
private fun HomeScreen(
    nowPlaying: MarantzClient.NowPlaying,
    onSettings: () -> Unit,
    onRadio: () -> Unit,
    onFavorites: () -> Unit,
    onSource: (String,String) -> Unit
) {
    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stereo-Installatie", color = Text, fontSize = 31.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, "Instellingen", tint = Text, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        CardBlock {
            Icon(Icons.Filled.GraphicEq, null, tint = Gold, modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            Text("Nu afspelen", color = Muted, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(nowPlaying.station.ifBlank { "HoefVinyl Radio Live" }, color = Text, fontSize = 29.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            if (nowPlaying.artist.isNotBlank()) Text(nowPlaying.artist, color = Gold, fontSize = 19.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            if (nowPlaying.title.isNotBlank()) Text(nowPlaying.title, color = Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onFavorites,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
            ) {
                Icon(Icons.Outlined.FavoriteBorder, null)
                Spacer(Modifier.width(10.dp))
                Text("Zender aan favorieten toevoegen", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Snel kiezen", color = Text, fontSize = 29.sp)
        Spacer(Modifier.height(16.dp))
        QuickRow(
            QuickTile(Icons.Filled.Radio, "Internet Radio") { onRadio() },
            QuickTile(Icons.Filled.Storage, "Muziekserver") { onSource("SISERVER","Muziekserver") }
        )
        Spacer(Modifier.height(12.dp))
        QuickRow(
            QuickTile(Icons.Filled.Usb, "USB") { onSource("SIUSB","USB") },
            QuickTile(Icons.Filled.Star, "Favorieten") { onFavorites() }
        )
    }
}

private data class QuickTileData(val icon: ImageVector, val label: String, val onClick: () -> Unit)
private fun QuickTile(icon: ImageVector, label: String, onClick: () -> Unit) = QuickTileData(icon,label,onClick)

@Composable
private fun QuickRow(left: QuickTileData, right: QuickTileData) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        QuickCard(left, Modifier.weight(1f)); QuickCard(right, Modifier.weight(1f))
    }
}

@Composable
private fun QuickCard(item: QuickTileData, modifier: Modifier) {
    Surface(modifier = modifier.height(162.dp).clickable { item.onClick() }, shape = RoundedCornerShape(28.dp), color = CardBg) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(item.icon, null, tint = Gold, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text(item.label, color = Gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RadioScreen(nowPlaying: MarantzClient.NowPlaying, onPower: (Boolean)->Unit, onRefresh:()->Unit) {
    Page {
        Text("Radio", color = Text, fontSize = 31.sp); Spacer(Modifier.height(26.dp))
        CardBlock {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GoldButton("NA8005 aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f)) { onPower(true) }
                GoldButton("NA8005 uit", Icons.Filled.PowerOff, Modifier.weight(1f)) { onPower(false) }
            }
        }
        Spacer(Modifier.height(24.dp))
        CardBlock {
            Text("Nu op de radio", color = Muted, fontSize = 20.sp); Spacer(Modifier.height(10.dp))
            Text(nowPlaying.station.ifBlank { "HoefVinyl Radio Live" }, color = Text, fontSize = 30.sp)
            if (nowPlaying.artist.isNotBlank()) Text(nowPlaying.artist, color = Gold, fontSize = 23.sp)
            if (nowPlaying.title.isNotBlank()) Text(nowPlaying.title, color = Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            GoldButton("Zender bewaren", Icons.Outlined.FavoriteBorder, Modifier.fillMaxWidth()) {}
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlineAction("Vorige", Icons.Filled.SkipPrevious, Modifier.weight(1f)) {}
                OutlineAction("Volgende", Icons.Filled.SkipNext, Modifier.weight(1f)) {}
            }
        }
        Spacer(Modifier.height(24.dp))
        CardBlock {
            Text("Zoek radiostation", color = Text, fontSize = 27.sp); Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = "", onValueChange = {}, enabled = false,
                placeholder = { Text("Naam of zoekwoord", color = Muted) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4B50), contentColor = Muted),
                shape = RoundedCornerShape(30.dp)) {
                Icon(Icons.Filled.Search, null); Spacer(Modifier.width(8.dp)); Text("Zoeken bij vTuner")
            }
        }
    }
}

@Composable
private fun FavoritesScreen(
    favorites: List<MarantzClient.Favorite>, onRefresh:()->Unit,
    onPlay:(MarantzClient.Favorite)->Unit, onDelete:(MarantzClient.Favorite)->Unit
) {
    Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 30.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Marantz-favorieten", color = Text, fontSize = 31.sp)
                Text("Tik op een station om het direct af te spelen.", color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Vernieuwen", tint = Text, modifier = Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(22.dp))
        if (favorites.isEmpty()) Text("Er staan nog geen favorieten in de NA8005.", color = Muted, fontSize = 20.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(favorites, key = { it.position + it.name }) { fav ->
                Surface(modifier = Modifier.fillMaxWidth().height(132.dp).clickable { onPlay(fav) },
                    color = CardBg, shape = RoundedCornerShape(28.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fav.position, color = Gold, fontSize = 19.sp, modifier = Modifier.width(54.dp))
                        Icon(Icons.Filled.Radio, null, tint = Gold, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(fav.name, color = Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("Tik om af te spelen", color = Muted, fontSize = 18.sp)
                        }
                        IconButton(onClick = { onDelete(fav) }) {
                            Icon(Icons.Filled.Delete, "Verwijderen", tint = Text, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmplifierScreen(onCommand:(String,String)->Unit, onAll:(Boolean)->Unit) {
    Page {
        Text("PM8003 versterker", color = Text, fontSize = 31.sp); Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GoldButton("Alles aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f)) { onAll(true) }
            OutlineAction("Alles uit", Icons.Filled.PowerOff, Modifier.weight(1f)) { onAll(false) }
        }
        Spacer(Modifier.height(22.dp)); Text("Alleen PM8003", color = Text, fontSize = 27.sp); Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GoldButton("Aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f)) { onCommand("POWER_ON","Aan-opdracht naar PM8003 verzonden") }
            GoldButton("Stand-by", Icons.Filled.PowerOff, Modifier.weight(1f)) { onCommand("POWER_OFF","Stand-by-opdracht naar PM8003 verzonden") }
        }
        Spacer(Modifier.height(24.dp))
        CardBlock {
            Text("Volume", color = Text, fontSize = 28.sp); Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                GoldButton("Zachter", Icons.Filled.VolumeDown, Modifier.weight(1f)) { onCommand("VOLUME_DOWN","Volume zachter") }
                GoldButton("Harder", Icons.Filled.VolumeUp, Modifier.weight(1f)) { onCommand("VOLUME_UP","Volume harder") }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                GoldButton("Dempen", Icons.Filled.VolumeOff, Modifier.weight(1f)) { onCommand("MUTE_ON","Dempen-opdracht verzonden") }
                GoldButton("Geluid aan", Icons.Filled.VolumeUp, Modifier.weight(1f)) { onCommand("MUTE_OFF","Geluid-aan-opdracht verzonden") }
            }
        }
        Spacer(Modifier.height(26.dp)); Text("Ingang kiezen", color = Text, fontSize = 28.sp); Spacer(Modifier.height(18.dp))
        QuickRow(QuickTile(Icons.Filled.Album, "CD") { onCommand("INPUT_CD","PM8003 ingang CD") },
            QuickTile(Icons.Filled.Radio, "Tuner") { onCommand("INPUT_TUNER","PM8003 ingang Tuner") })
        Spacer(Modifier.height(12.dp))
        QuickRow(QuickTile(Icons.Filled.Cable, "AUX") { onCommand("INPUT_AUX","PM8003 ingang AUX") },
            QuickTile(Icons.Filled.Album, "Phono") { onCommand("INPUT_PHONO","PM8003 ingang Phono") })
    }
}

@Composable
private fun SourcesScreen(ip:String,onSelect:(String,String)->Unit) {
    Page {
        Text("Bronnen", color = Text, fontSize = 31.sp); Spacer(Modifier.height(24.dp))
        Text("Kies wat je wilt beluisteren op $ip", color = Muted, fontSize = 18.sp); Spacer(Modifier.height(24.dp))
        val rows = listOf(
            QuickTile(Icons.Filled.Radio,"Internet Radio"){onSelect("SIIRADIO","Internet Radio")} to QuickTile(Icons.Filled.Storage,"Muziekserver / NAS"){onSelect("SISERVER","Muziekserver / NAS")},
            QuickTile(Icons.Filled.Usb,"USB"){onSelect("SIUSB","USB")} to QuickTile(Icons.Filled.Star,"Favorieten"){onSelect("FAVORITES","Favorieten")},
            QuickTile(Icons.Filled.MusicNote,"Spotify"){onSelect("SISPOTIFY","Spotify")} to QuickTile(Icons.Filled.Cable,"Digitaal coax"){onSelect("SICOAXIAL","Digitaal coax")},
            QuickTile(Icons.Filled.GraphicEq,"Digitaal optisch"){onSelect("SIOPTICAL","Digitaal optisch")} to QuickTile(Icons.Filled.Computer,"USB-DAC"){onSelect("SIUSBDAC","USB-DAC")}
        )
        rows.forEachIndexed { i,p -> QuickRow(p.first,p.second); if (i < rows.lastIndex) Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CardBlock(content:@Composable ColumnScope.()->Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = CardBg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Composable
private fun GoldButton(text:String, icon:ImageVector, modifier:Modifier=Modifier, onClick:()->Unit) {
    Button(onClick = onClick, modifier = modifier.height(108.dp), shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Purple)) {
        Icon(icon,null,modifier=Modifier.size(31.dp)); Spacer(Modifier.width(12.dp))
        Text(text,fontSize=18.sp,fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun OutlineAction(text:String, icon:ImageVector, modifier:Modifier=Modifier, onClick:()->Unit) {
    OutlinedButton(onClick=onClick, modifier=modifier.height(108.dp), shape=RoundedCornerShape(30.dp),
        colors=ButtonDefaults.outlinedButtonColors(contentColor=Gold)) {
        Icon(icon,null,modifier=Modifier.size(30.dp)); Spacer(Modifier.width(10.dp))
        Text(text,fontSize=18.sp,fontWeight=FontWeight.Bold)
    }
}
