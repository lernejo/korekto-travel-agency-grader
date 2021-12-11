package com.github.lernejo.korekto.grader.travel_agency;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.util.List;

public interface TravelAgencyApiClient {

    @POST("api/inscription")
    @Headers("Accept:application/json")
    Call<Void> postInscription(@Body Inscription inscription);


    @GET("api/travels")
    @Headers("Accept:application/json")
    Call<List<Travel>> getTravels();

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
