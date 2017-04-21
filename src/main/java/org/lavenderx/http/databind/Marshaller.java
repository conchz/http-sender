package org.lavenderx.http.databind;

@FunctionalInterface
public interface Marshaller {

    MarshalResult marshal(Object obj);
}
