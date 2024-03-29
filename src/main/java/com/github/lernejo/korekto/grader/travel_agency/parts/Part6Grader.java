package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.grader.travel_agency.PredictionApiClient;
import com.github.lernejo.korekto.grader.travel_agency.TravelAgencyApiClient;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutionHandle;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Part6Grader implements PartGrader<LaunchingContext> {

    private static final Logger logger = LoggerFactory.getLogger(Part6Grader.class);

    private static List<String> countries = List.of(
        "Bahrain",
        "Bangladesh",
        "Barbados",
        "Belarus",
        "Belgium",
        "Belize",
        "Benin",
        "Bhutan",
        "Bolivia",
        "Bosnia",
        "Botswana",
        "Brazil",
        "Brunei",
        "Bulgaria",
        "Eritrea",
        "Estonia",
        "Eswatini",
        "Ethiopia",
        "Fiji",
        "Finland",
        "France",
        "Gabon"
    );

    private final TravelAgencyApiClient.WeatherExpectation weatherExpectation;

    public Part6Grader(TravelAgencyApiClient.WeatherExpectation weatherExpectation) {
        this.weatherExpectation = weatherExpectation;
    }

    @Override
    public String name() {
        return "Part 6 - HTTP client and data coherence (" + weatherExpectation.name().toLowerCase(Locale.ROOT) + ")";
    }

    @Override
    public Double maxGrade() {
        return 2.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.hasCompilationFailed()) {
            return result(List.of("Not trying to start **site** server as compilation failed"), 0.0D);
        }
        if (context.siteServerFailed()) {
            return result(List.of("Skipping due to previous errors"), 0.0D);
        }
        TravelAgencyApiClient.Inscription inscription = generateInscription();

        int userCountryTemp = LaunchingContext.getRandomSource().nextInt(15) + 15;
        logger.info("Using " + inscription);
        Set<String> expectedMatchingCountries = buildMatchingCountries(inscription.userCountry());
        logger.info("Matching countries " + expectedMatchingCountries);
        var predictionFunction = buildPredictionFunction(userCountryTemp, expectedMatchingCountries, inscription);

        try (PredictionServer predictionServer = new PredictionServer(context.predictionServerPort, predictionFunction);
             MavenExecutionHandle ignored = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
                 Part1Grader.sbPluginGav + ":run -pl :site -Dspring-boot.run.jvmArguments='-Dserver.port=" + context.siteServerPort + " -DtackEnabled=true  -DtackRedirectPort=" + context.predictionServerPort + "'")) {
            Ports.waitForPortToBeListenedTo(context.siteServerPort, TimeUnit.SECONDS, context.serverStartTimeout);

            String inscriptionQuery = "POST `/api/inscription`";
            try {
                Response<Void> inscriptionResponse = context.travelAgencyApiClient.postInscription(inscription).execute();
                if (!inscriptionResponse.isSuccessful()) {
                    return result(List.of("Unsuccessful response of " + inscriptionQuery + ": " + inscriptionResponse.code()), 0.0D);
                }
            } catch (IOException e) {
                return result(List.of("Failed to call **site** " + inscriptionQuery + ": " + e.getMessage()), 0.0D);
            }


            String travelsQuery = "GET `/api/travels?userName=" + inscription.userName() + "`";
            try (var exHolder = context.newExceptionHolder()) {
                Response<List<TravelAgencyApiClient.Travel>> travelsResponse = context.travelAgencyApiClient.getTravels(inscription.userName()).execute();
                if (!travelsResponse.isSuccessful()) {
                    return result(List.of("Unsuccessful response of " + travelsQuery + ": " + travelsResponse.code()), 0.0D);
                } else if (exHolder.getLatestDeserializationProblem() != null) {
                    return result(List.of("Bad response payload expected something like:\n```\n" + TravelAgencyApiClient.SAMPLE_RESPONSE_PAYLOAD + "\n```\nBut got:\n```\n" + exHolder.getLatestDeserializationProblem().rawBody() + "\n```"), 0.0D);
                }
                if (predictionServer.exchanges.isEmpty()) {
                    return result(List.of("Expected calls to the prediction-engine API, but none was recorded"), 0.0D);
                }

                Set<String> actualMatchingCountries = travelsResponse.body().stream().map(t -> t.country().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
                if (!actualMatchingCountries.equals(expectedMatchingCountries)) {
                    return result(List.of(
                        "Expected the following countries to be returned:\n\t* "
                            + expectedMatchingCountries.stream().collect(Collectors.joining("\n\t* "))
                            + "\nBut found :\n\t* "
                            + actualMatchingCountries.stream().collect(Collectors.joining("\n\t* "))
                    ), 0.0D);
                }
            } catch (IOException e) {
                return result(List.of("Failed to call **site** " + travelsQuery + ": " + e.getMessage()), 0.0D);
            }

            return result(List.of(), maxGrade());
        } catch (CancellationException e) {
            return result(List.of("Server failed to start within " + context.serverStartTimeout + " sec."), 0.0D);
        } finally {
            Ports.waitForPortToBeFreed(context.siteServerPort, TimeUnit.SECONDS, 5L);
        }
    }

    private static Function<String, Optional<PredictionApiClient.Prediction>> buildPredictionFunction(int userCountryTemp, Set<String> matchingCountries, TravelAgencyApiClient.Inscription inscription) {
        return country -> {
            String lCountry = country.toLowerCase(Locale.ROOT);
            final PredictionApiClient.Prediction prediction;
            if (inscription.userCountry().equals(lCountry) || !matchingCountries.contains(lCountry)) {
                prediction = buildPrediction(country, userCountryTemp);
            } else {
                int diff = inscription.weatherExpectation() == TravelAgencyApiClient.WeatherExpectation.WARMER ? inscription.minimumTemperatureDistance() + 2 : (-inscription.minimumTemperatureDistance() - 2);
                double matchingTemp = userCountryTemp + diff;
                prediction = buildPrediction(country, matchingTemp);
            }
            //logger.info("" + prediction);
            return Optional.of(prediction);
        };
    }

    private static PredictionApiClient.Prediction buildPrediction(String country, double temp) {
        String date1 = LocalDate.now().toString();
        double temp1 = temp + 1;
        String date2 = LocalDate.now().minusDays(1).toString();
        double temp2 = temp - 1;
        return new PredictionApiClient.Prediction(country, List.of(
            new PredictionApiClient.TempPoint(date1, temp1),
            new PredictionApiClient.TempPoint(date2, temp2)
        ));
    }

    private TravelAgencyApiClient.Inscription generateInscription() {
        String userName = LaunchingContext.getRandomSource().nextUuid().toString().toLowerCase(Locale.ROOT);
        String userCountry = countries.get(LaunchingContext.getRandomSource().nextInt(countries.size() - 1));
        int minimumTemperatureDistance = LaunchingContext.getRandomSource().nextInt(10) + 6;

        return new TravelAgencyApiClient.Inscription(
            userName + "@lernejo.fr",
            userName,
            userCountry.toLowerCase(Locale.ROOT),
            weatherExpectation,
            minimumTemperatureDistance);
    }

    private Set<String> buildMatchingCountries(String userCountry) {
        int nbrOfMatchingCountries = LaunchingContext.getRandomSource().nextInt(4) + 2;
        Set<String> matchingCountries = new HashSet<>();
        do {
            String country = countries.get(LaunchingContext.getRandomSource().nextInt(countries.size() - 1)).toLowerCase(Locale.ROOT);
            if (!userCountry.equals(country) && !matchingCountries.contains(country)) {
                matchingCountries.add(country);
            }
        } while (matchingCountries.size() < nbrOfMatchingCountries);
        return matchingCountries;
    }
}
