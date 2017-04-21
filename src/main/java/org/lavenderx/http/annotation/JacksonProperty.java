package org.lavenderx.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JacksonProperty {

    String USE_DEFAULT_NAME = "";

    String value() default USE_DEFAULT_NAME;

    boolean required() default false;

    Mode mode() default Mode.ALL;

    /**
     * 用于在POST模式下, URL地址中和POST请求体中的参数限定
     */
    enum Mode {

        /**
         * 序列化之后当前参数只存在于URL地址中
         */
        ONLY_URL,

        /**
         * 序列化之后当前参数只存在于POST请求体中
         */
        ONLY_POST,

        ALL
    }
}
