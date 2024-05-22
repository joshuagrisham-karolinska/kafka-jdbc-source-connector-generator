package com.github.joshuagrisham.kafka.connect;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

public class QueryDecryptFunctionStringLookup implements StringLookup {

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
    //static final QueryDecryptFunctionStringLookup INSTANCE = new QueryDecryptFunctionStringLookup();

    private String datasourceName;
    public String getDatasourceName() {
        return datasourceName;
    }
    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    private Map<String, Map<String, String>> decryptionExpressions;
    public Map<String, Map<String, String>> getDecryptionExpressions() {
        return decryptionExpressions;
    }
    public void setDecryptionExpressions(Map<String, Map<String, String>> decryptionExpressions) {
        this.decryptionExpressions = decryptionExpressions;
    }

    private Map<String, Map<String, String>> decryptionParameters;
    public Map<String, Map<String, String>> getDecryptionParameters() {
        return decryptionParameters;
    }
    public void setDecryptionParameters(Map<String, Map<String, String>> decryptionParameters) {
        this.decryptionParameters = decryptionParameters;
    }

    public QueryDecryptFunctionStringLookup(String datasourceName, Map<String, Map<String, String>> decryptionExpressions,
        Map<String, Map<String, String>> decryptionParameters)
    {
        setDatasourceName(datasourceName);
        setDecryptionExpressions(decryptionExpressions);
        setDecryptionParameters(decryptionParameters);
    }

    @Override
    public String lookup(final String key) {
        if (decryptionExpressions == null || !decryptionExpressions.containsKey(this.datasourceName))
            throw new IllegalArgumentException("No named decryption expressions exist for datasource name \"" + this.datasourceName + "\"");

        if (key == null)
            throw new IllegalArgumentException("Bad Decrypt function format \"${decrypt:}\"; expected format is \"${decrypt:decryption_expression_name:encrypted_field_name}\"");

        final String[] keys = key.split(SPLIT_STR);
        final int keyLen = keys.length;
        if (keyLen != 2)
            throw new IllegalArgumentException("Bad Decrypt function format \"${decrypt:" + key + "}\"; expected format is \"${decrypt:decryption_expression_name:encrypted_field_name}\"");

        final String expressionName = keys[0];
        final String encryptedFieldName = StringUtils.substringAfter(key, SPLIT_CH);

        if (decryptionExpressions.get(this.datasourceName) == null || !decryptionExpressions.get(this.datasourceName).containsKey(expressionName))
            throw new IllegalArgumentException("Named decryption expression \"" + expressionName + "\" not found for datasource name \"" + this.datasourceName + "\"");

        // Fetch the configured decryption expression
        String decryptionExpression = decryptionExpressions.get(this.datasourceName).get(expressionName);

        // Set up encrypted_field_name substitution parameter
        Map<String, String> params = new LinkedHashMap<>();
        params.put("encryptedField", encryptedFieldName);

        // Fetch any additionally configured decryption parameters for this datasource
        if (decryptionParameters != null && decryptionParameters.containsKey(this.datasourceName))
            params.putAll(decryptionParameters.get(this.datasourceName));

        // Return the final expression after substituting any parameters (including the given encrypted_field_name)
        return new StringSubstitutor(params).replace(decryptionExpression);
    }

}
