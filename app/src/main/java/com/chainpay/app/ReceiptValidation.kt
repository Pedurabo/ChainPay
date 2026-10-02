package com.chainpay.app

object ReceiptValidation {

    fun isSuccessfulPaymentToMerchant(
        receipt: SepoliaReceipt,
        expectedMerchantAddress: String
    ): Boolean {

        if (
            receipt.status !=
                "0x1"
        ) {
            return false
        }

        return receipt
            .toAddress
            .trim()
            .equals(
                expectedMerchantAddress.trim(),
                ignoreCase = true
            )
    }
}
