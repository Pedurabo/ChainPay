package com.chainpay.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptValidationTest {

    private val merchantAddress =
        "0xde99775d33bb815d0E16Dff38d2eeb6B8A45eD3F"

    @Test
    fun successfulReceiptToExpectedMerchantIsAccepted() {

        val receipt =
            SepoliaReceipt(
                transactionHash =
                    "0xabc",
                fromAddress =
                    "0x1111111111111111111111111111111111111111",
                toAddress =
                    merchantAddress,
                status =
                    "0x1"
            )

        assertTrue(
            ReceiptValidation
                .isSuccessfulPaymentToMerchant(
                    receipt = receipt,
                    expectedMerchantAddress =
                        merchantAddress
                )
        )
    }

    @Test
    fun failedReceiptIsRejected() {

        val receipt =
            SepoliaReceipt(
                transactionHash =
                    "0xabc",
                fromAddress =
                    "0x1111111111111111111111111111111111111111",
                toAddress =
                    merchantAddress,
                status =
                    "0x0"
            )

        assertFalse(
            ReceiptValidation
                .isSuccessfulPaymentToMerchant(
                    receipt = receipt,
                    expectedMerchantAddress =
                        merchantAddress
                )
        )
    }

    @Test
    fun receiptToWrongMerchantIsRejected() {

        val receipt =
            SepoliaReceipt(
                transactionHash =
                    "0xabc",
                fromAddress =
                    "0x1111111111111111111111111111111111111111",
                toAddress =
                    "0x2222222222222222222222222222222222222222",
                status =
                    "0x1"
            )

        assertFalse(
            ReceiptValidation
                .isSuccessfulPaymentToMerchant(
                    receipt = receipt,
                    expectedMerchantAddress =
                        merchantAddress
                )
        )
    }

    @Test
    fun merchantAddressComparisonIsCaseInsensitive() {

        val receipt =
            SepoliaReceipt(
                transactionHash =
                    "0xabc",
                fromAddress =
                    "0x1111111111111111111111111111111111111111",
                toAddress =
                    merchantAddress.lowercase(),
                status =
                    "0x1"
            )

        assertTrue(
            ReceiptValidation
                .isSuccessfulPaymentToMerchant(
                    receipt = receipt,
                    expectedMerchantAddress =
                        merchantAddress.uppercase()
                )
        )
    }
}
