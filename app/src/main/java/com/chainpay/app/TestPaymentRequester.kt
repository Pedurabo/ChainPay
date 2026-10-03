package com.chainpay.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult

object TestPaymentRequester {

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
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
            !PaymentLogic.isValidEthereumAddress(
                from
            )
        ) {
            val message =
                "Connected wallet address is invalid."

            PaymentState.markError(PaymentAsset.ETH,
                message
            )

            showToast(
                context,
                message
            )

            return
        }

        if (
            !PaymentLogic.isValidEthereumAddress(
                to
            )
        ) {
            val message =
                "Enter a valid Ethereum recipient address."

            PaymentState.markError(PaymentAsset.ETH,
                message
            )

            showToast(
                context,
                message
            )

            return
        }
        val valueHex =
            try {

                PaymentLogic
                    .weiHex(
                        amount
                    )

            } catch (
                error: Exception
            ) {

                val message =
                    "Enter a valid positive ETH amount."

                PaymentState.markError(PaymentAsset.ETH,
                    message
                )

                showToast(
                    context,
                    message
                )

                return
            }

        val params =
            """[{"from":"$from","to":"$to","value":"$valueHex"}]"""

        val request =
            Request(
                method =
                    "eth_sendTransaction",
                params =
                    params,
                chainId =
                    PaymentLogic.SEPOLIA_CAIP_CHAIN_ID
            )

        PaymentState.markRequesting(PaymentAsset.ETH)

        try {

            AppKit.request(
                request = request,

                onSuccess = {
                        _: SentRequestResult ->

                    PaymentState
                        .markAwaitingWallet(PaymentAsset.ETH, walletAddress)

                    showToast(
                        context,
                        "Payment request sent to wallet."
                    )
                },

                onError = {
                        error: Throwable ->

                    val message =
                        "Payment request could not be sent. Check the wallet connection and try again."

                    PaymentState.markError(PaymentAsset.ETH,
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
                        "Payment request could not be sent. Check the wallet connection and try again."

            PaymentState.markError(PaymentAsset.ETH,
                message
            )

            showToast(
                context,
                message
            )
        }
    }
}
