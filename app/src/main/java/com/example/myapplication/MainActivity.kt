package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.componets.ControlButtonsLayout
import com.example.myapplication.ui.componets.IncomingCallComponent
import com.example.myapplication.ui.componets.SurfaceViewRendererComposable
import com.example.myapplication.ui.componets.WhoToCallLayout
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null
    private val myUserName = UUID.randomUUID().toString().substring(0, 2) // for random user id on every app start


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                val permissionRequestLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { permissions ->
                    if (permissions.all { it.value }) {
                        mainViewModel.init(myUserName)
                    }
                }
                LaunchedEffect(key1 = Unit) {
                    permissionRequestLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.RECORD_AUDIO,
                        )
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        val incomingCallState =
                            mainViewModel.incomingCallerSession.collectAsState(null)
                        if (incomingCallState.value != null) {
                            IncomingCallComponent(
                                incomingCallerName = incomingCallState.value?.name,
                                modifier = Modifier.weight(1f),
                                onAcceptPressed = mainViewModel::acceptCall,
                                onRejectPressed = mainViewModel::rejectCall)
                        } else {
                            WhoToCallLayout(
                                modifier = Modifier.weight(2f),
                                onStartCallButtonClicked = mainViewModel::startCall)
                        }
                        SurfaceViewRendererComposable(modifier = Modifier.weight(4f), onSurfaceReady = {remote ->
                            remoteSurfaceViewRenderer = remote
                            mainViewModel.setRemoteSurface(remote)
                        })
                        Spacer(
                            modifier = Modifier
                                .height(5.dp)
                                .background(color = Color.Gray)
                        )
                        SurfaceViewRendererComposable(modifier = Modifier.weight(4f), onSurfaceReady = {local ->
                            localSurfaceViewRenderer = local
                            mainViewModel.setLocalSurface(local)
                        })
                        ControlButtonsLayout(
                            modifier = Modifier.weight(1f),
                            onAudioButtonClicked = mainViewModel::audioButtonClicked,
                            onCameraButtonClicked = mainViewModel::videoButtonClicked,
                            onEndCallClicked = mainViewModel::onEndClicked,
                            onSwitchCameraClicked = mainViewModel::cameraSwitchClicked)


                    }

                }
            }
        }
    }
}

