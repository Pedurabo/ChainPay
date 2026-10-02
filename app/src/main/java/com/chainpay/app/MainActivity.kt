@file:OptIn(
    androidx.compose.material.ExperimentalMaterialApi::class
)

package com.chainpay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chainpay.app.data.WalletTransaction
import com.chainpay.app.ui.WalletUiState
import com.chainpay.app.viewmodel.WalletViewModel
import com.reown.appkit.client.AppKit
import com.reown.appkit.ui.appKitGraph
import androidx.compose.ui.platform.LocalContext
import com.reown.appkit.ui.components.button.rememberAppKitState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        handleWalletReturn(
            intent
        )

        setContent {

            MaterialTheme {

                Surface(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    ChainPayHost()
                }
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(intent)

        setIntent(intent)

        handleWalletReturn(intent)
    }

    private fun handleWalletReturn(
        intent: Intent?
    ) {

        val deepLink =
            intent?.dataString

        if (
            deepLink?.contains("wc_ev") ==
            true
        ) {

            AppKit.handleDeepLink(
                deepLink
            ) { error ->

                error.throwable
                    .printStackTrace()
            }
        }
    }
}

@Composable
private fun ChainPayHost() {

    val sheetState =
        rememberModalBottomSheetState(
            initialValue =
                ModalBottomSheetValue.Hidden,
            skipHalfExpanded =
                true
        )

    val bottomSheetNavigator =
        BottomSheetNavigator(
            sheetState
        )

    val navController =
        rememberNavController(
            bottomSheetNavigator
        )

    ModalBottomSheetLayout(
        bottomSheetNavigator =
            bottomSheetNavigator,
        sheetShape =
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp
            )
    ) {

        NavHost(
            navController =
                navController,
            startDestination =
                "chainpay-home"
        ) {

            composable(
                "chainpay-home"
            ) {

                ChainPayScreen(
                    navController =
                        navController
                )
            }

           

            composable(
                "merchant-mode"
            ) {

                MerchantReceiveScreen(
                    navController =
                        navController
                )
            }
 appKitGraph(
                navController
            )
        }
    }
}

@Composable
private fun ChainPayScreen(
    navController:
        androidx.navigation.NavController,
    viewModel:
        WalletViewModel =
        viewModel()
) {

    val state by
        viewModel.uiState
            .collectAsState()

    val appKitState =
        rememberAppKitState(
            navController =
                navController
        )

    val walletConnected by
        appKitState.isConnected
            .collectAsState(
                initial = false
            )

    LaunchedEffect(
        walletConnected
    ) {

        if (
            walletConnected
        ) {

            val connectedAddress =
                withContext(
                    Dispatchers.IO
                ) {

                    AppKit
                        .getAccount()
                        ?.address
                }

            if (
                !connectedAddress
                    .isNullOrBlank()
            ) {

                viewModel
                    .onWalletAddressChanged(
                        connectedAddress
                    )

                viewModel
                    .checkWallet()
            }
        }
    }

    WalletDashboard(
        state = state,
        isWalletConnected =
            walletConnected,
        appKitState =
            appKitState,
        onAddressChanged =
            viewModel::
                onWalletAddressChanged,
        onCheckWallet =
            viewModel::
                checkWallet,
        onOpenMerchantMode = {
            navController.navigate(
                "merchant-mode"
            )
        }

    )
}

