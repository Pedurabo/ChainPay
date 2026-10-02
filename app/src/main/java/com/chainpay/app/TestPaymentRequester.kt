package com.chainpay.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import java.math.BigDecimal

object TestPaymentRequester {

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val ethereumAddressRegex =
        Regex(
            "^0x[a-fA-F0-9]{40}$"
        )

    private fun showToast(
        context: Context,
        message: String
    ) {
        mainHandler.post {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun sendPayment(
        context: Context,
        walletAddress: String,
        recipientAddress: String,
        amountEth: String
    ) {

        val from =
            walletAddress.trim()

        val to =
            recipientAddress.trim()

        val amount =
            amountEth.trim()

        if (
            !ethereumAddressRegex.matches(
                from
            )
        ) {
            val message =
                "Connected wallet address is invalid."

            PaymentState.markError(
                message
            )

            showToast(
                context,
                message
            )

            return
        }

        if (
            !ethereumAddressRegex.matches(
                to
            )
        ) {
            val message =
                "Enter a valid Ethereum recipient address."

            PaymentState.markError(
                message
            )

            showToast(
                context,
                message
            )

            return
        }

        val wei =
            try {

                val eth =
                    BigDecimal(
                        amount
                    )

                if (
                    eth <= BigDecimal.ZERO
                ) {
                    throw IllegalArgumentException(
                        "Amount must be greater than zero."
                    )
                }

                eth
                    .movePointRight(18)
                    .toBigIntegerExact()

            } catch (
                error: Exception
            ) {

                val message =
                    error.message
                        ?: "Enter a valid ETH amount."

                PaymentState.markError(
                    message
                )

                showToast(
                    context,
                    message
                )

                return
            }

        val valueHex =
            "0x" +
                wei.toString(16)

        val params =
            """[{"from":"$from","to":"$to","value":"$valueHex"}]"""

        val request =
            Request(
                method =
                    "eth_sendTransaction",
                params =
                    params,
                chainId =
                    "eip155:11155111"
            )

        PaymentState.markRequesting()

        try {

            AppKit.request(
                request = request,

                onSuccess = {
                        _: SentRequestResult ->

                    PaymentState
                        .markAwaitingWallet()

                    showToast(
                        context,
                        "Payment request sent to wallet."
                    )
                },

                onError = {
                        error: Throwable ->

                    val message =
                        "Payment request failed: " +
                            (
                                error.localizedMessage
                                    ?: "Unknown error."
                            )

                    PaymentState.markError(
                        message
                    )

                    showToast(
                        context,
                        message
                    )
                }
            )

        } catch (
            error: Exception
        ) {

            val message =
                "Payment request failed: " +
                    (
                        error.localizedMessage
                            ?: "Unknown error."
                    )

            PaymentState.markError(
                message
            )

            showToast(
                context,
                message
            )
        }
    }
}
