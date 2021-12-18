package com.github.lernejo.korekto.grader.travel_agency;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SubjectForToolkitInclusion
public class SilentJacksonConverterFactory extends Converter.Factory {
    private final List<DeserializationProblem> latestDeserializationProblems = new ArrayList<>();

    private final ObjectMapper mapper;

    public static SilentJacksonConverterFactory create(ObjectMapper mapper) {
        if (mapper == null) {
            throw new NullPointerException("mapper == null");
        } else {
            return new SilentJacksonConverterFactory(mapper);
        }
    }

    private SilentJacksonConverterFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        JavaType javaType = this.mapper.getTypeFactory().constructType(type);
        ObjectReader reader = this.mapper.readerFor(javaType);
        return new SilentJacksonResponseBodyConverter(reader);
    }

    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        JavaType javaType = this.mapper.getTypeFactory().constructType(type);
        ObjectWriter writer = this.mapper.writerFor(javaType);
        return new SilentJacksonRequestBodyConverter(writer);
    }

    class SilentJacksonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
        private final ObjectReader adapter;

        SilentJacksonResponseBodyConverter(ObjectReader adapter) {
            this.adapter = adapter;
        }

        public T convert(ResponseBody value) throws IOException {
            String rawBody = value.string();
            try (value) {
                return this.adapter.readValue(rawBody);
            } catch (IOException e) {
                latestDeserializationProblems.add(new DeserializationProblem(rawBody, e));
                return null;
            }
        }
    }

    class SilentJacksonRequestBodyConverter<T> implements Converter<T, RequestBody> {
        private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");
        private final ObjectWriter adapter;

        SilentJacksonRequestBodyConverter(ObjectWriter adapter) {
            this.adapter = adapter;
        }

        public RequestBody convert(T value) throws IOException {
            byte[] bytes = this.adapter.writeValueAsBytes(value);
            return RequestBody.create(MEDIA_TYPE, bytes);
        }
    }

    public ExceptionHolder newExceptionHolder() {
        return new ExceptionHolder();
    }

    public class ExceptionHolder implements AutoCloseable {

        public DeserializationProblem getLatestDeserializationProblem() {
            return latestDeserializationProblems.isEmpty() ? null : latestDeserializationProblems.get(latestDeserializationProblems.size() - 1);
        }

        @Override
        public void close() {
            latestDeserializationProblems.clear();
        }
    }

    public record DeserializationProblem(String rawBody, IOException ex) {
    }
}

