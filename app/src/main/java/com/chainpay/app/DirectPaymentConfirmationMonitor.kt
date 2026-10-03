package com.chainpay.app

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object DirectPaymentConfirmationMonitor {

    private const val POLL_INTERVAL_MS =
        10_000L

    // 180 x 10 seconds = approximately 30 minutes.
    private const val MAX_ATTEMPTS =
        180

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    private val activeHashes =
        ConcurrentHashMap
            .newKeySet<String>()

    fun confirm(
        transactionHash: String
    ) {
        if (
            !activeHashes.add(
                transactionHash
            )
        ) {
            return
        }

        scope.launch {

            try {

                repeat(
                    MAX_ATTEMPTS
                ) {

                    try {

                        val receipt =
                            SepoliaReceiptVerifier
                                .getReceipt(
                                    transactionHash
                                )

                        if (
                            receipt ==
                            null
                        ) {

                            delay(
                                POLL_INTERVAL_MS
                            )

                            return@repeat
                        }

                        if (
                            receipt.status ==
                            "0x1"
                        ) {

                            PaymentState
                                .markConfirmed(
                                    transactionHash
                                )

                        } else {

                            PaymentState
                                .markReverted(
                                    transactionHash
                                )
                        }

                        return@launch

                    } catch (
                        _: Exception
                    ) {

                        delay(
                            POLL_INTERVAL_MS
                        )
                    }
                }

                PaymentState
                    .markConfirmationTimedOut(
                        transactionHash
                    )

            } finally {

                activeHashes.remove(
                    transactionHash
                )
            }
        }
    }
}
