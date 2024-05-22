package com.github.joshuagrisham.kafka.connect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class JdbcSourceConnectorTransformations {

    // TODO: Is there a better way to do this? Seems like they are all available to load but 
    // can't be seen by Quarkus Arc using @RegisterForReflection and injecting them to a list
    //@Inject
    //@All
    //List<Transformation<?>> transformations;

    private Map<String, Set<String>> all;
    public Map<String, Set<String>> getAll() {
        // build all on the first get
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

            // Add ExpandJson SMT
            all.put("com.github.joshuagrisham.kafka.connect.transforms.ExpandJson$Key", Set.of("fields", "schema.name.prefix"));
            all.put("com.github.joshuagrisham.kafka.connect.transforms.ExpandJson$Value", Set.of("fields", "schema.name.prefix"));

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
