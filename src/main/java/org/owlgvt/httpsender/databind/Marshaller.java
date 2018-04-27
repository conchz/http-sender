package org.owlgvt.httpsender.databind;

@FunctionalInterface
public interface Marshaller {

    MarshalResult marshal(Object obj);
}
