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

package com.blazebit.persistence.view.materialization.api;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class ChangeEvent {
    private final List<String> qualifiedTableName;
    private final ChangeEventKind kind;
    private final Serializable[] before;
    private final Serializable[] after;

    public ChangeEvent(List<String> qualifiedTableName, ChangeEventKind kind, Serializable[] before, Serializable[] after) {
        this.qualifiedTableName = qualifiedTableName;
        this.kind = kind;
        this.before = before;
        this.after = after;
    }

    public List<String> getQualifiedTableName() {
        return qualifiedTableName;
    }

    public ChangeEventKind getKind() {
        return kind;
    }

    public Serializable[] getBefore() {
        return before;
    }

    public Serializable[] getAfter() {
        return after;
    }
}
