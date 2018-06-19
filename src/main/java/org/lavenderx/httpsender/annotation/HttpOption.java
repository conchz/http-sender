package org.lavenderx.httpsender.annotation;

import org.lavenderx.httpsender.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpOption {

    HttpMethod method() default HttpMethod.GET;

    String url() default "/";

    /**
     * @see org.apache.http.entity.ContentType
     */
    String mimeType() default "text/plain";
}
