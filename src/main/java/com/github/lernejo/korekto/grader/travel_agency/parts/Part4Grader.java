package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.grader.travel_agency.PredictionApiClient;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutionHandle;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import static com.github.lernejo.korekto.grader.travel_agency.TravelAgencyGrader.PREDICTION_ENGINE_PORT;

public class Part4Grader implements PartGrader {

    static String sbPluginGav = "org.springframework.boot:spring-boot-maven-plugin:2.6.1";

    long serverStartTimeout = Long.parseLong(System.getProperty("SERVER_START_TIMEOUT", "40"));

    static List<TempBoundaries> tempBoundaries = List.of(
        new TempBoundaries("Brazil", 3, 9),
        new TempBoundaries("Botswana", 19, 23),
        new TempBoundaries("France", 8, 32),
        new TempBoundaries("Guyana", 24, 36)
    );

    @Override
    public String name() {
        return "Part 4 - Prediction API";
    }

    @Override
    public Double maxGrade() {
        return 2.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.compilationFailed()) {
            return result(List.of("Not trying to start **prediction-engine** server as compilation failed"), 0.0D);
        }

        // Download all needed deps without timer
        MavenExecutor.executeGoal(context.getExercise(), context.getConfiguration().getWorkspace(), sbPluginGav + ":help");

        try (MavenExecutionHandle ignored = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
            sbPluginGav + ":run -pl :prediction-engine -Dspring-boot.run.jvmArguments='-Dserver.port=" + PREDICTION_ENGINE_PORT + "'")) {
            Ports.waitForPortToBeListenedTo(PREDICTION_ENGINE_PORT, TimeUnit.SECONDS, serverStartTimeout);

            double grade = maxGrade();
            List<String> errors = new ArrayList<>();

            TempBoundaries tempBoundaries = Part4Grader.tempBoundaries.get(LaunchingContext.RANDOM.nextInt(Part4Grader.tempBoundaries.size() - 1));
            String query = "GET `/api/temperature?country=" + tempBoundaries.country + "`";
            try {
                Response<PredictionApiClient.Prediction> response = context.predictionApiClient.getTemperature(tempBoundaries.country).execute();
                if (!response.isSuccessful()) {
                    grade = 0;
                    errors.add("Unsuccessful response of " + query + ": " + response.code());
                } else {
                    if (!tempBoundaries.country.equalsIgnoreCase(response.body().country())) {
                        grade -= maxGrade() / 2;
                        errors.add(query + " should respond with a message containing the same country that was passed in the query, " +
                            "expected `" + tempBoundaries.country + "` but get `" + response.body().country() + "`");
                    }
                    if (response.body().temperatures().size() != 2) {
                        grade -= maxGrade() / 2;
                        errors.add(query + " should respond with a message containing temperatures of the last *two* days, but got " + response.body().temperatures().size() + " temperature(s)");
                    }
                    for (PredictionApiClient.TempPoint tempPoint : response.body().temperatures()) {
                        if (tempPoint.temperature() < tempBoundaries.min || tempPoint.temperature() > tempBoundaries.max) {
                            grade -= maxGrade() / 2;
                            errors.add(query + " should respond with temperatures generated from the given `countriesTempData.csv` file, " +
                                "however got a temperature of `" + tempPoint.temperature() + "` for **" + tempBoundaries.country + "** " +
                                "whereas it should be between `" + tempBoundaries.min + "` and `" + tempBoundaries.max + "`");
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                return result(List.of("Failed to call **prediction-engine** API: " + e.getMessage()), 0.0D);
            }

            return result(errors, grade);
        } catch (CancellationException e) {
            return result(List.of("Server failed to start within " + serverStartTimeout + " sec."), 0.0D);
        } finally {
            Ports.waitForPortToBeFreed(PREDICTION_ENGINE_PORT, TimeUnit.SECONDS, 5L);
        }
    }

    record TempBoundaries(String country, double min, double max) {
    }
}
