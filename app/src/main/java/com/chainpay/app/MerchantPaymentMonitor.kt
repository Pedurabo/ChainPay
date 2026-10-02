package com.chainpay.app

import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class MerchantPaymentMatch(
    val transactionHash: String,
    val fromAddress: String,
    val valueWei: String,
    val timestampSeconds: Long
)

object MerchantPaymentMonitor {

    private const val BASE_URL =
        "https://eth-sepolia.blockscout.com/api"

    fun findMatchingPayment(
        merchantAddress: String,
        requestedAmountEth: String,
        requestCreatedAtSeconds: Long
    ): MerchantPaymentMatch? {

        val requestedWei =
            BigDecimal(
                requestedAmountEth
            )
                .movePointRight(18)
                .toBigIntegerExact()
                .toString()

        val url =
            URL(
                "$BASE_URL" +
                    "?module=account" +
                    "&action=txlist" +
                    "&address=$merchantAddress" +
                    "&page=1" +
                    "&offset=25" +
                    "&sort=desc"
            )

        val connection =
            url.openConnection()
                as HttpURLConnection

        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                15000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val statusCode =
                connection.responseCode

            if (
                statusCode == 429
            ) {
                return null
            }

            if (
                statusCode !in 200..299
            ) {
                throw IllegalStateException(
                    "Blockscout returned HTTP $statusCode."
                )
            }

            val body =
                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val json =
                JSONObject(
                    body
                )

            val result =
                json.optJSONArray(
                    "result"
                )
                    ?: return null

            val normalizedMerchant =
                merchantAddress
                    .lowercase()

            for (
                index in 0 until result.length()
            ) {

                val transaction =
                    result.getJSONObject(
                        index
                    )

                val to =
                    transaction
                        .optString(
                            "to"
                        )
                        .lowercase()

                val valueWei =
                    transaction
                        .optString(
                            "value"
                        )

                val timestamp =
                    transaction
                        .optString(
                            "timeStamp"
                        )
                        .toLongOrNull()
                        ?: continue

                val isError =
                    transaction
                        .optString(
                            "isError"
                        )

                val receiptStatus =
                    transaction
                        .optString(
                            "txreceipt_status"
                        )

                val successful =
                    isError != "1" &&
                        receiptStatus != "0"

                val recentEnough =
                    timestamp >=
                        requestCreatedAtSeconds

                val matches =
                    to ==
                        normalizedMerchant &&
                        valueWei ==
                            requestedWei &&
                        successful &&
                        recentEnough

                if (
                    matches
                ) {

                    return MerchantPaymentMatch(
                        transactionHash =
                            transaction
                                .optString(
                                    "hash"
                                ),
                        fromAddress =
                            transaction
                                .optString(
                                    "from"
                                ),
                        valueWei =
                            valueWei,
                        timestampSeconds =
                            timestamp
                    )
                }
            }

            return null

        } finally {

            connection.disconnect()
        }
    }
}

