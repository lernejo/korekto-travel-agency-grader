package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenInvocationResult;

import java.nio.file.Files;
import java.util.List;

public class Part1Grader implements PartGrader {
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
        MavenInvocationResult invocationResult = MavenExecutor.executeGoal(context.getExercise(), context.getConfiguration().getWorkspace(), "clean", "test-compile");
        if (invocationResult.getStatus() != MavenInvocationResult.Status.OK) {
            context.setCompilationFailed();
            return result(List.of("Compilation failed, see `mvn test-compile`"), 0.0D);
        } else {
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
