package com.github.joshuagrisham.kafka.connect;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import io.quarkus.qute.CheckedTemplate;

@Path("/querier/configure")
public class JdbcSourceQuerierConfigResource {

    @ConfigMapping(prefix = "source.datasource")
    public interface DataSources {
        @WithParentName
        Map<String, DataSource> getDataSources();
    }

    public interface DataSource {
        String name();
        JdbcSourceQuerier.Dialect dialect();
        String jdbcUrl();
        String username();
        String password();
    }

    @Inject
    DataSources datasources;

    // TODO: Is there a better way to do this? Seems like they are all available to load but 
    // can't be seen by Quarkus Arc using @RegisterForReflection and injecting them to a list
    //@Inject
    //@All
    //List<Transformation<?>> transformations;
    public class TransformationConfigs {

        private Map<String, Set<String>> all;
        public Map<String, Set<String>> getAll() {
            if (all == null) {
                all = new LinkedHashMap<>();

                // If a Transformation has Key/Value inner classes then use those directly with the main class's CONFIG_DEF; otherwise use the main class

                // Add Connect's built-in transformations
                // Since we have imported org.apache.kafka.connect.transforms already then we can fetch the config key names from the Java classes directly for the built-in transformations
                all.put("org.apache.kafka.connect.transforms.Cast$Key", org.apache.kafka.connect.transforms.Cast.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.Cast$Value", org.apache.kafka.connect.transforms.Cast.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.DropHeaders", org.apache.kafka.connect.transforms.DropHeaders.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.ExtractField$Key", org.apache.kafka.connect.transforms.ExtractField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.ExtractField$Value", org.apache.kafka.connect.transforms.ExtractField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.Flatten$Key", org.apache.kafka.connect.transforms.Flatten.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.Flatten$Value", org.apache.kafka.connect.transforms.Flatten.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.HeaderFrom$Key", org.apache.kafka.connect.transforms.HeaderFrom.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.HeaderFrom$Value", org.apache.kafka.connect.transforms.HeaderFrom.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.HoistField$Key", org.apache.kafka.connect.transforms.HoistField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.HoistField$Value", org.apache.kafka.connect.transforms.HoistField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.InsertField$Key", org.apache.kafka.connect.transforms.InsertField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.InsertField$Value", org.apache.kafka.connect.transforms.InsertField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.InsertHeader", org.apache.kafka.connect.transforms.InsertHeader.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.MaskField$Key", org.apache.kafka.connect.transforms.MaskField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.MaskField$Value", org.apache.kafka.connect.transforms.MaskField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.RegexRouter", org.apache.kafka.connect.transforms.RegexRouter.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.ReplaceField$Key", org.apache.kafka.connect.transforms.ReplaceField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.ReplaceField$Value", org.apache.kafka.connect.transforms.ReplaceField.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.SetSchemaMetadata$Key", org.apache.kafka.connect.transforms.SetSchemaMetadata.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.SetSchemaMetadata$Value", org.apache.kafka.connect.transforms.SetSchemaMetadata.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.TimestampConverter$Key", org.apache.kafka.connect.transforms.TimestampConverter.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.TimestampConverter$Value", org.apache.kafka.connect.transforms.TimestampConverter.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.TimestampRouter", org.apache.kafka.connect.transforms.TimestampRouter.CONFIG_DEF.names());
                all.put("org.apache.kafka.connect.transforms.ValueToKey", org.apache.kafka.connect.transforms.ValueToKey.CONFIG_DEF.names());

                // For now we will not include the built-in 'Filter' SMT from the list as it relies on additional Connector config for Predicates that we are not exposing here (plus it has a bit weird/limited usage)
                // TODO: Do we want to support Predicates and if so then how should this exposed in the GUI? A whole separate section of the GUI and then that we add "predicates" as a configName option to all SMT types?
                //all.put("org.apache.kafka.connect.transforms.Filter", org.apache.kafka.connect.transforms.Filter.CONFIG_DEF.names());

                // Add ExpandJSON SMT
                all.put("com.redhat.insights.expandjsonsmt.ExpandJSON$Value", Set.of("sourceFields"));

                // Add Kryptonite's CipherField SMT
                Set<String> cipherFieldConfigKeys = Set.of(
                    "field_config",
                    "path_delimiter",
                    "field_mode",
                    "cipher_algorithm",
                    "cipher_data_key_identifier",
                    "cipher_data_keys",
                    "cipher_text_encoding",
                    "cipher_mode",
                    "key_source",
                    "kms_type",
                    "kms_config",
                    "kek_type",
                    "kek_config",
                    "kek_uri"
                );
                all.put("com.github.hpgrahsl.kafka.connect.transforms.kryptonite.CipherField$Key", cipherFieldConfigKeys);
                all.put("com.github.hpgrahsl.kafka.connect.transforms.kryptonite.CipherField$Value", cipherFieldConfigKeys);

            }
            return all;
        }

        public Set<String> getConfigKeys(String className) {
            return getAll().get(className);
        }

    }

