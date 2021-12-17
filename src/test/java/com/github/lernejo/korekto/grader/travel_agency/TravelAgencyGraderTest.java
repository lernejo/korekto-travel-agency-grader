package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.misc.OS;
import com.github.lernejo.korekto.toolkit.misc.Processes;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TravelAgencyGraderTest {

    private static final Path workspace = Paths.get("target/test_repositories").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        Processes.launch(OS.Companion.getCURRENT_OS().deleteDirectoryCommand(workspace.resolve("lernejo")), null);
        LaunchingContext.RANDOM = i -> 2;
        System.setProperty("SERVER_START_TIMEOUT", "20");
    }

    @ParameterizedTest(name = "(branch={1}) {0}")
    @MethodSource("branches")
    @EnabledIfSystemProperty(named = "github_token", matches = ".+")
    void check_project_stages(String title, String branchName, List<GradePart> expectedGradeParts) {
        Grader grader = Grader.Companion.load();
        String repoUrl = grader.slugToRepoUrl("lernejo");
        GradingConfiguration configuration = new GradingConfiguration(repoUrl, "", "", workspace);

        GradingContext context = execute(branchName, grader, configuration);

        assertThat(context)
            .as("Grading context")
            .isNotNull();

        assertThat(context.getGradeDetails().getParts()).containsExactlyElementsOf(expectedGradeParts);
    }

    private GradingContext execute(String branchName, Grader grader, GradingConfiguration configuration) {
        AtomicReference<GradingContext> contextHolder = new AtomicReference<>();
        new GradingJob()
            .addCloneStep()
            .addStep("switch-branch",
                (context) -> context
                    .getExercise()
                    .lookupNature(GitNature.class)
                    .get()
                    .inContext(git -> {
                        git.checkout(branchName);
                    }))
            .addStep("grading", grader)
            .addStep("report", context -> contextHolder.set(context))
            .run(configuration, grader::gradingContext);
        return contextHolder.get();
    }

    static Stream<Arguments> branches() {
        return Stream.of(
            arguments("Complete exercise", "main", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 4.0D, 4.0D, List.of()),
                new GradePart("Part 5 - Prediction API", 2.0D, 2.0D, List.of())
            ))
            ,
            arguments("Initial state after using the template", "initial", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=France`: 404"))
            ))
            ,
            arguments("Missing POM", "fail/part1/missing_pom", List.of(
                new GradePart("Part 1 - Compilation & Tests", 0.0D, 4.0D, List.of("Not a Maven project")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/missing_pom` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Not trying to start **prediction-engine** server as compilation failed"))
            ))
            ,
            arguments("Compilation failure", "fail/part1/compilation_failure", List.of(
                new GradePart("Part 1 - Compilation & Tests", 0.0D, 4.0D, List.of("Compilation failed, see `mvn test-compile`")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/compilation_failure` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Not trying to start **prediction-engine** server as compilation failed"))
            ))
            ,
            arguments("Test failure", "fail/part1/test_failure", List.of(
                new GradePart("Part 1 - Compilation & Tests", 2.0D, 4.0D, List.of("There are test failures, see `mvn verify`")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/test_failure` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=France`: 404"))
            ))
            ,
            arguments("Partial code coverage", "fail/part3/partial_coverage", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 2.93D, 4.0D, List.of("Code coverage: 55.0%, expected: > 80% with `mvn verify`")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=France`: 404"))
            ))
            ,
            arguments("No code coverage", "fail/part3/no_coverage", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("No JaCoCo report produced after `mvn verify`, check tests and plugins")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=France`: 404"))
            ))
            ,
            arguments("Prediction API: different country", "fail/part4/different_country", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("GET `/api/temperature?country=France` should respond with a message containing the same country that was passed in the query, expected `France` but get `another_country`"))
            ))
            ,
            arguments("Prediction API: wrong number of temperature points", "fail/part4/wrong_number_of_temperatures", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("GET `/api/temperature?country=France` should respond with a message containing temperatures of the last *two* days, but got 1 temperature(s)"))
            ))
            ,
            arguments("Prediction API: out of range temperature", "fail/part4/out_of_range_temperature", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("GET `/api/temperature?country=France` should respond with temperatures generated from the given `countriesTempData.csv` file, however got a temperature of `666.0` for **France** whereas it should be between `8.0` and `32.0`"))
            ))
            ,
            arguments("Prediction API: no server", "fail/part4/no_server_started", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Server failed to start within 20 sec."))
            ))
            ,
            arguments("Prediction API: bad response type", "fail/part4/invalid_api_response_type", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=82828282-8282-4282-8282-828282828282@lernejo.fr`: 404")),
                new GradePart("Part 5 - Prediction API", 0.6666666666666667, 2.0D, List.of("""
                    Bad response payload expected something like:
                    ```
                    {
                        "country": "a country",
                        "temperatures: [
                            {
                                "date": "2021-12-16",
                                "temperature": 3.25
                            }, {
                                "date": "2021-12-15",
                                "temperature": 7.52
                            }
                        ]
                    }
                    ```
                    But got:
                    ```
                    66.6
                    ```"""))
            ))
        );
    }
}
