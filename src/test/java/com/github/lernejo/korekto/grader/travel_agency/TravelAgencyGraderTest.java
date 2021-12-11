package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.misc.OS;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TravelAgencyGraderTest {

    private final Path workspace = Paths.get("target/test_repositories");

    @BeforeEach
    void setUp() {
        OS.Companion.getCURRENT_OS().deleteDirectoryCommand(workspace);
    }

    @ParameterizedTest(name = "(branch={1}) {0}")
    @MethodSource("branches")
    @EnabledIfSystemProperty(named = "github_token", matches = ".+")
    void check_project_stages(String title, String branchName, List<GradePart> expectedGradeParts) {
        Grader grader = Grader.Companion.load();
        String repoUrl = grader.slugToRepoUrl("lernejo");
        GradingConfiguration configuration = new GradingConfiguration(repoUrl, "", "", workspace);

        AtomicReference<GradingContext> contextHolder = new AtomicReference<>();
        new GradingJob()
                .addCloneStep()
                .addStep("switch-branch",
                        (conf, context) -> context
                                .getExercise()
                                .lookupNature(GitNature.class)
                                .get()
                                .inContext(git -> git.checkout(branchName)))
                .addStep("grading", grader)
                .addStep("report", (conf, context) -> contextHolder.set(context))
                .run(configuration);

        assertThat(contextHolder)
                .as("Grading context")
                .hasValueMatching(Objects::nonNull, "is present");

        assertThat(contextHolder.get().getGradeDetails().getParts()).containsExactlyElementsOf(expectedGradeParts);
    }

    static Stream<Arguments> branches() {
        return Stream.of(
                arguments("Initial state after using the template", "main", List.of(
                        new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                        new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of())
                ))
        );
    }
}
