package com.github.lernejo.korekto.grader.travel_agency.parts;

import java.util.Map;

record HttpEx(Request request, Response response) {

    static record Request(String verb, String url, Map<String, String> headers, String body) {
    }

    static record Response(int code, Map<String, String> headers, String body) {
    }
}
