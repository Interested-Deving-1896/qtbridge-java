## QtBridge Application Quick Start

How to use an init script to create a new QtBridge application with all the necessary files and structure.

### Initialization Script
Get the starter script to your local machine:
<!-- TODO QTBUG-142220: Replace CURL url with actual release URL when available -->
```
# Download the init script alongside the settings.gradle
curl -O 'https://code.qt.io/cgit/qt/qtbridge-java.git/plain/examples/starter/qtbridge.starter.gradle.kts?h=dev' -O 'https://code.qt.io/cgit/qt/qtbridge-java.git/plain/examples/starter/settings.gradle.kts?h=dev'
```
### Check usage or help
```
gradle --init-script qtbridge.starter.gradle.kts usage
```

### Generate a project
Run the initialization script using Gradle. This will create a new project in your current directory.

#### Default generation (Java):
```
gradle --init-script qtbridge.starter.gradle.kts
```

#### Custom generation (Kotlin):
```
gradle --init-script qtbridge.starter.gradle.kts \
  -PprojectName=MyAwesomeApp \
  -PpackageName=com.company.app \
  -Planguage=kotlin
```

### Configuration Options
You can customize the project generation using the following Gradle properties (-P flags):

| Parameter     | Description                               | Default       | Required |
|---------------|-------------------------------------------|---------------|----------|
| `projectName` | Your application name                     | `MyApp`       | No       |
| `packageName` | Java/Kotlin package name                  | `com.example` | No       |
| `language`    | Programming language (`java` or `kotlin`) | `java`        | No       |


### Generation output

The starter script creates a fully configured QtBridge project:

```
MyAwesomeApp/
├── src/
│   └── main/
│       ├── kotlin/           (or java/)
│       │   └── com/
│       │       └── company/
│       │           └── app/
│       │               ├── Main.kt       (or Main.java)
│       │               └── Controller.kt    (or Controller.java)
│       └── qml/
│           └── main.qml
├── build.gradle.kts
└── settings.gradle.kts
```

### Running your project

Once your project (e.g., `MyAwesomeApp`) has been generated, navigate into the folder and run it:
```
cd MyAwesomeApp
gradle MyAwesomeApp
```
