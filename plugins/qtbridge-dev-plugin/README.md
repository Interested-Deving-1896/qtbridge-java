## Qt Dev Plugins

This repository provides **development-oriented Gradle plugins** used across the QtBridge ecosystem to standardize
common tasks like publishing, native building, and dependency embedding.

| Plugin ID                   | Description                                                                                   |
|:----------------------------|:----------------------------------------------------------------------------------------------|
| `qtbridge.dev-publish`      | Provides a **unified publishing workflow** for QtBridge modules (libraries & Gradle plugins). |
| `qtbridge.dev-build-native` | **Builds native** QtBridge components.                                                        |
| `qtbridge.dev-embed`        | **Embeds** QtBridge runtime dependencies into the distribution.                               |

***

### QtBridge Publish Plugin

The `qtbridge.dev-publish` plugin standardizes the publishing process for QtBridge libraries and Gradle plugins. It
configures publications, generates documentation artifacts, and enables optional signing depending on the selected
environment.


### Applying the Plugin

The `qtbridge.dev-publish` plugin standardizes the publishing process for QtBridge modules. It **automatically detects**
if the project is a Library or a Gradle Plugin, configures publications, generates documentation artifacts, and enables
optional signing depending on the selected environment.

### Applying the Plugin

Add the plugin to your module’s `build.gradle.kts`:

```kotlin
plugins {
    id("qtbridge.dev-publish")
}
```

### Configuring Publishing

The plugin automatically detects the module type based on the applied plugins (`java-library` or `java-gradle-plugin`).
You no longer need to manually specify the module type.

Use the qtBridgePublishing extension to specify publication metadata.

```kotlin
qtBridgePublishing {
    moduleName = "MyPlugin"
    moduleDescription = "Description of your plugin"
    developerEmail = "me@example.com"
}
```

### Gradle Properties Required for Publishing

You need to configure signing and repository credentials in your `gradle.properties` if you want to publish to
production.

| Plugin ID                    | Description                                        | Required for |
|:-----------------------------|:---------------------------------------------------|:-------------|
| `signing.key`                | GPG Private Key used for signing artifacts.        | Production   |
| `signing.password`           | Password for the GPG Private Key.                  | Production   |
| `staging.repo.release.url`   | URL for the staging repository (releases).         | Staging      |
| `staging.repo.snapshot.url`  | URL for the staging repository (snapshots).        | Staging      |
| `staging.repo.username`      | Username for staging repository access.            | Staging      |
| `staging.repo.password`      | Password for staging repository access.            | Staging      |
| `prod.repo.release.url`      | URL for the production repository (Maven Central). | Production   |
| `prod.repo.username`         | Username for production repository access.         | Production   |
| `prod.repo.password`         | Password for production repository access.         | Production   |

**Notes:**

- The snapshot repository URL is derived automatically if the project version ends with `-SNAPSHOT`.
- The `publish.env` property controls the target environment.

### Selecting the Publishing Environment

| Plugin ID                                     | Environment | Description                                                               | Signing |
|:----------------------------------------------|:------------|---------------------------------------------------------------------------|---------|
| `./gradlew publish`                           | Local       | Publishes to the local Maven cache (~/.m2/repository). (Default)          | No      |
| `./gradlew publish -Ppublish.env=staging`     | Staging     | Publishes to the defined staging.repo.* snapshot or release repositories. | No      |
| `./gradlew publish -Ppublish.env=production ` | Production  | Publishes to the defined prod.repo.* (usually Maven Central).             | Yes     |

* Use `staging` for test or internal releases.
* Use `production` for publishing to Maven Central (signing will be applied automatically).

---

### Complete Example

Here's a full example of a module configured for publishing:

```kotlin
plugins {
    id("qtbridge.dev-publish")
    id("java-library") // id("java-gradle-plugin")
}

qtBridgePublishing {
    moduleName = "Core Library"
    moduleDescription = "Core functionality for integration"
}
```

**Publishing commands:**

```bash
# Publish multiple modules to maven local
./gradlew :qmlbridge:publish
./gradlew :qtbridge-plugin:publish
```
