# Qt Bridge - Java / Kotlin - Pre Release

> Copyright (C) 2025 The Qt Company Ltd.
> SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

- Contents:
    1. Introduction
    2. Get in touch
    3. Status
    4. Early Preview Quick Start
       1. macOS
       2. Linux
       3. Windows
       4. Troubleshooting
    5. End-user workflow
       1. Description
       2. Quick start
       3. Gradle Plugin and Maven Artifacts
    6. Bridge Building and Development
       1. Java Environment
       2. C++ Environment
       3. Qt dependencies
       4. Building and running the Project
    7. Java Bridge API Overview
       1. Classes and Annotations
       2. Example Code
    8. Terms and Conditions
       1. Additional Terms and Conditions

## Introduction

This documentation outlines the process required to set up the
development environment for Java/Kotlin Bridge. The Bridge allows applications
to bridge Java and Kotlin code to QML. The bridging is based on two main mechanisms:
- Java JNI (C++) native code to do the actual bridging. JNI allows the bridge to translate data and function calls between QML and Java
- KSP (Kotlin Symbol Processing) for processing the user's classes and annotations at build-time. KSP allows the bridge to introspect user-code and generate all needed bridging code

The main parts of the solution are:
- User code (application)
- Java Bridge Java files in a JAR file (.jar)
- Java Bridge compiled native (C++) plugin (.dylib/.so/.dll)
- Maven gradle plugin for shipping to end users
- Qt Libraries

## Status

