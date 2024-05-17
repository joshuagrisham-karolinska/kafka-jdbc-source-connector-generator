package com.github.joshuagrisham.avro;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;

/*
 * Documentation for XmlSchema is a bit tricky to come by. Good starting point: https://stackoverflow.com/a/27648494
 */

public class AvroXmlSchemaConverter {
    
    // Mapping from Avro primitive types to XSD types per documentation here: https://avro.apache.org/docs/1.11.1/specification/#complex-types
    public static final Map<Schema.Type, QName> TYPEMAP = Map.of(
        Schema.Type.BOOLEAN, Constants.XSD_BOOLEAN,
        Schema.Type.BYTES, Constants.XSD_STRING,
        Schema.Type.DOUBLE, Constants.XSD_DOUBLE,
        Schema.Type.ENUM, Constants.XSD_STRING,
        Schema.Type.FIXED, Constants.XSD_STRING,
        Schema.Type.FLOAT, Constants.XSD_FLOAT,
        Schema.Type.INT, Constants.XSD_INT,
        Schema.Type.LONG, Constants.XSD_LONG,
        Schema.Type.STRING, Constants.XSD_STRING,
        Schema.Type.NULL, Constants.XSD_ANYTYPE // It would be quite strange to have a field with only type NULL outside of a UNION, but in case that happens just map it to xsd:anyType
    );

    private static XmlSchemaElement getXmlSchemaElement(QName name, Schema avroSchema, XmlSchema parentXmlSchema, boolean useAvroEncoderSyntax) {
        return getXmlSchemaElement(name, avroSchema, parentXmlSchema, useAvroEncoderSyntax, false);
    }

    private static XmlSchemaElement getXmlSchemaElement(QName name, Schema avroSchema, XmlSchema parentXmlSchema, boolean useAvroEncoderSyntax, boolean isOptional) {
        return getXmlSchemaElement(name, avroSchema, parentXmlSchema, useAvroEncoderSyntax, isOptional, false);
    }

