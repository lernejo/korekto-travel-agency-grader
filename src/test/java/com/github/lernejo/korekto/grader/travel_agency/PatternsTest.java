package com.github.lernejo.korekto.grader.travel_agency;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternsTest {

    @ParameterizedTest
    @CsvSource({
        "Unsuccessful response of GET `/api/travels\\?userName=.{36}`: 404, Unsuccessful response of GET `/api/travels?userName=83848586-8788-498a-8b8c-8d8e8f909192`: 404"
    })
    void cases(String rawPattern, String strToMatch) {
        Pattern pattern = Pattern.compile(rawPattern);

        Matcher matcher = pattern.matcher(strToMatch);

        Assertions.assertThat(matcher.matches()).isTrue();
    }
}
