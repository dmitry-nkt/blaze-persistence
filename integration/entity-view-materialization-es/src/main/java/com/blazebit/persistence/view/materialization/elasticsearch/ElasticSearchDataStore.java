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

package com.blazebit.persistence.view.materialization.elasticsearch;

import com.blazebit.persistence.view.materialization.api.spi.DataStore;
import com.blazebit.persistence.view.materialization.impl.CalciteEntityViewDriverConnectionFactory;
import com.blazebit.persistence.view.materialization.impl.MaterializationDescriptor;
import com.blazebit.persistence.view.materialization.impl.MaterializationFieldDescriptor;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.MaterializedViewTable;
import org.apache.calcite.schema.impl.ViewTableMacro;

import java.util.Collections;
import java.util.List;

public class ElasticSearchDataStore implements DataStore {

    private final CalciteConnection calciteConnection;
    private final String rawSchemaName;
    private final SchemaPlus rawSchema;
    private final SchemaPlus schema;

    public ElasticSearchDataStore(CalciteConnection calciteConnection, String rawSchemaName, SchemaPlus rawSchema, SchemaPlus schema) {
        this.calciteConnection = calciteConnection;
        this.rawSchemaName = rawSchemaName;
        this.rawSchema = rawSchema;
        this.schema = schema;
    }

    @Override
    public void registerMaterialization(SchemaPlus sourceSchema, MaterializationDescriptor descriptor) {
        String viewName = descriptor.getTableName() + "_view";
        CalciteSchema calciteSchema = CalciteSchema.from(schema);
        List<String> viewPath = calciteSchema.path(viewName);
        List<String> viewSchemaPath = calciteSchema.getPath().get(0);
        StringBuilder viewSqlSb = new StringBuilder();
        viewSqlSb.append("select ");

        for (MaterializationFieldDescriptor fieldDescriptor : descriptor.getFields()) {
            viewSqlSb.append("cast(_MAP['").append(fieldDescriptor.getName()).append("'] AS ");
            // NOTE: Upper casing the field name to use as an alias is kind of a hack needed to satisfy the strict type checking of materialized views
            // Materialized views use upper cased field names because they are sourced from a JdbcSchema that only has upper cased column names
            // During substitution of a query with a materialized view, the row types are also checked regarding their names, so we need this here
            viewSqlSb.append(fieldDescriptor.getType().getSimpleType()).append(") AS \"").append(fieldDescriptor.getName().toUpperCase()).append("\",");
        }

        viewSqlSb.setCharAt(viewSqlSb.length() - 1, ' ');
        viewSqlSb.append("from \"")
                .append(rawSchemaName).append("\".\"")
                .append(descriptor.getTableName()).append('"');

        schema.add(viewName, new ViewTableMacro(calciteSchema, viewSqlSb.toString(), viewSchemaPath, viewPath, false));

        String materializedViewName = "$" + viewName;
        CalciteSchema calciteMaterializedSchema = CalciteSchema.from(sourceSchema);
        List<String> materializedViewPath = calciteMaterializedSchema.path(materializedViewName);
        List<String> materializedViewSchemaPath = calciteMaterializedSchema.getPath().get(0);

        schema.add(
                materializedViewName,
                MaterializedViewTable.create(
                        calciteSchema,
                        descriptor.getSql(),
                        materializedViewSchemaPath,
                        materializedViewPath,
                        viewName,
                        true
                )
        );
    }

    @Override
    public void validateMaterialization(SchemaPlus sourceSchema, String tableName, RelDataType expectedRowType, List<String> errors) {
        // Elastic search is dynamically typed, so we can't really do any checks. The best we could do is check if existing columns aren't wrongly typed
    }
}
