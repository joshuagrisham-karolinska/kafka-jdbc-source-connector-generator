package com.github.joshuagrisham.kafka.connect;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;

import org.apache.avro.generic.GenericData;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.joshuagrisham.avro.AvroXmlDataConverter;
import com.github.joshuagrisham.avro.AvroXmlSchemaConverter;

import io.confluent.connect.avro.AvroData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class JdbcSourceConnectorUtils {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
        .build();
    private static final AvroData AVRODATA = new AvroData(1);

    public String prettyPrintJson(String uglyJson) throws JsonProcessingException {
        Object jsonObject = MAPPER.readValue(uglyJson, Object.class);
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

    public GenericData.Record getAvroRecord(org.apache.kafka.connect.data.Schema schema, Object value) {
        return (GenericData.Record) AVRODATA.fromConnectData(schema, value);
    }

    public String getAvroSchema(org.apache.kafka.connect.data.Schema schema, Object value) throws JsonProcessingException {
        return prettyPrintJson(getAvroRecord(schema, value).getSchema().toString());
    }

    public String getAvroJson(org.apache.kafka.connect.data.Schema schema, Object value) throws JsonProcessingException {
        return prettyPrintJson(getAvroRecord(schema, value).toString());
    }

    public String getTransformedResultsSchema(JdbcSourceQuerier querier) throws ConnectException, InstantiationException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException,
        ClassNotFoundException, SQLException, JsonProcessingException
    {
        SourceRecord record = querier.peekTransformedResults();
        if (record == null)
            return null;

        return prettyPrintJson(getAvroRecord(record.valueSchema(), record.value()).getSchema().toString());
    }

    public List<String> getTransformedResultsAsXml(JdbcSourceQuerier querier) throws JsonMappingException, ConnectException,
        JsonProcessingException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        NoSuchMethodException, SecurityException, ClassNotFoundException, SQLException
    {
        List<String> results = new ArrayList<>();
        for (SourceRecord record : querier.getTransformedResults()) {
            GenericData.Record avroRecord = getAvroRecord(record.valueSchema(), record.value());
            results.add(AvroXmlDataConverter.convert(avroRecord, false, true));
        }
        return results;
    }

    public String getTransformedResultsXmlSchema(JdbcSourceQuerier querier) throws ConnectException, InstantiationException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException,
        ClassNotFoundException, SQLException
    {
        SourceRecord record = querier.peekTransformedResults();
        if (record == null)
            return null;

        GenericData.Record avroRecord = getAvroRecord(record.valueSchema(), record.value());
        StringWriter stringWriter = new StringWriter();
        AvroXmlSchemaConverter.convert(avroRecord.getSchema()).write(stringWriter, Map.of(OutputKeys.INDENT, "yes"));
        return stringWriter.toString();
    }

    public static boolean isBlank(String string) {
        if (string == null)
            return true;
        return string.isBlank();
    }

    public String getConnectorPropertiesJson(JdbcSourceQuerier querier) throws JsonProcessingException {

        String querierString = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(querier);
        // mask the password
        querierString = querierString
            .replaceAll("\"password\"\s{0,}:\s{0,}\"" + querier.getPassword() + "\"",
                "\"password\": \"********\"");
        return querierString;
    }

}
