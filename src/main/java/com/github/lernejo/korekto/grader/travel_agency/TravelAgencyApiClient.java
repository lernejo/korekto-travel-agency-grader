package com.github.lernejo.korekto.grader.travel_agency;

import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface TravelAgencyApiClient {

    String SAMPLE_RESPONSE_PAYLOAD = """
        [
            {
                "country": "a country",
                "temperature": 3.25
            }, {
                "country": "another country",
                "temperature": 7.52
            }
        ]
        """.stripTrailing();

    @POST("api/inscription")
    @Headers({
        "Accept:application/json",
        "Content-Type:application/json"})
    Call<Void> postInscription(@Body Inscription inscription);


    @GET("api/travels")
    @Headers("Accept:application/json")
    Call<List<Travel>> getTravels(@Query("userName") String userName);

    record Inscription(String userEmail,
                       String userName,
                       String userCountry,
                       WeatherExpectation weatherExpectation,
                       int minimumTemperatureDistance) {
    }

    record Travel(String country, double temperature) {
    }

    enum WeatherExpectation {
        WARMER, COLDER;
    }
}
