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

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.ConnectionFactory;

import java.sql.SQLException;

public class CalciteEntityViewDataSource extends BasicDataSource {

    private final CalciteEntityViewDriverConnectionFactory factory;

    public CalciteEntityViewDataSource(CalciteEntityViewDriverConnectionFactory factory) {
        this.factory = factory;
        setDefaultReadOnly(true);
    }

    @Override
    protected ConnectionFactory createConnectionFactory() throws SQLException {
        return factory;
    }
}
