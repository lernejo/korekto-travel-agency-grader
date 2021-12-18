package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.grader.travel_agency.PredictionApiClient;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

class PredictionServer implements AutoCloseable {

    private final int port;
    private final Function<String, PredictionApiClient.Prediction> predictionFunction;
    private final HttpServer server;
    private final ExecutorService executorService;
    public final List<HttpEx> exchanges = new ArrayList<>();

    PredictionServer(int port) {
        this(port, PredictionServer::defaultPrediction);
    }

    private static PredictionApiClient.Prediction defaultPrediction(String country) {
        Dataset.TempBoundaries tempBoundaries = Dataset.getByCountry(country);
        String date1 = LocalDate.now().toString();
        double temp1 = generateTemp(tempBoundaries);
        String date2 = LocalDate.now().minusDays(1).toString();
        double temp2 = generateTemp(tempBoundaries);

        return new PredictionApiClient.Prediction(country, List.of(
            new PredictionApiClient.TempPoint(date1, temp1),
            new PredictionApiClient.TempPoint(date2, temp2)
        ));
    }

    private static double generateTemp(Dataset.TempBoundaries tempBoundaries) {
        return LaunchingContext.RANDOM.nextInt((int) ((Math.round(tempBoundaries.max()) - Math.round(tempBoundaries.min())) * 100)) / 100 + tempBoundaries.min();
    }

    PredictionServer(int port, Function<String, PredictionApiClient.Prediction> predictionFunction) {
        this.port = port;
        this.predictionFunction = predictionFunction;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        executorService = Executors.newFixedThreadPool(1);
        server.setExecutor(executorService);
        server.createContext("/api/temperature", new CallHandler(exchanges));
        server.start();
    }

    @Override
    public void close() {
        server.stop(0);
        executorService.shutdownNow();
        Ports.waitForPortToBeFreed(port, TimeUnit.SECONDS, 5L);
    }

    private class CallHandler implements HttpHandler {
        private final List<HttpEx> logs;

        public CallHandler(List<HttpEx> logs) {
            this.logs = logs;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> query = parseQuery(exchange.getRequestURI());
                if (query.containsKey("country")) {
                    PredictionApiClient.Prediction body = predictionFunction.apply(query.get("country"));
                    String rawBody = LaunchingContext.OBJECT_MAPPER.writeValueAsString(body);
                    int code = 200;
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(code, rawBody.length());
                    logs.add(new HttpEx(
                        new HttpEx.Request(exchange.getRequestMethod().toUpperCase(), exchange.getRequestURI().toString(), toMap(exchange.getRequestHeaders()), null),
                        new HttpEx.Response(code, toMap(exchange.getResponseHeaders()), rawBody)));
                    OutputStream os = exchange.getResponseBody();
                    os.write(rawBody.getBytes(StandardCharsets.UTF_8));
                    os.close();
                } else {
                    badRequest(exchange, readInputStream(exchange.getRequestBody()));
                }
            } else {
                notFound(exchange, readInputStream(exchange.getRequestBody()));
            }
        }

        @SubjectForToolkitInclusion
        private static Map<String, String> parseQuery(URI requestURI) {
            if (requestURI.getQuery() == null) {
                return Collections.emptyMap();
            } else {
                String[] entries = requestURI.getQuery().split("&");
                return Arrays.stream(entries)
                    .map(Entry::from)
                    .collect(Collectors.toMap(e -> e.key, e -> e.value, (m1, m2) -> m1));
            }
        }

        @SubjectForToolkitInclusion
        private void notFound(HttpExchange exchange, String originalBody) throws IOException {
            emptyResponseWithCode(exchange, originalBody, 404);
        }

        @SubjectForToolkitInclusion
        private void badRequest(HttpExchange exchange, String originalBody) throws IOException {
            emptyResponseWithCode(exchange, originalBody, 400);
        }

        private void emptyResponseWithCode(HttpExchange exchange, String originalBody, int i) throws IOException {
            logs.add(new HttpEx(
                new HttpEx.Request(exchange.getRequestMethod().toUpperCase(), exchange.getRequestURI().toString(), toMap(exchange.getRequestHeaders()), originalBody),
                null));
            exchange.sendResponseHeaders(i, 0);
            exchange.getResponseBody().close();
        }

        @SubjectForToolkitInclusion
        private Map<String, String> toMap(Map<String, List<String>> headers) {
            return headers.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e -> toRawHeaderValue(e.getValue())));
        }

        private String toRawHeaderValue(List<String> v) {
            return String.join(",", v);
        }

        @SubjectForToolkitInclusion
        private static String readInputStream(InputStream is) {
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
                return textBuilder.toString();
            } catch (IOException e) {
                return null;
            }
        }
    }

    record Entry(String key, String value) {
        static Entry from(String entry) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                return new Entry(parts[0], parts[1]);
            } else {
                return new Entry(entry, null);
            }
        }
    }
}
