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

package com.blazebit.persistence.impl.hibernate;

import javax.persistence.TypedQuery;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.persistence.ObjectBuilder;
import com.blazebit.persistence.impl.jpa.ObjectBuilderJPAQueryAdapter;
import com.blazebit.persistence.spi.CteQueryWrapper;
import com.blazebit.persistence.spi.QueryTransformer;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
@ServiceProvider(QueryTransformer.class)
public class HibernateQueryTransformer implements QueryTransformer {

    @Override
    public <X> TypedQuery<X> transformQuery(TypedQuery<?> query, ObjectBuilder<X> objectBuilder) {
        if (query instanceof CteQueryWrapper) {
            return new ObjectBuilderJPAQueryAdapter<X>(query, objectBuilder);
        }
        
        return new ObjectBuilderJPAQueryAdapter<X>(query, objectBuilder);
    }

}
