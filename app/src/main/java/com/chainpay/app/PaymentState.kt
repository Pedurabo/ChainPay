package com.chainpay.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PaymentAsset {
    ETH,
    USDC
}

enum class PaymentStatus {
    IDLE,
    REQUESTING,
    AWAITING_WALLET,
    SUBMITTED,
    CONFIRMATION_TIMEOUT,
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
                status == PaymentStatus.AWAITING_WALLET ||
                status == PaymentStatus.SUBMITTED
}

object PaymentState {

    private const val PREFS =
        "chainpay_pending_payment"

    private const val KEY_ASSET =
        "asset"

    private const val KEY_WALLET =
        "wallet"

    private const val KEY_TIMESTAMP =
        "timestamp"

    private const val KEY_PHASE =
        "phase"

    private const val KEY_HASH =
        "transaction_hash"

    private const val MAX_PENDING_AGE_MS =
        30L * 60L * 1000L

    private const val MAX_SUBMITTED_AGE_MS =
        24L * 60L * 60L * 1000L

    private const val MAX_TIMED_OUT_AGE_MS =
        7L * 24L * 60L * 60L * 1000L

    private var applicationContext:
        Context? =
        null

    private var activeAsset:
        PaymentAsset? =
        null

    private fun initialEthState() =
        PaymentUiState(
            message = "Ready to send Sepolia ETH."
        )

    private fun initialUsdcState() =
        PaymentUiState(
            message = "Ready to send Sepolia USDC."
        )

    private val _ethState =
        MutableStateFlow(
            initialEthState()
        )

    val ethState:
        StateFlow<PaymentUiState> =
        _ethState.asStateFlow()

    private val _usdcState =
        MutableStateFlow(
            initialUsdcState()
        )

    val usdcState:
        StateFlow<PaymentUiState> =
        _usdcState.asStateFlow()

    fun initialize(
        context: Context
    ) {
        applicationContext =
            context.applicationContext
    }

