package no.nav.melosys.tjenester.gui.util;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ResponseBodyMatchers {

    private final ObjectMapper objectMapper;

    private ResponseBodyMatchers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> ResultMatcher containsObjectAsJson(
        Object expectedObject,
        Class<T> targetClass) {
        return mvcResult -> {
            String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            T actualObject = objectMapper.readValue(json, targetClass);
            assertThat(actualObject).usingRecursiveComparison().isEqualTo(expectedObject);
        };
    }

    public <T> ResultMatcher containsObjectAsJson(
        Object expectedObject,
        TypeReference<T> valueTypeRef) {
        return mvcResult -> {
            String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            T actualObject = objectMapper.readValue(json, valueTypeRef);
            assertThat(actualObject).usingRecursiveComparison().isEqualTo(expectedObject);
        };
    }

    public ResultMatcher containsError(
        String expectedFieldName,
        String expectedMessage) {
        return mvcResult -> {
            String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            Map<String, String> errorMap = objectMapper.readValue(json, new TypeReference<HashMap<String, String>>() {
            });
            assertThat(errorMap.get(expectedFieldName))
                .isNotNull()
                .isEqualTo(expectedMessage);
        };
    }

    public static ResponseBodyMatchers responseBody(ObjectMapper objectMapper) {
        return new ResponseBodyMatchers(objectMapper);
    }

}
