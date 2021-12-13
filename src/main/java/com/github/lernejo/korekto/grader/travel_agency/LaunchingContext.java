package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.GradingContext;

import java.util.Random;

public class LaunchingContext extends GradingContext {
    private static final Random r = new Random();
    // Mutable for tests
    public static RandomSupplier RANDOM = r::nextInt;

    public final TravelAgencyApiClient travelAgencyApiClient;
    public final PredictionApiClient predictionApiClient;
    private boolean compilationFailed;
    private boolean testFailed;
    private boolean predictionServerFailed;

    LaunchingContext(GradingConfiguration configuration, TravelAgencyApiClient travelAgencyApiClient, PredictionApiClient predictionApiClient) {
        super(configuration);
        this.travelAgencyApiClient = travelAgencyApiClient;
        this.predictionApiClient = predictionApiClient;
    }

    public void setCompilationFailed() {
        compilationFailed = true;
        setTestFailed();
    }

    public void setTestFailed() {
        testFailed = true;
        setPredictionServerFailed();
    }

    public void setPredictionServerFailed() {
        predictionServerFailed = true;
    }

    public boolean compilationFailed() {
        return compilationFailed;
    }

    public boolean testFailed() {
        return testFailed;
    }
}
