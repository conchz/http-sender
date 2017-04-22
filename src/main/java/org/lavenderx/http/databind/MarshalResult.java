package org.lavenderx.http.databind;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.lavenderx.http.utils.NonNullToStringStyle;

import java.io.Serializable;

public class MarshalResult implements Serializable {

    /**
     * Request URL, not include domain
     */
    private final String url;

    private final Object requestBody;

    public MarshalResult(final String url, final Object requestBody) {
        this.url = url;
        this.requestBody = requestBody;
    }

    public final String url() {
        return this.url;
    }

    public final Object requestBody() {
        return this.requestBody;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, NonNullToStringStyle.NON_NULL_JSON_STYLE);
    }
}
