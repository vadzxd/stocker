import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.10"
    id("org.jetbrains.intellij.platform") version "2.0.1"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

//repositories {
//    mavenCentral()
//    intellijPlatform {
//        defaultRepositories()
//    }
//}

repositories {
    // 注释掉或删除原有的 mavenCentral() 和 intellijPlatform {}
    // mavenCentral() // 可以注释掉，因为阿里云包含了
    // intellijPlatform { defaultRepositories() } // 如果慢可以注释掉

    // 添加阿里云 Maven 中央仓库镜像
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/google/") // 如果有 Android 相关依赖，也加上
    maven("https://maven.aliyun.com/repository/gradle-plugin/") // Gradle 插件依赖

    // 对于 JetBrains 平台特定的依赖，可能需要单独配置 JetBrains 的镜像，或者保留原始配置作为补充
    // 如果阿里云镜像不包含所有的 JetBrains 依赖，可以保留：
    intellijPlatform {
        defaultRepositories() // 考虑是否保留，如果依然慢可以尝试查找 JetBrains 的国内镜像或不使用
    }
}

dependencies {
    implementation("org.apache.commons:commons-text:1.12.0")
    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))
        pluginVerifier()
        instrumentationTools()
    }
}

changelog {
    version.set(properties("pluginVersion"))
    path.set("${project.projectDir}/CHANGELOG.md")
    groups.set(emptyList())
}

val pluginDescription = """
    <div>
      <p>
        Stocker is a JetBrains IDE extension dashboard for investors to track
        realtime stock market conditions.
      </p>
      <h2>Tutorial</h2>
      <p>
        All instructions can be found at
        <a href="https://nszihan.com/2021/04/11/stocker">here</a>.
      </p>
      <h2>Licence</h2>
      <a href="https://raw.githubusercontent.com/WhiteVermouth/intellij-investor-dashboard/master/LICENSE">Apache 2.0 License</a>
      <h2>Donation</h2>
      <p>If you like this plugin, you can <a href="https://www.buymeacoffee.com/nszihan">buy me a cup of coffee</a>. Thank you!</p>
    </div>
""".trimIndent()

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")
        description = pluginDescription
        changeNotes = provider {
            changelog.renderItem(changelog.getLatest(), Changelog.OutputType.HTML)
        }
        ideaVersion {
            untilBuild = provider { null }
        }
    }
    publishing {
        token = System.getProperty("jetbrains.token")
    }
    pluginVerification {
        ides {
            recommended()
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity, IntelliJPlatformType.IntellijIdeaUltimate)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = "241"
                untilBuild = "242.*"
            }
        }
        failureLevel = listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS, VerifyPluginTask.FailureLevel.INVALID_PLUGIN
        )
    }
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}
