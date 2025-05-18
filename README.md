# InLock - Secure Asset Management with Blockchain

InLock is a cross-platform mobile application for secure asset tracking and ownership verification using blockchain technology, NFC, and QR codes.

## Features

- Asset registration and tracking on a distributed blockchain network
- Secure ownership transfer via NFC or QR code authentication
- Multi-role system for manufacturers, owners, and administrators
- Real-time verification of asset authenticity and ownership history
- Cross-platform support for Android and iOS via Kotlin Multiplatform

## Architecture

- Built with Kotlin Multiplatform and Compose Multiplatform for shared UI
- Implements MVI (Model-View-Intent) architecture pattern for predictable state management
- Firebase integration for user authentication and data storage
- Custom blockchain implementation for immutable ownership records

## Tech Stack

- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) for UI
- [Koin](https://insert-koin.io/) for dependency injection
- [Ktor](https://ktor.io/) for networking with the blockchain backend
- [Kotlinx.Serialization](https://github.com/Kotlin/kotlinx.serialization) for JSON handling
- [Voyager](https://voyager.adriel.cafe/) for navigation and screen models

## License

Licensed under the Apache License, Version 2.0.
