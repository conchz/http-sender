package org.owlgvt.httpsender.databind;

import org.owlgvt.httpsender.SenderException;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class DefaultUnmarshaller extends Unmarshaller {

    @Override
    public <T> T unmarshal(String content, Class<T> valueType) {
        if (isEmpty(content)) {
            return null;
        }

        try {
            return mapper.readValue(content, valueType);
        } catch (Exception e) {
            throw new SenderException("Exception on unmarshalling: " + content, e);
        }
    }
}
