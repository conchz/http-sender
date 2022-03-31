package com.github.conchz.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public enum HttpMethod {

    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;


    private static final Map<String, HttpMethod> mappings = new HashMap<>(8);

    static {
        for (HttpMethod httpMethod : values()) {
            mappings.put(httpMethod.name(), httpMethod);
        }
    }


    /**
     * Resolve the given method value to an {@code HttpMethod}.
     *
     * @param method the method value as a String
     * @return the corresponding {@code HttpMethod}, or {@code null} if not found
     */
    public static HttpMethod resolve(String method) {
        return (Objects.nonNull(method) ? mappings.get(method) : null);
    }


    /**
     * Determine whether this {@code HttpMethod} matches the given method value.
     *
     * @param method the method value as a String
     * @return {@code true} if it matches, {@code false} otherwise
     */
    public boolean matches(String method) {
        return (this == resolve(method));
    }

}
