package com.chainpay.app

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class SepoliaReceipt(
    val transactionHash: String,
    val fromAddress: String,
    val toAddress: String,
    val status: String
)

object SepoliaReceiptVerifier {

    private const val RPC_URL =
        "https://ethereum-sepolia-rpc.publicnode.com"

    fun getReceipt(
        transactionHash: String
    ): SepoliaReceipt? {

        val connection =
            URL(RPC_URL)
                .openConnection()
                as HttpURLConnection

        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                15000

            connection.doOutput =
                true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val request =
                JSONObject()
                    .put(
                        "jsonrpc",
                        "2.0"
                    )
                    .put(
                        "id",
                        1
                    )
                    .put(
                        "method",
                        "eth_getTransactionReceipt"
                    )
                    .put(
                        "params",
                        JSONArray()
                            .put(
                                transactionHash
                            )
                    )

            connection
                .outputStream
                .bufferedWriter()
                .use {
                    writer ->

                    writer.write(
                        request.toString()
                    )
                }

            val responseCode =
                connection.responseCode

            if (
                responseCode !in 200..299
            ) {
                throw IllegalStateException(
                    "Sepolia RPC returned HTTP $responseCode."
                )
            }

            val response =
                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val json =
                JSONObject(
                    response
                )

            if (
                json.has(
                    "error"
                )
            ) {

                throw IllegalStateException(
                    json
                        .getJSONObject(
                            "error"
                        )
                        .optString(
                            "message",
                            "Sepolia RPC error."
                        )
                )
            }

            if (
                json.isNull(
                    "result"
                )
            ) {
                return null
            }

            val result =
                json.getJSONObject(
                    "result"
                )

            return SepoliaReceipt(
                transactionHash =
                    result.optString(
                        "transactionHash",
                        transactionHash
                    ),
                fromAddress =
                    result.optString(
                        "from"
                    ),
                toAddress =
                    result.optString(
                        "to"
                    ),
                status =
                    result.optString(
                        "status"
                    )
            )

        } finally {

            connection.disconnect()
        }
    }
}
