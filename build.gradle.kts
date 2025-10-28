import com.android.build.gradle.BaseExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Global configuration for all test tasks
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

buildscript {
    dependencies {
        classpath(libs.jacoco.core) // Use the latest version
        classpath(libs.vanniktech.jacoco.plugin)
    }
    repositories {
        google()
    }
}

// Dynamically configure Android subprojects
subprojects {
    // afterEvaluate ensures this logic runs after the subproject has been configured
    afterEvaluate {
        if (project.plugins.hasPlugin("com.android.library") || project.plugins.hasPlugin("com.android.application")) {

            apply(plugin = "jacoco")

            configure<JacocoPluginExtension> {
                toolVersion = libs.versions.jacoco.get()
            }

            val androidExtension = project.extensions.getByType(BaseExtension::class)

            // Loop through all flavors and build types to create a task for each variant
            androidExtension.productFlavors.ifEmpty { listOf(null) }.forEach { flavor ->
                androidExtension.buildTypes.forEach { buildType ->

                    val variantNameUpper = buildType.name.replaceFirstChar { it.uppercase() }
                    val flavorNameUpper = flavor?.name?.replaceFirstChar { it.uppercase() } ?: ""
                    val testTaskName = "test${flavorNameUpper}${variantNameUpper}UnitTest"
                    val jacocoReportTaskName = "jacoco${flavorNameUpper}${variantNameUpper}Report"
                    val jacocoCoverageReportTaskName = "jacoco${flavorNameUpper}${variantNameUpper}CoverageReport"


                    // The directory name where AGP generates the .exec file
                    val testCoverageDir = if (flavor?.name?.isNotBlank() == true) {
                        "${flavor.name}${variantNameUpper}UnitTest"
                    } else {
                        "${buildType.name}UnitTest"
                    }

                    // Define files to exclude from the report (generated code, Dagger, etc.)
                    val coverageExclusions = listOf(
                        "**/*ScreenKt.class",
                        "**/*Activity**",
                        "**/*ActivityKt*",
                        "**/*Fragmnt.class",
                        "**/*FragmentKt*",
                        "**/dagger/**",
                        "**/hilt/**",
                        "**/generated/**",
                        "**/*_HiltComponents*",
                        "**/*Dagger*",
                        "**/*Hilt*",
                        "**/*\$*",
                        "**/*_Factory*",
                        "**/*_Impl*",
                        "**/*_MembersInjector*",
                        "**/*_Module*",
                        "**/*_Subcomponent*",
                        "**/*_Component*",
                        "**/theme/**",
                        "**/di/**",
                        "**/components/**",
                        "**/navigation/**",
                        "**AppDatabase**",
                        "**/data/mapper/**",
                    )

                    tasks.register<JacocoReport>(jacocoReportTaskName) {
                        group = "Reporting"
                        description =
                            "Generate Jacoco coverage reports for the ${flavor?.name ?: ""} ${buildType.name} variant."

                        dependsOn(testTaskName)

                        finalizedBy(tasks.named(jacocoCoverageReportTaskName).get())
                        jacocoClasspath = configurations.getByName("jacocoAnt")

                        // CRITICAL: Path to your compiled Kotlin/Java classes.
                        // A mismatch here is the #1 cause of empty reports!
                        classDirectories.from(
                            fileTree(project.buildDir) {
                                include(
                                    "**/tmp/kotlin-classes/${buildType.name}/**/*.class",
                                    "**/intermediates/javac/${buildType.name}/classes/**/*.class"
                                )
                                exclude(coverageExclusions)
                            }
                        )

                        // CRITICAL: Path to your source code for generating human-readable reports.
                        sourceDirectories.setFrom(
                            files(
                                "${project.projectDir}/src/main/kotlin",
                                "${project.projectDir}/src/main/java"
                            )
                        )

                        // CRITICAL: Path to the raw execution data file (*.exec) generated by the test task.
                        executionData.setFrom(
                            project.layout.buildDirectory.file("outputs/unit_test_code_coverage/$testCoverageDir/$testTaskName.exec")
                        )

                        reports {
                            xml.required.set(true)
                            html.required.set(true)
                            html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html/$testCoverageDir"))
                        }
                    }

                    tasks.register<JacocoCoverageVerification>(jacocoCoverageReportTaskName) {
                        // classDirectories e executionData devem ser os mesmos do generateJacocoCoverageReport
                        val reportTask = tasks.named<JacocoReport>(jacocoReportTaskName).get()

                        classDirectories.setFrom(reportTask.classDirectories)
                        executionData.setFrom(reportTask.executionData)

                        violationRules {
                            rule {
                                limit {
                                    counter = "LINE"
                                    value = "COVEREDRATIO"
                                    // mínimo de 80% de cobertura
                                    minimum = "0.8".toBigDecimal()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}