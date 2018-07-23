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
import com.blazebit.persistence.view.materialization.api.spi.DataStoreFactory;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchSchemaFactory;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.HashMap;
import java.util.Map;

public class ElasticSearchDataStoreFactory implements DataStoreFactory {

    private final String coordinates;
    private final String userConfig;
    private final String index;

    public ElasticSearchDataStoreFactory(String coordinates, String userConfig, String index) {
        this.coordinates = coordinates;
        this.userConfig = userConfig;
        this.index = index;
    }

    @Override
    public DataStore create(CalciteConnection connection, String name) {
        Map<String, Object> operands = new HashMap<>();
        operands.put("coordinates", coordinates);
        operands.put("userConfig", userConfig);
        operands.put("index", index);
        SchemaPlus rootSchema = connection.getRootSchema();
        String virtualSchemaName = name + "_virt";
        Schema rawSchema = new ElasticsearchSchemaFactory().create(rootSchema, name, operands);
        Schema schema = new AbstractSchema();
        return new ElasticSearchDataStore(connection, name, rootSchema.add(name, rawSchema), rootSchema.add(virtualSchemaName, schema));
    }
}
