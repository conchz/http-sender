package org.lavenderx.httpsender.databind;

@FunctionalInterface
public interface Marshaller {

    MarshalResult marshal(Object obj);
}
