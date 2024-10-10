package com.example.myapplication.socket

import com.example.myapplication.model.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message:MessageModel)
}