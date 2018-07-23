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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

public class TxProtectedConnectionHandler implements InvocationHandler {

    private final Object connection;
    private final Connection realConnection;

    public TxProtectedConnectionHandler(Object connection, Connection realConnection) {
        this.connection = connection;
        this.realConnection = realConnection;
    }

    public static Connection wrap(Connection connection, Connection realConnection) {
        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class[]{ Connection.class },
                new TxProtectedConnectionHandler(connection, realConnection)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Statement.class.isAssignableFrom(method.getReturnType())
                || DatabaseMetaData.class.isAssignableFrom(method.getReturnType())) {
            Object delegate = method.invoke(connection, args);
            return Proxy.newProxyInstance(
                    connection.getClass().getClassLoader(),
                    new Class[]{ method.getReturnType() },
                    new ConnectionReturningHandler(proxy, delegate)
            );
        }

        switch (method.getName()) {
            case "commit":
            case "rollback":
            case "setSavepoint":
            case "releaseSavepoint":
                return method.invoke(realConnection, args);
        }

        return method.invoke(connection, args);
    }
}
