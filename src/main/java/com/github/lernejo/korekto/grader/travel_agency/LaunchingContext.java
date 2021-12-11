package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.GradingContext;

public class LaunchingContext extends GradingContext {
    public final TravelAgencyApiClient client;
    public boolean compilationFailed;
    public boolean testFailed;

    LaunchingContext(GradingConfiguration configuration, TravelAgencyApiClient client) {
        super(configuration);
        this.client = client;
    }
}
