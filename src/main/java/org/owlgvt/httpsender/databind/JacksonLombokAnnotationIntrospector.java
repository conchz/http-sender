package org.owlgvt.httpsender.databind;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.google.common.base.Preconditions;
import org.owlgvt.httpsender.annotation.JacksonNaming;
import org.owlgvt.httpsender.annotation.JacksonProperty;

import java.lang.annotation.Annotation;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class JacksonLombokAnnotationIntrospector extends JacksonAnnotationIntrospector implements Versioned {

    @Override
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }

    @Override
    public boolean isAnnotationBundle(Annotation ann) {
        Class<?> clz = ann.annotationType();
        return JacksonProperty.class == clz || super.isAnnotationBundle(ann);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        return getPropertyName(a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        return getPropertyName(a);
    }

    @Override
    public Object findNamingStrategy(AnnotatedClass ac) {
        JacksonNaming ann = _findAnnotation(ac, JacksonNaming.class);
        return (ann == null) ? null : ann.value();
    }

    private PropertyName getPropertyName(Annotated a) {
        if (a instanceof AnnotatedField) {
            JacksonProperty annotation = a.getAnnotated().getAnnotation(JacksonProperty.class);
            if (Objects.nonNull(annotation)) {
                String value = annotation.value();
                Preconditions.checkArgument(!isEmpty(value), "@JacksonProperty's value can not be null or empty");
                return PropertyName.construct(value);
            }
        }

        return null;
    }
}
