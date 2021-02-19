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

package org.renameme.plugin.analysis.kuromoji;

import org.apache.lucene.analysis.Analyzer;
import org.renameme.index.analysis.AnalyzerProvider;
import org.renameme.index.analysis.CharFilterFactory;
import org.renameme.index.analysis.JapaneseStopTokenFilterFactory;
import org.renameme.index.analysis.KuromojiAnalyzerProvider;
import org.renameme.index.analysis.KuromojiBaseFormFilterFactory;
import org.renameme.index.analysis.KuromojiIterationMarkCharFilterFactory;
import org.renameme.index.analysis.KuromojiKatakanaStemmerFactory;
import org.renameme.index.analysis.KuromojiNumberFilterFactory;
import org.renameme.index.analysis.KuromojiPartOfSpeechFilterFactory;
import org.renameme.index.analysis.KuromojiReadingFormFilterFactory;
import org.renameme.index.analysis.KuromojiTokenizerFactory;
import org.renameme.index.analysis.TokenFilterFactory;
import org.renameme.index.analysis.TokenizerFactory;
import org.renameme.indices.analysis.AnalysisModule.AnalysisProvider;
import org.renameme.plugins.AnalysisPlugin;
import org.renameme.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class AnalysisKuromojiPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return singletonMap("kuromoji_iteration_mark", KuromojiIterationMarkCharFilterFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("kuromoji_baseform", KuromojiBaseFormFilterFactory::new);
        extra.put("kuromoji_part_of_speech", KuromojiPartOfSpeechFilterFactory::new);
        extra.put("kuromoji_readingform", KuromojiReadingFormFilterFactory::new);
        extra.put("kuromoji_stemmer", KuromojiKatakanaStemmerFactory::new);
        extra.put("ja_stop", JapaneseStopTokenFilterFactory::new);
        extra.put("kuromoji_number", KuromojiNumberFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("kuromoji_tokenizer", KuromojiTokenizerFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("kuromoji", KuromojiAnalyzerProvider::new);
    }
}
