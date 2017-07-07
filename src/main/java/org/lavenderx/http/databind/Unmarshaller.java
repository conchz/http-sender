package org.lavenderx.http.databind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lavenderx.http.SenderException;
import org.lavenderx.http.utils.JacksonUtils;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class Unmarshaller {

    protected static ObjectMapper mapper = JacksonUtils.getObjectMapper();

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
