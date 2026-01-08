## QtBridge Project Starter

Quick and easy way to create a new QtBridge application with all the necessary files and structure.

### Installation
Download the starter script to your local machine:

```
# Download the init script alongside the settings.gradle
# todo QTBUG-142220: Replace with actual release URL when available
curl -O url/to/qtbridge.starter.gradle.kts -O url/to/settings.gradle.kts
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
