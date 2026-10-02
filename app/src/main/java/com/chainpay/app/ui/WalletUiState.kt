package com.chainpay.app.ui

import com.chainpay.app.data.WalletTransaction

data class WalletUiState(
    val walletAddress: String = "",
    val ethBalance: String = "--",
    val usdcBalance: String = "--",
    val network: String = "Ethereum Mainnet",
    val status: String =
        "Enter a wallet address to begin.",
    val isLoading: Boolean = false,
    val hasLoadedWallet: Boolean = false,
    val errorMessage: String? = null,
    val transactions:
        List<WalletTransaction> =
        emptyList()
)
