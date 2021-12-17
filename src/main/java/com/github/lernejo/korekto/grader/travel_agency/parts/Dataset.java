package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;

import java.util.List;

class Dataset {

    static List<TempBoundaries> tempBoundaries = List.of(
        new TempBoundaries("Brazil", 3, 9),
        new TempBoundaries("Botswana", 19, 23),
        new TempBoundaries("France", 8, 32),
        new TempBoundaries("Guyana", 24, 36)
    );

    static TempBoundaries getOne() {
        return tempBoundaries.get(LaunchingContext.RANDOM.nextInt(tempBoundaries.size() - 1));
    }

    static TempBoundaries getByCountry(String country) {
        return tempBoundaries.stream()
            .filter(tb -> tb.country.equalsIgnoreCase(country))
            .findFirst().orElse(null);
    }

    record TempBoundaries(String country, double min, double max) {
    }
}
