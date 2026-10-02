package com.chainpay.app

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UsdcPaymentLogicTest {

    private val recipient =
        "0xde99775d33bb815d0E16Dff38d2eeb6B8A45eD3F"

    @Test
    fun oneUsdcUsesSixDecimals() {

        assertEquals(
            BigInteger(
                "1000000"
            ),
            UsdcPaymentLogic
                .usdcToBaseUnits(
                    "1"
                )
        )
    }

    @Test
    fun fractionalUsdcConvertsExactly() {

        assertEquals(
            BigInteger(
                "1250000"
            ),
            UsdcPaymentLogic
                .usdcToBaseUnits(
                    "1.25"
                )
        )
    }

    @Test
    fun smallestUsdcUnitIsAccepted() {

        assertEquals(
            BigInteger.ONE,
            UsdcPaymentLogic
                .usdcToBaseUnits(
                    "0.000001"
                )
        )
    }

    @Test
    fun zeroUsdcIsRejected() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {

            UsdcPaymentLogic
                .usdcToBaseUnits(
                    "0"
                )
        }
    }

    @Test
    fun negativeUsdcIsRejected() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {

            UsdcPaymentLogic
                .usdcToBaseUnits(
                    "-1"
                )
        }
    }

    @Test
    fun moreThanSixDecimalsIsRejected() {

        assertThrows(
            ArithmeticException::class.java
        ) {

            UsdcPaymentLogic
                .usdcToBaseUnits(
                    "0.0000001"
                )
        }
    }

    @Test
    fun sepolaUsdcContractIsCorrect() {

        assertEquals(
            "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238",
            UsdcPaymentLogic
                .SEPOLIA_USDC_CONTRACT
        )
    }

    @Test
    fun transferCalldataIsCorrect() {

        assertEquals(
            "0xa9059cbb" +
                "000000000000000000000000" +
                "de99775d33bb815d0e16dff38d2eeb6b8a45ed3f" +
                "00000000000000000000000000000000000000000000000000000000000f4240",
            UsdcPaymentLogic
                .transferData(
                    recipientAddress =
                        recipient,
                    amountUsdc =
                        "1"
                )
        )
    }

    @Test
    fun invalidRecipientIsRejected() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {

            UsdcPaymentLogic
                .transferData(
                    recipientAddress =
                        "0x1234",
                    amountUsdc =
                        "1"
                )
        }
    }
}
