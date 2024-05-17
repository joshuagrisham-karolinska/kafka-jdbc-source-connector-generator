package com.github.joshuagrisham.kafka.connect;

import java.util.Map;

public class TransformationDefinition {
    public String className;
    public Map<String, ?> config;

    public TransformationDefinition() {}

    public TransformationDefinition(String className, Map<String, ?> config) {
        this.className = className;
        this.config = config;
    }
}
