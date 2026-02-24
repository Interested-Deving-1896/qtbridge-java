# QML LSP Support for Qt Bridge for JVM for IntelliJ

An IntelliJ Platform plugin providing QML language support powered by the Language Server Protocol (LSP).

The plugin integrates both **native IntelliJ LSP** and **LSP4IJ** to deliver modern IDE features for QML development.

---

## Features

### Code Completion
- Completion filtering based on QML scope
- Backend support via native LSP and LSP4IJ

### Diagnostics
- Real-time error and warning highlighting
- Server-driven validation feedback
- Inline problem reporting

### Hover Documentation
- Symbol documentation via LSP hover

### Navigation
- Go to Definition

---

## Architecture

The plugin supports two LSP integration strategies:

### 1. Native IntelliJ LSP (Experimental)
- Direct integration with IntelliJ platform APIs
- Currently experimental and not the primary integration path

### 2. LSP4IJ (Recommended)
- Primary and most tested integration path
- Abstraction layer for LSP client integration
- Simplified server wiring and lifecycle management

Both backends expose equivalent functionality and can be selected via plugin configuration.

---

## Requirements

- IntelliJ IDEA 2025.3+ (or compatible IntelliJ Platform IDE)
- A compatible QML Language Server installed on the system

---

## Installation

### Development

Run in a sandboxed IDE instance:
```bash
./gradlew runIde
```

### Local Install

Install both Qt Bridge for JVM plugin and its LSP4IJ dependency directly into your local IntelliJ IDEA:
```bash
./gradlew installPlugin
```
Restart IntelliJ IDEA to apply.

Or build both plugin zips into `build/distributions/`:
```bash
./gradlew buildPluginPack
```

Install via **Settings → Plugins → Install Plugin from Disk**
- Install `lsp4ij-*.zip` first, then `qtbridge-intellij-plugin-*.zip`, and restart the IDE.

---

## Known Limitations

- Native IntelliJ LSP integration is experimental and may be unstable
- Hover documentation is work in progress
