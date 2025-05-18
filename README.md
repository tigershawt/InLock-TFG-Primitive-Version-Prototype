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
## Nota sobre el uso de herramientas de IA
Durante el desarrollo del presente proyecto se ha hecho uso puntual de herramientas de inteligencia artificial generativa, concretamente Claude (Anthropic), con el objetivo de asistir en tareas específicas de depuración de código (debugging) y generación de estructuras automatizadas de logging. Esta asistencia se ha limitado exclusivamente a la implementación de componentes secundarios, como:

- Estructuras de logging en el backend Python (Flask)
- Formatos de serialización/deserialización JSON en la aplicación móvil
- Patrones de depuración en la implementación del DAG blockchain

En ningún caso estas herramientas han sustituido el trabajo intelectual, técnico o conceptual desarrollado por el autor. Todo el diseño arquitectónico, la implementación de la lógica de negocio, y el desarrollo de los componentes críticos (autenticación, transferencia de activos, validación NFC) han sido concebidos, implementados y validados de forma íntegra y original por el autor.

## License
Licensed under the Apache License, Version 2.0.