    @Synchronized
    fun restorePending(
        walletAddress: String
    ) {
        val context =
            applicationContext
                ?: return

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val assetName =
            prefs.getString(
                KEY_ASSET,
                null
            )
                ?: return

        val persistedWallet =
            prefs.getString(
                KEY_WALLET,
                null
            )
                ?: run {
                    clearPersistedPending()
                    return
                }

        val timestamp =
            prefs.getLong(
                KEY_TIMESTAMP,
                0L
            )

        val phase =
            prefs.getString(
                KEY_PHASE,
                null
            )
                ?: "AWAITING_WALLET"

        val age =
            System.currentTimeMillis() -
                timestamp

        val maximumAge =
            when (
                phase
            ) {
                "SUBMITTED" ->
                    MAX_SUBMITTED_AGE_MS

                "TIMED_OUT" ->
                    MAX_TIMED_OUT_AGE_MS

                else ->
                    MAX_PENDING_AGE_MS
            }

        if (
            timestamp <= 0L ||
            age < 0L ||
            age > maximumAge ||
            !persistedWallet.equals(
                walletAddress,
                ignoreCase = true
            )
        ) {
            clearPersistedPending()
            return
        }

        val asset =
            try {
                PaymentAsset.valueOf(
                    assetName
                )
            } catch (
                _: IllegalArgumentException
            ) {
                clearPersistedPending()
                return
            }

        activeAsset =
            asset

        if (
            phase ==
            "TIMED_OUT"
        ) {

            val transactionHash =
                prefs.getString(
                    KEY_HASH,
                    null
                )

            if (
                transactionHash
                    .isNullOrBlank()
            ) {
                clearPersistedPending()
                activeAsset = null
                return
            }

            activeAsset =
                null

            update(
                asset,
                PaymentUiState(
                    status =
                        PaymentStatus.CONFIRMATION_TIMEOUT,
                    message =
                        "Recovered transaction confirmation timed out. The transaction may still confirm later; check the hash in the Sepolia explorer.",
                    transactionHash =
                        transactionHash
                )
            )

            return
        }
        if (
            phase ==
            "SUBMITTED"
        ) {

            val transactionHash =
                prefs.getString(
                    KEY_HASH,
                    null
                )

            if (
                transactionHash
                    .isNullOrBlank()
            ) {
                clearPersistedPending()
                activeAsset = null
                return
            }

            update(
                asset,
                PaymentUiState(
                    status =
                        PaymentStatus.SUBMITTED,
                    message =
                        when (asset) {
                            PaymentAsset.ETH ->
                                "Recovered submitted ETH payment. Waiting for Sepolia confirmation."

                            PaymentAsset.USDC ->
                                "Recovered submitted USDC payment. Waiting for Sepolia confirmation."
                        },
                    transactionHash =
                        transactionHash
                )
            )

            DirectPaymentConfirmationMonitor
                .confirm(
                    transactionHash
                )

            return
        }

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.AWAITING_WALLET,
                message =
                    when (asset) {
                        PaymentAsset.ETH ->
                            "Recovered pending ETH payment. Check your wallet for its current status."

                        PaymentAsset.USDC ->
                            "Recovered pending USDC payment. Check your wallet for its current status."
                    }
            )
        )
    }

    @Synchronized
    fun markRequesting(
        asset: PaymentAsset
    ) {
        activeAsset =
            asset

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.REQUESTING,
                message =
                    when (asset) {
                        PaymentAsset.ETH ->
                            "Preparing ETH payment request."

                        PaymentAsset.USDC ->
                            "Preparing USDC payment request."
                    }
            )
        )
    }

    @Synchronized
    fun markAwaitingWallet(
        asset: PaymentAsset,
        walletAddress: String
    ) {
        if (activeAsset != asset) {
            return
        }

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.AWAITING_WALLET,
                message =
                    when (asset) {
                        PaymentAsset.ETH ->
                            "ETH payment sent to wallet. Review and approve it."

                        PaymentAsset.USDC ->
                            "USDC payment sent to wallet. Review and approve it."
                    }
            )
        )

        persistPending(
            asset = asset,
            walletAddress = walletAddress
        )
    }

    @Synchronized
    fun markError(
        asset: PaymentAsset,
        message: String
    ) {
        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.ERROR,
                message =
                    message
            )
        )

        if (activeAsset == asset) {
            activeAsset = null
        }

        clearPersistedPending()
    }

    @Synchronized
    fun markSubmitted(
        transactionHash: String
    ) {
        val asset =
            activeAsset
                ?: return

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.SUBMITTED,
                message =
                    when (asset) {
                        PaymentAsset.ETH ->
                            "ETH transaction submitted. Waiting for Sepolia confirmation."

                        PaymentAsset.USDC ->
                            "USDC transaction submitted. Waiting for Sepolia confirmation."
                    },
                transactionHash =
                    transactionHash
            )
        )

        persistSubmitted(
            asset =
                asset,
            transactionHash =
                transactionHash
        )
    }

    @Synchronized
    fun markConfirmed(
        transactionHash: String
    ) {
        val asset =
            activeAsset
                ?: return

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.SUCCESS,
                message =
                    when (asset) {
                        PaymentAsset.ETH ->
                            "ETH payment confirmed on Sepolia."

                        PaymentAsset.USDC ->
                            "USDC payment confirmed on Sepolia."
                    },
                transactionHash =
                    transactionHash
            )
        )

        activeAsset =
            null

        clearPersistedPending()
    }

    @Synchronized
    fun markConfirmationTimedOut(
        transactionHash: String
    ) {
        val asset =
            activeAsset
                ?: return

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.CONFIRMATION_TIMEOUT,
                message =
                    "Confirmation timed out. The transaction may still confirm later; check the transaction hash in the Sepolia explorer.",
                transactionHash =
                    transactionHash
            )
        )

        persistTimedOut(
            asset =
                asset,
            transactionHash =
                transactionHash
        )

        activeAsset =
            null
    }
    @Synchronized
    fun markReverted(
        transactionHash: String
    ) {
        val asset =
            activeAsset
                ?: return

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.ERROR,
                message =
                    "Transaction was mined but reverted on Sepolia.",
                transactionHash =
                    transactionHash
            )
        )

        activeAsset =
            null

        clearPersistedPending()
    }
    @Synchronized
    fun markSuccess(
        transactionHash: String
    ) {
        val asset =
            activeAsset
                ?: return

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.SUCCESS,
                message =
                    when (asset) {
                        PaymentAsset.ETH ->
                            "ETH payment submitted successfully."

                        PaymentAsset.USDC ->
                            "USDC payment submitted successfully."
                    },
                transactionHash =
                    transactionHash
            )
        )

        activeAsset = null

        clearPersistedPending()
    }

    @Synchronized
    fun markError(
        message: String
    ) {
        val asset =
            activeAsset
                ?: run {
                    clearPersistedPending()
                    return
                }

        update(
            asset,
            PaymentUiState(
                status =
                    PaymentStatus.ERROR,
                message =
                    message
            )
        )

        activeAsset = null

        clearPersistedPending()
    }

    @Synchronized
    fun reset() {
        _ethState.value =
            initialEthState()

        _usdcState.value =
            initialUsdcState()

        activeAsset = null

        clearPersistedPending()
    }

    private fun persistPending(
        asset: PaymentAsset,
        walletAddress: String
    ) {
        val context =
            applicationContext
                ?: return

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_ASSET,
                asset.name
            )
            .putString(
                KEY_WALLET,
                walletAddress
            )
            .putString(
                KEY_PHASE,
                "AWAITING_WALLET"
            )
            .remove(
                KEY_HASH
            )
            .putLong(
                KEY_TIMESTAMP,
                System.currentTimeMillis()
            )
            .apply()
    }

    private fun persistSubmitted(
        asset: PaymentAsset,
        transactionHash: String
    ) {
        val context =
            applicationContext
                ?: return

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val walletAddress =
            prefs.getString(
                KEY_WALLET,
                null
            )
                ?: return

        prefs
            .edit()
            .putString(
                KEY_ASSET,
                asset.name
            )
            .putString(
                KEY_WALLET,
                walletAddress
            )
            .putString(
                KEY_PHASE,
                "SUBMITTED"
            )
            .putString(
                KEY_HASH,
                transactionHash
            )
            .putLong(
                KEY_TIMESTAMP,
                System.currentTimeMillis()
            )
            .apply()
    }

    private fun persistTimedOut(
        asset: PaymentAsset,
        transactionHash: String
    ) {
        val context =
            applicationContext
                ?: return

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val walletAddress =
            prefs.getString(
                KEY_WALLET,
                null
            )
                ?: return

        prefs
            .edit()
            .putString(
                KEY_ASSET,
                asset.name
            )
            .putString(
                KEY_WALLET,
                walletAddress
            )
            .putString(
                KEY_PHASE,
                "TIMED_OUT"
            )
            .putString(
                KEY_HASH,
                transactionHash
            )
            .putLong(
                KEY_TIMESTAMP,
                System.currentTimeMillis()
            )
            .apply()
    }
    private fun clearPersistedPending() {
        applicationContext
            ?.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            ?.edit()
            ?.clear()
            ?.apply()
    }

    private fun update(
        asset: PaymentAsset,
        state: PaymentUiState
    ) {
        when (asset) {
            PaymentAsset.ETH ->
                _ethState.value =
                    state

            PaymentAsset.USDC ->
                _usdcState.value =
                    state
        }
    }
}
