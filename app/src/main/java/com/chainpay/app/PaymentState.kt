package com.chainpay.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PaymentStatus {
    IDLE,
    REQUESTING,
    AWAITING_WALLET,
    SUCCESS,
    ERROR
}

data class PaymentUiState(
    val status: PaymentStatus = PaymentStatus.IDLE,
    val message: String = "Ready to send a Sepolia payment.",
    val transactionHash: String? = null
) {
    val isPending: Boolean
        get() =
            status == PaymentStatus.REQUESTING ||
                status == PaymentStatus.AWAITING_WALLET
}

object PaymentState {

    private val _state =
        MutableStateFlow(
            PaymentUiState()
        )

    val state:
        StateFlow<PaymentUiState> =
        _state.asStateFlow()

    fun markRequesting() {
        _state.value =
            PaymentUiState(
                status = PaymentStatus.REQUESTING,
                message = "Preparing payment request..."
            )
    }

    fun markAwaitingWallet() {
        _state.value =
            PaymentUiState(
                status = PaymentStatus.AWAITING_WALLET,
                message = "Payment sent to wallet. Review and approve it."
            )
    }

    fun markSuccess(
        transactionHash: String
    ) {
        _state.value =
            PaymentUiState(
                status = PaymentStatus.SUCCESS,
                message = "Payment submitted successfully.",
                transactionHash = transactionHash
            )
    }

    fun markError(
        message: String
    ) {
        _state.value =
            PaymentUiState(
                status = PaymentStatus.ERROR,
                message = message
            )
    }

    fun reset() {
        _state.value =
            PaymentUiState()
    }
}

