package ru.orangesoftware.main.protocol

interface IOTransactionDeleteListener {
    fun afterDeletingTransaction(id: Long)
}