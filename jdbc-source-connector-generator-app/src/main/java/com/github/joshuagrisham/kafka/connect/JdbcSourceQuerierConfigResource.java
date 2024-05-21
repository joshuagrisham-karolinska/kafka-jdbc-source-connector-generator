package com.github.joshuagrisham.kafka.connect;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.resteasy.reactive.RestForm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;

@Path("/querier/configure")
public class JdbcSourceQuerierConfigResource {

    private final ObjectMapper MAPPER = new ObjectMapper();

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance query();
        public static native TemplateInstance connector();
        public static native TemplateInstance transformationDisplay(TransformationDefinition transformationDefinition,
            String transformationDefinitionJson);
        public static native TemplateInstance transformationAdd(String uniqueFormName);
        public static native TemplateInstance transformationEdit(String uniqueFormName,
            TransformationDefinition transformationDefinition, String transformationDefinitionJson);
        public static native TemplateInstance transformationConfigEdit(String uniqueFormName, String className,
            String selectedConfigKey, String configValue);
    }

    @GET
    @Path("/query")
    public TemplateInstance getConfigureQuery() {
        return Templates.query();
    }

    @GET
    @Path("/connector")
    public TemplateInstance getConfigureConnector() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        return Templates.connector();
    }

    @GET
    @Path("/transformation/add")
    public TemplateInstance getTransformationAdd() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        return Templates.transformationEdit("transformation-" + UUID.randomUUID().toString(), null, null);
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
        return Templates.transformationEdit("transformation-" + UUID.randomUUID().toString(), def, defJson);
    }

    @POST
    @Path("/transformation/config/edit")
    public TemplateInstance postTransformationConfigEdit(@RestForm String uniqueFormName, @RestForm String className) throws ClassNotFoundException {
        return Templates.transformationConfigEdit(uniqueFormName, className, null, null);
    }

}
