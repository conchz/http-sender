package com.github.conchz.http.databind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.conchz.http.HttpMethod;
import com.github.conchz.http.SenderException;
import com.github.conchz.http.annotation.HttpOption;
import com.github.conchz.http.annotation.JacksonProperty;
import com.github.conchz.http.utils.JacksonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.asynchttpclient.util.Utf8UrlEncoder.encodeQueryElement;

public class StandardMarshaller implements Marshaller {

    private static ObjectMapper mapper = JacksonUtils.getObjectMapper();

    @Override
    public MarshalResult marshal(Object obj) {
        try {
            HttpOption httpOption = obj.getClass().getAnnotation(HttpOption.class);
            HttpMethod method = httpOption.method();
            String requestUrl = httpOption.url();
            String requestString = mapper.writeValueAsString(obj);
            final Map<String, Object> requestParams = mapper.readValue(requestString, new TypeReference<Map<String, Object>>() {
            });

            final StringJoiner urlParams = new StringJoiner("&");
            for (Map.Entry<String, Object> entry : requestParams.entrySet()) {
                if (Objects.isNull(entry.getValue())
                        || entry.getValue().getClass().isArray()
                        || entry.getValue() instanceof List<?>
                        || entry.getValue() instanceof Map<?, ?>) {
                    continue;
                }

                String value = entry.getValue().toString();
                if (value.trim().length() > 0) {
                    if (requestUrl.contains("{" + entry.getKey() + "}")) {
                        requestUrl = requestUrl.replaceAll("\\{" + entry.getKey() + "\\}",
                                encodeQueryElement(value));
                    } else {
                        urlParams.add(entry.getKey() + "=" + encodeQueryElement(value));
                    }
                }
            }

            if (HttpMethod.GET == method) {
                if (urlParams.length() > 0) {
                    if (requestUrl.contains("?")) {
                        requestUrl = requestUrl + "&" + urlParams.toString();
                    } else {
                        requestUrl = requestUrl + "?" + urlParams.toString();
                    }
                }
            }

            MarshalResult result = new MarshalResult(requestUrl, null);

            if (HttpMethod.POST == method) {
                Arrays.stream(obj.getClass().getDeclaredFields())
                        .map(field -> field.getAnnotation(JacksonProperty.class))
                        .filter(Objects::nonNull)
                        .forEach(ann -> {
                            if (JacksonProperty.Mode.URL == ann.mode() && isNotEmpty(ann.value())) {
                                requestParams.remove(ann.value());
                            }
                        });

                if (Objects.equals(APPLICATION_JSON.getMimeType(), httpOption.mimeType())) {
                    result = new MarshalResult(requestUrl, mapper.writeValueAsString(requestParams));
                } else if (Objects.equals(APPLICATION_FORM_URLENCODED.getMimeType(), httpOption.mimeType())) {
                    result = new MarshalResult(requestUrl, requestParams);
                } else {
                    throw new SenderException("Unsupported mimeType on marshalling request");
                }
            }

            return result;
        } catch (Exception e) {
            throw new SenderException(e);
        }
    }
}