    public final TransformationConfigs TRANSFORMATIONS = new TransformationConfigs();

    private final ObjectMapper MAPPER = new ObjectMapper();

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance query(Map<String, DataSource> datasources, JdbcSourceQuerier.Dialect[] dialects);
        public static native TemplateInstance connector();
        public static native TemplateInstance transformationDisplay(TransformationDefinition transformationDefinition,
            String transformationDefinitionJson);
        public static native TemplateInstance transformationAdd(String uniqueFormName, TransformationConfigs transformationConfigs);
        public static native TemplateInstance transformationEdit(String uniqueFormName, TransformationConfigs transformationConfigs,
            TransformationDefinition transformationDefinition, String transformationDefinitionJson);
        public static native TemplateInstance transformationConfigEdit(String uniqueFormName, Set<String> configKeys, String selectedConfigKey, String configValue);
    }

    @GET
    @Path("/query")
    public TemplateInstance getConfigureQuery() {
        return Templates.query(datasources.getDataSources(), JdbcSourceQuerier.Dialect.values());
    }

    @GET
    @Path("/connector")
    public TemplateInstance getConfigureConnector() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        return Templates.connector();
    }

    @GET
    @Path("/transformation/add")
    public TemplateInstance getTransformationAdd() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        return Templates.transformationEdit("transformation-" + UUID.randomUUID().toString(), TRANSFORMATIONS, null, null);
    }

    @POST
    @Path("/transformation/display")
    public TemplateInstance postTransformationDisplay(
        @RestForm String transformationDefinition,
        @RestForm String className,
        @RestForm List<String> configName,
        @RestForm List<String> configValue
    ) throws JsonMappingException, JsonProcessingException {
        if (transformationDefinition != null && !transformationDefinition.isBlank()) {
            TransformationDefinition def = MAPPER.readValue(transformationDefinition, TransformationDefinition.class);
            String defJson = MAPPER.writeValueAsString(def);
            return Templates.transformationDisplay(def, defJson);
        }
         else {
            Map<String, String> configsAsMap = new LinkedHashMap<>();
            for (int i = 0; i < configName.size(); i++) {
                if (!configName.get(i).isBlank())
                    configsAsMap.put(configName.get(i), configValue.get(i));
            }
            TransformationDefinition def = new TransformationDefinition(className, configsAsMap);
            String defJson = MAPPER.writeValueAsString(def);
            return Templates.transformationDisplay(def, defJson);
        }
    }

    @POST
    @Path("/transformation/edit")
    public TemplateInstance postTransformationEdit(@RestForm String transformationDefinition) throws JsonMappingException, JsonProcessingException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        TransformationDefinition def = MAPPER.readValue(transformationDefinition, TransformationDefinition.class);
        String defJson = MAPPER.writeValueAsString(def);
        return Templates.transformationEdit("transformation-" + UUID.randomUUID().toString(), TRANSFORMATIONS, def, defJson);
    }

    @POST
    @Path("/transformation/config/edit")
    public TemplateInstance postTransformationConfigEdit(@RestForm String uniqueFormName, @RestForm String className) throws ClassNotFoundException {
        return Templates.transformationConfigEdit(uniqueFormName, TRANSFORMATIONS.getConfigKeys(className), null, null);
    }

}
