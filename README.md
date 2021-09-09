# Dash DAPI Client for JVM

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
- To use it with gradle, add `mavenLocal()` to the `repositories` list in your `build.gradle` file and add `org.dashj.platform:dapi-client:0.21-SNAPSHOT` as dependency. 

# Usage
Add mavenCentral() to the `repositories` list in your `build.gradle`
```groovy
dependencies {
    implementation 'org.dashj.platform:dpp:0.21-SNAPSHOT'
}
```

# Tests
Run tests with `gradle build test`

# KtLint
Check using ktlint:
```shell
./gradlew ktlintCheck
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
