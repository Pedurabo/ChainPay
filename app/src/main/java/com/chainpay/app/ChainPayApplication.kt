package com.chainpay.app

import android.app.Application
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.NetworkClientTimeout
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import java.util.concurrent.TimeUnit

class ChainPayApplication :
    Application() {

    companion object {
        private const val TAG = "CHAINPAY_WC"
    }

    override fun onCreate() {

        super.onCreate()

        PaymentState.initialize(this)

        check(
            BuildConfig.REOWN_PROJECT_ID
                .isNotBlank()
        ) {
            "Missing Reown Project ID."
        }

        val metadata =
            Core.Model.AppMetaData(
                name = "ChainPay",
                description =
                    "Android stablecoin payment application",
                url =
                    "https://github.com/Pedurabo/ChainPay",
                icons =
                    emptyList(),
                redirect =
                    "chainpay://request",
                appLink =
                    null,
                linkMode = false
            )

        CoreClient.initialize(
            application = this,
            projectId =
                BuildConfig
                    .REOWN_PROJECT_ID,
            metaData =
                metadata,
            networkClientTimeout =
                NetworkClientTimeout(
                    timeout = 60,
                    timeUnit =
                        TimeUnit.SECONDS
                )
        ) {
            if (BuildConfig.DEBUG) {
                if (BuildConfig.DEBUG) Log.e(
                    TAG,
                    "REOWN_INITIALIZATION_ERROR",
                    it.throwable
                )
            }
        }

        AppKit.initialize(
            Modal.Params.Init(
                core = CoreClient
            ),
            onSuccess = {

                AppKit.setChains(
                    AppKitChainsPresets
                        .ethChains
                        .values
                        .toList()
                )

                AppKit.setDelegate(
                    object : AppKit.ModalDelegate {

                        override fun onSessionApproved(
                            approvedSession: Modal.Model.ApprovedSession
                        ) {
                            if (BuildConfig.DEBUG) Log.d(
                                TAG,
                                "SESSION_APPROVED: $approvedSession"
                            )
                        }

                        override fun onSessionRejected(
                            rejectedSession: Modal.Model.RejectedSession
                        ) {
                            if (BuildConfig.DEBUG) Log.e(
                                TAG,
                                "SESSION_REJECTED: $rejectedSession"
                            )
                        }

                        override fun onSessionUpdate(
                            updatedSession: Modal.Model.UpdatedSession
                        ) {
                            if (BuildConfig.DEBUG) Log.d(
                                TAG,
                                "SESSION_UPDATED: $updatedSession"
                            )
                        }

                        @Suppress("DEPRECATION")
                        override fun onSessionEvent(
                            sessionEvent: Modal.Model.SessionEvent
                        ) {
                        }

                        override fun onSessionEvent(
                            sessionEvent: Modal.Model.Event
                        ) {
                        }

                        override fun onSessionExtend(
                            session: Modal.Model.Session
                        ) {
                        }

                        override fun onSessionDelete(
                            deletedSession: Modal.Model.DeletedSession
                        ) {
                            if (BuildConfig.DEBUG) Log.d(
                                TAG,
                                "SESSION_DELETED: $deletedSession"
                            )
                        }

                        override fun onSessionRequestResponse(
                            response: Modal.Model.SessionRequestResponse
                        ) {

                            when (val result = response.result) {

                                is Modal.Model.JsonRpcResponse.JsonRpcResult -> {

                                    if (BuildConfig.DEBUG) Log.d(
                                        TAG,
                                        "TX_RESPONSE_SUCCESS " +
                                            "method=${response.method} " +
                                            "chain=${response.chainId} " +
                                            "id=${result.id} " +
                                            "result=${result.result}"
                                    )

                                    if (
                                        response.method ==
                                            "eth_sendTransaction"
                                    ) {

                                        val transactionHash =
                                            result.result
                                                ?.toString()

                                        if (
                                            !transactionHash
                                                .isNullOrBlank()
                                        ) {

                                            PaymentState
                                                .markSubmitted(
                                                    transactionHash
                                                )

                                            DirectPaymentConfirmationMonitor
                                                .confirm(
                                                    transactionHash
                                                )


                                        } else {

                                            PaymentState
                                                .markError(
                                                    "Wallet returned no transaction hash."
                                                )
                                        }
                                    }
                                }

                                is Modal.Model.JsonRpcResponse.JsonRpcError -> {

                                    if (BuildConfig.DEBUG) Log.e(
                                        TAG,
                                        "TX_RESPONSE_ERROR " +
                                            "method=${response.method} " +
                                            "chain=${response.chainId} " +
                                            "id=${result.id} " +
                                            "code=${result.code} " +
                                            "message=${result.message}"
                                    )

                                    if (
                                        response.method ==
                                            "eth_sendTransaction"
                                    ) {

                                        PaymentState
                                            .markError(
                                                "Wallet rejected payment: " +
                                                    result.message
                                            )
                                    }
                                }
                            }
                        }

                        override fun onProposalExpired(
                            proposal: Modal.Model.ExpiredProposal
                        ) {
                            if (BuildConfig.DEBUG) Log.e(
                                TAG,
                                "PROPOSAL_EXPIRED: $proposal"
                            )
                        }

                        override fun onRequestExpired(
                            request: Modal.Model.ExpiredRequest
                        ) {
                            if (BuildConfig.DEBUG) Log.e(
                                TAG,
                                "REQUEST_EXPIRED: $request"
                            )

                            PaymentState
                                .markError(
                                    "Payment request expired."
                                )
                        }

                        override fun onConnectionStateChange(
                            state: Modal.Model.ConnectionState
                        ) {
                            if (BuildConfig.DEBUG) Log.d(
                                TAG,
                                "CONNECTION_AVAILABLE=${state.isAvailable}"
                            )
                        }

                        override fun onError(
                            error: Modal.Model.Error
                        ) {
                            if (BuildConfig.DEBUG) Log.e(
                                TAG,
                                "APPKIT_ERROR",
                                error.throwable
                            )
                        }
                    }
                )

                if (BuildConfig.DEBUG) Log.d(
                    TAG,
                    "AppKit delegate installed"
                )
            },
            onError = {
                if (BuildConfig.DEBUG) Log.e(
                    TAG,
                    "APPKIT_INIT_ERROR",
                    it.throwable
                )
            }
        )
    }
}
