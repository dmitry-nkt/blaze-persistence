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

package com.blazebit.persistence.spi;

import javax.persistence.metamodel.Type;

/**
 * This is a wrapper around the JPA {@link Type} that allows additionally efficient access to properties of the metamodel.
 *
 * @param <X> The Java type represented by this extended type
 * @author Christian Beikov
 * @since 1.3.0
 */
public interface ExtendedType<X> {

    /**
     * Returns the underlying JPA type.
     *
     * @return The JPA type
     */
    public Type<X> getType();
}

