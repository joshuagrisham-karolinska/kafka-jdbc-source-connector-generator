package com.github.joshuagrisham.kafka.connect;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Schema.Type;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.transforms.Transformation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.confluent.connect.jdbc.dialect.DatabaseDialect;
import io.confluent.connect.jdbc.dialect.DatabaseDialects;
import io.confluent.connect.jdbc.source.JdbcSourceConnectorConfig;
import io.confluent.connect.jdbc.source.JdbcSourceTaskConfig;
import io.confluent.connect.jdbc.source.TimestampIncrementingOffset;
import io.confluent.connect.jdbc.source.TimestampIncrementingTableQuerier;

@JsonIgnoreProperties({"queryResultsAsStructs", "querySchema"})
public class JdbcSourceQuerier {

    public enum Dialect {
        SQLSERVER("SqlServerDatabaseDialect"),
        POSTGRES("PostgreSqlDatabaseDialect");
        //TODO test/implement more of them? [Db2DatabaseDialect, MySqlDatabaseDialect, SybaseDatabaseDialect, GenericDatabaseDialect, OracleDatabaseDialect, SqlServerDatabaseDialect, PostgreSqlDatabaseDialect, SqliteDatabaseDialect, DerbyDatabaseDialect, SapHanaDatabaseDialect, VerticaDatabaseDialect]

        public final String className;
        private Dialect(String className) {
            this.className = className;
        }
    }

    private static final Map<Dialect, String> NO_FILTER_SUFFIX_FORMATS = Map.of(
        Dialect.POSTGRES, "",
        Dialect.SQLSERVER, " ORDER BY 1"
    );

    private static final Map<Dialect, String> LIMIT_SUFFIX_FORMATS = Map.of(
        Dialect.POSTGRES, " LIMIT %d",
        Dialect.SQLSERVER, " OFFSET 0 ROWS FETCH NEXT %d ROWS ONLY"
    );

    private Dialect dialect;
    public Dialect getDialect() {
        return dialect;
    }
    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

