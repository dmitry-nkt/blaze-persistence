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
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;

import java.util.List;

public class JdbcDataStore implements DataStore {

    private final CalciteConnection calciteConnection;
    private final SchemaPlus schemaPlus;

    public JdbcDataStore(CalciteConnection calciteConnection, SchemaPlus schemaPlus) {
        this.calciteConnection = calciteConnection;
        this.schemaPlus = schemaPlus;
    }

    @Override
    public void registerMaterialization(SchemaPlus sourceSchema, MaterializationDescriptor descriptor) {
        CalciteEntityViewDriverConnectionFactory.addMaterializedView(sourceSchema, schemaPlus, descriptor.getTableName(), descriptor.getSql());
    }

    @Override
    public void validateMaterialization(SchemaPlus sourceSchema, String tableName, RelDataType expectedRowType, List<String> errors) {
        Table materializationTable = schemaPlus.getTable(tableName);
        RelDataType rowType = materializationTable.getRowType(calciteConnection.getTypeFactory());
        if (!isCompatible(rowType, expectedRowType)) {
            errors.add(" - The structure of the materialization table '" + tableName + "' is incompatible with the expected structure: " + tableSignature(expectedRowType));
        }

    }

    private static boolean isCompatible(RelDataType sourceType, RelDataType targetType) {
        if (!sourceType.isStruct()) {
            if (targetType.isStruct()) {
                return false;
            }
            if (!sourceType.getSqlTypeName().equals(targetType.getSqlTypeName())) {
                return false;
            }
            if (sourceType.isNullable() && !targetType.isNullable()) {
                return false;
            }

            // We already checked nullability and don't care about char sets or collations
            // So the toString check is sufficient
            return sourceType.getSqlTypeName().equals(targetType.getSqlTypeName());
        } else if (!targetType.isStruct()) {
            return false;
        }

        List<RelDataTypeField> fieldList = sourceType.getFieldList();
        if (fieldList.size() != targetType.getFieldCount()) {
            return false;
        }
        for (RelDataTypeField relDataTypeField : fieldList) {
            RelDataTypeField field = targetType.getField(relDataTypeField.getName(), false, true);
            if (field == null || !isCompatible(field.getType(), relDataTypeField.getType())) {
                return false;
            }
        }

        return true;
    }

    private static String tableSignature(RelDataType type) {
        StringBuilder sb = new StringBuilder();
        for (RelDataTypeField relDataTypeField : type.getFieldList()) {
            sb.append(',');
            sb.append(relDataTypeField.getName());
            sb.append(' ');
            sb.append(relDataTypeField.getType());

        }
        sb.setCharAt(0, '(');
        sb.append(')');
        return sb.toString();
    }
}
