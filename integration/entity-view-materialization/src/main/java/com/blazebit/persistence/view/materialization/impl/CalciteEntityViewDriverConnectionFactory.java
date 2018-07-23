/*
 * Copyright 2014 - 2018 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.view.materialization.impl;

import com.blazebit.persistence.view.materialization.api.spi.DataStore;
import com.blazebit.persistence.view.materialization.api.spi.DataStoreFactory;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.MaterializedViewTable;
import org.apache.commons.dbcp2.DriverConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CalciteEntityViewDriverConnectionFactory extends DriverConnectionFactory {

    private final String name;
    private final String schema;
    private final String catalog;
    private final Map<String, DataStoreFactory> schemas;
    private final List<MaterializationDescriptor> materializationDescriptors;

    public CalciteEntityViewDriverConnectionFactory(String name, String schema, String catalog, Map<String, DataStoreFactory> schemas, List<MaterializationDescriptor> materializationDescriptors) {
        super(new Driver(), "jdbc:calcite:lex=MYSQL_ANSI;materializationsEnabled=true;", null);
        this.name = name;
        this.schema = schema;
        this.catalog = catalog;
        this.schemas = schemas;
        this.materializationDescriptors = materializationDescriptors;
    }

    @Override
    public Connection createConnection() throws SQLException {
        Connection connection = super.createConnection();
        CalciteConnection calciteConnection = (CalciteConnection) connection;
        SchemaPlus rootSchema = calciteConnection.getRootSchema();
        JdbcSchema jdbcSchema = JdbcSchema.create(
                rootSchema,
                name,
                ThreadLocalDataSource.INSTANCE,
                catalog,
                schema
        );
        SchemaPlus subSchema = rootSchema.add(name, jdbcSchema);
        calciteConnection.setSchema(name);

        Map<String, DataStore> dataStores = new HashMap<>();
        dataStores.put(name, new JdbcDataStore(calciteConnection, subSchema));

        for (Map.Entry<String, DataStoreFactory> schemaEntry : schemas.entrySet()) {
            DataStore dataStore = schemaEntry.getValue().create(calciteConnection, schemaEntry.getKey());
            dataStores.put(schemaEntry.getKey(), dataStore);
        }

        for (MaterializationDescriptor descriptor : materializationDescriptors) {
            dataStores.get(descriptor.getSchema()).registerMaterialization(subSchema, descriptor);
        }

        return connection;
    }

    public static boolean addMaterializedView(SchemaPlus schema, SchemaPlus materializationSchema, String tableName, String sql) {
        String viewName = "$" + tableName;
        CalciteSchema calciteSchema = CalciteSchema.from(materializationSchema);
        List<String> viewPath = calciteSchema.path(viewName);
        List<String> viewSchemaPath = CalciteSchema.from(schema).getPath().get(0);
        boolean existing = materializationSchema.getTable(tableName) != null;
        if (!existing) {
            return false;
        }

        schema.add(
                viewName,
                MaterializedViewTable.create(
                        calciteSchema,
                        sql,
                        viewSchemaPath,
                        viewPath,
                        tableName,
                        existing
                )
        );
        return true;
    }
}