Bridge for Java/Kotlin is currently in early preview, and in active development.
It can be compiled, run, and tested out on the major desktop platforms. Notable limitations include:
- You need to set up Bridge development environment to use it, instead
  of relying the Bridge Gradle plugin to download necessary components. See
  [Early Preview Quick Start](#early-preview-quick-start) for setting up the
  environment
- APIs may change or even be removed
- There are many known issues and [missing features](https://qt-project.atlassian.net/browse/QTBUG-134776)

## Get in touch

You can reach us in the Qt Forum, specifically in the [Qt Bridges
category](https://forum.qt.io/category/78/qt-bridges).
For Qt bug tracker users there's also the [JavaQt Bridge task](https://qt-project.atlassian.net/browse/QTBUG-134776).

## Early Preview Quick Start
This chapter provides hands-on instructions for setting up the development environment.
**This setup is needed only for the time being** - in future the needed components will be
downloaded automatically by the Qt Bridge Gradle plugin.

Supported platforms:
- macOS (`arm64`, `x86_64`),
- Linux (`arm64`, `x86_64`),
- Windows (`arm64`, `x64`)

The needed components are:
- This repository i)
- Qt 6.10+ (Qt 6.8, Qt 6.5) ii)
- Gradle 8.14.2+
- CMake 3.16+
- C++ Toolchain
- OpenJDK 21

i) Clone this repository
```bash
git clone https://code.qt.io/qt/qtbridge-java.git/
cd qtbridge-java
```
ii) To get Qt please see [Qt Download Page](https://www.qt.io/development/download), or
[compile it from sources](https://doc.qt.io/qt-6/build-sources.html). Qt 6.10 is the
required minimum and tested version, but for early experimentation purposes
Qt 6.8 and Qt 6.5 should work too

Following are example command line instructions for different platforms, adjust as needed.
It is also possible to use an IDE for development. For this purpose we've tested [VS Code](https://code.visualstudio.com/download) and [Intellij IDEA](https://www.jetbrains.com/idea/).
Their setup is not covered here though. In summary you'll open the top level directory
as a Gradle folder (VS Code) / project (IntelliJ), and make sure you have needed environment
configured.

### macOS

```bash
# Ensure Qt is on PATH or Qt6_DIR is set, adjust as needed
export PATH=~/Qt/6.10.1/macos/bin:$PATH
# or
export Qt6_DIR=~/Qt/6.10.1/macos

# Ensure C++ toolchain is installed
xcode-select --install
# Verify C++ toolchain installation
clang++ --version

# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install JDK
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
# Verify JDK installation
javac --version

# Install Gradle
brew install gradle
# Test Gradle installation
gradle --version

# Generate Gradle wrapper (can take a long time on first run)
gradle wrapper

# Run an example application
./gradlew colorpaletteclient
```

### Linux

These instructions are on Ubuntu 24.04 arm64.

```bash
# Ensure Qt is on PATH or Qt6_DIR is set, adjust as needed
export PATH=~/Qt/6.10.1/gcc_arm64/bin:$PATH
# or
export Qt6_DIR=~/Qt/6.10.1/gcc_arm64

# Ensure build tools and other essential packages are installed
sudo apt install build-essential cmake gradle openjdk-21-jdk
gcc --version
gradle --version
cmake --version
javac --version

# Ensure right Java is used (in case system has multiple)
readlink -f "$(which javac)" # For checking which Java is on PATH
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64

# Generate Gradle wrapper (can take a long time on first run)
gradle wrapper

# Run an example application
./gradlew colorpaletteclient
```

### Windows

```bash
# Install C++ toolchain:
https://visualstudio.microsoft.com/downloads/
# Install Gradle, for example:
https://gradle.org/install/
# Install CMake, for example:
https://cmake.org/download/
# Install OpenJDK, for example:
https://learn.microsoft.com/en-us/java/openjdk/download

# Set needed directories on PATH (adjust paths)
SET PATH=C:\path\to\CMake\bin;%PATH%
SET JAVA_HOME=C:\path\to\jdk-21
SET PATH=%JAVA_HOME%\bin;%PATH%
SET PATH=C:\path\to\gradle-9.2.1\bin;%PATH%
SET PATH=%USERPROFILE%\Qt\6.10.1\msvc2022_arm64\bin;%PATH%

# Set up C++ environment, for example (adjust path as needed)
"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" arm64
# Verify they're accessible
where cl
gradle --version
javac --version
qmake --version

# Generate Gradle wrapper (can take a long time on first run)
gradle wrapper

# Run an example application
gradlew colorpaletteclient

```

### Troubleshooting

#### Cleanups
Use Gradle clean to remove earlier builds
```bash
gradle clean
```

If Gradle stops finding for example CMake, AWT, or Qt, sometimes it helps to restart
the Gradle daemons on the terminal which has the right environment variables set. Gradle
daemon is a long-lived background process and stopping it forces it to restart with the
right environment:
```bash
gradle --stop
```

Bridge plugin may have also downloaded libraries in a cache which may cause confusion
```bash
rm -fr ~/.gradle/caches/qt-downloads
```

#### Linux: Could NOT find WrapVulkanHeaders

```bash
sudo apt install libgl1-mesa-dev libvulkan-dev vulkan-tools
```

## End-user Workflow

Effortless first development experience for non-Qt developers is a priority for
Qt Bridges. With Java Bridge, the plan is to publish a Maven Gradle plugin
and related artifacts either on Maven or on Qt download site.

### Description

Qt Bridge allows any developer to set up their Java/Kotlin project using the standard
Gradle workflow. Qt provides a Gradle plugin hosted on Maven Central that integrates seamlessly
into your existing project structure.

The setup process is designed to be minimal. Developers only need to declare the Qt Bridge plugin
in their application's Gradle files. The plugin handles everything else automatically, including
downloading and configuring the environment.

### Quick start
To quickly get started with a new QtBridge application, you can use the included starter example:

[See the starter Project Quickstart](examples/starter/README.md)

#### Project Configuration

Add the Qt Bridge Gradle plugin to your project by updating the following files:
###### settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://android.qt.io/maven/releases") }
    }
}
```

**Using Maven Local:**

If you are working with a locally published version of the Qt Bridge Gradle plugin and qmlbridge module (for development or testing), add `mavenLocal()` to the repositories:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}
```
For more information about publishing and developing Qt Bridge modules, see the [Qt dev Gradle plugins](plugins/qtbridge-dev-plugin/README.md).

###### build.gradle.kts
```kotlin
plugins {
    id("org.qtproject.qt.bridge.qtbridge-plugin") version "0.1"
}

qtBridge {
    application {
        name = "myApp"  // optional: creates running task with this name (defaults to project's name)
        mainClass = "com.example.MyQtApplication" // optional: if set, creates run task
        jvmArgs.addAll("-Xmx512m") // additional JVM arguments may be passed like this
    }

    qml {
        entryPoint = "App.qml"  // optional: auto-discovers Main.qml or main.qml if not set
        sourcePath = "src/ui/qml"  // default: "src/main/qml"
        importPaths.from(file("/workspace/custom/qml"))  // optional: for adding additional QML modules
    }

    // optional configuration
    qtLibraryPath = "/path/to/Qt/libs"
    qtBridgeLibraryPath = "/path/to/bridge/native/lib"
}
```
If you provide a `name` and `mainClass` in the configuration above, you can launch your app directly from the terminal:
```shell
./gradlew myApp
```

