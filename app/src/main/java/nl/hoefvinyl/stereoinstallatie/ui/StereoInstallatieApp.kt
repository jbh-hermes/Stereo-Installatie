package nl.hoefvinyl.stereoinstallatie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.hoefvinyl.stereoinstallatie.ui.theme.*

private enum class Tab(val label:String, val icon:ImageVector) {
    START("Start", Icons.Filled.Home),
    RADIO("Radio", Icons.Filled.Radio),
    FAVORIETEN("Favorieten", Icons.Filled.Favorite),
    VERSTERKER("Versterker", Icons.Filled.Speaker),
    BRONNEN("Bronnen", Icons.Filled.Apps)
}

@Composable
fun StereoInstallatieApp() {
    var tab by remember { mutableStateOf(Tab.START) }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = NavigationBackground,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
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
                Tab.START -> StartScreen(onOpen = { tab = it })
                Tab.RADIO -> RadioScreen()
                Tab.FAVORIETEN -> FavoritesScreen()
                Tab.VERSTERKER -> AmplifierScreen()
                Tab.BRONNEN -> SourcesScreen(onOpen = { tab = it })
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
            .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(title, color = PrimaryText, fontSize = 30.sp, fontWeight = FontWeight.Normal)
        content()
    }
}

@Composable
private fun StartScreen(onOpen:(Tab)->Unit) {
    Page("Stereo-Installatie") {
        AppCard {
            Icon(Icons.Filled.GraphicEq, null, tint = Gold, modifier = Modifier.size(50.dp).align(Alignment.CenterHorizontally))
            Text("Nu afspelen", color = SecondaryText, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Nog geen station", color = PrimaryText, fontSize = 26.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Artiest", color = Gold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Titel", color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            VintageVuMeters()
        }
        Text("Snel kiezen", color = PrimaryText, fontSize = 26.sp)
        QuickGrid(
            listOf(
                Triple(Icons.Filled.Radio,"Internet Radio"){ onOpen(Tab.RADIO) },
                Triple(Icons.Filled.Storage,"Muziekserver"){ },
                Triple(Icons.Filled.Usb,"USB"){ },
                Triple(Icons.Filled.Star,"Favorieten"){ onOpen(Tab.FAVORIETEN) }
            )
        )
    }
}

@Composable
private fun RadioScreen() {
    Page("Radio") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoldAction("NA8005 aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f))
            GoldAction("NA8005 uit", Icons.Filled.PowerOff, Modifier.weight(1f))
        }
        AppCard {
            Text("Nu op de radio", color = SecondaryText, fontSize = 18.sp)
            Text("Nog geen station", color = PrimaryText, fontSize = 25.sp)
            Text("Artiest", color = Gold, fontSize = 20.sp)
            Text("Titel", color = PrimaryText, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineAction("Vorige", Icons.Filled.SkipPrevious, Modifier.weight(1f))
                OutlineAction("Volgende", Icons.Filled.SkipNext, Modifier.weight(1f))
            }
            GoldAction("Zender bewaren", Icons.Outlined.FavoriteBorder, Modifier.fillMaxWidth())
        }
        AppCard {
            Text("Zoek radiostation", color = PrimaryText, fontSize = 24.sp)
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Naam of zoekwoord") },
                modifier = Modifier.fillMaxWidth()
            )
            GoldAction("Zoeken", Icons.Filled.Search, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FavoritesScreen() {
    Page("Marantz-favorieten") {
        Text("50 slots • tik op een station om af te spelen", color = SecondaryText, fontSize = 16.sp)
        AppCard {
            repeat(5) { i ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("%02d".format(i+1), color = Gold, fontSize = 18.sp, modifier = Modifier.width(44.dp))
                    Icon(Icons.Filled.Radio, null, tint = Gold)
                    Spacer(Modifier.width(12.dp))
                    Text("Vrij slot", color = SecondaryText, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.DeleteOutline, null, tint = SecondaryText)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldAction("Toevoegen", Icons.Filled.Add, Modifier.weight(1f))
            OutlineAction("Vernieuwen", Icons.Filled.Refresh, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AmplifierScreen() {
    Page("PM8003 versterker") {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoldAction("Alles aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f))
            OutlineAction("Alles uit", Icons.Filled.PowerOff, Modifier.weight(1f))
        }
        Text("Alleen PM8003", color = PrimaryText, fontSize = 25.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GoldAction("Aan", Icons.Filled.PowerSettingsNew, Modifier.weight(1f))
            GoldAction("Stand-by", Icons.Filled.PowerOff, Modifier.weight(1f))
        }
        AppCard {
            Text("Volume", color = PrimaryText, fontSize = 26.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldAction("Zachter", Icons.Filled.VolumeDown, Modifier.weight(1f))
                GoldAction("Harder", Icons.Filled.VolumeUp, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldAction("Dempen", Icons.Filled.VolumeOff, Modifier.weight(1f))
                GoldAction("Geluid aan", Icons.Filled.VolumeUp, Modifier.weight(1f))
            }
        }
        Text("Ingang kiezen", color = PrimaryText, fontSize = 25.sp)
        QuickGrid(
            listOf(
                Triple(Icons.Filled.Album,"CD"){},
                Triple(Icons.Filled.Radio,"Tuner"){},
                Triple(Icons.Filled.Cable,"AUX"){},
                Triple(Icons.Filled.Album,"Phono"){}
            )
        )
    }
}

@Composable
private fun SourcesScreen(onOpen:(Tab)->Unit) {
    Page("Bronnen") {
        QuickGrid(
            listOf(
                Triple(Icons.Filled.Radio,"Internet Radio"){ onOpen(Tab.RADIO) },
                Triple(Icons.Filled.Storage,"Muziekserver / NAS"){},
                Triple(Icons.Filled.Usb,"USB"){},
                Triple(Icons.Filled.Star,"Favorieten"){ onOpen(Tab.FAVORIETEN) },
                Triple(Icons.Filled.MusicNote,"Spotify"){},
                Triple(Icons.Filled.Cable,"Digitaal coax"){},
                Triple(Icons.Filled.GraphicEq,"Digitaal optisch"){},
                Triple(Icons.Filled.Computer,"USB-DAC"){}
            )
        )
    }
}

@Composable
private fun AppCard(content:@Composable ColumnScope.()->Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(26.dp)
    ) {
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
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
private fun GoldAction(text:String, icon:ImageVector, modifier:Modifier = Modifier) {
    Button(
        onClick = {},
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = PurpleText)
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun OutlineAction(text:String, icon:ImageVector, modifier:Modifier = Modifier) {
    OutlinedButton(
        onClick = {},
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun VintageVuMeters() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Surface(
                modifier = Modifier.weight(1f).height(70.dp),
                color = MeterFace,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("−20   −10   −3   0   +3", color = MeterInk, fontSize = 10.sp)
                }
            }
        }
    }
}
