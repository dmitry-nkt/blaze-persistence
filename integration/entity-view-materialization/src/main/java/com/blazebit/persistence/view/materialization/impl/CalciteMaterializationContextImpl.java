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

import com.blazebit.annotation.AnnotationUtils;
import com.blazebit.exception.ExceptionUtils;
import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.From;
import com.blazebit.persistence.ObjectBuilder;
import com.blazebit.persistence.parser.expression.ExpressionFactory;
import com.blazebit.persistence.spi.ExtendedQuerySupport;
import com.blazebit.persistence.spi.JpaProvider;
import com.blazebit.persistence.spi.JpaProviderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.blazebit.persistence.view.materialization.api.Materialization;
import com.blazebit.persistence.view.materialization.api.MaterializationContext;
import com.blazebit.persistence.view.materialization.api.ChangeEvent;
import com.blazebit.persistence.view.materialization.api.MaterializationDdl;
import com.blazebit.persistence.view.materialization.api.RefreshEvent;
import com.blazebit.persistence.view.impl.EntityViewConfiguration;
import com.blazebit.persistence.view.impl.EntityViewManagerImpl;
import com.blazebit.persistence.view.impl.macro.DefaultViewRootJpqlMacro;
import com.blazebit.persistence.view.impl.metamodel.ManagedViewTypeImpl;
import com.blazebit.persistence.view.impl.metamodel.MappingConstructorImpl;
import com.blazebit.persistence.view.impl.objectbuilder.ViewTypeObjectBuilderTemplate;
import com.blazebit.persistence.view.materialization.api.spi.DataStore;
import com.blazebit.persistence.view.materialization.api.spi.DataStoreFactory;
import com.blazebit.persistence.view.metamodel.ViewType;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptPredicateList;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexTableInputRef;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class CalciteMaterializationContextImpl implements MaterializationContext {

    private static final Logger LOG = Logger.getLogger(CalciteMaterializationContextImpl.class.getName());

    private final EntityManagerFactory emf;
    private final JpaProviderFactory jpaProviderFactory;
    private final DataSource calciteConnectionPool;
    private final Map<List<String>, List<RexNode>> tableToDependentPredicateMap;
    private final Map<List<String>, Set<String>> tableToMaterializationsMap;
    private final Map<String, MaterializationDescriptor> materializationDescriptors;

    private CalciteMaterializationContextImpl(EntityManagerFactory emf, JpaProviderFactory jpaProviderFactory, DataSource calciteConnectionPool, Map<List<String>, List<RexNode>> tableToDependentPredicateMap, Map<List<String>, Set<String>> tableToMaterializationsMap, Map<String, MaterializationDescriptor> materializationDescriptors) {
        this.emf = emf;
        this.jpaProviderFactory = jpaProviderFactory;
        this.calciteConnectionPool = calciteConnectionPool;
        this.tableToDependentPredicateMap = tableToDependentPredicateMap;
        this.tableToMaterializationsMap = tableToMaterializationsMap;
        this.materializationDescriptors = materializationDescriptors;
    }

    @Override
    public List<RefreshEvent> computeRefreshEvents(ChangeEvent event) {
        Set<String> materializations = tableToMaterializationsMap.get(event.getQualifiedTableName());
        if (materializations == null) {
            return Collections.emptyList();
        }

        List<RefreshEvent> refreshEvents = new ArrayList<>(materializations.size());
        for (String materialization : materializations) {
            refreshEvents.add(new RefreshEvent(materialization, Collections.<Serializable[]>emptyList()));
        }

        return refreshEvents;
    }

    @Override
    public void processRefreshEvents(EntityManager em, List<RefreshEvent> refreshEvents) {
        for (RefreshEvent refreshEvent : refreshEvents) {
            MaterializationDescriptor descriptor = materializationDescriptors.get(refreshEvent.getMaterializationName().toUpperCase());
            StringBuilder sb = new StringBuilder();
            sb.append("insert into ");
            sb.append(descriptor.getTableName());
            int startIdx = sb.length();
            for (MaterializationFieldDescriptor fieldDescriptor : descriptor.getFields()) {
                sb.append(',');
                sb.append(fieldDescriptor.getName());
            }
            sb.setCharAt(startIdx, '(');
            sb.append(") ");
            sb.append(descriptor.getSql());

            em.createNativeQuery("delete from " + descriptor.getTableName()).executeUpdate();
            em.createNativeQuery(sb.toString()).executeUpdate();
        }
    }

    @Override
    public EntityManager createEntityManager() {
        EntityManager em = null;

        try {
            em = emf.createEntityManager();
            JpaProvider jpaProvider = jpaProviderFactory.createJpaProvider(em);
            Connection realConnection = jpaProvider.getConnection(em);
            Connection connection = CloseProtectedConnectionHandler.wrap(realConnection);
            ThreadLocalDataSource.CONNECTION.set(connection);
            Connection calciteConnection = calciteConnectionPool.getConnection();
            jpaProvider.setConnection(em, TxProtectedConnectionHandler.wrap(calciteConnection, realConnection));
            return ConnectionClearingEntityManagerHandler.wrap(em, realConnection);
        } catch (Throwable t) {
            if (em != null) {
                em.close();
            }
            ThreadLocalDataSource.CONNECTION.remove();
            ExceptionUtils.doThrow(t);
            return null;
        }
    }

    @Override
    public List<MaterializationDdl> generateSchema() {
        List<MaterializationDdl> list = new ArrayList<>(materializationDescriptors.size());
        StringBuilder sb = new StringBuilder();

        for (MaterializationDescriptor descriptor : materializationDescriptors.values()) {
            sb.append("create table ");
            sb.append(descriptor.getTableName());
            int startIdx = sb.length();
            for (MaterializationFieldDescriptor fieldDescriptor : descriptor.getFields()) {
                sb.append(',');
                sb.append(fieldDescriptor.getName());
                sb.append(' ');
                sb.append(fieldDescriptor.getType());
            }
            sb.setCharAt(startIdx, '(');
            sb.append(')');
            list.add(new SqlTableMaterializationDdl(sb.toString()));
            sb.setLength(0);
        }

        return list;
    }

    public static CalciteMaterializationContextImpl create(EntityViewManager evm, CriteriaBuilderFactory criteriaBuilderFactory, EntityManagerFactory emf) {
        return create(evm, criteriaBuilderFactory, emf, null, Collections.EMPTY_MAP);
    }

    public static CalciteMaterializationContextImpl create(EntityViewManager evm, CriteriaBuilderFactory criteriaBuilderFactory, EntityManagerFactory emf, Map<String, DataStoreFactory> schemas) {
        return create(evm, criteriaBuilderFactory, emf, null, schemas);
    }

    public static CalciteMaterializationContextImpl create(EntityViewManager evm, CriteriaBuilderFactory criteriaBuilderFactory, EntityManagerFactory emf, String defaultSchemaName, Map<String, DataStoreFactory> schemas) {
        EntityManager em = null;

        try {
            em = emf.createEntityManager();
            JpaProvider jpaProvider = criteriaBuilderFactory.getService(JpaProviderFactory.class).createJpaProvider(em);
            Connection connection = CloseProtectedConnectionHandler.wrap(jpaProvider.getConnection(em));
            ThreadLocalDataSource.CONNECTION.set(connection);

            CalciteConnection calciteConnection = (CalciteConnection) new Driver()
                    .connect("jdbc:calcite:lex=MYSQL_ANSI;materializationsEnabled=true;", null);

            String name = defaultSchemaName == null || defaultSchemaName.isEmpty() ? "adhoc" : defaultSchemaName;
            String schema;
            String catalog;
            if (connection.getMetaData().getJDBCMinorVersion() > 0) {
                schema = connection.getSchema();
            } else {
                schema = null;
            }
            catalog = null;

            Map<String, DataStore> dataStores = new HashMap<>();
            JdbcSchema jdbcSchema = JdbcSchema.create(
                    calciteConnection.getRootSchema(),
                    name,
                    ThreadLocalDataSource.INSTANCE,
                    catalog,
                    schema
            );
            calciteConnection.getRootSchema().add(name, jdbcSchema);
            SchemaPlus subSchema = calciteConnection.getRootSchema().getSubSchema(name);
            dataStores.put(name, new JdbcDataStore(calciteConnection, subSchema));

            for (Map.Entry<String, DataStoreFactory> schemaEntry : schemas.entrySet()) {
                dataStores.put(schemaEntry.getKey(), schemaEntry.getValue().create(calciteConnection, schemaEntry.getKey()));
            }

            calciteConnection.setSchema(name);

            return create((EntityViewManagerImpl) evm, criteriaBuilderFactory, em, calciteConnection, subSchema, name, schema, catalog, schemas, dataStores);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (em != null) {
                em.close();
            }
            ThreadLocalDataSource.CONNECTION.remove();
        }
    }

    private static CalciteMaterializationContextImpl create(EntityViewManagerImpl evm, CriteriaBuilderFactory criteriaBuilderFactory, EntityManager em, CalciteConnection calciteConnection, SchemaPlus schema, String name, String schemaName, String catalog, Map<String, DataStoreFactory> schemas, Map<String, DataStore> dataStores) throws SQLException {
        ExtendedQuerySupport extendedQuerySupport = criteriaBuilderFactory.getService(ExtendedQuerySupport.class);
        Map<List<String>, List<RexNode>> tableToDependentPredicateMap = new HashMap<>();
        Map<List<String>, Set<String>> tableToMaterializationsMap = new HashMap<>();
        Map<String, MaterializationDescriptor> materializationDescriptors = new HashMap<>();
        List<MaterializationDescriptor> foundMaterializationDescriptors = new ArrayList<>();
        List<String> errors = new ArrayList<>(0);

        for (ManagedViewTypeImpl<?> viewType : (Set<ManagedViewTypeImpl<?>>) (Set<?>) evm.getMetamodel().getManagedViews()) {
            Materialization materializationDefinition = AnnotationUtils.findAnnotation(viewType.getJavaType(), Materialization.class);
            if (materializationDefinition == null) {
                continue;
            }

            String materializationSchemaName = materializationDefinition.schema();
            if (materializationSchemaName.isEmpty()) {
                materializationSchemaName = name;
            }
            String tableName = materializationDefinition.name();
            SchemaPlus materializationSchema = calciteConnection.getRootSchema().getSubSchema(materializationSchemaName);

            if (materializationSchema == null) {
                errors.add(" - Unknown schema '" + materializationSchemaName + "' defined for: " + viewType.getJavaType().getName());
                continue;
            }

            DataStore dataStore = dataStores.get(materializationSchemaName);
            CriteriaBuilder<?> criteriaBuilder = criteriaBuilderFactory.create(em, viewType.getEntityClass());

            @SuppressWarnings("unchecked")
            EntityViewSetting<Object, CriteriaBuilder<Object>> setting = EntityViewSetting.create((Class<Object>) viewType.getJavaType());
            evm.applySetting(setting, criteriaBuilder);

            TypedQuery<?> query = criteriaBuilder.getQuery();
            String sql = extendedQuerySupport.getSql(em, query);
//            List<String> fields = template.getFields();
            List<String> fields = getFieldNames(sql);
            String finalSql = renameProjections(sql, fields);

            RelNode rel = sqlToRel(calciteConnection, schema.getName(), finalSql, CalciteSchema.from(schema).getPath().get(0));
            RelDataType expectedRowType = rel.getRowType();

            List<MaterializationFieldDescriptor> fieldDescriptors = new ArrayList<>(expectedRowType.getFieldCount());

            for (int i = 0; i < expectedRowType.getFieldCount(); i++) {
                RelDataTypeField relDataTypeField = expectedRowType.getFieldList().get(i);
                MaterializationFieldTypeDescriptor fieldTypeDescriptor = new MaterializationFieldTypeDescriptor(
                        relDataTypeField.getType().toString(),
                        relDataTypeField.getType().getFullTypeString()
                );
                fieldDescriptors.add(new MaterializationFieldDescriptor(fields.get(i), fieldTypeDescriptor));
            }

            MaterializationDescriptor descriptor = new MaterializationDescriptor(materializationSchemaName, tableName, finalSql, fieldDescriptors);
            materializationDescriptors.put(tableName, descriptor);

            boolean existing = materializationSchema.getTable(tableName) != null;

            if (existing) {
                dataStore.registerMaterialization(schema, descriptor);
                dataStore.validateMaterialization(schema, tableName, expectedRowType, errors);
                initMaps(tableToDependentPredicateMap, tableToMaterializationsMap, tableName, rel);
                foundMaterializationDescriptors.add(descriptor);
            } else {
                LOG.warning("The materialization table '" + tableName + "' could not be found on the remote data store!");
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Could not initialize materialization context because:\n" + errors);
        }

        JpaProviderFactory jpaProviderFactory = criteriaBuilderFactory.getService(JpaProviderFactory.class);
        CalciteEntityViewDriverConnectionFactory connectionFactory = new CalciteEntityViewDriverConnectionFactory(
                name,
                schemaName,
                catalog,
                schemas,
                foundMaterializationDescriptors
        );
        return new CalciteMaterializationContextImpl(
                em.getEntityManagerFactory(),
                jpaProviderFactory,
                new CalciteEntityViewDataSource(connectionFactory),
                tableToDependentPredicateMap,
                tableToMaterializationsMap,
                materializationDescriptors
        );
    }

    private static String renameProjections(String queryString, List<String> fields) {
        SqlParser.Config parserConfig = SqlParser.Config.DEFAULT;
        SqlParser parser = SqlParser.create(queryString, parserConfig);
        SqlSelect sqlNode;
        try {
            sqlNode = (SqlSelect) parser.parseQuery();
        } catch (SqlParseException e) {
            throw new RuntimeException("parse failed", e);
        }

        SqlNodeList selectList = sqlNode.getSelectList();
        for (int i = 0; i < selectList.size(); i++) {
            SqlCall call = (SqlCall) selectList.get(i);
            SqlIdentifier identifier = call.operand(1);
            // upper case because the JDBC table schema also has everything upper cased
            call.setOperand(1, new SqlIdentifier(fields.get(i).toUpperCase(), identifier.getParserPosition()));
        }

        return sqlNode.toSqlString(SqlDialect.CALCITE).getSql();
    }

    private static List<String> getFieldNames(String queryString) {
        SqlParser.Config parserConfig = SqlParser.Config.DEFAULT;
        SqlParser parser = SqlParser.create(queryString, parserConfig);
        SqlSelect sqlNode;
        try {
            sqlNode = (SqlSelect) parser.parseQuery();
        } catch (SqlParseException e) {
            throw new RuntimeException("parse failed", e);
        }

        SqlNodeList selectList = sqlNode.getSelectList();
        List<String> fields = new ArrayList<>(selectList.size());
        Set<String> fieldSet = new HashSet<>(selectList.size());
        for (int i = 0; i < selectList.size(); i++) {
            SqlCall call = (SqlCall) selectList.get(i);
            SqlIdentifier item = call.operand(0);
            String name = item.names.get(item.names.size() - 1);
            String fieldName = name;
            for (int j = 0; !fieldSet.add(fieldName); j++) {
                fieldName = name + j;
            }
            fields.add(fieldName);
        }
        return fields;
    }

    private static void initMaps(Map<List<String>, List<RexNode>> tableToDependentPredicateMap, Map<List<String>, Set<String>> tableToMaterializationsMap, String tableName, RelNode rel) {
        final RelMetadataQuery mq = RelMetadataQuery.instance();
        RelOptPredicateList inputSet = mq.getAllPredicates(rel);
        Set<RexTableInputRef.RelTableRef> tableReferences = mq.getTableReferences(rel);
        for (RexTableInputRef.RelTableRef ref : tableReferences) {
            List<String> qualifiedName = ref.getTable().getQualifiedName();
            List<RexNode> predicates = tableToDependentPredicateMap.get(qualifiedName);
            if (predicates == null) {
                predicates = new ArrayList<>(inputSet.pulledUpPredicates.size());
                tableToDependentPredicateMap.put(qualifiedName, predicates);
            }
            predicates.addAll(inputSet.pulledUpPredicates);

            Set<String> materializationTables = tableToMaterializationsMap.get(qualifiedName);
            if (materializationTables == null) {
                materializationTables = new HashSet<>();
                tableToMaterializationsMap.put(qualifiedName, materializationTables);
            }
            materializationTables.add(tableName);
        }
    }

    private static RexBuilder createRexBuilder(JavaTypeFactory typeFactory) {
        return new RexBuilder(typeFactory);
    }

    private static CalciteCatalogReader createCatalogReader(CalciteConnection connection, String schemaName) {
        SchemaPlus defaultSchema = connection.getRootSchema().getSubSchema(schemaName);
        SchemaPlus rootSchema = rootSchema(defaultSchema);
        CalciteConnectionConfig connectionConfig = connection.config();

        return new CalciteCatalogReader(
                CalciteSchema.from(rootSchema),
                CalciteSchema.from(defaultSchema).path(null),
                connection.getTypeFactory(), connectionConfig);
    }

    private static SchemaPlus rootSchema(SchemaPlus schema) {
        for (;;) {
            if (schema.getParentSchema() == null) {
                return schema;
            }
            schema = schema.getParentSchema();
        }
    }

    private static RelNode sqlToRel(CalciteConnection connection, String schemaName, String queryString, List<String> schemaPath) {
        final FrameworkConfig config = Frameworks.newConfigBuilder()
                .parserConfig(SqlParser.Config.DEFAULT)
                .defaultSchema(connection.getRootSchema())
                .build();
        CalciteConnectionConfig connectionConfig = connection.config();
        SqlOperatorTable operatorTable = config.getOperatorTable();
        SqlParser.Config parserConfig = SqlParser.Config.DEFAULT;
        SqlParser parser = SqlParser.create(queryString, parserConfig);
        SqlNode sqlNode;
        try {
            sqlNode = parser.parseQuery();
        } catch (SqlParseException e) {
            throw new RuntimeException("parse failed", e);
        }

        final SqlConformance conformance = connectionConfig.conformance();
        final CalciteCatalogReader catalogReader = createCatalogReader(connection, schemaName).withSchemaPath(schemaPath);
        final SqlValidator validator = new CalciteSqlValidator(operatorTable, catalogReader, connection.getTypeFactory(), conformance);
        validator.setIdentifierExpansion(true);
        final SqlNode validatedSqlNode = validator.validate(sqlNode);

        final SqlToRelConverter.Config sqlToRelConfig = SqlToRelConverter.configBuilder()
                .withTrimUnusedFields(true).build();
        RexBuilder rexBuilder = createRexBuilder(connection.getTypeFactory());
        RelOptPlanner planner = new MockRelOptPlanner();
        final RelOptCluster cluster = RelOptCluster.create(planner, rexBuilder);
        SqlToRelConverter sqlToRelConverter = new SqlToRelConverter(null, validator, catalogReader, cluster,
                config.getConvertletTable(), sqlToRelConfig);
        RelRoot root = sqlToRelConverter.convertQuery(validatedSqlNode, true, false);

        root.rel.getCluster().setMetadataProvider(DefaultRelMetadataProvider.INSTANCE);
        return root.rel;
    }

}
