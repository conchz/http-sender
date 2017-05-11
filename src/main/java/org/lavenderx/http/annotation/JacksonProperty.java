package org.lavenderx.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JacksonProperty {

    String value() default EMPTY;

    Mode mode() default Mode.ALL;

    /**
     * When HTTP method is POST, the <code>Mode</code>is used to control request parameter's range.
     */
    enum Mode {

        /**
         * After serialize, the current parameter will only exist in URL.
         */
        URL,

        /**
         * After serialize, the current parameter will only exist in POST body.
         */
        POST,

        ALL
    }
}
