package org.lavenderx.http.databind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lavenderx.http.SenderException;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class Unmarshaller {

    protected static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setAnnotationIntrospector(new JacksonLombokAnnotationIntrospector());
    }

    public <T> T unmarshal(String content, TypeReference<T> valueTypeRef) {
        if (isEmpty(content)) {
            return null;
        }

        try {
            return mapper.readValue(content, valueTypeRef);
        } catch (Exception e) {
            throw new SenderException("Exception on unmarshalling: " + content, e);
        }
    }

    public abstract <T> T unmarshal(String content, Class<T> valueType);
}
