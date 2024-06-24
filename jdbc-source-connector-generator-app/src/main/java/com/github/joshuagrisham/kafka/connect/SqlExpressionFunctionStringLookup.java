package com.github.joshuagrisham.kafka.connect;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

public class SqlExpressionFunctionStringLookup implements StringLookup {

    // Ideally, we could just extend AbstractStringLookup but it is an abstract class, so re-create some of it here to give roughly the same API
    // see: https://github.com/apache/commons-text/blob/master/src/main/java/org/apache/commons/text/lookup/AbstractStringLookup.java

    /**
     * The default split char.
     */
    protected static final char SPLIT_CH = ':';

    /**
     * The default split string.
     */
    protected static final String SPLIT_STR = String.valueOf(SPLIT_CH);

    /**
     * Creates a lookup key for a given file and key.
     */
    static String toLookupKey(final String left, final String right) {
        return toLookupKey(left, SPLIT_STR, right);
    }

    /**
     * Creates a lookup key for a given file and key.
     */
    static String toLookupKey(final String left, final String separator, final String right) {
        return left + separator + right;
    }

    /**
     * Defines the singleton for this class.
     */
    //static final SqlExpressionFunctionStringLookup INSTANCE = new SqlExpressionFunctionStringLookup();

    private String datasourceName;
    public String getDatasourceName() {
        return datasourceName;
    }
    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    private Map<String, Map<String, String>> expressions;
    public Map<String, Map<String, String>> getExpressions() {
        return expressions;
    }
    public void setExpressions(Map<String, Map<String, String>> expressions) {
        this.expressions = expressions;
    }

    private Map<String, Map<String, String>> parameters;
    public Map<String, Map<String, String>> getParameters() {
        return parameters;
    }
    public void setParameters(Map<String, Map<String, String>> parameters) {
        this.parameters = parameters;
    }

    public SqlExpressionFunctionStringLookup(String datasourceName, Map<String, Map<String, String>> expressions,
        Map<String, Map<String, String>> parameters)
    {
        setDatasourceName(datasourceName);
        setExpressions(expressions);
        setParameters(parameters);
    }

    @Override
    public String lookup(final String key) {
        if (expressions == null || !expressions.containsKey(this.datasourceName))
            throw new IllegalArgumentException("No named SQL expressions exist for datasource name \"" + this.datasourceName + "\"");

        if (key == null)
            throw new IllegalArgumentException("Bad SQL expression function format \"${sql:}\"; expected format is \"${sql:expression_name:expression_value}\"");

        final String[] keys = key.split(SPLIT_STR);
        final int keyLen = keys.length;
        if (keyLen != 2)
            throw new IllegalArgumentException("Bad SQL expression function format \"${sql:" + key + "}\"; expected format is \"${sql:expression_name:expression_value}\"");

        final String expressionName = keys[0];
        final String expressionValue = StringUtils.substringAfter(key, SPLIT_CH);

        if (expressions.get(this.datasourceName) == null || !expressions.get(this.datasourceName).containsKey(expressionName))
            throw new IllegalArgumentException("Named SQL expression \"" + expressionName + "\" not found for datasource name \"" + this.datasourceName + "\"");

        // Fetch the configured SQL expression
        String expression = expressions.get(this.datasourceName).get(expressionName);

        // Set up expression_value substitution parameter
        Map<String, String> params = new LinkedHashMap<>();
        params.put("expressionValue", expressionValue);

        // Fetch any additionally configured SQL expression parameters for this datasource
        if (parameters != null && parameters.containsKey(this.datasourceName))
            params.putAll(parameters.get(this.datasourceName));

        // Return the final expression after substituting any parameters (including the given expression_value)
        return new StringSubstitutor(params).replace(expression);
    }

}
