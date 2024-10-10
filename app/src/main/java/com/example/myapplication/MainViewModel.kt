package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.IceCandidateModel
import com.example.myapplication.model.MessageModel
import com.example.myapplication.socket.NewMessageInterface
import com.example.myapplication.socket.SocketRepository
import com.example.myapplication.webrtc.PeerConnectionObserver
import com.example.myapplication.webrtc.RTCClient
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject


@SuppressLint("StaticFieldLeak")
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val socketRepository: SocketRepository

) : ViewModel(), NewMessageInterface {


    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null

    fun setLocalSurface(view: SurfaceViewRenderer) {
        this.localSurfaceViewRenderer = view
    }

    fun setRemoteSurface(view: SurfaceViewRenderer) {
        this.remoteSurfaceViewRenderer = view
    }

    private val gson = Gson()
    private var username: String? = ""
    private var rtcClient: RTCClient? = null
    private var target: String = ""
    val incomingCallerSession: MutableStateFlow<MessageModel?> = MutableStateFlow(null)


    fun init(username: String) {
        this.username = username
        socketRepository.initSocket(username, this)
        rtcClient =
            RTCClient(application, username, socketRepository, object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    rtcClient?.addIceCandidate(p0)
                    val candidate = hashMapOf(
                        "sdpMid" to p0?.sdpMid,
                        "sdpMLineIndex" to p0?.sdpMLineIndex,
                        "sdpCandidate" to p0?.sdp,
                    )
                    socketRepository.sendMessageToSocket(
                        MessageModel(
                            "ice_candidate", username, target, candidate
                        )
                    )
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)

                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    p0?.videoTracks?.get(0)?.addSink(remoteSurfaceViewRenderer)
                }

            })
        rtcClient?.initSurfaceView(localSurfaceViewRenderer!!)
        rtcClient?.initSurfaceView(remoteSurfaceViewRenderer!!)
        rtcClient?.startLocalVideo(localSurfaceViewRenderer!!)

    }

    fun startCall(target: String) {
        this.target = target
        socketRepository.sendMessageToSocket(
            MessageModel(
                "start_call", username, target, null
            )
        )
    }

    fun acceptCall() {
        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            incomingCallerSession.value?.data.toString()
        )
        target=incomingCallerSession.value?.name!!
        rtcClient?.onRemoteSessionReceived(session)
        rtcClient?.answer(incomingCallerSession.value?.name!!)
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            incomingCallerSession.emit(null)
        }
    }

    fun audioButtonClicked(boolean: Boolean) {
        rtcClient?.toggleAudio(boolean)
    }

    fun videoButtonClicked(boolean: Boolean) {
        rtcClient?.toggleCamera(boolean)
    }

    fun cameraSwitchClicked() {
        rtcClient?.switchCamera()
    }

    fun onEndClicked() {
        rtcClient?.endCall()
        remoteSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.clearImage()
        remoteSurfaceViewRenderer?.isVisible = false
    }


    override fun onCleared() {
        localSurfaceViewRenderer?.release()
        localSurfaceViewRenderer?.clearImage()
        localSurfaceViewRenderer = null
        remoteSurfaceViewRenderer?.release()
        remoteSurfaceViewRenderer?.clearImage()
        remoteSurfaceViewRenderer = null
        super.onCleared()
    }

    override fun onNewMessage(message: MessageModel) {
        CoroutineScope(Dispatchers.Main).launch {
            when (message.type) {
                "call_response" -> {
                    if (message.data == "user is not online") {
                        Toast.makeText(application, "user is not available", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        rtcClient?.call(target)
                    }
                }

                "offer_received" -> {
                    remoteSurfaceViewRenderer?.isVisible = true
                    viewModelScope.launch {
                        incomingCallerSession.emit(message)
                    }
                }

                "answer_received" -> {
                    val session = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        message.data.toString()
                    )
                    rtcClient?.onRemoteSessionReceived(session)
                }

                "ice_candidate" -> {
                    try {
                        val receivingCandidate =
                            gson.fromJson(gson.toJson(message.data), IceCandidateModel::class.java)
                        rtcClient?.addIceCandidate(
                            IceCandidate(
                                receivingCandidate.sdpMid,
                                Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                                receivingCandidate.sdpCandidate
                            )
                        )
                    }catch (e:Exception){
                        e.printStackTrace()
                    }

                }
            }

        }

    }


}