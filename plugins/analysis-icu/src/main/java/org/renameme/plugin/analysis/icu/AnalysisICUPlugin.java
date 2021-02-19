/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.renameme.plugin.analysis.icu;

import static java.util.Collections.singletonMap;

import org.apache.lucene.analysis.Analyzer;
import org.renameme.common.io.stream.NamedWriteableRegistry;
import org.renameme.index.analysis.AnalyzerProvider;
import org.renameme.index.analysis.CharFilterFactory;
import org.renameme.index.analysis.IcuAnalyzerProvider;
import org.renameme.index.analysis.IcuCollationTokenFilterFactory;
import org.renameme.index.analysis.IcuFoldingTokenFilterFactory;
import org.renameme.index.analysis.IcuNormalizerCharFilterFactory;
import org.renameme.index.analysis.IcuNormalizerTokenFilterFactory;
import org.renameme.index.analysis.IcuTokenizerFactory;
import org.renameme.index.analysis.IcuTransformTokenFilterFactory;
import org.renameme.index.analysis.TokenFilterFactory;
import org.renameme.index.analysis.TokenizerFactory;
import org.renameme.index.mapper.ICUCollationKeywordFieldMapper;
import org.renameme.index.mapper.Mapper;
import org.renameme.indices.analysis.AnalysisModule.AnalysisProvider;
import org.renameme.plugins.AnalysisPlugin;
import org.renameme.plugins.MapperPlugin;
import org.renameme.plugins.Plugin;
import org.renameme.search.DocValueFormat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisICUPlugin extends Plugin implements AnalysisPlugin, MapperPlugin {
    @Override
    public Map<String, AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return singletonMap("icu_normalizer", IcuNormalizerCharFilterFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("icu_normalizer", IcuNormalizerTokenFilterFactory::new);
        extra.put("icu_folding", IcuFoldingTokenFilterFactory::new);
        extra.put("icu_collation", IcuCollationTokenFilterFactory::new);
        extra.put("icu_transform", IcuTransformTokenFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("icu_analyzer", IcuAnalyzerProvider::new);
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("icu_tokenizer", IcuTokenizerFactory::new);
    }

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        return Collections.singletonMap(ICUCollationKeywordFieldMapper.CONTENT_TYPE, new ICUCollationKeywordFieldMapper.TypeParser());
    }

    @Override
    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return Collections.singletonList(
            new NamedWriteableRegistry.Entry(
                DocValueFormat.class,
                ICUCollationKeywordFieldMapper.CollationFieldType.COLLATE_FORMAT.getWriteableName(),
                in -> ICUCollationKeywordFieldMapper.CollationFieldType.COLLATE_FORMAT
            )
        );
    }
}
