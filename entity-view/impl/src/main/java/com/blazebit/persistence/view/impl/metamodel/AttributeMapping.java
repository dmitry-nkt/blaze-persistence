/*
 * Copyright 2014 - 2017 Blazebit.
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

package com.blazebit.persistence.view.impl.metamodel;

import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.InverseRemoveStrategy;
import com.blazebit.persistence.view.Mapping;
import com.blazebit.persistence.view.metamodel.Type;
import com.blazebit.persistence.view.spi.EntityViewAttributeMapping;

import javax.persistence.metamodel.ManagedType;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public abstract class AttributeMapping implements EntityViewAttributeMapping {

    protected final ViewMapping viewMapping;
    protected final Annotation mapping;
    protected final MetamodelBootContext context;

    // Java types
    protected final boolean isCollection;
    protected final Class<?> typeClass;
    protected final Class<?> keyTypeClass;
    protected final Class<?> elementTypeClass;
    protected final Map<Class<?>, String> inheritanceSubtypeClassMappings;
    protected final Map<Class<?>, String> keyInheritanceSubtypeClassMappings;
    protected final Map<Class<?>, String> elementInheritanceSubtypeClassMappings;

    // Basic configs
    protected ContainerBehavior containerBehavior;
    protected Class<? extends Comparator<?>> comparatorClass;

    // Other configs
    protected Integer defaultBatchSize;

    // Resolved types
    protected Type<?> type;
    protected Type<?> keyType;
    protected Type<?> elementType;
    protected ViewMapping typeMapping;
    protected ViewMapping keyViewMapping;
    protected ViewMapping elementViewMapping;

    protected InheritanceViewMapping inheritanceSubtypeMappings;
    protected InheritanceViewMapping keyInheritanceSubtypeMappings;
    protected InheritanceViewMapping elementInheritanceSubtypeMappings;
    protected Map<ManagedViewTypeImpl<?>, String> inheritanceSubtypes;
    protected Map<ManagedViewTypeImpl<?>, String> keyInheritanceSubtypes;
    protected Map<ManagedViewTypeImpl<?>, String> elementInheritanceSubtypes;

    protected AbstractAttribute<?, ?> attribute;

    public AttributeMapping(ViewMapping viewMapping, Annotation mapping, MetamodelBootContext context, boolean isCollection, Class<?> typeClass, Class<?> keyTypeClass, Class<?> elementTypeClass, Map<Class<?>, String> inheritanceSubtypeClassMappings, Map<Class<?>, String> keyInheritanceSubtypeClassMappings, Map<Class<?>, String> elementInheritanceSubtypeClassMappings) {
        this.viewMapping = viewMapping;
        this.mapping = mapping;
        this.context = context;
        this.isCollection = isCollection;
        this.typeClass = typeClass;
        this.keyTypeClass = keyTypeClass;
        this.elementTypeClass = elementTypeClass;
        this.inheritanceSubtypeClassMappings = inheritanceSubtypeClassMappings;
        this.keyInheritanceSubtypeClassMappings = keyInheritanceSubtypeClassMappings;
        this.elementInheritanceSubtypeClassMappings = elementInheritanceSubtypeClassMappings;
    }

    public Annotation getMapping() {
        return mapping;
    }

    public ViewMapping getKeyViewMapping() {
        return keyViewMapping;
    }

    public ViewMapping getElementViewMapping() {
        return elementViewMapping;
    }

    public abstract boolean isId();

    public abstract boolean isVersion();

    @Override
    public boolean isCollection() {
        return isCollection;
    }

    @Override
    public ContainerBehavior getContainerBehavior() {
        return containerBehavior;
    }

    @Override
    public void setContainerDefault() {
        this.containerBehavior = ContainerBehavior.DEFAULT;
        this.comparatorClass = null;
    }

    @Override
    public void setContainerIndexed() {
        this.containerBehavior = ContainerBehavior.INDEXED;
        this.comparatorClass = null;
    }

    @Override
    public void setContainerOrdered() {
        this.containerBehavior = ContainerBehavior.ORDERED;
        this.comparatorClass = null;
    }

    @Override
    public void setContainerSorted(Class<? extends Comparator<?>> comparatorClass) {
        this.containerBehavior = ContainerBehavior.SORTED;
        this.comparatorClass = comparatorClass;
    }

    @Override
    public Class<? extends Comparator<?>> getComparatorClass() {
        return comparatorClass;
    }

    @Override
    public Integer getDefaultBatchSize() {
        return defaultBatchSize;
    }

    @Override
    public void setDefaultBatchSize(Integer defaultBatchSize) {
        this.defaultBatchSize = defaultBatchSize;
    }

    public abstract String getErrorLocation();

    public abstract String getMappedBy();

    public abstract Map<String, String> getWritableMappedByMappings();

    public abstract InverseRemoveStrategy getInverseRemoveStrategy();

    public boolean isSorted() {
        return containerBehavior == ContainerBehavior.SORTED;
    }

    public boolean determineIndexed(MetamodelBuildingContext context, ManagedType<?> managedType) {
        if (containerBehavior != null) {
            return containerBehavior == ContainerBehavior.INDEXED;
        }

        String mappingExpression;
        if (mapping instanceof IdMapping) {
            mappingExpression = ((IdMapping) mapping).value();
        } else if (mapping instanceof Mapping) {
            mappingExpression = ((Mapping) mapping).value();
        } else {
            // Correlated mappings, parameter mappings and subqueries are never indexed
            containerBehavior = ContainerBehavior.DEFAULT;
            return false;
        }
        if (MetamodelUtils.isIndexedList(context.getEntityMetamodel(), context.getExpressionFactory(), managedType.getJavaType(), AbstractAttribute.stripThisFromMapping(mappingExpression))) {
            containerBehavior = ContainerBehavior.INDEXED;
            return true;
        } else {
            containerBehavior = ContainerBehavior.DEFAULT;
            return false;
        }
    }

    @Override
    public Class<?> getTypeClass() {
        return typeClass;
    }

    @Override
    public Class<?> getKeyTypeClass() {
        return keyTypeClass;
    }

    @Override
    public Class<?> getElementTypeClass() {
        return elementTypeClass;
    }

    public Class<?> getJavaType(MetamodelBuildingContext context) {
        Type<?> t = getType(context);
        if (t == null) {
            return null;
        }
        return t.getJavaType();
    }

    public Type<?> getType(MetamodelBuildingContext context) {
        if (type != null) {
            return type;
        }
        if (typeMapping == null) {
            return type = context.getBasicType(typeClass);
        }
        return type = typeMapping.getManagedViewType(context);
    }

    public Type<?> getKeyType(MetamodelBuildingContext context) {
        if (keyType != null) {
            return keyType;
        }
        if (keyViewMapping == null) {
            return keyType = context.getBasicType(keyTypeClass);
        }
        return keyType = keyViewMapping.getManagedViewType(context);
    }

    public Type<?> getElementType(MetamodelBuildingContext context) {
        if (elementType != null) {
            return elementType;
        }
        if (elementViewMapping == null) {
            return elementType = context.getBasicType(elementTypeClass);
        }
        return elementType = elementViewMapping.getManagedViewType(context);
    }

    public Map<ManagedViewTypeImpl<?>, String> getInheritanceSubtypes(MetamodelBuildingContext context) {
        if (inheritanceSubtypes != null) {
            return inheritanceSubtypes;
        }
        return inheritanceSubtypes = initializeInheritanceSubtypes(inheritanceSubtypeMappings, typeMapping, context);
    }

    public Map<ManagedViewTypeImpl<?>, String> getKeyInheritanceSubtypes(MetamodelBuildingContext context) {
        if (keyInheritanceSubtypes != null) {
            return keyInheritanceSubtypes;
        }
        return keyInheritanceSubtypes = initializeInheritanceSubtypes(keyInheritanceSubtypeMappings, keyViewMapping, context);
    }

    public Map<ManagedViewTypeImpl<?>, String> getElementInheritanceSubtypes(MetamodelBuildingContext context) {
        if (elementInheritanceSubtypes != null) {
            return elementInheritanceSubtypes;
        }
        return elementInheritanceSubtypes = initializeInheritanceSubtypes(elementInheritanceSubtypeMappings, elementViewMapping, context);
    }

    @SuppressWarnings("unchecked")
    private Map<ManagedViewTypeImpl<?>, String> initializeInheritanceSubtypes(InheritanceViewMapping inheritanceSubtypeMappings, ViewMapping viewMapping, MetamodelBuildingContext context) {
        if (inheritanceSubtypeMappings == null || inheritanceSubtypeMappings.getInheritanceSubtypeMappings().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<ManagedViewTypeImpl<?>, String> map = new LinkedHashMap<>(inheritanceSubtypeMappings.getInheritanceSubtypeMappings().size());
        for (Map.Entry<ViewMapping, String> mappingEntry : inheritanceSubtypeMappings.getInheritanceSubtypeMappings().entrySet()) {
            String mapping = mappingEntry.getValue();
            if (mapping == null) {
                mapping = mappingEntry.getKey().determineInheritanceMapping(context);
                // An empty inheritance mapping signals that a subtype should actually be considered. If it was null it wouldn't be considered
                if (mapping == null) {
                    mapping = "";
                }
            }
            map.put(mappingEntry.getKey().getManagedViewType(context), mapping);
        }
        if (map.equals(viewMapping.getManagedViewType(context).getInheritanceSubtypeConfiguration())) {
            return (Map<ManagedViewTypeImpl<?>, String>) (Map<?, ?>) viewMapping.getManagedViewType(context).getInheritanceSubtypeConfiguration();
        } else {
            return Collections.unmodifiableMap(map);
        }
    }

    public void initializeViewMappings(MetamodelBuildingContext context, Set<Class<?>> dependencies) {
        if (context.isEntityView(typeClass)) {
            typeMapping = initializeDependentMapping(typeClass, context, dependencies);
            inheritanceSubtypeMappings = initializedInheritanceViewMappings(typeMapping, inheritanceSubtypeClassMappings, context, dependencies);
        }
        if (context.isEntityView(keyTypeClass)) {
            keyViewMapping = initializeDependentMapping(keyTypeClass, context, dependencies);
            keyInheritanceSubtypeMappings = initializedInheritanceViewMappings(keyViewMapping, keyInheritanceSubtypeClassMappings, context, dependencies);
        }
        if (context.isEntityView(elementTypeClass)) {
            elementViewMapping = initializeDependentMapping(elementTypeClass, context, dependencies);
            elementInheritanceSubtypeMappings = initializedInheritanceViewMappings(elementViewMapping, elementInheritanceSubtypeClassMappings, context, dependencies);
        }
    }

    private InheritanceViewMapping initializedInheritanceViewMappings(ViewMapping attributeMapping, Map<Class<?>, String> inheritanceMapping, MetamodelBuildingContext context, Set<Class<?>> dependencies) {
        InheritanceViewMapping inheritanceViewMapping;
        Map<ViewMapping, String> subtypeMappings = new HashMap<>();
        if (attributeMapping != null) {
            if (inheritanceMapping == null) {
                inheritanceViewMapping = attributeMapping.getDefaultInheritanceViewMapping();
            } else {
                subtypeMappings = new HashMap<>(inheritanceMapping.size() + 1);

                for (Map.Entry<Class<?>, String> mappingEntry : inheritanceMapping.entrySet()) {
                    ViewMapping subtypeMapping = initializeDependentMapping(mappingEntry.getKey(), context, dependencies);
                    if (subtypeMapping != null) {
                        subtypeMappings.put(subtypeMapping, mappingEntry.getValue());
                    }
                }

                inheritanceViewMapping = new InheritanceViewMapping(subtypeMappings);
                attributeMapping.getInheritanceViewMappings().add(inheritanceViewMapping);
                return inheritanceViewMapping;
            }
        } else {
            inheritanceViewMapping = new InheritanceViewMapping(subtypeMappings);
        }

        return inheritanceViewMapping;
    }

    protected ViewMapping initializeDependentMapping(Class<?> subviewClass, MetamodelBuildingContext context, Set<Class<?>> dependencies) {
        if (dependencies.contains(subviewClass)) {
            circularDependencyError(dependencies);
            return null;
        }

        dependencies.add(subviewClass);

        ViewMapping mapping = context.getViewMappings().get(subviewClass);
        if (mapping == null) {
            unknownSubviewType(subviewClass);
        } else {
            mapping.initializeViewMappings(context, dependencies, this);
        }

        dependencies.remove(subviewClass);

        return mapping;
    }

    public void circularDependencyError(Set<Class<?>> dependencies) {
        context.addError("A circular dependency is introduced at the " + getErrorLocation() + " in the following dependency set: " + Arrays.deepToString(dependencies.toArray()));
    }

    public void unknownSubviewType(Class<?> subviewClass) {
        context.addError("An unknown or unregistered subview type '" + subviewClass.getName() + "' is used at the " + getErrorLocation() + "!");
    }
}