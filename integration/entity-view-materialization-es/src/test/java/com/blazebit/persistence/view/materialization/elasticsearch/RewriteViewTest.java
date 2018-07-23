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

package com.blazebit.persistence.view.materialization.elasticsearch;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.testsuite.AbstractCoreTest;
import com.blazebit.persistence.testsuite.base.jpa.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.jpa.category.NoEclipselink;
import com.blazebit.persistence.testsuite.base.jpa.category.NoOpenJPA;
import com.blazebit.persistence.testsuite.entity.Document;
import com.blazebit.persistence.testsuite.entity.Person;
import com.blazebit.persistence.testsuite.tx.TxVoidWork;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.blazebit.persistence.view.EntityViews;
import com.blazebit.persistence.view.materialization.api.MaterializationContext;
import com.blazebit.persistence.view.materialization.api.spi.DataStoreFactory;
import com.blazebit.persistence.view.materialization.elasticsearch.model.DocumentSimpleView;
import com.blazebit.persistence.view.materialization.impl.CalciteMaterializationContextImpl;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
public class RewriteViewTest extends AbstractCoreTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private EsServer esServer;
    protected EntityViewManager evm;
    protected MaterializationContext materializationContext;

    public void initEvm(Class<?>... classes) {
        EntityViewConfiguration cfg = EntityViews.createDefaultConfiguration();
        for (Class<?> c : classes) {
            cfg.addEntityView(c);
        }
        evm = cfg.createEntityViewManager(cbf);
        materializationContext = CalciteMaterializationContextImpl.create(evm, cbf, emf, Collections.<String, DataStoreFactory>singletonMap(
                "es", new ElasticSearchDataStoreFactory(esServer.getCoordinates(), "{'cluster.name': 'test'}", "test")
        ));
    }

    private Document doc1;
    private Document doc2;

    @Before
    public void setUp() {
        cleanDatabase();
        transactional(new TxVoidWork() {
            @Override
            public void work(EntityManager em) {
                doc1 = new Document("doc1");
                doc2 = new Document("doc2");

                Person o1 = new Person("pers1");
                Person o2 = new Person("pers2");
                o1.getLocalized().put(1, "localized1");
                o2.getLocalized().put(1, "localized2");
                o1.setPartnerDocument(doc1);
                o2.setPartnerDocument(doc2);

                doc1.setAge(10);
                doc1.setOwner(o1);
                doc2.setAge(20);
                doc2.setOwner(o2);

                em.persist(o1);
                em.persist(o2);

                em.persist(doc1);
                em.persist(doc2);
            }
        });

        doc1 = em.find(Document.class, doc1.getId());
        doc2 = em.find(Document.class, doc2.getId());

        try {
            esServer = new EsServer(folder.newFolder("elasticsearch-data").getAbsolutePath());
            esServer.start();
            esServer.getNode().client().admin().indices().prepareCreate("test").get();
            esServer.getNode().client().admin().indices().preparePutMapping("test")
                    .setType("documentsimpleview_mat")
                    .setSource(
                    "{\"documentsimpleview_mat\": {\"properties\": {" +
                            "\"ID\":{\"type\":\"long\", \"store\": true}," +
                            "\"NAME\":{\"type\":\"text\", \"store\": true}" +
                            "}}}",
                            XContentType.JSON
                    )
                    .get();
            Client client = esServer.getNode().client();
            client.prepareIndex("test", "documentsimpleview_mat", "1")
                    .setSource(
                            XContentFactory.jsonBuilder()
                                .startObject()
                                    .field("ID", 1L)
                                    .field("NAME", "test1")
                                .endObject()
                    )
                    .get();
            client.prepareIndex("test", "documentsimpleview_mat", "2")
                    .setSource(
                            XContentFactory.jsonBuilder()
                                    .startObject()
                                    .field("ID", 2L)
                                    .field("NAME", "test2")
                                    .endObject()
                    )
                    .get();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @After
    public void stopNode() {
        esServer.stop();
    }

    @Test
    @Category({ NoEclipselink.class, NoDatanucleus.class, NoOpenJPA.class })
    public void testSimpleView() {
        initEvm(DocumentSimpleView.class);

        replaceEntityManager(materializationContext.createEntityManager());
        CriteriaBuilder<Document> criteria = cbf.create(em, Document.class, "d")
                .orderByAsc("id");
        EntityViewSetting<DocumentSimpleView, CriteriaBuilder<DocumentSimpleView>> setting;
        setting = EntityViewSetting.create(DocumentSimpleView.class);
        List<DocumentSimpleView> results = evm.applySetting(setting, criteria).getResultList();

        assertEquals(2, results.size());
        assertEquals("test1", results.get(0).getName());
        assertEquals("test2", results.get(1).getName());
    }

}
