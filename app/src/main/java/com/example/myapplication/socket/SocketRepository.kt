package com.example.myapplication.socket

import android.util.Log
import com.example.myapplication.model.MessageModel
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketRepository @Inject constructor() {
    private var webSocket:WebSocketClient?=null
    private var userName:String?= null
    private val TAG="socketRepository"
    private val gson=Gson()

    fun initSocket(username:String,messageInterface: NewMessageInterface){
        userName=username
        //if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        //if you are using your phone as emulator your local address, use cmd and then write ipconfig
        // and get your ethernet ipv4 , mine is : "ws://192.168.1.4:3000"
        //but if your websocket is deployed you add your websocket address here
        webSocket = object :WebSocketClient(URI("ws://192.168.1.4:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
               sendMessageToSocket(MessageModel(
                   "store_user",userName,null,null
               ))
            }

            override fun onMessage(message: String?) {
                try {
                    messageInterface.onNewMessage(gson.fromJson(message,MessageModel::class.java))
                }catch (e:Exception){
                        e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG,"onClose: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(TAG,"onError: $ex")
            }

        }
        webSocket?.connect()

    }

     fun sendMessageToSocket(message: MessageModel) {
        try {
            webSocket?.send(Gson().toJson(message))
        }catch (e:Exception){
            Log.d(TAG,"sendMessageToSocket $e")
        }


    }
}