### Gradle Plugin and Maven Artifacts

The Qt Bridge plugin core functions are:

- Detecting the host OS and architecture
- Resolving and downloading the correct Qt and Qt Bridge artifacts if not specified
- Configuring the application entry point (main class, JVM args)
- Managing QML source folder, imports, and automatic main QML discovery
- Providing optional overrides for Qt and native library paths

#### Under the Hood: Artifacts management
The plugin manages three critical components to ensure your application runs seamlessly across different environments:

| Artifact                     | Description                      | Selection Logic                    |
|------------------------------|----------------------------------|------------------------------------|
| **Qt Bridge SDK (JAR)**      | Java/Kotlin API classes          | Latest version (currently 0.1)     |
| **Qt Bridge native library** | Platform-specific native binary  | Automatically matches host OS/Arch |
| **Qt Framework libraries**   | Required shared Qt libraries     | Defaults to 6.10.0                 |

#### Known issues
- **Version mismatch:** The plugin ensures stability by downloading synchronized versions of the Qt framework and
the Bridge native library (currently **Qt 6.10.0**). However, if you manually specify a local path for one (using
`qtLibraryPath` or `qtBridgeLibraryPath`) while letting the plugin download the other from the server, you risk a version mismatch.
- **Platform support limitations:** The plugin currently supports the following environments for automatic artifact downloading:
    - **macOS:** Apple Silicon (arm64)
    - **Linux:** x86_64 (Tested on Ubuntu 24.04)
    - **Windows:** Support coming soon.

## Bridge Building and Development

For building Java Bridge two environments are needed:
1. **Java Environment**: Java Development Kit (JDK) for compiling Java code, and using JNI bridge to
   facilitate communication between Java and native C++ methods.
2. **C++ Environment**: CMake and C++ compiler for configuring and compiling the native library

### 1. Java Environment

The Java Compiler (`javac`) is a key tool for compiling Java source code into bytecode that can be executed by the Java
Virtual Machine (JVM). Follow these steps to set up your Java environment:

