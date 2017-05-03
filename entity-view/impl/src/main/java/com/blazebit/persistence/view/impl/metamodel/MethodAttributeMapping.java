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

import com.blazebit.persistence.impl.AttributePath;
import com.blazebit.persistence.impl.util.JpaMetamodelUtils;
import com.blazebit.persistence.view.CascadeType;
import com.blazebit.persistence.view.InverseRemoveStrategy;
import com.blazebit.persistence.view.Mapping;
import com.blazebit.persistence.view.MappingCorrelated;
import com.blazebit.persistence.view.MappingCorrelatedSimple;
import com.blazebit.persistence.view.MappingParameter;
import com.blazebit.persistence.view.MappingSubquery;
import com.blazebit.persistence.view.impl.metamodel.attribute.CorrelatedMethodCollectionAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.CorrelatedMethodListAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.CorrelatedMethodMappingSingularAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.CorrelatedMethodSetAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.MappingMethodCollectionAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.MappingMethodListAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.MappingMethodMapAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.MappingMethodSetAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.MappingMethodSingularAttribute;
import com.blazebit.persistence.view.impl.metamodel.attribute.SubqueryMethodSingularAttribute;
import com.blazebit.persistence.view.spi.EntityViewMapping;
import com.blazebit.persistence.view.spi.EntityViewMethodAttributeMapping;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class MethodAttributeMapping extends AttributeMapping implements EntityViewMethodAttributeMapping {

    private final String attributeName;
    private final Method method;

    // Updatable configs
    private Boolean isUpdatable;
    private Boolean isOptimisticLockProtected;
    private String mappedBy;
    private Map<String, String> writableMappedByMappings;
    private boolean mappedByResolved;
    private InverseRemoveStrategy inverseRemoveStrategy = InverseRemoveStrategy.SET_NULL;
    private Set<CascadeType> cascadeTypes = Collections.emptySet();
    private Set<Class<?>> cascadeSubtypeClasses;
    private Set<Class<?>> cascadePersistSubtypeClasses;
    private Set<Class<?>> cascadeUpdateSubtypeClasses;

    private Set<ViewMapping> cascadeSubtypeMappings;
    private Set<ViewMapping> cascadePersistSubtypeMappings;
    private Set<ViewMapping> cascadeUpdateSubtypeMappings;
    private Set<ManagedViewTypeImpl<?>> cascadeSubtypes;
    private Set<ManagedViewTypeImpl<?>> cascadePersistSubtypes;
    private Set<ManagedViewTypeImpl<?>> cascadeUpdateSubtypes;

    // TODO: attribute filter config

    public MethodAttributeMapping(ViewMapping viewMapping, Annotation mapping, MetamodelBootContext context, String attributeName, Method method, boolean isCollection, Class<?> typeClass, Class<?> keyTypeClass, Class<?> elementTypeClass,
                                  Map<Class<?>, String> inheritanceSubtypeClassMappings, Map<Class<?>, String> keyInheritanceSubtypeClassMappings, Map<Class<?>, String> elementInheritanceSubtypeClassMappings) {
        super(viewMapping, mapping, context, isCollection, typeClass, keyTypeClass, elementTypeClass, inheritanceSubtypeClassMappings, keyInheritanceSubtypeClassMappings, elementInheritanceSubtypeClassMappings);
        this.attributeName = attributeName;
        this.method = method;
    }

    @Override
    public EntityViewMapping getDeclaringView() {
        return viewMapping;
    }

    @Override
    public String getName() {
        return attributeName;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Boolean getUpdatable() {
        return isUpdatable;
    }

    public Boolean getOptimisticLockProtected() {
        return isOptimisticLockProtected;
    }

    @Override
    public boolean isId() {
        return viewMapping.getIdAttribute() == this;
    }

    @Override
    public boolean isVersion() {
        return viewMapping.getVersionAttribute() == this;
    }

    @Override
    public Set<CascadeType> getCascadeTypes() {
        return cascadeTypes;
    }

    @Override
    public void setUpdatable(boolean updatable, CascadeType[] cascadeTypes, Class<?>[] subtypes, Class<?>[] persistSubtypes, Class<?>[] updateSubtypes) {
        this.isUpdatable = updatable;
        this.cascadeTypes = new HashSet<>(Arrays.asList(cascadeTypes));
        this.cascadeSubtypeClasses = new HashSet<>(Arrays.asList(subtypes));
        this.cascadePersistSubtypeClasses = new HashSet<>(Arrays.asList(persistSubtypes));
        this.cascadeUpdateSubtypeClasses = new HashSet<>(Arrays.asList(updateSubtypes));
    }

    public void setOptimisticLockProtected(Boolean optimisticLockProtected) {
        isOptimisticLockProtected = optimisticLockProtected;
    }

    @Override
    public String getMappedBy() {
        return mappedBy;
    }

    @Override
    public Map<String, String> getWritableMappedByMappings() {
        return writableMappedByMappings;
    }

    @Override
    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
        this.mappedByResolved = true;
    }

    @Override
    public InverseRemoveStrategy getInverseRemoveStrategy() {
        return inverseRemoveStrategy;
    }

    @Override
    public void setInverseRemoveStrategy(InverseRemoveStrategy inverseRemoveStrategy) {
        if (inverseRemoveStrategy == null) {
            throw new IllegalArgumentException("Invalid null remove strategy!");
        }
        this.inverseRemoveStrategy = inverseRemoveStrategy;
    }

    public Set<ManagedViewTypeImpl<?>> getCascadeSubtypes(MetamodelBuildingContext context) {
        if (cascadeSubtypes != null) {
            return cascadeSubtypes;
        }
        return cascadeSubtypes = initializeCascadeSubtypes(cascadeSubtypeMappings, context);
    }

    public Set<ManagedViewTypeImpl<?>> getCascadePersistSubtypes(MetamodelBuildingContext context) {
        if (cascadePersistSubtypes != null) {
            return cascadePersistSubtypes;
        }
        return cascadePersistSubtypes = initializeCascadeSubtypes(cascadePersistSubtypeMappings, context);
    }

    public Set<ManagedViewTypeImpl<?>> getCascadeUpdateSubtypes(MetamodelBuildingContext context) {
        if (cascadeUpdateSubtypes != null) {
            return cascadeUpdateSubtypes;
        }
        return cascadeUpdateSubtypes = initializeCascadeSubtypes(cascadeUpdateSubtypeMappings, context);
    }

    private Set<ManagedViewTypeImpl<?>> initializeCascadeSubtypes(Set<ViewMapping> subtypeMappings, MetamodelBuildingContext context) {
        if (subtypeMappings == null || subtypeMappings.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ManagedViewTypeImpl<?>> subtypes = new HashSet<>(subtypeMappings.size());
        for (ViewMapping mapping : subtypeMappings) {
            subtypes.add(mapping.getManagedViewType(context));
        }
        return subtypes;
    }

    @Override
    public String getErrorLocation() {
        return getLocation(attributeName, method);
    }

    public static String getLocation(String attributeName, Method method) {
        return "attribute " + attributeName + "[" + methodReference(method) + "]";
    }

    @Override
    public void initializeViewMappings(MetamodelBuildingContext context, Set<Class<?>> dependencies) {
        super.initializeViewMappings(context, dependencies);

        if (isUpdatable == Boolean.TRUE) {
            this.cascadeSubtypeMappings = initializeDependentCascadeSubtypeMappings(context, dependencies, cascadeSubtypeClasses);
            this.cascadePersistSubtypeMappings = initializeDependentCascadeSubtypeMappings(context, dependencies, cascadePersistSubtypeClasses);
            this.cascadeUpdateSubtypeMappings = initializeDependentCascadeSubtypeMappings(context, dependencies, cascadeUpdateSubtypeClasses);
        }
    }

    private String determineMappedBy(ManagedType<?> managedType, String mapping, MetamodelBuildingContext context) {
        if (mappedByResolved) {
            return mappedBy;
        }

        if (mapping.isEmpty()) {
            return null;
        }
        if (!(managedType instanceof EntityType<?>)) {
            // Can't determine the inverse mapped by attribute of a non-entity
            return null;
        }

        // If we find a non-simple path, we don't even try to find a mapped by mapping
        for (int i = 0; i < mapping.length(); i++) {
            final char c = mapping.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                return null;
            }
        }

        try {
            AttributePath basicAttributePath = JpaMetamodelUtils.getAttributePath(context.getEntityMetamodel(), managedType, mapping);
            List<Attribute<?, ?>> attributes = basicAttributePath.getAttributes();
            for (int i = 0; i < attributes.size() - 1; i++) {
                if (attributes.get(i).getDeclaringType().getPersistenceType() != Type.PersistenceType.EMBEDDABLE) {
                    // If the mapping goes over a non-embeddable, we can't determine a mapped by attribute name
                    return null;
                }
            }
            return context.getJpaProvider().getMappedBy((EntityType<?>) managedType, mapping);
        } catch (IllegalArgumentException ex) {
            // if the mapping is invalid, we skip the determination as the error will be analyzed further at a later stage
            return null;
        }
    }

    private Map<String, String> determineWritableMappedByMappings(ManagedType<?> managedType, String mappedBy, MetamodelBuildingContext context) {
        ViewMapping elementViewMapping = getElementViewMapping();
        EntityType<?> elementType;
        if (elementViewMapping != null) {
            elementType = context.getEntityMetamodel().entity(elementViewMapping.getEntityClass());
        } else {
            elementType = context.getEntityMetamodel().entity(getElementTypeClass());
        }

        return context.getJpaProvider().getWritableMappedByMappings((EntityType<?>) managedType, elementType, mappedBy);
    }

    private Set<ViewMapping> initializeDependentCascadeSubtypeMappings(MetamodelBuildingContext context, Set<Class<?>> dependencies, Set<Class<?>> subtypes) {
        if (subtypes.size() == 0) {
            return Collections.emptySet();
        }

        Set<ViewMapping> subtypeMappings = new HashSet<>(subtypes.size());
        for (Class<?> type : subtypes) {
            ViewMapping subtypeMapping = initializeDependentMapping(type, context, dependencies);
            if (subtypeMapping != null) {
                subtypeMappings.add(subtypeMapping);
            }
        }

        return subtypeMappings;
    }

    public MethodAttributeMapping handleReplacement(AttributeMapping original) {
        if (original == null) {
            return this;
        }
        if (!(original instanceof MethodAttributeMapping)) {
            throw new IllegalStateException("Tried to replace attribute [" + original + "] with method attribute: " + this);
        }

        MethodAttributeMapping originalAttribute = (MethodAttributeMapping) original;
        // If the mapping is the same, just let it through
        if (mapping.equals(originalAttribute.getMapping())) {
            return originalAttribute;
        }

        // Also let through the attributes that are "specialized" in subclasses
        if (method.getDeclaringClass() != originalAttribute.getMethod().getDeclaringClass()
                && method.getDeclaringClass().isAssignableFrom(originalAttribute.getMethod().getDeclaringClass())) {
            // The method is overridden/specialized by the method of the existing attribute
            return originalAttribute;
        }

        // If the original is implicitly mapped, but this attribute isn't, we have to replace it
        if (originalAttribute.getMapping() instanceof MappingLiteral) {
            return this;
        }

        context.addError("Conflicting attribute mapping for attribute '" + attributeName + "' at the methods [" + methodReference(method) + ", " + methodReference(originalAttribute.getMethod()) + "] for managed view type '" + viewMapping.getEntityViewClass().getName() + "'");
        return originalAttribute;
    }

    private static String methodReference(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    // If you change something here don't forget to also update ParameterAttributeMapping#getParameterAttribute
    @SuppressWarnings("unchecked")
    public <X> AbstractMethodAttribute<? super X, ?> getMethodAttribute(ManagedViewTypeImpl<X> viewType, int dirtyStateIndex, MetamodelBuildingContext context) {
        if (attribute == null) {
            if (mapping instanceof MappingParameter) {
                mappedByResolved = true;
                attribute = new MappingMethodSingularAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                return (AbstractMethodAttribute<? super X, ?>) attribute;
            }

            if (!mappedByResolved && mapping instanceof Mapping) {
                ManagedType<?> managedType = context.getEntityMetamodel().getManagedType(viewType.getEntityClass());
                mappedBy = determineMappedBy(managedType, AbstractAttribute.stripThisFromMapping(((Mapping) mapping).value()), context);
            }
            mappedByResolved = true;
            if (mappedBy != null && dirtyStateIndex != -1) {
                ManagedType<?> managedType = context.getEntityMetamodel().getManagedType(viewType.getEntityClass());
                writableMappedByMappings = determineWritableMappedByMappings(managedType, mappedBy, context);
            }

            boolean correlated = mapping instanceof MappingCorrelated || mapping instanceof MappingCorrelatedSimple;

            if (isCollection) {
                if (Collection.class == typeClass) {
                    if (correlated) {
                        attribute = new CorrelatedMethodCollectionAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                    } else {
                        attribute = new MappingMethodCollectionAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                    }
                } else if (List.class == typeClass) {
                    if (correlated) {
                        attribute = new CorrelatedMethodListAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                    } else {
                        attribute = new MappingMethodListAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                    }
                } else if (Set.class == typeClass || SortedSet.class == typeClass || NavigableSet.class == typeClass) {
                    if (correlated) {
                        attribute = new CorrelatedMethodSetAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                    } else {
                        attribute = new MappingMethodSetAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                    }
                } else if (Map.class == typeClass || SortedMap.class == typeClass || NavigableMap.class == typeClass) {
                    if (correlated) {
                        context.addError("The mapping defined on method '" + viewType.getJavaType().getName() + "." + method.getName() + "' uses a Map type with a correlated mapping which is unsupported!");
                        attribute = null;
                    } else {
                        attribute = new MappingMethodMapAttribute<X, Object, Object>(viewType, this, context, dirtyStateIndex);
                    }
                } else {
                    context.addError("The mapping defined on method '" + viewType.getJavaType().getName() + "." + method.getName() + "' uses a an unknown collection type: " + typeClass.getName());
                }
            } else {
                if (mapping instanceof MappingSubquery) {
                    attribute = new SubqueryMethodSingularAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                } else if (correlated) {
                    attribute = new CorrelatedMethodMappingSingularAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                } else {
                    attribute = new MappingMethodSingularAttribute<X, Object>(viewType, this, context, dirtyStateIndex);
                }
            }
        } else if (dirtyStateIndex != -1) {
            throw new IllegalStateException("Already constructed attribute with dirtyStateIndex " + ((AbstractMethodAttribute<?, ?>) attribute).getDirtyStateIndex() + " but now a different index " + dirtyStateIndex + " is requested!");
        }

        return (AbstractMethodAttribute<? super X, ?>) attribute;
    }
}