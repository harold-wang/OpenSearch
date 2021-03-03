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

package org.renameme.index.analysis;

import org.renameme.common.settings.Settings;
import org.renameme.index.AbstractIndexComponent;
import org.renameme.index.IndexSettings;

public abstract class AbstractTokenizerFactory extends AbstractIndexComponent implements TokenizerFactory {
    protected final org.apache.lucene.util.Version version;
    private final String name;

    public AbstractTokenizerFactory(IndexSettings indexSettings, Settings settings, String name) {
        super(indexSettings);
        this.version = Analysis.parseAnalysisVersion(this.indexSettings.getSettings(), settings, logger);
        this.name = name;
    }

    public final org.apache.lucene.util.Version version() {
        return version;
    }

    @Override
    public String name() {
        return name;
    }
}
