package com.github.joshuagrisham.avro;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class AvroXmlDataConverter {

    public static String convert(String schemaPath, String dataPath) throws IOException {
        return convert(schemaPath, dataPath, false, false);
    }

    public static String convert(String schemaPath, String dataPath, boolean useAvroEncoderSyntax) throws IOException {
        return convert(new FileInputStream(schemaPath), new FileInputStream(dataPath), useAvroEncoderSyntax, false);
    }

    public static String convert(String schemaPath, String dataPath, boolean useAvroEncoderSyntax, boolean usePrettyPrinter) throws IOException {
        return convert(new FileInputStream(schemaPath), new FileInputStream(dataPath), useAvroEncoderSyntax, usePrettyPrinter);
    }

    public static String convert(FileInputStream schemaFileStream, FileInputStream dataFileStream) throws IOException {
        return convert(schemaFileStream, dataFileStream, false, false);
    }

    public static String convert(FileInputStream schemaFileStream, FileInputStream dataFileStream, boolean useAvroEncoderSyntax) throws IOException {
        return convert(parseAvroData(new Schema.Parser().parse(schemaFileStream), dataFileStream), useAvroEncoderSyntax, false);
    }

    public static String convert(FileInputStream schemaFileStream, FileInputStream dataFileStream, boolean useAvroEncoderSyntax, boolean usePrettyPrinter) throws IOException {
        return convert(parseAvroData(new Schema.Parser().parse(schemaFileStream), dataFileStream), useAvroEncoderSyntax, usePrettyPrinter);
    }

    public static String convert(GenericRecord record) throws JsonMappingException, JsonProcessingException {
        return convert(record, false, false);
    }

    public static String convert(GenericRecord record, boolean useAvroEncoderSyntax) throws JsonMappingException, JsonProcessingException {
        return convert(record, useAvroEncoderSyntax, false);
    }

    public static String convert(GenericRecord record, boolean useAvroEncoderSyntax, boolean usePrettyPrinter) throws JsonMappingException, JsonProcessingException {
        String recordAsJsonString = useAvroEncoderSyntax ? convertUsingAvroEncoder(record) : record.toString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(recordAsJsonString);
        XmlMapper xmlMapper = new XmlMapper();
        if (usePrettyPrinter)
            return xmlMapper.writer()
                .withDefaultPrettyPrinter()
                .withRootName(record.getSchema().getName())
                .writeValueAsString(jsonNode);
        else
            return xmlMapper.writer()
                .withRootName(record.getSchema().getName())
                .writeValueAsString(jsonNode);
    }

    public static String convertUsingAvroEncoder(GenericRecord record) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
            DatumWriter<GenericRecord> writer = record instanceof SpecificRecord ?
                new SpecificDatumWriter<>(record.getSchema()) :
                new GenericDatumWriter<>(record.getSchema());
            writer.write(record, jsonEncoder);
            jsonEncoder.flush();
            return outputStream.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to convert Avro to JSON.", e);
        }
    }

    public static GenericRecord parseAvroData(Schema avroSchema, InputStream inputStream) {
        try {
            DatumReader<GenericRecord> reader = new GenericDatumReader<>(avroSchema);
            JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(avroSchema, inputStream);
            return reader.read(null, jsonDecoder);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse Avro data.", e);
        }
    }

}
