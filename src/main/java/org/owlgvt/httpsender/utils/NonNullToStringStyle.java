package org.owlgvt.httpsender.utils;

import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Objects;

public final class NonNullToStringStyle extends ToStringStyle {

    public static final ToStringStyle NON_NULL_JSON_STYLE = new NonNullToStringStyle();

    private NonNullToStringStyle() {
        super();

        this.setUseClassName(false);
        this.setUseIdentityHashCode(false);

        this.setContentStart("{");
        this.setContentEnd("}");

        this.setArrayStart("[");
        this.setArrayEnd("]");

        this.setFieldSeparator(",");
        this.setFieldNameValueSeparator(":");

        this.setNullText("null");

        this.setSummaryObjectStartText("\"<");
        this.setSummaryObjectEndText(">\"");

        this.setSizeStartText("\"<size=");
        this.setSizeEndText(">\"");
    }

    /**
     * Override this to do checking of null, so only non-nulls are printed out in toString.
     *
     * @param buffer
     * @param fieldName
     * @param value
     * @param fullDetail
     */
    @Override
    public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {
        if (Objects.nonNull(value)) {
            super.append(buffer, fieldName, value, fullDetail);
        }
    }
}
