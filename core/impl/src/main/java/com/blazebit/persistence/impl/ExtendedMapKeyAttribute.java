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

package com.blazebit.persistence.impl;

import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.parser.MapKeyAttribute;
import com.blazebit.persistence.spi.ExtendedAttribute;
import com.blazebit.persistence.spi.JoinTable;
import com.blazebit.persistence.spi.JpaProvider;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.MapAttribute;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian Beikov
 * @since 1.3.0
 */
public class ExtendedMapKeyAttribute<X, Y> extends MapKeyAttribute<X, Y> implements ExtendedAttribute<X, Y> {

    private final ExtendedAttribute mapAttribute;

    public ExtendedMapKeyAttribute(ExtendedAttribute mapAttribute) {
        super((MapAttribute<?, Y, ?>) mapAttribute.getAttribute());
        this.mapAttribute = mapAttribute;
    }

    @Override
    public Attribute<X, ?> getAttribute() {
        return this;
    }

    @Override
    public List<Attribute<?, ?>> getAttributePath() {
        return Collections.<Attribute<?, ?>>singletonList(this);
    }

    @Override
    public Class<Y> getElementClass() {
        return super.getJavaType();
    }

    @Override
    public boolean hasCascadingDeleteCycle() {
        return false;
    }

    @Override
    public boolean isForeignJoinColumn() {
        return false;
    }

    @Override
    public boolean isColumnShared() {
        return false;
    }

    @Override
    public boolean isBag() {
        return false;
    }

    @Override
    public boolean isOrphanRemoval() {
        return false;
    }

    @Override
    public boolean isDeleteCascaded() {
        return false;
    }

    @Override
    public JpaProvider.ConstraintType getJoinTypeIndexedRequiresTreatFilter(JoinType joinType) {
        return JpaProvider.ConstraintType.NONE;
    }

    public Map<String, String> getWritableMappedByMappings(EntityType inverseType) {
        return null;
    }

    @Override
    public String getMappedBy() {
        return null;
    }

    @Override
    public JoinTable getJoinTable() {
        return null;
    }

    @Override
    public String[] getColumnNames() {
        return null;
    }

    @Override
    public String[] getColumnTypes() {
        return null;
    }
}
