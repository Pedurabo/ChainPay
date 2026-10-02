package com.chainpay.app

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentLogicTest {

    private val address =
        "0xde99775d33bb815d0E16Dff38d2eeb6B8A45eD3F"

    @Test
    fun validEthereumAddressIsAccepted() {

        assertTrue(
            PaymentLogic
                .isValidEthereumAddress(
                    address
                )
        )
    }

    @Test
    fun invalidEthereumAddressIsRejected() {

        assertFalse(
            PaymentLogic
                .isValidEthereumAddress(
                    "0x1234"
                )
        )
    }

    @Test
    fun oneMicroEthConvertsToExactWei() {

        assertEquals(
            BigInteger(
                "1000000000000"
            ),
            PaymentLogic
                .ethToWei(
                    "0.000001"
                )
        )
    }

    @Test
    fun oneEthConvertsToExactWei() {

        assertEquals(
            BigInteger(
                "1000000000000000000"
            ),
            PaymentLogic
                .ethToWei(
                    "1"
                )
        )
    }

    @Test
    fun zeroIsRejected() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {

            PaymentLogic
                .ethToWei(
                    "0"
                )
        }
    }

    @Test
    fun negativeAmountIsRejected() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {

            PaymentLogic
                .ethToWei(
                    "-1"
                )
        }
    }

    @Test
    fun excessivePrecisionIsRejected() {

        assertThrows(
            ArithmeticException::class.java
        ) {

            PaymentLogic
                .ethToWei(
                    "0.0000000000000000001"
                )
        }
    }

    @Test
    fun weiHexIsCorrect() {

        assertEquals(
            "0xe8d4a51000",
            PaymentLogic
                .weiHex(
                    "0.000001"
                )
        )
    }

    @Test
    fun erc681SepoliaUriIsCorrect() {

        assertEquals(
            "ethereum:" +
                address +
                "@11155111" +
                "?value=1000000000000",
            PaymentLogic
                .erc681SepoliaUri(
                    recipientAddress =
                        address,
                    amountEth =
                        "0.000001"
                )
        )
    }
}
