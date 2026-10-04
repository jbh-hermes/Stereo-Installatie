package nl.hoefvinyl.stereoinstallatie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import nl.hoefvinyl.stereoinstallatie.ui.StereoInstallatieApp
import nl.hoefvinyl.stereoinstallatie.ui.theme.StereoInstallatieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StereoInstallatieTheme {
                StereoInstallatieApp()
            }
        }
    }
}
