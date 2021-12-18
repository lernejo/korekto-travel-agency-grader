package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.misc.OS;
import com.github.lernejo.korekto.toolkit.misc.Processes;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.eclipse.jgit.api.ResetCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TravelAgencyGraderTest {

    private static final Path workspace = Paths.get("target/test_repositories").toAbsolutePath();

    @BeforeAll
    static void setUp() {
        Processes.launch(OS.Companion.getCURRENT_OS().deleteDirectoryCommand(workspace.resolve("lernejo")), null);
        AtomicInteger counter = new AtomicInteger();
        LaunchingContext.RANDOM = i -> counter.incrementAndGet() % i; // deterministic behavior
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
                        git.getGit().reset().setMode(ResetCommand.ResetType.HARD).call();
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
                new GradePart("Part 5 - Prediction API", 2.0D, 2.0D, List.of()),
                new GradePart("Part 6 - HTTP client and data coherence", 4.0D, 4.0D, List.of()),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Initial state after using the template", "initial", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=bbbcbdbe-bfc0-41c2-83c4-c5c6c7c8c9ca`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=Brazil`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Missing POM", "fail/part1/missing_pom", List.of(
                new GradePart("Part 1 - Compilation & Tests", 0.0D, 4.0D, List.of("Not a Maven project")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/missing_pom` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Not trying to start **prediction-engine** server as compilation failed")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Compilation failure", "fail/part1/compilation_failure", List.of(
                new GradePart("Part 1 - Compilation & Tests", 0.0D, 4.0D, List.of("Compilation failed, see `mvn test-compile`")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/compilation_failure` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Not trying to start **prediction-engine** server as compilation failed")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Test failure", "fail/part1/test_failure", List.of(
                new GradePart("Part 1 - Compilation & Tests", 2.0D, 4.0D, List.of("There are test failures, see `mvn verify`")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/test_failure` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=d5d6d7d8-d9da-4bdc-9dde-dfe0e1e2e3e4`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=France`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Partial code coverage", "fail/part3/partial_coverage", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 2.93D, 4.0D, List.of("Code coverage: 55.0%, expected: > 80% with `mvn verify`")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=ebecedee-eff0-41f2-b3f4-f5f6f7f8f9fa`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=Brazil`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("No code coverage", "fail/part3/no_coverage", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("No JaCoCo report produced after `mvn verify`, check tests and plugins")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=01020304-0506-4708-890a-0b0c0d0e0f10`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=Botswana`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: different country", "fail/part4/different_country", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=1718191a-1b1c-4d1e-9f20-212223242526`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("GET `/api/temperature?country=France` should respond with a message containing the same country that was passed in the query, expected `France` but get `another_country`")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: wrong number of temperature points", "fail/part4/wrong_number_of_temperatures", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=2d2e2f30-3132-4334-b536-3738393a3b3c`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("GET `/api/temperature?country=Brazil` should respond with a message containing temperatures of the last *two* days, but got 1 temperature(s)")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: out of range temperature", "fail/part4/out_of_range_temperature", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=43444546-4748-494a-8b4c-4d4e4f505152`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("GET `/api/temperature?country=Botswana` should respond with temperatures generated from the given `countriesTempData.csv` file, however got a temperature of `666.0` for **Botswana** whereas it should be between `19.0` and `23.0`")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: no server", "fail/part4/no_server_started", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=595a5b5c-5d5e-4f60-a162-636465666768`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Server failed to start within 20 sec.")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: bad response type", "fail/part4/invalid_api_response_type", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=6e6f7071-7273-4475-b677-78797a7b7c7d`: 404")),
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
                    ```""")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Bad Git history", "fail/partX/bad_git_history", List.of(
                new GradePart("Part 1 - Compilation & Tests", 0.0D, 4.0D, List.of("Not a Maven project")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/partX/bad_git_history` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Not trying to start **prediction-engine** server as compilation failed")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Git (proper descriptive messages)", -1.0D, null, List.of("`d360aea` Fix --> 1 word is too short", "`e744312` Another fix on  A --> Should be squashed on 470bae6")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Bad coding style", "fail/partY/bad_style", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 3.37D, 4.0D, List.of("Code coverage: 63.16%, expected: > 80% with `mvn verify`")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "Unsuccessful response of GET `/api/travels?userName=86878889-8a8b-4c8d-8e8f-909192939495`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Unsuccessful response of GET `/api/temperature?country=France`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence", 0.0D, 4.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -3.0D, null, List.of("fr.lernejo.prediction.LongClass\n" +
                    "            * L.3: Class has 113 lines, exceeding the maximum of 80", "fr.lernejo.travelsite.LongMethod\n" +
                    "            * L.5: Method has 44 lines, exceeding the maximum of 15", "fr.lernejo.travelsite.Pojo\n" +
                    "            * L.5: The field `machin` must have modifier `final`"))
            ))
        );
    }
}
