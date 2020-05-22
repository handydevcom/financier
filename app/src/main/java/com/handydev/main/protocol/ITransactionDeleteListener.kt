package com.handydev.main.protocol

interface IOTransactionDeleteListener {
    fun afterDeletingTransaction(id: Long)
}