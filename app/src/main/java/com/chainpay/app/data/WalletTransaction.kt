package com.chainpay.app.data

data class WalletTransaction(
    val hash: String,
    val from: String,
    val to: String,
    val valueEth: String,
    val timestamp: Long,
    val isIncoming: Boolean
)
