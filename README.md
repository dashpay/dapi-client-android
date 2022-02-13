# Dash DAPI Client for JVM

[![License](https://img.shields.io/github/license/dashevo/dapi-client-android)](https://github.com/dashevo/dapi-client-android/blob/master/LICENSE)
[![dashevo/android-dpp](https://tokei.rs/b1/github/dashevo/dapi-client-android?category=code)](https://github.com/dashevo/dapi-client-android)

| Branch | Tests                                                                                      | Coverage                                                                                                                             | Linting                                                                    |
|--------|--------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| master | [![Tests](https://github.com/dashevo/dapi-client-android/workflows/CI/badge.svg?branch=master)](https://github.com/dashevo/dapi-client-android/actions) | [![codecov](https://codecov.io/gh/dashevo/dapi-client-android/branch/master/graph/badge.svg)](https://codecov.io/gh/dashevo/dapi-client-android) | ![Lint](https://github.com/dashevo/dapi-client-android/workflows/Kotlin%20Linter/badge.svg) |



# Build
This depends on the `android-dpp` library
```
git clone https://github.com/github/dashevo/android-dpp.git
cd android-dpp
./gradlew assemble
```
Build this library:
```
git clone https://github.com/github/dashevo/dapi-client-android.git
cd dapi-client-android
./gradlew assemble
```
- After building, it will be available on the local Maven repository.
- To use it with gradle, add `mavenLocal()` to the `repositories` list in your `build.gradle` file and add `org.dashj.platform:dapi-client:0.22-SNAPSHOT` as dependency. 

# Usage
Add mavenCentral() to the `repositories` list in your `build.gradle`
```groovy
dependencies {
    implementation 'org.dashj.platform:dpp:0.22-SNAPSHOT'
}
```

# Tests
Run tests with `gradle build test`

# KtLint
Check using ktlint:
```shell
./gradlew ktlint
```
Format using ktlint:
```shell
./gradlew ktlintFormat
```

# Updating DPP
The .proto files are located here: https://github.com/dashevo/dapi-grpc.git (`/protos` directory)

In this project, they are in the `/src/main/proto` directory

# Publish to maven central
```  
./gradlew uploadArchives
```
