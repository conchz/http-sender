package org.lavenderx.http.databind;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.ContentType;
import org.asynchttpclient.util.Utf8UrlEncoder;
import org.lavenderx.http.HttpMethod;
import org.lavenderx.http.SenderException;
import org.lavenderx.http.annotation.HttpOption;
import org.lavenderx.http.annotation.JacksonProperty;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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

            for (Map.Entry<String, String> entry : requestParams.entrySet()) {
                Matcher matcher = Pattern.compile("\\{" + entry.getKey() + "\\}").matcher(requestUrl);
                int count = 0;
                while (matcher.find()) {
                    count++;
                }
                if (count == 0) {
                    continue;
                }
                requestUrl = requestUrl.replaceAll("\\{" + entry.getKey() + "\\}", Utf8UrlEncoder.encodeQueryElement(entry.getValue()));
            }

            MarshalResult result = new MarshalResult(requestUrl, null);

            if (HttpMethod.POST == method) {
                Arrays.stream(obj.getClass().getDeclaredFields())
                        .map(field -> field.getAnnotation(JacksonProperty.class))
                        .filter(Objects::nonNull)
                        .forEach(ann -> {
                            if (JacksonProperty.Mode.ONLY_URL == ann.mode() && isNotEmpty(ann.value())) {
                                requestParams.remove(ann.value());
                            }
                        });

                if (Objects.equals(ContentType.APPLICATION_JSON.getMimeType(), httpOption.mimeType())) {
                    result = new MarshalResult(requestUrl, mapper.writeValueAsString(requestParams));
                } else if (Objects.equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), httpOption.mimeType())) {
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
