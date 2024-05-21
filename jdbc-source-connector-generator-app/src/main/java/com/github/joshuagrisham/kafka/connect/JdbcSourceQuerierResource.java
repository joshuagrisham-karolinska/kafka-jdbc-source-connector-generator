package com.github.joshuagrisham.kafka.connect;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.apache.commons.text.StringSubstitutor;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;

import org.jboss.resteasy.reactive.RestForm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;

import com.github.joshuagrisham.kafka.connect.JdbcSourceConnectorDataSources.DataSource;
import com.github.joshuagrisham.kafka.connect.JdbcSourceConnectorDataSources.DataSources;

import static com.github.joshuagrisham.kafka.connect.JdbcSourceConnectorUtils.isBlank;

@Path("/querier")
public class JdbcSourceQuerierResource {

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    DataSources DATASOURCES;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();
        public static native TemplateInstance listQueryResults(JdbcSourceQuerier querier);
        public static native TemplateInstance listConnectorResults(JdbcSourceQuerier querier);
    }

    @GET
    public TemplateInstance getIndex() {
        return Templates.index();
    }
    public String connectSchemaAsJsonString(Schema schema) throws JsonProcessingException {
        Map<String, String> schemaMap = new LinkedHashMap<>();
        for (Field field : schema.fields()) {
            schemaMap.put(field.name(), field.schema().type().toString());
        }
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schemaMap);
    }

    @POST
    @Path("/results/query")
    public TemplateInstance getQueryResults(
        @RestForm String datasource,
        @RestForm JdbcSourceQuerier.Dialect dialect,
        @RestForm String jdbcUrl,
        @RestForm String username,
        @RestForm String password,

        @RestForm String query,
        @RestForm int rowsLimit,

        @RestForm JdbcSourceQuerier.Mode mode,
        @RestForm String timeZone,
        @RestForm List<String> timestampColumnNames,
        @RestForm String incrementingColumnName
    ) throws ConnectException, SQLException, JsonProcessingException {

        JdbcSourceQuerier.Dialect q_dialect;
        String q_jdbcUrl;
        String q_username;
        String q_password;

        // If user chose a pre-configured DataSource, use it
        if (!isBlank(datasource)) {
            DataSource ds = DATASOURCES.getAll().get(datasource);
            q_dialect = ds.dialect();
            q_jdbcUrl = ds.jdbcUrl();
            q_username = ds.username();
            q_password = ds.password();
        } else { // otherwise read all of the individual properties from the form
            q_dialect = dialect;
            q_jdbcUrl = jdbcUrl;
            q_username = username;
            q_password = password;
        }
        JdbcSourceQuerier querier = new JdbcSourceQuerier(
            q_dialect, q_jdbcUrl, q_username, q_password, query,
            mode, timestampColumnNames, TimeZone.getTimeZone(timeZone), incrementingColumnName, rowsLimit);

        try {
            return Templates.listQueryResults(querier)
                .data("querySchema", connectSchemaAsJsonString(querier.getQuerySchema()));
        } catch (TemplateException e) {
            System.out.println("EXCEPTION HERE: " + e.getMessage()); //TODO for some reason the exception is never caught here? like it is in another thread?
            return null;
        }

    }

    @POST
    @Path("/results/connector")
    public TemplateInstance getConnectorResults(
        @RestForm String datasource,
        @RestForm JdbcSourceQuerier.Dialect dialect,
        @RestForm String jdbcUrl,
        @RestForm String username,
        @RestForm String password,

        @RestForm String query,
        @RestForm int rowsLimit,

        @RestForm JdbcSourceQuerier.Mode mode,
        @RestForm String timeZone,
        @RestForm List<String> timestampColumnNames,
        @RestForm String incrementingColumnName,

        @RestForm String schemaMetadataName,
        @RestForm String keyField,
        @RestForm String personnummerField,

        @RestForm String originatingSystemId,
        @RestForm String originatingSystemIdentifiersField,
        @RestForm String originatingSystemIdentifiersFieldOperation,
        @RestForm String originatingSystemVersionIdField,
        @RestForm String originatingSystemVersionIdFieldOperation,
        @RestForm String originatingSystemTimeField,
        @RestForm String originatingSystemTimeFieldOperation,

        @RestForm List<String> transformationDefinition,

        @RestForm String pollIntervalMs,
        @RestForm String topicName
    ) throws JsonMappingException, JsonProcessingException, ConnectException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, SQLException {

        List<TransformationDefinition> transformations = new ArrayList<>();

        // Build up standard transformations based on their specific form inputs

        // key
        if (!isBlank(keyField)) {
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.ValueToKey",
                Map.of("fields", keyField)));
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.ExtractField$Key",
                Map.of("field", keyField)));
        }

        if (!isBlank(personnummerField)) {
            // encrypt personnummer
            /* TODO: somehow handle Tink key matter */
            transformations.add(new TransformationDefinition("com.github.hpgrahsl.kafka.connect.transforms.kryptonite.CipherField$Value",
                Map.of(
                    "cipher_mode", "ENCRYPT",
                    "cipher_data_keys", "[{\"identifier\":\"my-test-key-v1\",\"material\":{\"primaryKeyId\":1778269984,\"key\":[{\"keyData\":{\"typeUrl\":\"type.googleapis.com/google.crypto.tink.AesGcmKey\",\"value\":\"GhDJJr34UgUgHsYCd5HEnrAR\",\"keyMaterialType\":\"SYMMETRIC\"},\"status\":\"ENABLED\",\"keyId\":1778269984,\"outputPrefixType\":\"TINK\"}]}}]", // parameterize somehow?
                    "cipher_data_key_identifier", "my-test-key-v1", // parameterize somehow?
                    "field_config", String.format("[{\"name\":\"%s\"}]", personnummerField),
                    "field_mode", "ELEMENT"
                )));

            // copy personnummer to header
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.HeaderFrom$Value",
                Map.of(
                    "operation", "copy",
                    "fields", personnummerField,
                    "headers", "encrypted:http://electronichealth.se/identifier/personnummer" //TODO should this be parameterized somehow?
                )));
        }

        // openEHR feeder audit headers
        if (!isBlank(originatingSystemId) && 
            !isBlank(originatingSystemIdentifiersField) &&
            !isBlank(originatingSystemVersionIdField)
        ) {
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.InsertHeader",
                Map.of(
                    "header", "originating.system.id",
                    "value.literal", originatingSystemId
                )));
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.HeaderFrom$Value",
                Map.of(
                    "headers", "originating.system.identifiers",
                    "fields", originatingSystemIdentifiersField,
                    "operation", originatingSystemIdentifiersFieldOperation
                )));
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.HeaderFrom$Value",
                Map.of(
                    "headers", "originating.system.versionId",
                    "fields", originatingSystemVersionIdField,
                    "operation", originatingSystemVersionIdFieldOperation
                )));
        }

        // openEHR feeder audit originating.system.time header
        if (!isBlank(originatingSystemTimeField))
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.HeaderFrom$Value",
                Map.of(
                    "headers", "originating.system.time",
                    "fields", originatingSystemTimeField,
                    "operation", originatingSystemTimeFieldOperation
                )));


        // Now add all of the extra user-chosen transformations
        if (transformationDefinition != null) {
            for (String defString : transformationDefinition) {
                transformations.add(MAPPER.readValue(defString, TransformationDefinition.class));
            }
        }

        // set schema name at the end (otherwise it could be overwritten from other tranforms after)
        if (!isBlank(schemaMetadataName))
            transformations.add(new TransformationDefinition("org.apache.kafka.connect.transforms.SetSchemaMetadata$Value",
                Map.of("schema.name", schemaMetadataName)));


        // Build the JdbcSourceQuerier including transformations and return it with the template
        JdbcSourceQuerier.Dialect q_dialect;
        String q_jdbcUrl;
        String q_username;
        String q_password;

        // If user chose a pre-configured DataSource, use it
        if (datasource != null && !datasource.isBlank()) {
            DataSource ds = DATASOURCES.getAll().get(datasource);
            q_dialect = ds.dialect();
            q_jdbcUrl = ds.jdbcUrl();
            q_username = ds.username();
            q_password = ds.password();
        } else { // otherwise read all of the individual properties from the form
            q_dialect = dialect;
            q_jdbcUrl = jdbcUrl;
            q_username = username;
            q_password = password;
        }

        JdbcSourceQuerier querier = new JdbcSourceQuerier(
            q_dialect, q_jdbcUrl, q_username, q_password, query,
            mode, timestampColumnNames, TimeZone.getTimeZone(timeZone), incrementingColumnName, rowsLimit, transformations);

        return Templates.listConnectorResults(querier);
    }

}
