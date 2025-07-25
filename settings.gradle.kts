rootProject.name = "intellij-investor-dashboard"
pluginManagement {
    repositories {
        // 首先尝试阿里云的 Gradle 插件镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin/")
        }
        // 阿里云的公共仓库，可能也包含一些 Gradle 插件依赖
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
        }
        // 保留官方插件门户作为备用，如果阿里云没有找到，仍然可以从这里下载
        gradlePluginPortal()
        // 如果有必要，也可以保留或添加其他仓库，例如：
        // mavenCentral()
    }
}

// ... 其他内容 ...