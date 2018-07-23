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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.painless.PainlessPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class EsServer {

    public static final String HTTP_PORTS = "9201";
    public static final String TRANSPORT_PORTS = "9200";

    private static class MyNode extends Node {
        public MyNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
        }
    }

    private final Node node;
    private String coordinates;

    public EsServer(String path) {
        this(path, HTTP_PORTS, TRANSPORT_PORTS);
    }

    public EsServer(String path, String httpRange, String transportRange) {
        node = new MyNode(
                Settings.builder()
                        .put("cluster.name", "test")
                        .put("path.home", path)
                        .put("http.type", "netty4")
                        .put("http.enabled", "true")
                        .put("http.port", httpRange)
                        .put("transport.type", "netty4")
                        .put("transport.tcp.port", transportRange)
                        .put("discovery.type", "zen")
                        .build(),
                Arrays.asList(Netty4Plugin.class, PainlessPlugin.class));
    }

    public void start() {
        try {
            node.start();
            // find out port
            String localNodeId = node.client().admin().cluster().prepareState().get().getState().getNodes().getLocalNodeId();
            TransportAddress transportAddress = node.client().admin().cluster().prepareNodesInfo(localNodeId).get().getNodes().iterator().next().getHttp().address().publishAddress();
            coordinates = "{'" + transportAddress.getAddress() + "': " + transportAddress.getPort() + "}";
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void stop() {
        if (node != null) {
            try {
                node.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Node getNode() {
        return node;
    }

    public String getCoordinates() {
        return coordinates;
    }
}
