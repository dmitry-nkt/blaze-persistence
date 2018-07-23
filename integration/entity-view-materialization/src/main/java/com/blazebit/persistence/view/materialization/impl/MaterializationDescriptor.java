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

import java.util.List;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class MaterializationDescriptor {

    private final String schema;
    private final String tableName;
    private final String sql;
    private final List<MaterializationFieldDescriptor> fields;

    public MaterializationDescriptor(String schema, String tableName, String sql, List<MaterializationFieldDescriptor> fields) {
        this.schema = schema;
        this.tableName = tableName;
        this.sql = sql;
        this.fields = fields;
    }

    public String getSchema() {
        return schema;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSql() {
        return sql;
    }

    public List<MaterializationFieldDescriptor> getFields() {
        return fields;
    }
}
