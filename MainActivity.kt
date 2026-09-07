package com.blazemusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import android.content.ComponentName
import android.net.Uri
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private data class Track(val uri: Uri, val name: String, val artist: String="Artiste inconnu")
private data class DeezerState(val connected: Boolean=false, val playlists: List<DeezerPlaylist> = emptyList(), val error: String? = null)

class MainActivity : ComponentActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val tracks = mutableStateListOf<Track>()
    private var deezerState by mutableStateOf(DeezerState())
    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); tracks.add(Track(uri, uri.lastPathSegment ?: "Morceau")) }
        if (uris.isNotEmpty()) playAll()
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        handleDeezerCallback(intent)
        controllerFuture = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        setContent { App() }
    }
    private fun playAll(){ controllerFuture.addListener({ val c=controllerFuture.get(); c.setMediaItems(tracks.map{MediaItem.fromUri(it.uri)}); c.prepare(); c.play() }, mainExecutor) }
    @Composable fun App(){
        var tab by remember { mutableStateOf("Songs") }
        var search by remember { mutableStateOf("") }
        MaterialTheme(colorScheme=darkColorScheme()) {
            Scaffold(containerColor=MaterialTheme.colorScheme.background) { pad ->
                Column(Modifier.padding(pad).padding(horizontal=16.dp)) {
                    Spacer(Modifier.height(12.dp)); Text("BlazeMusic", style=MaterialTheme.typography.headlineMedium); Text("Lecteur musical local", color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(vertical=12.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("Songs","Albums","Artists","Playlists","Deezer","Settings").forEach{ FilterChip(selected=tab==it,onClick={tab=it},label={Text(it)}) } }
                    OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),placeholder={Text("Rechercher…")},singleLine=true)
                    Spacer(Modifier.height(10.dp))
                    if(tab=="Songs") { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({pickAudio.launch(arrayOf("audio/*"))}){Text("＋ Importer")}; Button({playAll()}){Text("▶ Tout lire")} }
                        LazyColumn(Modifier.fillMaxSize()){items(tracks.filter{it.name.contains(search,true)}){t->ListItem(headlineContent={Text(t.name)},supportingContent={Text(t.artist)},modifier=Modifier.fillMaxWidth())}}
                    } else when(tab){
                        "Albums"->Empty("Les albums seront regroupés automatiquement depuis les métadonnées audio.")
                        "Artists"->Empty("Les artistes seront indexés depuis les métadonnées.")
                        "Playlists"->Empty("Crée et organise tes playlists ici.")
                        "Deezer"->DeezerPanel()
                        else->SettingsPanel()
                    }
                }
            }
        }
    }
    @Composable fun Empty(s:String){Column(Modifier.fillMaxSize().padding(top=40.dp)){Text(s,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
    private fun connectDeezer() {
        if (DeezerConfig.CLIENT_ID.isBlank()) {
            deezerState = DeezerState(error = "Ajoute ton App ID Deezer dans DeezerClient.kt avant de te connecter.")
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, DeezerClient.authorizationUri()))
    }

    private fun handleDeezerCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "blazemusic" && data.host == "deezer" && data.path == "/callback") {
            val code = data.getQueryParameter("code")
            if (code != null) lifecycleScope.launch {
                runCatching {
                    val token = DeezerClient.exchangeCode(code)
                    getSharedPreferences("deezer", MODE_PRIVATE).edit().putString("token", token).apply()
                    DeezerClient.playlists(token)
                }.onSuccess { lists -> deezerState = DeezerState(true, lists) }
                 .onFailure { deezerState = DeezerState(error = "Connexion Deezer impossible : ${it.message}") }
            } else {
                deezerState = DeezerState(error = data.getQueryParameter("error_reason") ?: "Autorisation Deezer refusée")
            }
        }
    }

    @Composable fun DeezerPanel(){
        Column(Modifier.padding(top=18.dp)) {
            Text("Synchronisation Deezer",style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Connecte ton compte pour importer les playlists et leurs métadonnées. Les fichiers audio protégés de Deezer restent lus par Deezer et ne sont pas convertis en MP3.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button({connectDeezer()}){Text(if(deezerState.connected) "Reconnecter Deezer" else "Connecter Deezer")}
            deezerState.error?.let { Text(it, color=MaterialTheme.colorScheme.error, modifier=Modifier.padding(top=10.dp)) }
            if (deezerState.playlists.isNotEmpty()) {
                Spacer(Modifier.height(18.dp)); Text("Playlists synchronisées", style=MaterialTheme.typography.titleMedium)
                deezerState.playlists.forEach { Text("• ${it.title} — ${it.trackCount} titres", modifier=Modifier.padding(top=8.dp)) }
            }
        }
    }
    @Composable fun SettingsPanel(){Column(Modifier.padding(top=18.dp)){Text("Audio & lecture",style=MaterialTheme.typography.titleLarge);Text("Lecture en arrière-plan, vitesse, shuffle, répétition et contrôles système sont pris en charge par Media3.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(18.dp));Text("Égaliseur 10 bandes",style=MaterialTheme.typography.titleMedium);(0 until 10).forEach{Slider(value=0.5f,onValueChange={})}}}
}
