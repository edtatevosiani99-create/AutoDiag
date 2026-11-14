# AutoDiag — Android OBD-II Diagnostics

Android-first diagnostic application built with Kotlin and Jetpack Compose.

## Implemented in the current foundation
- Dark dashboard and four main sections: overview, connection, live data and fault codes.
- Virtual vehicle mode with changing demonstration telemetry and sample DTCs.
- Bluetooth Classic ELM327 SPP connection to an already paired adapter.
- Wi-Fi ELM327 TCP transport (configurable host and port; common defaults are shown in the UI).
- ELM initialization: ATZ, ATE0, ATL0, ATS0, ATH0, ATSP0 and protocol query.
- Standard OBD-II live PIDs: engine RPM (01 0C), coolant temperature (01 05), vehicle speed (01 0D), throttle position (01 11), control module voltage (01 42).
- DTC reads: stored (Mode 03), pending (Mode 07), permanent (Mode 0A), and clear command (Mode 04).
- Local descriptions for a starter set of common powertrain codes.
- Unit tests for DTC decoding.
- GitHub Actions workflow to build a debug APK and upload it as an artifact.

## Important compatibility boundaries
- The current live ECU implementation targets standard OBD-II powertrain diagnostics through ELM327-compatible adapters.
- ABS, SRS/Airbag, ESP, transmission-specific modules, manufacturer-specific UDS/KWP services, coding and adaptations are **not yet implemented**. They require vehicle-specific protocols, validated command sets and compatible hardware; the app must not present these modules as scanned when they were not.
- Bluetooth support currently targets Classic RFCOMM/SPP adapters. BLE-only adapters with proprietary GATT services need a separate transport implementation.
- Wi-Fi adapters vary in IP, port and TCP/UDP behavior; configure the values specified by the adapter vendor.
- Simulated values and simulated DTCs are test data, never real vehicle findings.
- Clearing DTCs can erase diagnostic evidence and readiness status. Save codes and freeze-frame information before clearing. The current build does not yet retrieve freeze-frame records.
- A low-cost or clone ELM327 may return incomplete or incorrect data. Missing PID responses are displayed as unavailable.

## Build
Open in Android Studio with JDK 17, Android SDK Platform 35 and Gradle 8.9 or newer compatible with Android Gradle Plugin 8.7.3.

A GitHub Actions workflow builds `app/build/outputs/apk/debug/app-debug.apk` on pushes to `main` and manual workflow dispatch.
