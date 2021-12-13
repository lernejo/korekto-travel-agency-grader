package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.GradingContext;

import java.util.Random;
import java.util.function.Supplier;

public class LaunchingContext extends GradingContext {
    private static final Random r = new Random();
    // Mutable for tests
    public static RandomSupplier RANDOM = r::nextInt;

    public final TravelAgencyApiClient travelAgencyApiClient;
    public final PredictionApiClient predictionApiClient;
    private final Supplier<SilentJacksonConverterFactory.ExceptionHolder> exceptionHolderSupplier;
    private boolean compilationFailed;
    private boolean testFailed;
    private boolean predictionServerFailed;

    LaunchingContext(GradingConfiguration configuration, TravelAgencyApiClient travelAgencyApiClient, PredictionApiClient predictionApiClient, Supplier<SilentJacksonConverterFactory.ExceptionHolder> exceptionHolderSupplier) {
        super(configuration);
        this.travelAgencyApiClient = travelAgencyApiClient;
        this.predictionApiClient = predictionApiClient;
        this.exceptionHolderSupplier = exceptionHolderSupplier;
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

    public SilentJacksonConverterFactory.ExceptionHolder newExceptionHolder() {
        return exceptionHolderSupplier.get();
    }
}
