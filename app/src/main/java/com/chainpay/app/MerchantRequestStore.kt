package com.chainpay.app

import android.content.Context

data class PersistedMerchantRequest(
    val active: Boolean = false,
    val merchantAddress: String = "",
    val requestedAmount: String = "",
    val paymentNote: String = "",
    val createdAtSeconds: Long = 0L,
    val submittedTransactionHash: String? = null,
    val receivedTransactionHash: String? = null,
    val receivedFromAddress: String? = null
)

object MerchantRequestStore {

    private const val PREFS =
        "chainpay_merchant_request"

    private const val KEY_ACTIVE =
        "active"

    private const val KEY_ADDRESS =
        "merchant_address"

    private const val KEY_AMOUNT =
        "requested_amount"

    private const val KEY_NOTE =
        "payment_note"

    private const val KEY_CREATED_AT =
        "created_at"

    private const val KEY_SUBMITTED_TX =
        "submitted_tx"

    private const val KEY_RECEIVED_TX =
        "received_tx"

    private const val KEY_FROM =
        "from_address"

    private const val KEY_AWAITING_WALLET_HASH =
        "awaiting_wallet_hash"

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun load(
        context: Context
    ): PersistedMerchantRequest {

        val prefs =
            prefs(
                context
            )

        return PersistedMerchantRequest(
            active =
                prefs.getBoolean(
                    KEY_ACTIVE,
                    false
                ),
            merchantAddress =
                prefs.getString(
                    KEY_ADDRESS,
                    ""
                ) ?: "",
            requestedAmount =
                prefs.getString(
                    KEY_AMOUNT,
                    ""
                ) ?: "",
            paymentNote =
                prefs.getString(
                    KEY_NOTE,
                    ""
                ) ?: "",
            createdAtSeconds =
                prefs.getLong(
                    KEY_CREATED_AT,
                    0L
                ),
            submittedTransactionHash =
                prefs.getString(
                    KEY_SUBMITTED_TX,
                    null
                ),
            receivedTransactionHash =
                prefs.getString(
                    KEY_RECEIVED_TX,
                    null
                ),
            receivedFromAddress =
                prefs.getString(
                    KEY_FROM,
                    null
                )
        )
    }

    fun saveRequest(
        context: Context,
        merchantAddress: String,
        requestedAmount: String,
        paymentNote: String,
        createdAtSeconds: Long
    ) {

        prefs(context)
            .edit()
            .putBoolean(
                KEY_ACTIVE,
                true
            )
            .putString(
                KEY_ADDRESS,
                merchantAddress
            )
            .putString(
                KEY_AMOUNT,
                requestedAmount
            )
            .putString(
                KEY_NOTE,
                paymentNote
            )
            .putLong(
                KEY_CREATED_AT,
                createdAtSeconds
            )
            .putBoolean(
                KEY_AWAITING_WALLET_HASH,
                false
            )
            .remove(
                KEY_SUBMITTED_TX
            )
            .remove(
                KEY_RECEIVED_TX
            )
            .remove(
                KEY_FROM
            )
            .apply()
    }

    fun armForWalletResponse(
        context: Context
    ) {

        prefs(context)
            .edit()
            .putBoolean(
                KEY_AWAITING_WALLET_HASH,
                true
            )
            .remove(
                KEY_SUBMITTED_TX
            )
            .apply()
    }

    fun saveSubmittedTransactionHash(
        context: Context,
        transactionHash: String
    ) {

        val prefs =
            prefs(
                context
            )

        val active =
            prefs.getBoolean(
                KEY_ACTIVE,
                false
            )

        val awaiting =
            prefs.getBoolean(
                KEY_AWAITING_WALLET_HASH,
                false
            )

        if (
            !active ||
            !awaiting
        ) {
            return
        }

        prefs
            .edit()
            .putString(
                KEY_SUBMITTED_TX,
                transactionHash
            )
            .putBoolean(
                KEY_AWAITING_WALLET_HASH,
                false
            )
            .apply()
    }

    fun saveReceivedPayment(
        context: Context,
        transactionHash: String,
        fromAddress: String
    ) {

        prefs(context)
            .edit()
            .putString(
                KEY_RECEIVED_TX,
                transactionHash
            )
            .putString(
                KEY_FROM,
                fromAddress
            )
            .apply()
    }

    fun clear(
        context: Context
    ) {

        prefs(context)
            .edit()
            .clear()
            .apply()
    }
}
