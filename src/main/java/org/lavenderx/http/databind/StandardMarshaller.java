package org.lavenderx.http.databind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lavenderx.http.HttpMethod;
import org.lavenderx.http.SenderException;
import org.lavenderx.http.annotation.HttpOption;
import org.lavenderx.http.annotation.JacksonProperty;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.asynchttpclient.util.Utf8UrlEncoder.encodeQueryElement;
import static org.lavenderx.http.HttpMethod.GET;
import static org.lavenderx.http.HttpMethod.POST;

public class StandardMarshaller implements Marshaller {

    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(MapperFeature.USE_STD_BEAN_NAMING);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setAnnotationIntrospector(new JacksonLombokAnnotationIntrospector());
    }

    @Override
    public MarshalResult marshal(Object obj) {
        try {
            HttpOption httpOption = obj.getClass().getAnnotation(HttpOption.class);
            HttpMethod method = httpOption.method();
            String requestUrl = httpOption.url();
            String requestString = mapper.writeValueAsString(obj);
            final Map<String, String> requestParams = mapper.readValue(requestString, new TypeReference<Map<String, String>>() {
            });

            final StringJoiner urlParams = new StringJoiner("&");
            for (Map.Entry<String, String> entry : requestParams.entrySet()) {
                if (requestUrl.contains(entry.getKey())) {
                    requestUrl = requestUrl.replaceAll("\\{" + entry.getKey() + "\\}", encodeQueryElement(entry.getValue()));
                } else {
                    if (isNotEmpty(entry.getValue())) {
                        urlParams.add(entry.getKey() + "=" + entry.getValue());
                    }
                }
            }

            if (GET == method) {
                if (urlParams.length() > 0) {
                    if (requestUrl.contains("?")) {
                        requestUrl = requestUrl + "&" + urlParams.toString();
                    } else {
                        requestUrl = requestUrl + "?" + urlParams.toString();
                    }
                }
            }

            MarshalResult result = new MarshalResult(requestUrl, null);

            if (POST == method) {
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
