# ChainPay

**Native Android wallet, blockchain and crypto-payment integration built with Kotlin and Jetpack Compose.**

ChainPay demonstrates practical Android/Web3 integration: Ethereum data, external-wallet connectivity, ETH and ERC-20 transaction preparation, confirmation monitoring, restart recovery and merchant payment requests.

**Current demo version:** 0.9.0

**Repository:** https://github.com/Pedurabo/ChainPay

## What ChainPay demonstrates

- Ethereum JSON-RPC integration from Android
- ETH and USDC wallet balances
- recent Ethereum transaction history
- Reown / WalletConnect external-wallet sessions
- Sepolia ETH payment requests
- ERC-20 USDC calldata generation
- transaction receipt confirmation
- pending-payment recovery after app restart
- merchant payment requests and QR sharing
- transaction explorer integration
- Android release and security hardening

ChainPay does not request or store wallet seed phrases or private keys.

## Wallet dashboard

The read-only dashboard uses Ethereum Mainnet and supports:

- ETH balance
- USDC balance
- recent transaction history
- incoming/outgoing transaction direction
- Ethereum address validation
- loading and error states

Dashboard architecture:

```text
Jetpack Compose UI
        |
        v
WalletViewModel
        |
        v
EthereumRepository
        |
        +---- Ethereum JSON-RPC
        |
        +---- Blockscout
```

## Wallet connection

ChainPay uses Reown / WalletConnect to hand transaction approval to an external Android wallet.

Implemented wallet behavior includes:

- session pairing
- connected-address recovery
- session recovery after app restart
- wallet disconnection detection
- cleanup after a real session disconnect
- Sepolia transaction requests

SafePal has provided the working end-to-end Sepolia ETH payment path during development.

## Direct payments

Direct ETH payments use Sepolia rather than Mainnet.

The payment lifecycle is:

```text
IDLE
  |
  v
REQUESTING
  |
  v
AWAITING_WALLET
  |
  v
SUBMITTED
  |
  +----> SUCCESS / confirmed
  |
  +----> ERROR / reverted
  |
  +----> CONFIRMATION_TIMEOUT
```

A returned transaction hash means submitted, not confirmed. ChainPay polls the Sepolia transaction receipt before reporting confirmation.

Submitted payment state is persisted so confirmation monitoring can recover after process death or app restart.

## USDC support

ChainPay implements Sepolia USDC transfer preparation using ERC-20 `transfer(address,uint256)`.

Implemented behavior includes:

- Sepolia USDC contract targeting
- 6-decimal amount conversion
- transfer calldata encoding
- rejection of invalid or over-precision amounts
- zero native ETH value for token transfer requests
- external-wallet transaction submission
- independent ETH and USDC payment state

**Current limitation:** the live Sepolia USDC transfer is still pending test-USDC funding. The implementation and tests are present, but this repository does not claim an end-to-end live token transfer that has not yet been performed.

## Merchant Mode

Merchant Mode supports:

- Sepolia ETH payment-request creation
- merchant address and amount validation
- optional payment note
- ERC-681 payment URI generation
- QR-code generation
- copying the payment URI
- Android share sheet
- payment monitoring
- sender and transaction-hash display
- Sepolia explorer navigation

Merchant monitoring remains separate from direct-wallet payment state.

## Network responsibilities

| Feature | Network |
| --- | --- |
| ETH balance | Ethereum Mainnet |
| USDC balance | Ethereum Mainnet |
| Recent transactions | Ethereum Mainnet |
| Direct ETH payments | Sepolia |
| Direct USDC payments | Sepolia |
| Merchant payment requests | Sepolia |
| Receipt confirmation | Sepolia |

This separation lets ChainPay demonstrate real public blockchain reads while keeping payment testing on a test network.

## Technology

- Kotlin
- Jetpack Compose
- Android SDK 37
- Minimum SDK 26
- JDK 17
- Gradle 9.6
- Android Gradle Plugin 9.4.0
- Reown AppKit / WalletConnect
- Ethereum JSON-RPC
- Blockscout API
- Kotlin coroutines and StateFlow
- ERC-20 ABI encoding
- ZXing QR generation

## Security approach

- private keys and seed phrases remain in the external wallet
- Android backups are disabled
- cleartext network traffic is disabled
- raw SDK exceptions are not shown directly to users
- sensitive diagnostic logging is debug-only
- transaction submission and confirmation are separate states
- local credential configuration is excluded from Git
- provider credentials embedded in an APK are not treated as true secrets

Production-secret credentials should be kept behind an appropriate backend rather than embedded in an Android client.

## Local setup

Requirements:

- Android Studio / Android SDK
- JDK 17
- Android device or emulator
- WalletConnect-compatible wallet for payment testing
- Reown project ID

Clone:

```bash
git clone https://github.com/Pedurabo/ChainPay
cd ChainPay
```

Configure local-only values in `local.properties`. Do not commit this file.

Example:

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
REOWN_PROJECT_ID=YOUR_REOWN_PROJECT_ID
```

Build on Windows:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Build the release variant:

```powershell
.\gradlew.bat assembleRelease
```

## Verified project behavior

Development regression testing has covered:

- debug build, install and launch
- release compilation
- wallet-session recovery
- wallet disconnection
- Sepolia ETH transaction submission
- transaction receipt confirmation
- submitted-payment restart recovery
- confirmation timeout handling
- merchant request sharing
- merchant payment monitoring
- explorer navigation
- user-facing error sanitization

## Status

**Completed:**

- blockchain reader
- wallet dashboard
- ETH and USDC balances
- transaction history
- external wallet connection
- Sepolia ETH payments
- receipt confirmation
- restart-safe payment recovery
- Merchant Mode
- QR/share workflow
- security hardening
- release build configuration
- launcher branding

**Remaining before 1.0:**

- fund the test wallet with Sepolia USDC
- execute and verify a live USDC transfer
- capture final portfolio screenshots / demo evidence
- final release review

## Portfolio focus

> I integrate Web3 wallets, token balances, blockchain transactions and crypto/stablecoin payments into Android apps using Kotlin.

ChainPay focuses on the Android engineering surrounding blockchain transactions: validation, wallet handoff, persistence, lifecycle recovery, confirmation, failure handling, security and merchant UX.

## Disclaimer

ChainPay is currently a portfolio/testnet application. Sepolia assets have no real monetary value.

It should not be treated as production financial infrastructure without additional security review, testing, operational controls and production backend design.
