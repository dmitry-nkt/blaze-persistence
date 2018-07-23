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

import javax.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public class ConnectionClearingEntityManagerHandler implements InvocationHandler {

    private final Object em;
    private final Connection connection;

    public ConnectionClearingEntityManagerHandler(Object em, Connection connection) {
        this.em = em;
        this.connection = connection;
    }

    public static EntityManager wrap(EntityManager em, Connection connection) {
        return (EntityManager) Proxy.newProxyInstance(
                em.getClass().getClassLoader(),
                new Class[]{ EntityManager.class },
                new ConnectionClearingEntityManagerHandler(em, connection)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(em, args);
        } finally {
            if ("close".equals(method.getName())) {
                connection.close();
                ThreadLocalDataSource.CONNECTION.remove();
            }
        }
    }
}
