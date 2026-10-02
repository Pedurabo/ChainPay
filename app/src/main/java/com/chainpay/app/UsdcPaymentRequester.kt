package com.chainpay.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult

object UsdcPaymentRequester {

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

    fun sendUsdcPayment(
        context: Context,
        walletAddress: String,
        recipientAddress: String,
        amountUsdc: String
    ) {

        val from =
            walletAddress.trim()

        val recipient =
            recipientAddress.trim()

        val amount =
            amountUsdc.trim()

        if (
            !PaymentLogic
                .isValidEthereumAddress(
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

        val transferData =
            try {

                UsdcPaymentLogic
                    .transferData(
                        recipientAddress =
                            recipient,
                        amountUsdc =
                            amount
                    )

            } catch (
                error: Exception
            ) {

                val message =
                    error.message
                        ?: "Enter a valid USDC payment."

                PaymentState.markError(
                    message
                )

                showToast(
                    context,
                    message
                )

                return
            }

        val params =
            """[{"from":"$from","to":"${UsdcPaymentLogic.SEPOLIA_USDC_CONTRACT}","value":"0x0","data":"$transferData"}]"""

        val request =
            Request(
                method =
                    "eth_sendTransaction",
                params =
                    params,
                chainId =
                    PaymentLogic
                        .SEPOLIA_CAIP_CHAIN_ID
            )

        PaymentState.markRequesting()

        try {

            AppKit.request(
                request =
                    request,

                onSuccess = {
                        _: SentRequestResult ->

                    PaymentState
                        .markAwaitingWallet()

                    showToast(
                        context,
                        "USDC payment request sent to wallet."
                    )
                },

                onError = {
                        error: Throwable ->

                    val message =
                        "USDC payment request failed: " +
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
                "USDC payment request failed: " +
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
