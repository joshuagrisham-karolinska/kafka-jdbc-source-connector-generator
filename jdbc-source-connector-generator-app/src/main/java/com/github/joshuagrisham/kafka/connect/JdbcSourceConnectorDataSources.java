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

}