- **Install the Java Development Kit (JDK)**:
    - Download and install the JDK from
      the [Oracle website](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or use a package
      manager for your operating system.
    - Ensure that the `JAVA_HOME` environment variable points to your JDK installation directory.
- **Verify Installation**: `javac -version`

Currently, we use **Java version 21** for the project.

### 2. C++ Environment

- **Install CMake**: Follow the instructions on the CMake website to install CMake for your operating system.
- **Install a C++ Compiler** Follow the instructions of your Qt version to set up the proper toolchain.

For CMake and C++ compiler versions follow the instructions of your Qt version.

### 3. Qt dependencies

To use Qt with JNI and Java, you'll need to install the necessary Qt libraries and configure them for your C++ project.
At minimum the project needs QtCore and QtDeclarative modules.

Current implementation is not bound to any specific Qt version, and versions starting from Qt 6.8 are expected to work.
It is however worth mentioning that Java bridge uses private Qt APIs to build and register metaobjects.  While those
APIs are fairly stable, it may also break portability.

### 4. Building and running the project

This project uses Gradle as the build system. All commands should be run from the project root directory using the
wrapper (`./gradlew`).

#### 1. Initial setup and gradle tasks

| Command              | Description                                                                        |
|----------------------|------------------------------------------------------------------------------------|
| ` gradle wrapper`    | Creates the Gradle wrapper scripts (`gradlew`/`gradlew.bat`).                      |
| `./gradlew tasks`    | Lists all available tasks within the project.                                      |
| `./gradlew check`    | Builds all artifacts, compiles the native bridge library, and runs all unit tests. |
| `./gradlew docs:all` | Builds java documentation                                                          |

#### 2. Managing the Native QtBridge Library

The project relies on a platform-specific native library (the QtBridge) for runtime execution. This library must be
located where the Java/Kotlin runtime can find it.

#### Required Library Files

Ensure the directory specified for library lookup contains the correct file for your operating system:

| Platform    | Required Library File     | Example Path                             |
|-------------|---------------------------|------------------------------------------|
| **Windows** | `QtBridgeNative.dll`      | `C:\QtBridge\lib`                        |
| **macOS**   | `libQtBridgeNative.dylib` | `/Users/username/QtBridge/lib/mac-arm64` |
| **Linux**   | `libQtBridgeNative.so`    | `/home/username/QtBridge/lib/x86_64`     |

#### A. Specifying the library path

If you have already built the native library, inform the JVM of its path using a system property or environment
variable.

| Option          | Description                         | Example Command                                                     |
|-----------------|-------------------------------------|---------------------------------------------------------------------|
| System Property | `-Dqtbridge.native.dir=<path>`      | `./gradlew check -Dqtbridge.native.dir=path/to/lib/mac-arm64`       |
| Env variable    | `export QTBRIDGE_NATIVE_DIR=<path>` | `export QTBRIDGE_NATIVE_DIR=path/to/lib/mac-arm64; ./gradlew check` |

#### B. Running example applications

Example applications (e.g., `colorpaletteclient`) support automatic library management:

- **Production Mode**: is when an application is using Qt Bridge plugin from MavenCentral or other different source.
  If the needed libraries (qt and qt bridge native) are missing, they will be automatically downloaded.
- **Development Mode**: is when an example application is running within this repository. In other words, using the local qmlbridge and the local qtbrige-plugin

| Mode                     | Command                                                          | Behavior                                                         |
|--------------------------|------------------------------------------------------------------|------------------------------------------------------------------|
| Production               | `./gradlew colorpaletteclient`                                   | The plugin downloads the native library if missing, then runs.   |
| Production & Development | `./gradlew colorpaletteclient -Dqtbridge.native.dir=path/to/lib` | Runs application with manually specified native library.         |
| Development              | `./gradlew colorpaletteclient`                                   | Builds the native library from source, then runs.                |

#### 3. Advanced exec options

| Task                                  | Configuration                                                        | Command Example                                                 |
|---------------------------------------|----------------------------------------------------------------------|-----------------------------------------------------------------|
| Run benchmarks                        | Enables benchmark tests using the -Pbenchmark property.              | `./gradlew check -Pbenchmark`                                   |
| Benchmarks with Path                  | Uses a manually specified qt bridge native library path.             | `./gradlew check -Pbenchmark -Dqtbridge.native.dir=path/to/lib` |
| Example in dev mode but force non-dev | Forces the plugin to download qt and qt bridge libraries if missing. | `./gradlew colorpaletteclient -Pqt.plugin.mode="non-dev"`       |
| Log level (critical,warning,info,off) | Defines log level to use at runtime, defaults to critical & warning  | `./gradlew colorpaletteclient -Pqtbridge.log.level=info`        |

#### Summary of configurations

| Option                         | Type                 | Target                                                     | Description                                                                    |
|--------------------------------|----------------------|------------------------------------------------------------|--------------------------------------------------------------------------------|
| `-Dqtbridge.native.dir=<path>` | System Property      | `check` task and example applications                      | Specifies the directory containing the compiled native library.                |
| `QTBRIDGE_NATIVE_DIR=<path>`   | Environment Variable | `check` task and example applications                      | Specifies the directory containing the compiled native library.                |
| `-Pbenchmark`                  | Project Property     | `check` task                                               | Enables benchmark tests during execution.                                      |
| `-Pqt.plugin.mode`             | Project Property     | Examples tasks or any project that applies qtbridge-plugin | Allows downloading qt bridge and qt libraries if missing in the caller project |
| `-Pqtbridge.native.buildtype`  | Project Property     | QtBridge native library build itself                       | Specifies the build type. Allowed values: release|debug. Default: release      |

## Java Bridge API Overview

### Classes and Annotations

#### QtQuickApplication

QtQuickApplication is the entrypoint for QML bridge execution. Its usage is:

```java
import org.qtbridge.app.QtQuickApplication;

public class Main {

    public static void main(String[] args) {
        final QtQuickApplication app = new QtQuickApplication(args);
        app.execute();
    }
}
```

#### @QMLRegistrable

@QMLRegistrable annotation marks classes for bridging to QML. Both singletons and regular
QML instantiable types can be registered. The annotation is analogous with having a C++ QObject
which defines a QML_ELEMENT macro.

Following shows the possible composition of such class:

```text
@QMLRegistrable(name, module, singleton)
 ├── QtProperty<type> (usually many)
 ├── QtListModel<type> (usually one or none)
 ├── All public Methods are registered as invokable functions (usually many)
 ├── @QMLSignals (signal interface) (one interface with many signals)
 └── @QMLComplete (optional, on completion handler annotation)
```

#### Properties (`QtProperty`)
  Properties are represented by instances of `QtProperty`. A `QtProperty` wraps a value and automatically notifies
  QML when the value changes. Updates from either the Java-side or the QML-side are reflected on the other side.
  QtProperty supports simple types (String, numbers, booleans, chars), `URI`, enums, Java collections,
  `Map<String, ?>`, plain arrays (primitive/boxed + String), and `@QMLRegistrable` objects.
  Collections/maps/arrays are stored as shallow snapshots (collections/maps as unmodifiable views).

  For observing changes on the QML-side normal QML bindings and signal catching mechanisms work.
  On the Java-side QtProperty the property value-change observation is provided with a callback
  mechanism (`onValueChanged`).

#### List Model (`QtListModel`)
  Java bridge provides an end-user API for bridging lists which can be used as list models
  on the QML side. QtListModel is a regular Java class with Java-like interface for storing
  items on a list. Under the hood this list is bridged to QML as a QAbstractListModel. Editing
  is possible from both Java- and QML side.

#### Public methods as Invokable Functions
  By default, all public methods defined in a `@QMLRegistrable`class are automatically registered
  as QML invokable slots. This means that any public method (without needing additional annotations)
  can be called from QML.

#### Signals (Using `@QMLSignals`)
  Instead of manually writing and managing signal methods inside your class, you define
  a separate Java interface representing your signals.

#### Completion Handler (Using @QMLComplete)
`@QMLComplete` marks a method to run after the QML engine finishes creating a @QMLRegistrable instance. The handler is
called for objects instantiated by QML. It is not invoked for singletons or for instances you create manually on the Java side.

The annotated method:
- Must take no parameters and must return void
- Must appear at most once per `QMLRegistrable`-annotated class

### Example Code

Following illustrates a simple Java-side example:

```java
// Registers this class as QML singleton
@QMLRegistrable(singleton = true)
public class FruitBasket {
    // Establishes a binding to a callback interface that is used to emit signals or notifications
    // from Java to QML. This allows QML to react to specific events like validation failures or updates.
    public interface QmlCallback {
        void basketSold(Integer price);
        void basketStolen();
    }
    @QMLSignals
    QmlCallback qmlCallback;

    // A QtListModel of strings, bridged to QML as a QAbstractListModel.
    // This allows it to be used in model-driven QML components
    public final QtListModel<String> fruitList =
            new QtListModel<>(new ArrayList<>(Arrays.asList("Mango", "Kiwi")));

    // An Integer bridged to QML. Can be used on QML-side as a standard read-write property
    public QtProperty<Integer> fruitBasketPrice = new QtProperty<>(24);

    {
        // Observe fruitList changes
        fruitList.onSizeChanged(() -> System.out.println("List size changed to: " + fruitList.size()));
        // Observe price changes
        fruitBasketPrice.onValueChanged(() -> System.out.println("Fruit basket price changed"));
    }

    // Function that is invokable from QML
    public void sellAllFruits() {
        System.out.println("Selling all fruits");
        // Inform QML that sale was a success
        qmlCallback.basketSold(25);
    }
}
```

## Terms and Conditions

If you, your employer, or the legal entity you act on behalf of hold commercial license(s) with a Qt
Group entity, Qt Bridges constitutes Pre-Release Code under the Qt License/Frame Agreement governing
those licenses, and that agreement's terms and conditions relating to Pre-Release Code apply to your
use of Qt Bridges as found in this repo.
This Qt Bridges repo may provide links or access to third-party libraries or code (collectively
"Third-Party Software") to implement various functions. Use or distribution of Third-Party Software
is discretionary and in all respects subject to applicable license terms of applicable third-party
right holders.

### Additional Terms and Conditions

The Qt Bridge for Java is built using the OpenJDK (https://openjdk.org) and Kotlin
(https://kotlinlang.org)

OpenJDK is licensed under the GNU General Public License, version 2, with the Classpath Exception.

Kotlin SDK and runtime are licensed under the Apache License, version 2.0

No modifications were made to the OpenJDK source code, and Qt Bridge for Java does not require any
modifications.

This project is not affiliated with or endorsed by Oracle and/or its affiliates.

"Java" and "OpenJDK" are trademarks or registered trademarks of Oracle and/or its affiliates.
