package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenInvocationResult;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.PomModifier;
import com.github.lernejo.tack.http.HttpTack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Part1Grader implements PartGrader<LaunchingContext> {

    static String sbPluginGav = "org.springframework.boot:spring-boot-maven-plugin:2.6.1";

    @Override
    public String name() {
        return "Part 1 - Compilation & Tests";
    }

    @Override
    public Double maxGrade() {
        return 4.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        if (!Files.exists(context.getExercise().getRoot().resolve("pom.xml"))) {
            context.setCompilationFailed();
            return result(List.of("Not a Maven project"), 0.0D);
        }

        Path sitePath = context.getExercise().getRoot().resolve("site");
        Path sitePomPath = sitePath.resolve("pom.xml");
        if (Files.exists(sitePomPath)) {
            PomModifier.addRepository(sitePomPath, "jitpack.io", "https://jitpack.io");
            PomModifier.addDependency(sitePomPath, "com.github.lernejo", "http-tack", HttpTack.getVersion());
            HttpTack.installOnSources(sitePath.resolve("src/main/java"));
        }

        MavenInvocationResult invocationResult = MavenExecutor.executeGoal(context.getExercise(), context.getConfiguration().getWorkspace(), "clean", "test-compile");
        if (invocationResult.getStatus() != MavenInvocationResult.Status.OK) {
            context.setCompilationFailed();
            return result(List.of("Compilation failed, see `mvn test-compile`"), 0.0D);
        } else {
            // Download all needed deps without timer
            MavenExecutor.executeGoal(context.getExercise(), context.getConfiguration().getWorkspace(), sbPluginGav + ":help");
            // Install project dependencies to be able to execute a module solely even if it depends on another
            MavenExecutor.executeGoal(context.getExercise(), context.getConfiguration().getWorkspace(), "install");

            MavenInvocationResult testRun = MavenExecutor.executeGoal(context.getExercise(), context.getConfiguration().getWorkspace(), "verify");
            if (testRun.getStatus() != MavenInvocationResult.Status.OK) {
                context.setTestFailed();
                return result(List.of("There are test failures, see `mvn verify`"), maxGrade() / 2);
            } else {
                return result(List.of(), maxGrade());
            }
        }
    }
}
