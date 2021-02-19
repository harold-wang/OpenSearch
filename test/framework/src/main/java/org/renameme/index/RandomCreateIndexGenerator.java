/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.renameme.index;

import org.renameme.action.admin.indices.alias.Alias;
import org.renameme.action.admin.indices.create.CreateIndexRequest;
import org.renameme.common.settings.Settings;
import org.renameme.common.xcontent.XContentBuilder;
import org.renameme.common.xcontent.XContentFactory;
import org.renameme.common.xcontent.XContentType;
import org.renameme.test.ESTestCase;

import java.io.IOException;

import static org.renameme.cluster.metadata.IndexMetadata.SETTING_NUMBER_OF_REPLICAS;
import static org.renameme.cluster.metadata.IndexMetadata.SETTING_NUMBER_OF_SHARDS;
import static org.renameme.test.ESTestCase.randomAlphaOfLength;
import static org.renameme.test.ESTestCase.randomBoolean;
import static org.renameme.test.ESTestCase.randomFrom;
import static org.renameme.test.ESTestCase.randomIntBetween;

public final class RandomCreateIndexGenerator {

    private RandomCreateIndexGenerator() {}

    /**
     * Returns a random {@link CreateIndexRequest}.
     *
     * Randomizes the index name, the aliases, mappings and settings associated with the
     * index. If present, the mapping definition will be nested under a type name.
     */
    public static CreateIndexRequest randomCreateIndexRequest() throws IOException {
        String index = ESTestCase.randomAlphaOfLength(5);
        CreateIndexRequest request = new CreateIndexRequest(index);
        randomAliases(request);
        if (ESTestCase.randomBoolean()) {
            String type = ESTestCase.randomAlphaOfLength(5);
            request.mapping(type, randomMapping(type));
        }
        if (ESTestCase.randomBoolean()) {
            request.settings(randomIndexSettings());
        }
        return request;
    }

    /**
     * Returns a {@link Settings} instance which include random values for
     * {@link org.renameme.cluster.metadata.IndexMetadata#SETTING_NUMBER_OF_SHARDS} and
     * {@link org.renameme.cluster.metadata.IndexMetadata#SETTING_NUMBER_OF_REPLICAS}
     */
    public static Settings randomIndexSettings() {
        Settings.Builder builder = Settings.builder();

        if (ESTestCase.randomBoolean()) {
            int numberOfShards = ESTestCase.randomIntBetween(1, 10);
            builder.put(SETTING_NUMBER_OF_SHARDS, numberOfShards);
        }

        if (ESTestCase.randomBoolean()) {
            int numberOfReplicas = ESTestCase.randomIntBetween(1, 10);
            builder.put(SETTING_NUMBER_OF_REPLICAS, numberOfReplicas);
        }

        return builder.build();
    }

    /**
     * Creates a random mapping, with the mapping definition nested
     * under the given type name.
     */
    public static XContentBuilder randomMapping(String type) throws IOException {
        XContentBuilder builder = XContentFactory.contentBuilder(randomFrom(XContentType.values()));
        builder.startObject().startObject(type);

        randomMappingFields(builder, true);

        builder.endObject().endObject();
        return builder;
    }

    /**
     * Adds random mapping fields to the provided {@link XContentBuilder}
     */
    public static void randomMappingFields(XContentBuilder builder, boolean allowObjectField) throws IOException {
        builder.startObject("properties");

        int fieldsNo = ESTestCase.randomIntBetween(0, 5);
        for (int i = 0; i < fieldsNo; i++) {
            builder.startObject(ESTestCase.randomAlphaOfLength(5));

            if (allowObjectField && ESTestCase.randomBoolean()) {
                randomMappingFields(builder, false);
            } else {
                builder.field("type", "text");
            }

            builder.endObject();
        }

        builder.endObject();
    }

    /**
     * Sets random aliases to the provided {@link CreateIndexRequest}
     */
    public static void randomAliases(CreateIndexRequest request) {
        int aliasesNo = ESTestCase.randomIntBetween(0, 2);
        for (int i = 0; i < aliasesNo; i++) {
            request.alias(randomAlias());
        }
    }

    public static Alias randomAlias() {
        Alias alias = new Alias(ESTestCase.randomAlphaOfLength(5));

        if (ESTestCase.randomBoolean()) {
            if (ESTestCase.randomBoolean()) {
                alias.routing(ESTestCase.randomAlphaOfLength(5));
            } else {
                if (ESTestCase.randomBoolean()) {
                    alias.indexRouting(ESTestCase.randomAlphaOfLength(5));
                }
                if (ESTestCase.randomBoolean()) {
                    alias.searchRouting(ESTestCase.randomAlphaOfLength(5));
                }
            }
        }

        if (ESTestCase.randomBoolean()) {
            alias.filter("{\"term\":{\"year\":2016}}");
        }

        if (ESTestCase.randomBoolean()) {
            alias.writeIndex(ESTestCase.randomBoolean());
        }

        return alias;
    }
}
