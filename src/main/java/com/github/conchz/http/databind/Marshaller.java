package com.github.conchz.http.databind;

@FunctionalInterface
public interface Marshaller {

    MarshalResult marshal(Object obj);
}
