package com.handydev.financier.protocol

interface IOTransactionDeleteListener {
    fun afterDeletingTransaction(id: Long)
}