package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.grader.travel_agency.PredictionApiClient;
import com.github.lernejo.korekto.grader.travel_agency.parts.Dataset.TempBoundaries;
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


public class Part5Grader implements PartGrader {

    @Override
    public String name() {
        return "Part 5 - Prediction API";
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

        try (MavenExecutionHandle ignored = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
            Part1Grader.sbPluginGav + ":run -pl :prediction-engine -Dspring-boot.run.jvmArguments='-Dserver.port=" + context.predictionServerPort + "'")) {
            Ports.waitForPortToBeListenedTo(context.predictionServerPort, TimeUnit.SECONDS, context.serverStartTimeout);

            double grade = maxGrade();
            List<String> errors = new ArrayList<>();

            TempBoundaries tempBoundaries = Dataset.getOne();
            String query = "GET `/api/temperature?country=" + tempBoundaries.country() + "`";
            try (var exHolder = context.newExceptionHolder()) {
                Response<PredictionApiClient.Prediction> response = context.predictionApiClient.getTemperature(tempBoundaries.country()).execute();
                if (!response.isSuccessful()) {
                    grade = 0;
                    errors.add("Unsuccessful response of " + query + ": " + response.code());
                } else if (exHolder.getLatestDeserializationProblem() != null) {
                    grade -= maxGrade() * (2.0 / 3);
                    errors.add("Bad response payload expected something like:\n```\n" + PredictionApiClient.SAMPLE_RESPONSE_PAYLOAD + "\n```\nBut got:\n```\n" + exHolder.getLatestDeserializationProblem().rawBody() + "\n```");
                } else {
                    if (!tempBoundaries.country().equalsIgnoreCase(response.body().country())) {
                        grade -= maxGrade() / 2;
                        errors.add(query + " should respond with a message containing the same country that was passed in the query, " +
                            "expected `" + tempBoundaries.country() + "` but get `" + response.body().country() + "`");
                    }
                    if (response.body().temperatures().size() != 2) {
                        grade -= maxGrade() / 2;
                        errors.add(query + " should respond with a message containing temperatures of the last *two* days, but got " + response.body().temperatures().size() + " temperature(s)");
                    }
                    for (PredictionApiClient.TempPoint tempPoint : response.body().temperatures()) {
                        if (tempPoint.temperature() < tempBoundaries.min() || tempPoint.temperature() > tempBoundaries.max()) {
                            grade -= maxGrade() / 2;
                            errors.add(query + " should respond with temperatures generated from the given `countriesTempData.csv` file, " +
                                "however got a temperature of `" + tempPoint.temperature() + "` for **" + tempBoundaries.country() + "** " +
                                "whereas it should be between `" + tempBoundaries.min() + "` and `" + tempBoundaries.max() + "`");
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                return result(List.of("Failed to call **prediction-engine** API: " + e.getMessage()), 0.0D);
            }
            return result(errors, grade);
        } catch (CancellationException e) {
            return result(List.of("Server failed to start within " + context.serverStartTimeout + " sec."), 0.0D);
        } finally {
            Ports.waitForPortToBeFreed(context.predictionServerPort, TimeUnit.SECONDS, 5L);
        }
    }
}
