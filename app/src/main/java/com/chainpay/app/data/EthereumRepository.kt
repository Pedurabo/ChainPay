package com.chainpay.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class EthereumRepository {

    companion object {

        private const val RPC_URL =
            "https://ethereum-rpc.publicnode.com"

        private const val BLOCKSCOUT_URL =
            "https://eth.blockscout.com/api"

        private const val USDC_CONTRACT =
            "0xA0b86991c6218b36c1d19d4a2e9eb0cE3606eB48"

        const val NETWORK_NAME =
            "Ethereum Mainnet"
    }

    fun getBalance(
        address: String
    ): String {

        val requestBody =
            """
            {
              "jsonrpc": "2.0",
              "method": "eth_getBalance",
              "params": [
                "$address",
                "latest"
              ],
              "id": 1
            }
            """.trimIndent()

        val response =
            postJson(
                RPC_URL,
                requestBody
            )

        val json =
            JSONObject(response)

        checkRpcError(json)

        return weiHexToEth(
            json.optString("result")
        )
    }

    fun getUsdcBalance(
        address: String
    ): String {

        val cleanAddress =
            address
                .removePrefix("0x")
                .lowercase()

        val paddedAddress =
            cleanAddress
                .padStart(
                    64,
                    '0'
                )

        /*
         * ERC-20:
         *
         * balanceOf(address)
         *
         * Function selector:
         * 70a08231
         */
        val callData =
            "0x70a08231$paddedAddress"

        val requestBody =
            """
            {
              "jsonrpc": "2.0",
              "method": "eth_call",
              "params": [
                {
                  "to": "$USDC_CONTRACT",
                  "data": "$callData"
                },
                "latest"
              ],
              "id": 2
            }
            """.trimIndent()

        val response =
            postJson(
                RPC_URL,
                requestBody
            )

        val json =
            JSONObject(response)

        checkRpcError(json)

        val result =
            json.optString("result")

        if (
            result.isBlank() ||
            !result.startsWith("0x")
        ) {
            throw IllegalStateException(
                "Invalid USDC balance response."
            )
        }

        val hex =
            result.removePrefix("0x")

        val rawUnits =
            if (hex.isBlank()) {
                BigInteger.ZERO
            } else {
                BigInteger(
                    hex,
                    16
                )
            }

        /*
         * USDC uses 6 decimals.
         */
        return BigDecimal(rawUnits)
            .divide(
                BigDecimal.TEN.pow(6),
                6,
                RoundingMode.DOWN
            )
            .stripTrailingZeros()
            .toPlainString()
    }

    fun getRecentTransactions(
        address: String
    ): List<WalletTransaction> {

        val encodedAddress =
            URLEncoder.encode(
                address,
                "UTF-8"
            )

        val url =
            "$BLOCKSCOUT_URL" +
                "?module=account" +
                "&action=txlist" +
                "&address=$encodedAddress" +
                "&page=1" +
                "&offset=10" +
                "&sort=desc"

        val response =
            getJson(url)

        val json =
            JSONObject(response)

        val result =
            json.opt("result")

        if (result !is JSONArray) {
            throw IllegalStateException(
                json.optString(
                    "message",
                    "Transaction history unavailable."
                )
            )
        }

        val walletLower =
            address.lowercase()

        val transactions =
            mutableListOf<WalletTransaction>()

        for (
            index in 0 until result.length()
        ) {

            val item =
                result.getJSONObject(index)

            val from =
                item.optString("from")

            val to =
                item.optString("to")

            val valueWei =
                item.optString(
                    "value",
                    "0"
                )

            val timestamp =
                item.optString(
                    "timeStamp",
                    "0"
                )
                    .toLongOrNull()
                    ?: 0L

            transactions +=
                WalletTransaction(
                    hash =
                        item.optString("hash"),
                    from = from,
                    to = to,
                    valueEth =
                        decimalWeiToEth(
                            valueWei
                        ),
                    timestamp = timestamp,
                    isIncoming =
                        to.lowercase() ==
                            walletLower
                )
        }

        return transactions
    }

    private fun checkRpcError(
        json: JSONObject
    ) {

        if (json.has("error")) {

            throw IllegalStateException(
                json.getJSONObject("error")
                    .optString(
                        "message",
                        "Ethereum RPC error"
                    )
            )
        }
    }

    private fun postJson(
        url: String,
        body: String
    ): String {

        val connection =
            URL(url).openConnection()
                as HttpURLConnection

        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                15_000

            connection.doOutput =
                true

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.outputStream
                .bufferedWriter()
                .use {

                    it.write(body)
                    it.flush()
                }

            return readConnection(
                connection
            )

        } finally {

            connection.disconnect()
        }
    }

    private fun getJson(
        url: String
    ): String {

        val connection =
            URL(url).openConnection()
                as HttpURLConnection

        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                15_000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            return readConnection(
                connection
            )

        } finally {

            connection.disconnect()
        }
    }

    private fun readConnection(
        connection:
            HttpURLConnection
    ): String {

        val responseCode =
            connection.responseCode

        if (
            responseCode in 200..299
        ) {

            return connection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }
        }

        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.use {
                    it.readText()
                }

        throw IllegalStateException(
            "HTTP $responseCode" +
                if (
                    error.isNullOrBlank()
                ) {
                    ""
                } else {
                    ": $error"
                }
        )
    }

    private fun weiHexToEth(
        hexadecimalWei: String
    ): String {

        if (
            hexadecimalWei.isBlank() ||
            !hexadecimalWei.startsWith(
                "0x"
            )
        ) {
            throw IllegalStateException(
                "Invalid ETH balance."
            )
        }

        val hex =
            hexadecimalWei
                .removePrefix("0x")

        val wei =
            if (hex.isBlank()) {

                BigInteger.ZERO

            } else {

                BigInteger(
                    hex,
                    16
                )
            }

        return BigDecimal(wei)
            .divide(
                BigDecimal.TEN.pow(18),
                8,
                RoundingMode.DOWN
            )
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun decimalWeiToEth(
        decimalWei: String
    ): String {

        val wei =
            decimalWei
                .toBigDecimalOrNull()
                ?: BigDecimal.ZERO

        return wei
            .divide(
                BigDecimal.TEN.pow(18),
                8,
                RoundingMode.DOWN
            )
            .stripTrailingZeros()
            .toPlainString()
    }
}
