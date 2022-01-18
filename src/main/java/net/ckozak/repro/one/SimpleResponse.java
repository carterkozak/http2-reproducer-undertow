package net.ckozak.repro.one;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class SimpleResponse {
    private static final SimpleResponse INSTANCE = new SimpleResponse();

    private SimpleResponse() {}

    @Override
    public String toString() {
        return "SimpleResponse{}";
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SimpleResponse of() {
        return INSTANCE;
    }
}
