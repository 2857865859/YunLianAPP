pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        // 阿里云镜像：国内可直连，优先使用，避免去连被墙的 Gradle Plugin Portal
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") } // 镜像 Gradle Plugin Portal（KSP 插件标记在这里）
        maven { url = uri("https://maven.aliyun.com/repository/google") }         // 镜像 Google Maven
        maven { url = uri("https://maven.aliyun.com/repository/public") }          // 镜像 Maven Central 等公共仓
        // 兜底（若上面镜像不可用，仍会回退到这里）
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "yunlian"

include(":app")
