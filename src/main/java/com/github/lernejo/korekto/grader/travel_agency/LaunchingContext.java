package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;

@SubjectForToolkitInclusion
public class LaunchingContext {
    private final TravelAgencyApiClient client;
    public boolean compilationFailed;
    public boolean testFailed;

    LaunchingContext(TravelAgencyApiClient client) {
        this.client = client;
    }
}
