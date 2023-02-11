package com.github.lernejo.korekto.grader.travel_agency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.GradingContext;
import com.github.lernejo.korekto.toolkit.partgrader.MavenContext;
import retrofit2.Retrofit;

import java.util.Random;
import java.util.function.Supplier;

public class LaunchingContext extends GradingContext implements MavenContext {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public final TravelAgencyApiClient travelAgencyApiClient;
    public final PredictionApiClient predictionApiClient;
    public final int predictionServerPort = 7000 + getRandomSource().nextInt(600);
    public final int siteServerPort = 9000 + getRandomSource().nextInt(600);
    public final long serverStartTimeout = Long.parseLong(System.getProperty("SERVER_START_TIMEOUT", "40"));

    private final Supplier<SilentJacksonConverterFactory.ExceptionHolder> exceptionHolderSupplier;
    private boolean compilationFailed;
    private boolean testFailed;
    private boolean siteServerFailed;

    LaunchingContext(GradingConfiguration configuration) {
        super(configuration);
        SilentJacksonConverterFactory jacksonConverterFactory = SilentJacksonConverterFactory.create(OBJECT_MAPPER);
        this.travelAgencyApiClient = new Retrofit.Builder()
            .baseUrl("http://localhost:" + siteServerPort + "/")
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create(TravelAgencyApiClient.class);

        this.predictionApiClient = new Retrofit.Builder()
            .baseUrl("http://localhost:" + predictionServerPort + "/")
            .addConverterFactory(jacksonConverterFactory)
            .build()
            .create(PredictionApiClient.class);
        this.exceptionHolderSupplier = jacksonConverterFactory::newExceptionHolder;
    }

    public void setSiteServerFailed() {
        siteServerFailed = true;
    }

    public boolean siteServerFailed() {
        return siteServerFailed;
    }

    public SilentJacksonConverterFactory.ExceptionHolder newExceptionHolder() {
        return exceptionHolderSupplier.get();
    }

    @Override
    public boolean hasCompilationFailed() {
        return compilationFailed;
    }

    @Override
    public boolean hasTestFailed() {
        return testFailed;
    }

    @Override
    public void markAsCompilationFailed() {
        compilationFailed = true;
        markAsTestFailed();
        setSiteServerFailed();
    }

    @Override
    public void markAsTestFailed() {
        testFailed = true;
    }
}
