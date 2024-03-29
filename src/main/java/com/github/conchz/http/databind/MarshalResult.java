package com.github.conchz.http.databind;

import com.github.conchz.http.utils.NonNullToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
