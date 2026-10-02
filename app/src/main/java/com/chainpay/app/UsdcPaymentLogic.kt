package com.chainpay.app

import java.math.BigDecimal
import java.math.BigInteger

object UsdcPaymentLogic {

    const val SEPOLIA_USDC_CONTRACT =
        "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238"

    const val USDC_DECIMALS =
        6

    private const val TRANSFER_SELECTOR =
        "a9059cbb"

    fun usdcToBaseUnits(
        amountUsdc: String
    ): BigInteger {

        val amount =
            BigDecimal(
                amountUsdc.trim()
            )

        require(
            amount > BigDecimal.ZERO
        ) {
            "Amount must be greater than zero."
        }

        return amount
            .movePointRight(
                USDC_DECIMALS
            )
            .toBigIntegerExact()
    }

    fun transferData(
        recipientAddress: String,
        amountUsdc: String
    ): String {

        val recipient =
            recipientAddress.trim()

        require(
            PaymentLogic
                .isValidEthereumAddress(
                    recipient
                )
        ) {
            "Invalid Ethereum recipient address."
        }

        val recipientWord =
            recipient
                .removePrefix(
                    "0x"
                )
                .lowercase()
                .padStart(
                    64,
                    '0'
                )

        val amountWord =
            usdcToBaseUnits(
                amountUsdc
            )
                .toString(
                    16
                )
                .padStart(
                    64,
                    '0'
                )

        return "0x" +
            TRANSFER_SELECTOR +
            recipientWord +
            amountWord
    }
}