    private static XmlSchemaElement getXmlSchemaElement(QName name, Schema avroSchema, XmlSchema parentXmlSchema, boolean useAvroEncoderSyntax, boolean isOptional, boolean topLevel) {

        XmlSchemaElement element = new XmlSchemaElement(parentXmlSchema, topLevel);

        element.setName(name.getLocalPart());
        if (isOptional)
            element.setMinOccurs(0);

        switch (avroSchema.getType()) {

            /* Complex Types */

            case UNION:
                // Unions will either be:
                // - optional fields (NULL + something else), or
                // - multiple different types

                // if using "Avro Encoder Syntax" for data conversion (instead of assuming data converted will first be marshalled as String) then Unions will look like this:
                // /element name={field.name}/complexType/sequence/{union fields}
                if (useAvroEncoderSyntax) {
                    XmlSchemaComplexType unionComplexType = new XmlSchemaComplexType(parentXmlSchema, topLevel);
                    XmlSchemaSequence unionSequence = new XmlSchemaSequence();
                    for (Schema unionFieldSchema : avroSchema.getTypes()) {
                        if (unionFieldSchema.getType() != Schema.Type.NULL) {
                            XmlSchemaElement unionElement = getXmlSchemaElement(
                                new QName(name.getNamespaceURI(),
                                    unionFieldSchema.getName()),
                                unionFieldSchema,
                                parentXmlSchema,
                                useAvroEncoderSyntax,
                                true);
                            if (unionFieldSchema.getType() == Schema.Type.RECORD)
                                unionElement.setName(unionFieldSchema.getFullName());
                            unionSequence.getItems().add(unionElement);
                        }
                    }
                    unionComplexType.setParticle(unionSequence);
                    element.setType(unionComplexType);
                    element.setMinOccurs(avroSchema.isNullable() ? 0 : 1);               
                } else {
                    // Otherwise, we are using a simplified string marshalling syntax where the contents of Union will be at the field's element level

                    // If this is just an optional field, find the non-null entry and return its element with isOptional=true
                    if (avroSchema.getTypes().size() == 2 && avroSchema.isNullable()) {
                        for (Schema unionFieldSchema : avroSchema.getTypes())
                            if (unionFieldSchema.getType() != Schema.Type.NULL) {
                                element = getXmlSchemaElement(name, unionFieldSchema, parentXmlSchema, useAvroEncoderSyntax, true);
                            }
                    } else {
                        // otherwise the contents at the element level can be multiple types, so just allow anything
                        element.setSchemaTypeName(Constants.XSD_ANYTYPE);
                    }
                }

                break;

            case RECORD:
                // Records should return /element name={field.name}/complexType/{sequence or all}/{record fields}
                // Prefer all over sequence, but if Fields has any Arrays then it will need to be a sequence to allow for maxOccurs > 1
                XmlSchemaComplexType recordComplexType = new XmlSchemaComplexType(parentXmlSchema, topLevel);
                XmlSchemaAll recordAll = new XmlSchemaAll();
                XmlSchemaSequence recordSequence = new XmlSchemaSequence();

                // Peek into Fields to see if there are any arrays
                boolean hasArray = false;
                for (Field recordField : avroSchema.getFields()) {
                    if (recordField.schema().getType() == Schema.Type.ARRAY)
                        hasArray = true;
                }

                // Now loop through and add each field to the right list
                for (Field recordField : avroSchema.getFields()) {
                    XmlSchemaElement fieldElement = getXmlSchemaElement(
                        new QName(name.getNamespaceURI(),
                            recordField.name()),
                        recordField.schema(),
                        parentXmlSchema,
                        useAvroEncoderSyntax);
                    if (hasArray)
                        recordSequence.getItems().add(fieldElement);
                    else
                        recordAll.getItems().add(fieldElement);
                }
                recordComplexType.setParticle(hasArray ? recordSequence : recordAll);

                // Since XMLSchema-Core seems to automatically create a named complexType for the top level then we have to handle it differently
                // Basically just set the SchemaTypeName to the QName of the complexType that it creates instead of embedding it within the Record's complexType
                if (topLevel) {
                    recordComplexType.setName(avroSchema.getName());
                    element.setSchemaTypeName(new QName(name.getNamespaceURI(), avroSchema.getName()));
                } else {
                    element.setType(recordComplexType);
                }

                break;

            case ARRAY:
                // Array should return an element of its content type (ElementType) with maxOccurs="unbounded"
                element = getXmlSchemaElement(name, avroSchema.getElementType(), parentXmlSchema, useAvroEncoderSyntax, isOptional);
                element.setMaxOccurs(Long.MAX_VALUE);
                break;

            case MAP:
                // Maps come in as `"mapName": {"key1": "value1", "key2": "value2"}` which is like a Record, but the keys are not pre-determined
                // Best guess right now is to just give an unbounded xs:any here and allow any element name and type? like this: /element name={field.name}/complexType/sequence/any
                XmlSchemaComplexType mapComplexType = new XmlSchemaComplexType(parentXmlSchema, topLevel);
                XmlSchemaSequence mapSequence = new XmlSchemaSequence();
                XmlSchemaAny mapAny = new XmlSchemaAny();
                if (isOptional)
                    mapAny.setMinOccurs(0);
                mapAny.setMaxOccurs(Long.MAX_VALUE);
                mapSequence.getItems().add(mapAny);
                mapComplexType.setParticle(mapSequence);

                // Same topLevel issue here as with Record, but that the name should be taken from the parameter
                if (topLevel) {
                    mapComplexType.setName(name.getLocalPart());
                    element.setSchemaTypeName(name);
                } else {
                    element.setType(mapComplexType);
                }

                break;


            /* Primitive Types */

            // Primitive types should return /element name={field.name} type={simple type}

            case NULL:
                // For NULL we will also just set minOccurs to 0, otherwise it can be handled like all other primitive types
                element.setMinOccurs(0);
            case ENUM:
                // TODO for ENUM we could get "fancier" and restrict the values in the XSD? but for now it can just be mapped to a plain string
            case BOOLEAN:
            case BYTES:
            case DOUBLE:
            case FIXED:
            case FLOAT:
            case INT:
            case LONG:
            case STRING:
                element.setSchemaTypeName(TYPEMAP.get(avroSchema.getType()));
                break;


            default:
                throw new UnsupportedOperationException("Unsupported Avro Schema Type: " + avroSchema.getType().getName());
        }

        return element;
    }

    public static XmlSchema convert(String schemaPath) throws IOException {
        return convert(schemaPath, false);
    }

    public static XmlSchema convert(String schemaPath, boolean useAvroEncoderSyntax) throws IOException {
        return convert(new FileInputStream(schemaPath), useAvroEncoderSyntax);
    }

    public static XmlSchema convert(FileInputStream schemaFileStream) throws IOException {
        return convert(schemaFileStream, false);
    }

    public static XmlSchema convert(FileInputStream schemaFileStream, boolean useAvroEncoderSyntax) throws IOException {
        return convert(new Schema.Parser().parse(schemaFileStream), useAvroEncoderSyntax);
    }

    public static XmlSchema convert(Schema avroSchema) {
        return convert(avroSchema, false);
    }

    public static XmlSchema convert(Schema avroSchema, boolean useAvroEncoderSyntax) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        QName schemaQName;

        // Only Record, Enum, and Fixed support name and namespace properties
        switch (avroSchema.getType()) {
            case RECORD:
            case ENUM:
            case FIXED:
                schemaQName = new QName(avroSchema.getNamespace(), avroSchema.getName());
                break;
            default:
                // Just hard-code the name "value" if the schema root is not a Record, Enum, or Fixed
                schemaQName = new QName("local", "value");
                break;
        }

        XmlSchema xmlSchema = new XmlSchema(schemaQName.getNamespaceURI(), collection);
        xmlSchema.getElements().put(schemaQName, getXmlSchemaElement(schemaQName, avroSchema, xmlSchema, useAvroEncoderSyntax, false, true));
        return xmlSchema;
    }

}
