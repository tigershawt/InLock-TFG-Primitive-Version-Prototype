package com.jetbrains.kmpapp.utils

expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
}

