package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.misc.OS;
import com.github.lernejo.korekto.toolkit.misc.Processes;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.eclipse.jgit.api.ResetCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TravelAgencyGraderTest {

    private static final Path workspace = Paths.get("target/test_repositories").toAbsolutePath();

    @BeforeAll
    static void setUpAll() {
        Processes.launch(OS.Companion.getCURRENT_OS().deleteDirectoryCommand(workspace.resolve("lernejo")), null);
        System.setProperty("SERVER_START_TIMEOUT", "20");
    }

    @BeforeEach
    void setUp() {
        AtomicInteger counter = new AtomicInteger();
        LaunchingContext.RANDOM = i -> counter.incrementAndGet() % i; // deterministic behavior
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

        assertThat(context.getGradeDetails().getParts())
            .usingComparatorForType(new GrapePartComparator(), GradePart.class)
            .containsExactlyElementsOf(expectedGradeParts);
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
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 2.0D, 2.0D, List.of()),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 2.0D, 2.0D, List.of()),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Initial state after using the template", "initial", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("\\pUnsuccessful response of GET `/api/temperature\\?country=.+`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
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
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Not trying to start **site** server as compilation failed")),
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
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Test failure", "fail/part1/test_failure", List.of(
                new GradePart("Part 1 - Compilation & Tests", 2.0D, 4.0D, List.of("There are test failures, see `mvn verify`")),
                new GradePart("Part 2 - CI", 1.0D, 2.0D, List.of("Latest CI run of branch `fail/part1/test_failure` was expected to be in *success* state but found: failure")),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("Coverage not available when there is test failures")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("\\pUnsuccessful response of GET `/api/temperature\\?country=.+`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Partial code coverage", "fail/part3/partial_coverage", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 2.93D, 4.0D, List.of("Code coverage: 55.0%, expected: > 80% with `mvn verify`")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("\\pUnsuccessful response of GET `/api/temperature\\?country=.+`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("No code coverage", "fail/part3/no_coverage", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 0.0D, 4.0D, List.of("No JaCoCo report produced after `mvn verify`, check tests and plugins")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("\\pUnsuccessful response of GET `/api/temperature\\?country=.+`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: different country", "fail/part4/different_country", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("\\pGET `/api/temperature\\?country=(?<country>.+)` should respond with a message containing the same country that was passed in the query, expected `\\k<country>` but get `another_country`")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: wrong number of temperature points", "fail/part4/wrong_number_of_temperatures", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("\\pGET `/api/temperature\\?country=.+` should respond with a message containing temperatures of the last \\*two\\* days, but got 1 temperature\\(s\\)")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: out of range temperature", "fail/part4/out_of_range_temperature", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 1.0D, 2.0D, List.of("\\pGET `/api/temperature\\?country=(?<country>.+)` should respond with temperatures generated from the given `countriesTempData.csv` file, however got a temperature of `666.0` for \\*\\*\\k<country>\\*\\* whereas it should be between `19.0` and `23.0`")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: no server", "fail/part4/no_server_started", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("Server failed to start within 20 sec.")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Prediction API: bad response type", "fail/part4/invalid_api_response_type", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 4.0D, 4.0D, List.of()),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
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
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
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
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Not trying to start **site** server as compilation failed")),
                new GradePart("Git (proper descriptive messages)", -1.0D, null, List.of("`d360aea` Fix --> 1 word is too short", "`e744312` Another fix on  A --> Should be squashed on 470bae6")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            ))
            ,
            arguments("Bad coding style", "fail/partY/bad_style", List.of(
                new GradePart("Part 1 - Compilation & Tests", 4.0D, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 2.0D, 2.0D, List.of()),
                new GradePart("Part 3 - Code Coverage", 3.37D, 4.0D, List.of("Code coverage: 63.16%, expected: > 80% with `mvn verify`")),
                new GradePart("Part 4 - Site API structure", 0.0D, 4.0D, List.of("Unsuccessful response of POST `/api/inscription`: 404", "\\pUnsuccessful response of GET `/api/travels\\?userName=.{36}`: 404")),
                new GradePart("Part 5 - Prediction API", 0.0D, 2.0D, List.of("\\pUnsuccessful response of GET `/api/temperature\\?country=.+`: 404")),
                new GradePart("Part 6 - HTTP client and data coherence (colder)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Part 6 - HTTP client and data coherence (warmer)", 0.0D, 2.0D, List.of("Skipping due to previous errors")),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -3.0D, null, List.of("fr.lernejo.prediction.LongClass\n" +
                    "            * L.3: Class has 113 lines, exceeding the maximum of 80", "fr.lernejo.travelsite.LongMethod\n" +
                    "            * L.5: Method has 44 lines, exceeding the maximum of 15", "fr.lernejo.travelsite.Pojo\n" +
                    "            * L.5: The field `machin` must have modifier `final`"))
            ))
        );
    }

    public static final class GrapePartComparator implements Comparator<GradePart> {

        private static final Logger logger = LoggerFactory.getLogger(GrapePartComparator.class);

        @Override
        public int compare(GradePart o1, GradePart o2) {
            boolean sameId = Objects.equals(o1.getId(), o2.getId());
            boolean sameGrade = Objects.equals(o1.getGrade(), o2.getGrade());
            boolean sameMaxGrade = Objects.equals(o1.getMaxGrade(), o2.getMaxGrade());

            int differentMessages = 0;
            List<String> leftComments = new ArrayList<>(o1.getComments());
            for (String expectedComment : o2.getComments()) {
                if(expectedComment.startsWith("\\p")) {
                    String rawPattern = expectedComment.substring(2);
                    Pattern expectedPattern = Pattern.compile(rawPattern);
                    Optional<String> first = leftComments.stream().filter(c -> expectedPattern.matcher(c).matches()).findFirst();
                    if (first.isPresent()) {
                        leftComments.remove(first.get());
                    } else {
                        //logger.warn("Pattern [" + rawPattern + "] did not match anything among: " + o1.getComments());
                        differentMessages++;
                    }
                } else {
                    if(leftComments.contains(expectedComment)) {
                        leftComments.remove(expectedComment);
                    } else {
                        differentMessages++;
                    }
                }
            }

            return (sameId ? 0 : 10000) + (sameGrade ? 0 : 1000) + (sameMaxGrade ? 0 : 100) + differentMessages;
        }
    }
}
