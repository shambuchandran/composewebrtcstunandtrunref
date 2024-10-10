package com.example.myapplication.model

data class MessageModel(
    val type:String,
    val name:String?=null,
    val target:String?=null,
    val data:Any?=null
)