@Composable
private fun WalletDashboard(
    state: WalletUiState,
    isWalletConnected: Boolean,
    appKitState:
        com.reown.appkit.ui.components.button.AppKitState,
    onAddressChanged:
        (String) -> Unit,
    onCheckWallet:
        () -> Unit,
    onOpenMerchantMode:
        () -> Unit

) {

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement =
            Arrangement
                .spacedBy(16.dp)
    ) {

        item {

            Text(
                text =
                    "ChainPay",
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )
        }

        item {

            Text(
                text =
                    "Android stablecoin payment wallet"
            )
        }

        item {

            WalletConnectionCard(
                isConnected =
                    isWalletConnected,
                appKitState =
                    appKitState,
                walletAddress =
                    state.walletAddress
            )
        }

        item {

            NetworkCard(
                network =
                    state.network
            )
        }

        item {

            Button(
                onClick =
                    onOpenMerchantMode,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "Merchant Mode"
                )
            }
        }

        item {

            OutlinedTextField(
                value =
                    state.walletAddress,
                onValueChange =
                    onAddressChanged,
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Ethereum wallet address"
                    )
                },
                placeholder = {
                    Text(
                        "0x..."
                    )
                },
                singleLine = true
            )
        }

        item {

            Button(
                onClick =
                    onCheckWallet,
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !state.isLoading
            ) {

                Text(
                    if (
                        state.isLoading
                    ) {
                        "Loading wallet..."
                    } else {
                        "Check Wallet"
                    }
                )
            }
        }

        item {

            BalanceCard(
                label =
                    "ETH Balance",
                value =
                    if (
                        state.isLoading
                    ) {
                        "..."
                    } else {
                        "${state.ethBalance} ETH"
                    }
            )
        }

        item {

            BalanceCard(
                label =
                    "USDC Balance",
                value =
                    if (
                        state.isLoading
                    ) {
                        "..."
                    } else {
                        "${state.usdcBalance} USDC"
                    }
            )
        }

        state.errorMessage
            ?.let { message ->

                item {

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .padding(
                                        16.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "Error",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    message
                            )
                        }
                    }
                }
            }

        if (
            state.hasLoadedWallet
        ) {

            item {

                HorizontalDivider()
            }

            item {

                Text(
                    text =
                        "Recent transactions",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                state.transactions
                    .isEmpty()
            ) {

                item {

                    Text(
                        text =
                            "No recent transactions found."
                    )
                }

            } else {

                items(
                    state.transactions
                ) { transaction ->

                    TransactionCard(
                        transaction
                    )
                }
            }
        }

        item {

            HorizontalDivider()
        }

        item {

            Text(
                text =
                    state.status,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        item {

            Text(
                text =
                    "Connection is enabled, but ChainPay still cannot send funds in this milestone.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}

@Composable
private fun WalletConnectionCard(
    isConnected: Boolean,
    appKitState:
        com.reown.appkit.ui.components.button.AppKitState,
    walletAddress: String
) {

    val context =
        LocalContext.current

    val paymentState by
        PaymentState.state
            .collectAsState()

    var recipientAddress by
        rememberSaveable {
            mutableStateOf("")
        }

    var amountEth by
        rememberSaveable {
            mutableStateOf(
                "0.000001"
            )
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                ),
            verticalArrangement =
                Arrangement
                    .spacedBy(12.dp)
        ) {

            Text(
                text =
                    "Wallet connection",
                fontWeight =
                    FontWeight.Bold
            )

            if (
                isConnected
            ) {

                Text(
                    text =
                        "Connected",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                if (
                    walletAddress
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            shorten(
                                walletAddress
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }

                Text(
                    text =
                        "Sepolia payment",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Testnet only. Enter the recipient and the amount of Sepolia ETH to send.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

                OutlinedTextField(
                    value =
                        recipientAddress,
                    onValueChange = {
                        recipientAddress =
                            it
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    label = {
                        Text(
                            "Recipient address"
                        )
                    },
                    placeholder = {
                        Text(
                            "0x..."
                        )
                    },
                    singleLine =
                        true
                )

                OutlinedTextField(
                    value =
                        amountEth,
                    onValueChange = {
                        amountEth =
                            it
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    label = {
                        Text(
                            "Amount (Sepolia ETH)"
                        )
                    },
                    placeholder = {
                        Text(
                            "0.000001"
                        )
                    },
                    singleLine =
                        true
                )

                Button(
                    onClick = {

                        TestPaymentRequester
                            .sendPayment(
                                context =
                                    context,
                                walletAddress =
                                    walletAddress,
                                recipientAddress =
                                    recipientAddress,
                                amountEth =
                                    amountEth
                            )
                    },
                    enabled =
                        walletAddress
                            .isNotBlank() &&
                            recipientAddress
                                .isNotBlank() &&
                            amountEth
                                .isNotBlank() &&
                            !paymentState
                                .isPending,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        if (
                            paymentState
                                .isPending
                        ) {
                            "Payment pending..."
                        } else {
                            "Send Sepolia Payment"
                        }
                    )
                }

                HorizontalDivider()

                Text(
                    text =
                        "Payment status",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        paymentState.message,
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                paymentState
                    .transactionHash
                    ?.let {
                            transactionHash ->

                        Text(
                            text =
                                "Transaction hash",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                shorten(
                                    transactionHash
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )

                        Button(
                            onClick = {

                                val clipboard =
                                    context.getSystemService(
                                        android.content.ClipboardManager::class.java
                                    )

                                clipboard.setPrimaryClip(
                                    android.content.ClipData
                                        .newPlainText(
                                            "ChainPay transaction hash",
                                            transactionHash
                                        )
                                )

                                android.widget.Toast
                                    .makeText(
                                        context,
                                        "Transaction hash copied.",
                                        android.widget.Toast.LENGTH_SHORT
                                    )
                                    .show()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text(
                                "Copy Transaction Hash"
                            )
                        }

                        Button(
                            onClick = {

                                val explorerUrl =
                                    "https://sepolia.etherscan.io/tx/$transactionHash"

                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        android.net.Uri.parse(
                                            explorerUrl
                                        )
                                    )

                                context.startActivity(
                                    intent
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text(
                                "View on Sepolia Explorer"
                            )
                        }

                        Button(
                            onClick = {

                                recipientAddress =
                                    ""

                                amountEth =
                                    "0.000001"

                                PaymentState
                                    .reset()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text(
                                "New Payment"
                            )
                        }
                    }

            } else {

                Text(
                    text =
                        "Connect a Sepolia Ethereum wallet to ChainPay."
                )

                Button(
                    onClick = {
                        DirectWalletConnector
                            .connect(
                                context
                            )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        "Connect Wallet"
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantReceiveScreen(
    navController:
        androidx.navigation.NavController,
    viewModel:
        WalletViewModel =
        viewModel()
) {

    val context =
        LocalContext.current

    val savedMerchantRequest =
        remember {
            MerchantRequestStore
                .load(
                    context
                )
        }



    val state by
        viewModel.uiState
            .collectAsState()

    var merchantAddress by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .merchantAddress
            )
        }

    var requestedAmount by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .requestedAmount
            )
        }

    var paymentNote by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .paymentNote
            )
        }

    var requestCreated by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .active
            )
        }

    var requestError by
        rememberSaveable {
            mutableStateOf<String?>(null)
        }

    var requestCreatedAtSeconds by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .createdAtSeconds
            )
        }

    var receivedTransactionHash by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .receivedTransactionHash
            )
        }

    var receivedFromAddress by
        rememberSaveable {
            mutableStateOf(
                savedMerchantRequest
                    .receivedFromAddress
            )
        }

    var paymentMonitorError by
        rememberSaveable {
            mutableStateOf<String?>(
                null
            )
        }

    var isCheckingPayment by
        rememberSaveable {
            mutableStateOf(false)
        }

    LaunchedEffect(
        Unit
    ) {

        if (
            merchantAddress.isBlank()
        ) {

            val connectedAddress =
                withContext(
                    Dispatchers.IO
                ) {

                    AppKit
                        .getAccount()
                        ?.address
                }

            if (
                !connectedAddress
                    .isNullOrBlank()
            ) {

                merchantAddress =
                    connectedAddress
            }
        }
    }

    LaunchedEffect(
        requestCreated,
        merchantAddress
    ) {

        if (
            !requestCreated
        ) {
            return@LaunchedEffect
        }

        while (
            requestCreated &&
            receivedTransactionHash ==
                null
        ) {

            val savedRequest =
                MerchantRequestStore
                    .load(
                        context
                    )

            val submittedHash =
                savedRequest
                    .submittedTransactionHash

            if (
                submittedHash
                    .isNullOrBlank()
            ) {

                isCheckingPayment =
                    false

                paymentMonitorError =
                    null

                delay(
                    3000L
                )

                continue
            }

            isCheckingPayment =
                true

            paymentMonitorError =
                null

            try {

                val receipt =
                    withContext(
                        Dispatchers.IO
                    ) {

                        SepoliaReceiptVerifier
                            .getReceipt(
                                submittedHash
                            )
                    }

                if (
                    receipt ==
                        null
                ) {

                    delay(
                        10000L
                    )

                    continue
                }

                if (
                    receipt.status !=
                        "0x1"
                ) {

                    paymentMonitorError =
                        "Transaction failed on Sepolia."

                    isCheckingPayment =
                        false

                    break
                }

                if (
                    !receipt.toAddress
                        .equals(
                            merchantAddress,
                            ignoreCase =
                                true
                        )
                ) {

                    paymentMonitorError =
                        "Confirmed transaction recipient does not match this request."

                    isCheckingPayment =
                        false

                    break
                }

                receivedTransactionHash =
                    receipt.transactionHash

                receivedFromAddress =
                    receipt.fromAddress

                MerchantRequestStore
                    .saveReceivedPayment(
                        context =
                            context,
                        transactionHash =
                            receipt.transactionHash,
                        fromAddress =
                            receipt.fromAddress
                    )

                isCheckingPayment =
                    false

                break

            } catch (
                error: Exception
            ) {

                paymentMonitorError =
                    error.message
                        ?: "Sepolia receipt verification failed."

                isCheckingPayment =
                    false

                delay(
                    10000L
                )
            }
        }
    }
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "Merchant Receive",
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )
        }

        item {
            Text(
                "Create a Sepolia payment request for a customer."
            )
        }

        item {
            Button(
                onClick = {
                    navController.popBackStack()
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Back to Wallet")
            }
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Payment request",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text = "Network: Sepolia",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    OutlinedTextField(
                        value = merchantAddress,
                        onValueChange = {
                            merchantAddress = it
                            requestCreated = false
                            requestError = null
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text("Receiving address")
                        },
                        placeholder = {
                            Text("0x...")
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = requestedAmount,
                        onValueChange = {
                            requestedAmount = it
                            requestCreated = false
                            requestError = null
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text("Amount (Sepolia ETH)")
                        },
                        placeholder = {
                            Text("0.001")
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = paymentNote,
                        onValueChange = {
                            paymentNote = it
                            requestCreated = false
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                "Payment note (optional)"
                            )
                        },
                        placeholder = {
                            Text("Order #1234")
                        },
                        singleLine = true
                    )

                    Button(
                        onClick = {

                            val address =
                                merchantAddress.trim()

                            val amount =
                                requestedAmount.trim()

                            val validAddress =
                                Regex(
                                    "^0x[a-fA-F0-9]{40}$"
                                ).matches(address)

                            val validAmount =
                                try {
                                    java.math.BigDecimal(
                                        amount
                                    ) >
                                        java.math.BigDecimal.ZERO
                                } catch (
                                    error: Exception
                                ) {
                                    false
                                }

                            when {

                                !validAddress -> {
                                    requestCreated = false
                                    requestError =
                                        "Enter a valid Ethereum receiving address."
                                }

                                !validAmount -> {
                                    requestCreated = false
                                    requestError =
                                        "Enter an amount greater than zero."
                                }

                                else -> {
                                    merchantAddress =
                                        address

                                    requestedAmount =
                                        amount

                                    requestError = null

                                    receivedTransactionHash =
                                        null

                                    receivedFromAddress =
                                        null

                                    paymentMonitorError =
                                        null

                                    requestCreatedAtSeconds =
                                        System.currentTimeMillis() /
                                            1000L

                                    MerchantRequestStore
                                        .saveRequest(
                                            context =
                                                context,
                                            merchantAddress =
                                                merchantAddress,
                                            requestedAmount =
                                                requestedAmount,
                                            paymentNote =
                                                paymentNote,
                                            createdAtSeconds =
                                                requestCreatedAtSeconds
                                        )

                                    requestCreated = true
                                }
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Create Payment Request"
                        )
                    }
                }
            }
        }

        requestError
            ?.let { message ->

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Request error",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(message)
                        }
                    }
                }
            }

        if (requestCreated) {

            item {

                val paymentWei =
                    remember(
                        merchantAddress,
                        requestedAmount
                    ) {

                        java.math.BigDecimal(
                            requestedAmount
                        )
                            .movePointRight(18)
                            .toBigIntegerExact()
                            .toString()
                    }

                val paymentUri =
                    remember(
                        merchantAddress,
                        paymentWei
                    ) {

                        "ethereum:" +
                            merchantAddress +
                            "@11155111" +
                            "?value=" +
                            paymentWei
                    }

                val paymentQr =
                    remember(
                        paymentUri
                    ) {

                        generatePaymentQrCode(
                            paymentUri
                        )
                    }



                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            text =
                                "Payment Request Ready",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text("Network")

                        Text(
                            text = "Sepolia",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text("Receive")

                        Text(
                            text =
                                "$requestedAmount ETH",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text("To")

                        Text(
                            shorten(
                                merchantAddress
                            )
                        )

                        if (
                            paymentNote.isNotBlank()
                        ) {

                            Text("Reference")

                            Text(paymentNote)
                        }

                        HorizontalDivider()

                        Text(
                            text =
                                "Scan to pay",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Image(
                            bitmap =
                                paymentQr
                                    .asImageBitmap(),
                            contentDescription =
                                "Sepolia payment QR code",
                            modifier =
                                Modifier
                                    .size(
                                        260.dp
                                    )
                        )

                        Text(
                            text =
                                "ERC-681 Payment URI",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "ethereum:" +
                                    shorten(
                                        merchantAddress
                                    ) +
                                    "@11155111",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )

                        Button(
                            onClick = {

                                val clipboard =
                                    context
                                        .getSystemService(
                                            android.content.ClipboardManager::class.java
                                        )

                                clipboard
                                    .setPrimaryClip(
                                        android.content.ClipData
                                            .newPlainText(
                                                "ChainPay payment request",
                                                paymentUri
                                            )
                                    )

                                android.widget.Toast
                                    .makeText(
                                        context,
                                        "Payment URI copied.",
                                        android.widget.Toast.LENGTH_SHORT
                                    )
                                    .show()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Text(
                                "Copy Payment URI"
                            )
                        }

                        if (
                            receivedTransactionHash !=
                                null
                        ) {

                            HorizontalDivider()

                            Text(
                                text =
                                    "PAID",
                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Payment received",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "$requestedAmount Sepolia ETH",
                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Network: Sepolia",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            receivedFromAddress
                                ?.let {
                                        sender ->

                                    Text(
                                        text =
                                            "Paid by",
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        text =
                                            shorten(
                                                sender
                                            )
                                    )
                                }

                            Text(
                                text =
                                    "Transaction hash",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    shorten(
                                        receivedTransactionHash
                                            ?: ""
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )

                            Button(
                                onClick = {

                                    val hash =
                                        receivedTransactionHash
                                            ?: return@Button

                                    val clipboard =
                                        context
                                            .getSystemService(
                                                android.content.ClipboardManager::class.java
                                            )

                                    clipboard
                                        .setPrimaryClip(
                                            android.content.ClipData
                                                .newPlainText(
                                                    "ChainPay transaction hash",
                                                    hash
                                                )
                                        )

                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            "Transaction hash copied.",
                                            android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Text(
                                    "Copy Transaction Hash"
                                )
                            }

                            Button(
                                onClick = {

                                    val hash =
                                        receivedTransactionHash
                                            ?: return@Button

                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(
                                                "https://sepolia.etherscan.io/tx/$hash"
                                            )
                                        )

                                    context.startActivity(
                                        intent
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Text(
                                    "View Received Transaction"
                                )
                            }

                        } else {

                            Text(
                                text =
                                    if (
                                        isCheckingPayment
                                    ) {
                                        "Confirming Sepolia transaction..."
                                    } else {
                                        "Waiting for customer payment"
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )

                            if (
                                merchantAddress.isNotBlank()
                            ) {

                                Button(
                                    onClick = {

                                        MerchantRequestStore
                                            .armForWalletResponse(
                                                context
                                            )

                                        TestPaymentRequester
                                            .sendPayment(
                                                context =
                                                    context,
                                                walletAddress =
                                                    merchantAddress,
                                                recipientAddress =
                                                    merchantAddress,
                                                amountEth =
                                                    requestedAmount
                                            )
                                    },
                                    enabled =
                                        !isCheckingPayment &&
                                            receivedTransactionHash ==
                                                null,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                ) {

                                    Text(
                                        "Test Pay This Request"
                                    )
                                }

                                Text(
                                    text =
                                        "One-device test: sends this exact request from your connected wallet.",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }

                            paymentMonitorError
                                ?.let {
                                        monitorError ->

                                    Text(
                                        text =
                                            "Payment check error: " +
                                                monitorError,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }
                        }

                        Button(
                            onClick = {
                                requestCreated = false
                                requestedAmount = ""
                                paymentNote = ""
                                requestError = null

                                requestCreatedAtSeconds =
                                    0L

                                receivedTransactionHash =
                                    null

                                receivedFromAddress =
                                    null

                                paymentMonitorError =
                                    null

                                isCheckingPayment =
                                    false

                                MerchantRequestStore
                                    .clear(
                                        context
                                    )
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text("New Request")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun NetworkCard(
    network: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),
            horizontalArrangement =
                Arrangement
                    .SpaceBetween
        ) {

            Text(
                text =
                    "Network"
            )

            Text(
                text =
                    network,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BalanceCard(
    label: String,
    value: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            verticalArrangement =
                Arrangement
                    .spacedBy(
                        8.dp
                    )
        ) {

            Text(
                text =
                    label
            )

            Text(
                text =
                    value,
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction:
        WalletTransaction
) {

    val direction =
        if (
            transaction.isIncoming
        ) {
            "Received"
        } else {
            "Sent"
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement
                    .spacedBy(
                        4.dp
                    )
        ) {

            Text(
                text =
                    direction,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "${transaction.valueEth} ETH"
            )

            Text(
                text =
                    shorten(
                        transaction.hash
                    )
            )

            Text(
                text =
                    formatTime(
                        transaction.timestamp
                    )
            )
        }
    }
}

private fun shorten(
    value: String
): String {

    if (
        value.length <= 18
    ) {
        return value
    }

    return value
        .take(10) +
        "..." +
        value.takeLast(8)
}

private fun formatTime(
    seconds: Long
): String {

    if (
        seconds <= 0
    ) {
        return "Unknown time"
    }

    return SimpleDateFormat(
        "dd MMM yyyy, HH:mm",
        Locale.getDefault()
    ).format(
        Date(
            seconds *
                1000L
        )
    )
}

private fun generatePaymentQrCode(
    content: String,
    size: Int = 700
): android.graphics.Bitmap {

    val matrix =
        com.google.zxing.MultiFormatWriter()
            .encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                size,
                size
            )

    val pixels =
        IntArray(
            size * size
        )

    for (
        y in 0 until size
    ) {

        for (
            x in 0 until size
        ) {

            pixels[
                y * size + x
            ] =
                if (
                    matrix[
                        x,
                        y
                    ]
                ) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
        }
    }

    return android.graphics.Bitmap
        .createBitmap(
            size,
            size,
            android.graphics.Bitmap.Config.RGB_565
        )
        .apply {

            setPixels(
                pixels,
                0,
                size,
                0,
                0,
                size,
                size
            )
        }
}











