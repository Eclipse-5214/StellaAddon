plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    alias(libs.plugins.ksp)
    `maven-publish`
}

val mc = stonecutter.current.version
val loader = "fabric"
val modID = property("mod.id") as String

version = "${property("mod.version")}+${mc}"
base.archivesName = modID

repositories {
    @Suppress("UnstableApiUsage")
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    mavenCentral()
    maven("https://jitpack.io")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
    strictMaven("https://repo.hypixel.net/repository/Hypixel", "net.hypixel")
    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://maven.teamresourceful.com/repository/maven-public/", "tech.thatgravyboat", "com.terraformersmc", "me.owdding")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    ksp(libs.stella.ksp)
    runtimeOnly(libs.devauth)

    implementation("fabric-api".mc(mc))
    implementation(libs.fabric.loader)
    implementation(libs.fabric.language.kotlin)
    implementation("stella".mc(mc))
}

ksp {
    arg("stella.id", modID)
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    //accessWidenerPath = rootProject.file("src/main/resources/template.accesswidener")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()
fun DependencyHandler.shadow(dep: Any) { include(dep); implementation(dep) }