    private String jdbcUrl;
    public String getJdbcUrl() {
        return jdbcUrl;
    }
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    private String username;
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    private String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    private String query;
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query.replaceAll("[\\t\\n\\r]+"," ");
    }

    private transient JdbcSourceTaskConfig config;
    public JdbcSourceTaskConfig getConfig() {
        return config;
    }
    public void setConfig(JdbcSourceTaskConfig config) {
        this.config = config;
    }

    private transient DatabaseDialect connectorDialect;
    public DatabaseDialect getConnectorDialect() {
        return connectorDialect;
    }
    public void setConnectorDialect(DatabaseDialect connectorDialect) {        
        this.connectorDialect = connectorDialect;
    }
    /**
     * Sets field values for all given parameters and builds and sets the {@link JdbcSourceQuerier#connectorDialect}.
     * @param dialect
     * @param config
     */
    public void setConnectorDialect(Dialect dialect, JdbcSourceTaskConfig config) {
        setDialect(dialect);
        setConfig(config);
        setConnectorDialect(DatabaseDialects.create(dialect.className, config));
    }
    /**
     * Sets field values for all given parameters, builds and sets a new {@link JdbcSourceTaskConfig} using the given
     * parameter values, and finally builds and sets the {@link JdbcSourceQuerier#connectorDialect}.
     * @param dialect
     * @param jdbcUrl
     * @param username
     * @param password
     * @param query
     */
    public void setConnectorDialect(Dialect dialect, String jdbcUrl, String username, String password, String query) {
        setJdbcUrl(jdbcUrl);
        setUsername(username);
        setPassword(password);
        setQuery(query);

        JdbcSourceTaskConfig config = new JdbcSourceTaskConfig(Map.of(
            "connection.url", jdbcUrl,
            "connection.username", username,
            "connection.password", password,
            "query", getQuery(),
            "tables", "" // tables is required but not used here
            ));

        setConnectorDialect(dialect, config);
    }

    public enum Mode {
        TIMESTAMP_INCREMENTING("timestamp+incrementing"),
        TIMESTAMP("timestamp"),
        INCREMENTING("incrementing");

        public final String value;
        private Mode(String value) {
            this.value = value;
        }
    }

    private Mode mode;
    public Mode getMode() {
        return mode;
    }
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private List<String> timestampColumnNames;
    public List<String> getTimestampColumnNames() {
        return timestampColumnNames;
    }
    public void setTimestampColumnNames(List<String> timestampColumnNames) {
        this.timestampColumnNames = timestampColumnNames;
    }

    private TimeZone timeZone;
    public TimeZone getTimeZone() {
        return timeZone;
    }
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    private String incrementingColumnName;
    public String getIncrementingColumnName() {
        return incrementingColumnName;
    }
    public void setIncrementingColumnName(String incrementingColumnName) {
        this.incrementingColumnName = incrementingColumnName;
    }

    public static final int MAX_ROWS_LIMIT = 100;
    private int rowsLimit;
    public int getRowsLimit() {
        return rowsLimit;
    }
    public void setRowsLimit(int rowsLimit) {
        if (rowsLimit > MAX_ROWS_LIMIT)
            throw new IllegalArgumentException("rowsLimit cannot be greater than the maximum allowed value of " + MAX_ROWS_LIMIT);
        this.rowsLimit = rowsLimit;
    }

    private List<TransformationDefinition> transformations;
    public List<TransformationDefinition> getTransformations() {
        return transformations;
    }
    public void setTransformations(List<TransformationDefinition> transformations) {
        this.transformations = transformations;
    }

    private transient TimestampIncrementingTableQuerier connectorQuerier;
    public TimestampIncrementingTableQuerier getConnectorQuerier() {
        return connectorQuerier;
    }
    public void setConnectorQuerier(TimestampIncrementingTableQuerier connectorQuerier) {
        this.connectorQuerier = connectorQuerier;
    }

    /**
     * Performs all setup for building and returning the {@link TimestampIncrementingTableQuerier} based on the
     * instance's current field values.
     * @return the newly set {@link TimestampIncrementingTableQuerier} which can also be later retrieved from
     * the {@link JdbcSourceQuerier#getConnectorQuerier()} method
     */
    public TimestampIncrementingTableQuerier setup() {

        setConnectorDialect(dialect, jdbcUrl, username, password, query);

        String querySuffix = String.format(LIMIT_SUFFIX_FORMATS.get(dialect), getRowsLimit());

        if ((getIncrementingColumnName() == null || getIncrementingColumnName().isBlank()) &&
            (getTimestampColumnNames() == null || getTimestampColumnNames().size() == 0))
            querySuffix = NO_FILTER_SUFFIX_FORMATS.get(dialect) + querySuffix;

        connectorQuerier = new TimestampIncrementingTableQuerier(
            getConnectorDialect(),
            TimestampIncrementingTableQuerier.QueryMode.QUERY,
            getQuery(), // tableName or query
            "", // topicPrefix
            getMode() != Mode.INCREMENTING ? getTimestampColumnNames() : null, // timestampColumnNames
            getMode() != Mode.TIMESTAMP ? getIncrementingColumnName() : null, // incrementingColumnName
            new TimestampIncrementingOffset(null, null).toMap(), // offsetMap; just default to start
            0L, // Timestamp delay (end time of query will be "now" minus this delay; should be 0 for our purposes)
            getTimeZone(),
            querySuffix,
            JdbcSourceConnectorConfig.TimestampGranularity.CONNECT_LOGICAL);

        setConnectorQuerier(connectorQuerier);
        return connectorQuerier;
    }

    private transient List<SourceRecord> queryResults;
    public List<SourceRecord> getQueryResults() throws ConnectException, SQLException {
        if (queryResults == null)
            return fetchQueryResults();
        else
            return queryResults;
    }
    public List<SourceRecord> fetchQueryResults() throws ConnectException, SQLException {
        if (connectorQuerier == null) // if connectorQuerier is not set, try to run setup()
            setup(); // will fail if not all necessary fields have been set
        connectorQuerier.maybeStartQuery(connectorDialect.getConnection());
        queryResults = new ArrayList<>();
        while (connectorQuerier.next()) {
            queryResults.add(connectorQuerier.extractRecord());
        }
        return queryResults;
    }
    public SourceRecord peekQueryResults() throws ConnectException, SQLException {
        if (getQueryResults().size() > 0)
            return getQueryResults().get(0);
        return null;
    }

    public List<Struct> getQueryResultsAsStructs() throws ConnectException, SQLException {
        List<Struct> structs = new ArrayList<>();
        for (SourceRecord record : getQueryResults()) {
            if (record.valueSchema().type() == Type.STRUCT) {
                structs.add((Struct) record.value());
            }
        }
        return structs;
    }

    public Schema getQuerySchema() throws ConnectException, SQLException {
        if (getQueryResults().size() > 0)
            return getQueryResults().get(0).valueSchema();
        return null;
    }

    private transient List<SourceRecord> transformedResults;
    public List<SourceRecord> getTransformedResults() throws ConnectException, SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        transformedResults = new ArrayList<>();
        for (SourceRecord record : getQueryResults()) {
            if (transformations != null) {
                for (TransformationDefinition transformation : transformations) {
                    record = buildTransformationInstance(transformation).apply(record);
                }
            }
            transformedResults.add(record);
        }
        return transformedResults;
    }

    public SourceRecord peekTransformedResults() throws ConnectException, SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        if (getTransformedResults().size() > 0)
            return getTransformedResults().get(0);
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Transformation<SourceRecord> buildTransformationInstance(String className)
        throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        NoSuchMethodException, SecurityException, ClassNotFoundException
    {
        return (Transformation<SourceRecord>) Class.forName(className).getDeclaredConstructor().newInstance();
    }

    public static Transformation<SourceRecord> buildTransformationInstance(String className, Map<String, ?> configs)
        throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        NoSuchMethodException, SecurityException, ClassNotFoundException {
        return buildTransformationInstance(new TransformationDefinition(className, configs));
    }

    public static Transformation<SourceRecord> buildTransformationInstance(TransformationDefinition definition)
        throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        NoSuchMethodException, SecurityException, ClassNotFoundException
    {
        Transformation<SourceRecord> transformation = buildTransformationInstance(definition.className);
        transformation.configure(definition.config);
        return transformation;
    }

    public JdbcSourceQuerier() {
        setRowsLimit(MAX_ROWS_LIMIT);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query) {
        this(dialect, jdbcUrl, username, password, query, null, null, null, null, MAX_ROWS_LIMIT, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, int rowsLimit) {
        this(dialect, jdbcUrl, username, password, query, null, null, null, null, rowsLimit, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, int rowsLimit,
        List<TransformationDefinition> transformations) {
        this(dialect, jdbcUrl, username, password, query, null, null, null, null, rowsLimit, transformations);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        String incrementingColumnName) {
        this(dialect, jdbcUrl, username, password, query, mode, null, null, incrementingColumnName, MAX_ROWS_LIMIT, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        String incrementingColumnName, int rowsLimit) {
        this(dialect, jdbcUrl, username, password, query, mode, null, null, incrementingColumnName, rowsLimit, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        String incrementingColumnName, int rowsLimit, List<TransformationDefinition> transformations) {
        this(dialect, jdbcUrl, username, password, query, mode, null, null, incrementingColumnName, rowsLimit, transformations);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        List<String> timestampColumnNames, TimeZone timeZone) {
        this(dialect, jdbcUrl, username, password, query, mode, timestampColumnNames, timeZone, null, MAX_ROWS_LIMIT, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        List<String> timestampColumnNames, TimeZone timeZone, int rowsLimit) {
        this(dialect, jdbcUrl, username, password, query, mode, timestampColumnNames, timeZone, null, rowsLimit, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        List<String> timestampColumnNames, TimeZone timeZone, int rowsLimit, List<TransformationDefinition> transformations) {
        this(dialect, jdbcUrl, username, password, query, mode, timestampColumnNames, timeZone, null, rowsLimit, transformations);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        List<String> timestampColumnNames, TimeZone timeZone, String incrementingColumnName) {
        this(dialect, jdbcUrl, username, password, query, mode, timestampColumnNames, timeZone, incrementingColumnName, MAX_ROWS_LIMIT, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        List<String> timestampColumnNames, TimeZone timeZone, String incrementingColumnName, int rowsLimit) {
        this(dialect, jdbcUrl, username, password, query, mode, timestampColumnNames, timeZone, incrementingColumnName, rowsLimit, null);
    }

    public JdbcSourceQuerier(Dialect dialect, String jdbcUrl, String username, String password, String query, Mode mode,
        List<String> timestampColumnNames, TimeZone timeZone, String incrementingColumnName, int rowsLimit,
        List<TransformationDefinition> transformations) {

        setDialect(dialect);
        setJdbcUrl(jdbcUrl);
        setUsername(username);
        setPassword(password);
        setQuery(query);
        setMode(mode);
        setTimestampColumnNames(timestampColumnNames);
        setTimeZone(timeZone);
        setIncrementingColumnName(incrementingColumnName);
        setRowsLimit(rowsLimit);
        setTransformations(transformations);

        setup();

    }

}
