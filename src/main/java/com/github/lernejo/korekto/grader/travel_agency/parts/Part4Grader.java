package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.grader.travel_agency.TravelAgencyApiClient;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutionHandle;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class Part4Grader implements PartGrader {

    @Override
    public String name() {
        return "Part 4 - Site API structure";
    }

    @Override
    public Double maxGrade() {
        return 4.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.compilationFailed()) {
            return result(List.of("Not trying to start **site** server as compilation failed"), 0.0D);
        }

        try (PredictionServer predictionServer = new PredictionServer(context.predictionServerPort);
             MavenExecutionHandle ignored = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
                 Part1Grader.sbPluginGav + ":run -pl :site -Dspring-boot.run.jvmArguments='-Dserver.port=" + context.siteServerPort + " -DtackEnabled=true  -DtackRedirectPort=" + context.predictionServerPort + "'")) {
            Ports.waitForPortToBeListenedTo(context.siteServerPort, TimeUnit.SECONDS, context.serverStartTimeout);

            double grade = maxGrade();
            List<String> errors = new ArrayList<>();

            String inscriptionQuery = "POST `/api/inscription`";

            TravelAgencyApiClient.Inscription inscription = generateInscription();
            String travelsQuery = "GET `/api/travels?userName=" + inscription.userName() + "`";
            try {
                Response<Void> inscriptionResponse = context.travelAgencyApiClient.postInscription(inscription).execute();
                if (!inscriptionResponse.isSuccessful()) {
                    grade -= maxGrade() / 2;
                    errors.add("Unsuccessful response of " + inscriptionQuery + ": " + inscriptionResponse.code());
                }
            } catch (IOException e) {
                return result(List.of("Failed to call **site** " + inscriptionQuery + ": " + e.getMessage()), 0.0D);
            }

            try {
                Response<List<TravelAgencyApiClient.Travel>> travelsResponse = context.travelAgencyApiClient.getTravels(inscription.userName()).execute();
                if (!travelsResponse.isSuccessful()) {
                    grade -= maxGrade() / 2;
                    errors.add("Unsuccessful response of " + travelsQuery + ": " + travelsResponse.code());
                } else if (travelsResponse.body().isEmpty()) {
                    grade -= maxGrade() * (1.0 / 3);
                    errors.add("Empty response of " + travelsQuery + ": []");
                }
            } catch (IOException e) {
                return result(List.of("Failed to call **site** " + travelsQuery + ": " + e.getMessage()), 0.0D);
            }

            return result(errors, grade);
        } catch (CancellationException e) {
            return result(List.of("Server failed to start within " + context.serverStartTimeout + " sec."), 0.0D);
        } finally {
            Ports.waitForPortToBeFreed(context.siteServerPort, TimeUnit.SECONDS, 5L);
        }
    }

    private TravelAgencyApiClient.Inscription generateInscription() {
        String username = LaunchingContext.RANDOM.nextUuid().toString().toLowerCase(Locale.ROOT);
        return new TravelAgencyApiClient.Inscription(
            username,
            username + "@lernejo.fr",
            Dataset.getOne().country(),
            LaunchingContext.RANDOM.nextBoolean() ? TravelAgencyApiClient.WeatherExpectation.WARMER : TravelAgencyApiClient.WeatherExpectation.COLDER,
            LaunchingContext.RANDOM.nextInt(6) + 4
        );
    }
}
