plugins {
    application
    alias(libs.plugins.openjfx)
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

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

val serverStartScripts = tasks.register<CreateStartScripts>("serverStartScripts") {
    mainClass.set("hashkitty.java.server.ServerApp")
    applicationName = "server"
    outputDir = layout.buildDirectory.get().dir("server-scripts").asFile
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

distributions {
    main {
        contents {
            from(serverStartScripts) {
                into("bin")
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}
