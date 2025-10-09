plugins {
    application
    alias(libs.plugins.openjfx)
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // WebSocket support
    implementation(libs.java.websocket)

    // JavaFX
    implementation(libs.javafx.controls)
    implementation(libs.javafx.fxml)

    // QR Code Generation (ZXing)
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)

    // JSON Processing (Gson)
    implementation(libs.gson)

    // Zip file handling (for .hhk files)
    implementation(libs.zip4j)

    // SSH library for remote sniffing
    implementation(libs.jsch)

    // HTTP Client for API requests
    implementation(libs.okhttp)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("hashkitty.java.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

javafx {
    version = libs.versions.javafx.get()
    modules.set(listOf("javafx.controls", "javafx.fxml"))
}
