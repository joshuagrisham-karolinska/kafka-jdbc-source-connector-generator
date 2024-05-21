package com.github.joshuagrisham.kafka.connect;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named
public class JdbcSourceConnectorDataSources {

    @ConfigMapping(prefix = "source.datasource")
    public interface DataSources {
        @WithParentName
        Map<String, DataSource> getAll();
    }

    public interface DataSource {
        String name();
        JdbcSourceQuerier.Dialect dialect();
        String jdbcUrl();
        String username();
        String password();
    }

    @Inject
    private DataSources all;
    public Map<String, DataSource> getAll() {
        return all.getAll();
    }

    public DataSource get(String name) {
        return getAll().get(name);
    }

    public JdbcSourceQuerier.Dialect[] getDialects() {
        return JdbcSourceQuerier.Dialect.values();
    }

    public class DataSourceImpl implements DataSource {

        private String name;
        @Override
        public String name() {
            return this.name;
        }

        private JdbcSourceQuerier.Dialect dialect;
        @Override
        public JdbcSourceQuerier.Dialect dialect() {
            return this.dialect;
        }

        private String jdbcUrl;
        @Override
        public String jdbcUrl() {
            return this.jdbcUrl;
        }

        private String username;
        @Override
        public String username() {
            return this.username;
        }

        private String password;
        @Override
        public String password() {
            return this.password;
        }

        private DataSourceImpl(String name, JdbcSourceQuerier.Dialect dialect, String jdbcUrl, String username, String password) {
            this.name = name;
            this.dialect = dialect;
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        private static DataSource create(String name, JdbcSourceQuerier.Dialect dialect, String jdbcUrl, String username, String password) {
            return new JdbcSourceConnectorDataSources().new DataSourceImpl(name, dialect, jdbcUrl, username, password);
        }
    }

    public static DataSource create(String name, JdbcSourceQuerier.Dialect dialect, String jdbcUrl, String username, String password) {
        return DataSourceImpl.create(name, dialect, jdbcUrl, username, password);
    }

}
