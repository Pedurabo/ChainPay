# ChainPay

Native Android Web3 payment application built with Kotlin and Jetpack Compose.

ChainPay combines Ethereum wallet data, WalletConnect-based transactions, QR merchant payment requests, and direct Sepolia transaction confirmation in a native Android application.

> Status: active development. Payment and Merchant Mode currently use Ethereum Sepolia testnet.

## Features

### Wallet Dashboard
- Ethereum Mainnet ETH balance
- ERC-20 USDC balance
- Recent Ethereum transaction history
- Ethereum address validation
- ViewModel + StateFlow driven Compose UI

### Wallet Connection
- Reown AppKit / WalletConnect integration
- External wallet approval
- Sepolia session support
- Wallet JSON-RPC response handling
- Deep-link return handling
- Physical-device flow validated with SafePal

### Payments
- Recipient and ETH amount entry
- ETH to wei conversion
- Explicit Sepolia chain binding: eip155:11155111
- eth_sendTransaction requests
- Pending, success, and error states
- Transaction hash display and copy
- Sepolia explorer integration

### Merchant Mode
- Merchant receiving address
- Requested ETH amount
- Optional payment reference
- ERC-681 payment URI
- ZXing QR-code generation
- Persistent active merchant requests
- One-device payment testing

### On-chain Confirmation
ChainPay captures the exact transaction hash returned by the wallet and verifies it directly through Sepolia JSON-RPC using eth_getTransactionReceipt.

A successful receipt with the expected recipient changes the merchant request state to Payment received.

This avoids depending on rate-limited explorer transaction-list polling.

## Stack

- Kotlin
- Jetpack Compose
- ViewModel
- Coroutines / Flow
- Reown AppKit
- WalletConnect
- Ethereum JSON-RPC
- ERC-20
- ERC-681
- ZXing
- Android SharedPreferences

## Networks

| Feature | Network |
| --- | --- |
| Wallet dashboard | Ethereum Mainnet |
| ETH and USDC reads | Ethereum Mainnet |
| Recent dashboard transactions | Ethereum Mainnet |
| Wallet payment testing | Sepolia |
| Merchant Mode | Sepolia |
| Merchant payment confirmation | Sepolia |

A Sepolia payment therefore does not currently appear in the Mainnet recent-transactions list.

## Milestones

- [x] M1 - Ethereum blockchain reader
- [x] M2 - Wallet dashboard, transaction history, and USDC
- [x] M3 - WalletConnect / Reown integration
- [x] M4 - Sepolia payment flow and payment-result UX
- [x] M5A - Merchant Receive
- [x] M5B - ERC-681 QR payment requests
- [x] M5C - Persistent merchant request and direct receipt confirmation
- [ ] M5D - Merchant UX polish
- [ ] M6 - Release and portfolio hardening

## Current Limitations

- Payments currently use Sepolia test ETH.
- The dashboard currently reads Ethereum Mainnet while Merchant Mode uses Sepolia.
- USDC sending is not implemented yet.
- Merchant confirmation currently follows the transaction submitted by ChainPay rather than discovering arbitrary incoming payments.
- Broader wallet interoperability and automated testing are still in progress.

## Security

Never commit wallet seed phrases, private keys, signing keys, RPC credentials, or WalletConnect/Reown credentials.

Local configuration and secrets must remain outside source control.

## Build

Requirements: Android Studio, JDK 17, and the Android SDK.

Debug build:

    ./gradlew assembleDebug

## Next

- Merchant payment UX polish
- Stablecoin payment support
- Payment history
- Broader wallet testing
- Automated tests
- Release hardening

## Author

Developed by [Joshua Wabulo](https://github.com/Pedurabo).
