package com.chainpay.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chainpay.app.data.EthereumRepository
import com.chainpay.app.ui.WalletUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletViewModel :
    ViewModel() {

    private val repository =
        EthereumRepository()

    private val _uiState =
        MutableStateFlow(
            WalletUiState()
        )

    val uiState:
        StateFlow<WalletUiState> =
        _uiState.asStateFlow()

    fun onWalletAddressChanged(
        address: String
    ) {

        _uiState.update {

            it.copy(
                walletAddress = address,
                errorMessage = null,
                transactions =
                    emptyList(),
                status =
                    "Ready to check wallet."
            )
        }
    }

    fun clearConnectedWallet() {

        _uiState.update {

            it.copy(
                walletAddress = "",
                ethBalance = "--",
                usdcBalance = "--",
                isLoading = false,
                hasLoadedWallet = false,
                errorMessage = null,
                transactions =
                    emptyList(),
                status =
                    "Wallet disconnected."
            )
        }
    }
    fun checkWallet() {

        val address =
            _uiState.value
                .walletAddress
                .trim()

        if (
            !Regex(
                "^0x[a-fA-F0-9]{40}$"
            ).matches(address)
        ) {

            _uiState.update {

                it.copy(
                    ethBalance = "--",
                    usdcBalance = "--",
                    transactions =
                        emptyList(),
                    hasLoadedWallet =
                        false,
                    errorMessage =
                        "Enter a valid Ethereum address.",
                    status =
                        "Address validation failed."
                )
            }

            return
        }

        _uiState.update {

            it.copy(
                isLoading = true,
                errorMessage = null,
                status =
                    "Loading ETH, USDC and transactions..."
            )
        }

        viewModelScope.launch {

            try {

                val walletData =
                    withContext(
                        Dispatchers.IO
                    ) {

                        val ethBalance =
                            repository
                                .getBalance(
                                    address
                                )

                        val usdcBalance =
                            repository
                                .getUsdcBalance(
                                    address
                                )

                        val transactions =
                            repository
                                .getRecentTransactions(
                                    address
                                )

                        Triple(
                            ethBalance,
                            usdcBalance,
                            transactions
                        )
                    }

                _uiState.update {

                    it.copy(
                        walletAddress =
                            address,
                        ethBalance =
                            walletData.first,
                        usdcBalance =
                            walletData.second,
                        transactions =
                            walletData.third,
                        network =
                            EthereumRepository
                                .NETWORK_NAME,
                        isLoading =
                            false,
                        hasLoadedWallet =
                            true,
                        errorMessage =
                            null,
                        status =
                            "Wallet data updated."
                    )
                }

            } catch (
                exception: Exception
            ) {

                _uiState.update {

                    it.copy(
                        isLoading =
                            false,
                        errorMessage =
                            exception.message
                                ?: "Unknown error.",
                        status =
                            "Wallet request failed."
                    )
                }
            }
        }
    }
}
