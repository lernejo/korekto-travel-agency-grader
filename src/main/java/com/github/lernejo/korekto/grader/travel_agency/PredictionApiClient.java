package com.github.lernejo.korekto.grader.travel_agency;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

import java.util.List;

public interface PredictionApiClient {

    String SAMPLE_RESPONSE_PAYLOAD = """
        {
            "country": "a country",
            "temperatures: [
                {
                    "date": "2021-12-16",
                    "temperature": 3.25
                }, {
                    "date": "2021-12-15",
                    "temperature": 7.52
                }
            ]
        }
        """.stripTrailing();

    @GET("api/temperature")
    @Headers("Accept:application/json")
    Call<Prediction> getTemperature(@Query("country") String country);

    record Prediction(String country, List<TempPoint> temperatures) {
    }

    record TempPoint(String date, double temperature) {
    }
}
