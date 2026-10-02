package com.chainpay.app

import java.math.BigDecimal
import java.math.BigInteger

object PaymentLogic {

    const val SEPOLIA_CAIP_CHAIN_ID =
        "eip155:11155111"

    private val ethereumAddressRegex =
        Regex(
            "^0x[a-fA-F0-9]{40}$"
        )

    fun isValidEthereumAddress(
        address: String
    ): Boolean {

        return ethereumAddressRegex
            .matches(
                address.trim()
            )
    }

    fun ethToWei(
        amountEth: String
    ): BigInteger {

        val amount =
            BigDecimal(
                amountEth.trim()
            )

        require(
            amount > BigDecimal.ZERO
        ) {
            "Amount must be greater than zero."
        }

        return amount
            .movePointRight(18)
            .toBigIntegerExact()
    }

    fun weiHex(
        amountEth: String
    ): String {

        return "0x" +
            ethToWei(
                amountEth
            ).toString(16)
    }

    fun erc681SepoliaUri(
        recipientAddress: String,
        amountEth: String
    ): String {

        val recipient =
            recipientAddress.trim()

        require(
            isValidEthereumAddress(
                recipient
            )
        ) {
            "Invalid Ethereum recipient address."
        }

        return "ethereum:" +
            recipient +
            "@11155111" +
            "?value=" +
            ethToWei(
                amountEth
            )
    }
}
