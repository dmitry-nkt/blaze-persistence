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

package com.blazebit.persistence.view.materialization.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
@Entity
@Table(name = "DOCUMENTSIMPLEWITHOWNERVIEW_MAT")
public class DocumentSimpleWithOwnerViewEntity {

    @Id
    private Long id;
    @Basic(optional = false)
    private String name;
    @Basic(optional = false)
    private Long owner_id;
    @Basic(optional = false)
    @Column(name = "name0")
    private String owner_name;

    public DocumentSimpleWithOwnerViewEntity() {
    }

    public DocumentSimpleWithOwnerViewEntity(Long id, String name, Long owner_id, String owner_name) {
        this.id = id;
        this.name = name;
        this.owner_id = owner_id;
        this.owner_name = owner_name;
    }
}
