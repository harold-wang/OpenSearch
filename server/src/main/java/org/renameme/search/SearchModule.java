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

package org.renameme.search;

import org.apache.lucene.search.BooleanQuery;
import org.renameme.common.NamedRegistry;
import org.renameme.common.ParseField;
import org.renameme.common.geo.GeoShapeType;
import org.renameme.common.geo.ShapesAvailability;
import org.renameme.common.io.stream.NamedWriteableRegistry;
import org.renameme.common.io.stream.NamedWriteableRegistry.Entry;
import org.renameme.common.io.stream.Writeable;
import org.renameme.common.settings.Setting;
import org.renameme.common.settings.Settings;
import org.renameme.common.xcontent.NamedXContentRegistry;
import org.renameme.common.xcontent.ParseFieldRegistry;
import org.renameme.common.xcontent.XContentParser;
import org.renameme.index.query.BoolQueryBuilder;
import org.renameme.index.query.BoostingQueryBuilder;
import org.renameme.index.query.CommonTermsQueryBuilder;
import org.renameme.index.query.ConstantScoreQueryBuilder;
import org.renameme.index.query.DisMaxQueryBuilder;
import org.renameme.index.query.DistanceFeatureQueryBuilder;
import org.renameme.index.query.ExistsQueryBuilder;
import org.renameme.index.query.FieldMaskingSpanQueryBuilder;
import org.renameme.index.query.FuzzyQueryBuilder;
import org.renameme.index.query.GeoBoundingBoxQueryBuilder;
import org.renameme.index.query.GeoDistanceQueryBuilder;
import org.renameme.index.query.GeoPolygonQueryBuilder;
import org.renameme.index.query.GeoShapeQueryBuilder;
import org.renameme.index.query.IdsQueryBuilder;
import org.renameme.index.query.IntervalQueryBuilder;
import org.renameme.index.query.IntervalsSourceProvider;
import org.renameme.index.query.MatchAllQueryBuilder;
import org.renameme.index.query.MatchBoolPrefixQueryBuilder;
import org.renameme.index.query.MatchNoneQueryBuilder;
import org.renameme.index.query.MatchPhrasePrefixQueryBuilder;
import org.renameme.index.query.MatchPhraseQueryBuilder;
import org.renameme.index.query.MatchQueryBuilder;
import org.renameme.index.query.MoreLikeThisQueryBuilder;
import org.renameme.index.query.MultiMatchQueryBuilder;
import org.renameme.index.query.NestedQueryBuilder;
import org.renameme.index.query.PrefixQueryBuilder;
import org.renameme.index.query.QueryBuilder;
import org.renameme.index.query.QueryStringQueryBuilder;
import org.renameme.index.query.RangeQueryBuilder;
import org.renameme.index.query.RegexpQueryBuilder;
import org.renameme.index.query.ScriptQueryBuilder;
import org.renameme.index.query.SimpleQueryStringBuilder;
import org.renameme.index.query.SpanContainingQueryBuilder;
import org.renameme.index.query.SpanFirstQueryBuilder;
import org.renameme.index.query.SpanMultiTermQueryBuilder;
import org.renameme.index.query.SpanNearQueryBuilder;
import org.renameme.index.query.SpanNearQueryBuilder.SpanGapQueryBuilder;
import org.renameme.index.query.SpanNotQueryBuilder;
import org.renameme.index.query.SpanOrQueryBuilder;
import org.renameme.index.query.SpanTermQueryBuilder;
import org.renameme.index.query.SpanWithinQueryBuilder;
import org.renameme.index.query.TermQueryBuilder;
import org.renameme.index.query.TermsQueryBuilder;
import org.renameme.index.query.TermsSetQueryBuilder;
import org.renameme.index.query.TypeQueryBuilder;
import org.renameme.index.query.WildcardQueryBuilder;
import org.renameme.index.query.WrapperQueryBuilder;
import org.renameme.index.query.functionscore.ExponentialDecayFunctionBuilder;
import org.renameme.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.renameme.index.query.functionscore.FunctionScoreQueryBuilder;
import org.renameme.index.query.functionscore.GaussDecayFunctionBuilder;
import org.renameme.index.query.functionscore.LinearDecayFunctionBuilder;
import org.renameme.index.query.functionscore.RandomScoreFunctionBuilder;
import org.renameme.index.query.functionscore.ScoreFunctionBuilder;
import org.renameme.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.renameme.index.query.functionscore.ScriptScoreQueryBuilder;
import org.renameme.index.query.functionscore.WeightBuilder;
import org.renameme.plugins.SearchPlugin;
import org.renameme.plugins.SearchPlugin.AggregationSpec;
import org.renameme.plugins.SearchPlugin.FetchPhaseConstructionContext;
import org.renameme.plugins.SearchPlugin.PipelineAggregationSpec;
import org.renameme.plugins.SearchPlugin.QuerySpec;
import org.renameme.plugins.SearchPlugin.RescorerSpec;
import org.renameme.plugins.SearchPlugin.ScoreFunctionSpec;
import org.renameme.plugins.SearchPlugin.SearchExtSpec;
import org.renameme.plugins.SearchPlugin.SearchExtensionSpec;
import org.renameme.plugins.SearchPlugin.SignificanceHeuristicSpec;
import org.renameme.plugins.SearchPlugin.SuggesterSpec;
import org.renameme.search.aggregations.AggregationBuilder;
import org.renameme.search.aggregations.BaseAggregationBuilder;
import org.renameme.search.aggregations.InternalAggregation;
import org.renameme.search.aggregations.PipelineAggregationBuilder;
import org.renameme.search.aggregations.bucket.adjacency.AdjacencyMatrixAggregationBuilder;
import org.renameme.search.aggregations.bucket.adjacency.InternalAdjacencyMatrix;
import org.renameme.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.renameme.search.aggregations.bucket.composite.InternalComposite;
import org.renameme.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.renameme.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.renameme.search.aggregations.bucket.filter.InternalFilter;
import org.renameme.search.aggregations.bucket.filter.InternalFilters;
import org.renameme.search.aggregations.bucket.geogrid.GeoHashGridAggregationBuilder;
import org.renameme.search.aggregations.bucket.geogrid.GeoTileGridAggregationBuilder;
import org.renameme.search.aggregations.bucket.geogrid.InternalGeoHashGrid;
import org.renameme.search.aggregations.bucket.geogrid.InternalGeoTileGrid;
import org.renameme.search.aggregations.bucket.global.GlobalAggregationBuilder;
import org.renameme.search.aggregations.bucket.global.InternalGlobal;
import org.renameme.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import org.renameme.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.renameme.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.renameme.search.aggregations.bucket.histogram.InternalAutoDateHistogram;
import org.renameme.search.aggregations.bucket.histogram.InternalDateHistogram;
import org.renameme.search.aggregations.bucket.histogram.InternalHistogram;
import org.renameme.search.aggregations.bucket.histogram.InternalVariableWidthHistogram;
import org.renameme.search.aggregations.bucket.histogram.VariableWidthHistogramAggregationBuilder;
import org.renameme.search.aggregations.bucket.missing.InternalMissing;
import org.renameme.search.aggregations.bucket.missing.MissingAggregationBuilder;
import org.renameme.search.aggregations.bucket.nested.InternalNested;
import org.renameme.search.aggregations.bucket.nested.InternalReverseNested;
import org.renameme.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.renameme.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import org.renameme.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.renameme.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import org.renameme.search.aggregations.bucket.range.InternalBinaryRange;
import org.renameme.search.aggregations.bucket.range.InternalDateRange;
import org.renameme.search.aggregations.bucket.range.InternalGeoDistance;
import org.renameme.search.aggregations.bucket.range.InternalRange;
import org.renameme.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import org.renameme.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.renameme.search.aggregations.bucket.sampler.DiversifiedAggregationBuilder;
import org.renameme.search.aggregations.bucket.sampler.InternalSampler;
import org.renameme.search.aggregations.bucket.sampler.SamplerAggregationBuilder;
import org.renameme.search.aggregations.bucket.sampler.UnmappedSampler;
import org.renameme.search.aggregations.bucket.terms.DoubleTerms;
import org.renameme.search.aggregations.bucket.terms.LongRareTerms;
import org.renameme.search.aggregations.bucket.terms.LongTerms;
import org.renameme.search.aggregations.bucket.terms.RareTermsAggregationBuilder;
import org.renameme.search.aggregations.bucket.terms.SignificantLongTerms;
import org.renameme.search.aggregations.bucket.terms.SignificantStringTerms;
import org.renameme.search.aggregations.bucket.terms.SignificantTermsAggregationBuilder;
import org.renameme.search.aggregations.bucket.terms.SignificantTextAggregationBuilder;
import org.renameme.search.aggregations.bucket.terms.StringRareTerms;
import org.renameme.search.aggregations.bucket.terms.StringTerms;
import org.renameme.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.renameme.search.aggregations.bucket.terms.UnmappedRareTerms;
import org.renameme.search.aggregations.bucket.terms.UnmappedSignificantTerms;
import org.renameme.search.aggregations.bucket.terms.UnmappedTerms;
import org.renameme.search.aggregations.bucket.terms.heuristic.ChiSquare;
import org.renameme.search.aggregations.bucket.terms.heuristic.GND;
import org.renameme.search.aggregations.bucket.terms.heuristic.JLHScore;
import org.renameme.search.aggregations.bucket.terms.heuristic.MutualInformation;
import org.renameme.search.aggregations.bucket.terms.heuristic.PercentageScore;
import org.renameme.search.aggregations.bucket.terms.heuristic.ScriptHeuristic;
import org.renameme.search.aggregations.bucket.terms.heuristic.SignificanceHeuristic;
import org.renameme.search.aggregations.metrics.AvgAggregationBuilder;
import org.renameme.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.renameme.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import org.renameme.search.aggregations.metrics.GeoBoundsAggregationBuilder;
import org.renameme.search.aggregations.metrics.GeoCentroidAggregationBuilder;
import org.renameme.search.aggregations.metrics.InternalAvg;
import org.renameme.search.aggregations.metrics.InternalCardinality;
import org.renameme.search.aggregations.metrics.InternalExtendedStats;
import org.renameme.search.aggregations.metrics.InternalGeoBounds;
import org.renameme.search.aggregations.metrics.InternalGeoCentroid;
import org.renameme.search.aggregations.metrics.InternalHDRPercentileRanks;
import org.renameme.search.aggregations.metrics.InternalHDRPercentiles;
import org.renameme.search.aggregations.metrics.InternalMax;
import org.renameme.search.aggregations.metrics.InternalMedianAbsoluteDeviation;
import org.renameme.search.aggregations.metrics.InternalMin;
import org.renameme.search.aggregations.metrics.InternalScriptedMetric;
import org.renameme.search.aggregations.metrics.InternalStats;
import org.renameme.search.aggregations.metrics.InternalSum;
import org.renameme.search.aggregations.metrics.InternalTDigestPercentileRanks;
import org.renameme.search.aggregations.metrics.InternalTDigestPercentiles;
import org.renameme.search.aggregations.metrics.InternalTopHits;
import org.renameme.search.aggregations.metrics.InternalValueCount;
import org.renameme.search.aggregations.metrics.InternalWeightedAvg;
import org.renameme.search.aggregations.metrics.MaxAggregationBuilder;
import org.renameme.search.aggregations.metrics.MedianAbsoluteDeviationAggregationBuilder;
import org.renameme.search.aggregations.metrics.MinAggregationBuilder;
import org.renameme.search.aggregations.metrics.PercentileRanksAggregationBuilder;
import org.renameme.search.aggregations.metrics.PercentilesAggregationBuilder;
import org.renameme.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import org.renameme.search.aggregations.metrics.StatsAggregationBuilder;
import org.renameme.search.aggregations.metrics.SumAggregationBuilder;
import org.renameme.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.renameme.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.renameme.search.aggregations.metrics.WeightedAvgAggregationBuilder;
import org.renameme.search.aggregations.pipeline.AvgBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.AvgBucketPipelineAggregator;
import org.renameme.search.aggregations.pipeline.BucketScriptPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.BucketScriptPipelineAggregator;
import org.renameme.search.aggregations.pipeline.BucketSelectorPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.BucketSelectorPipelineAggregator;
import org.renameme.search.aggregations.pipeline.BucketSortPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.BucketSortPipelineAggregator;
import org.renameme.search.aggregations.pipeline.CumulativeSumPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.CumulativeSumPipelineAggregator;
import org.renameme.search.aggregations.pipeline.DerivativePipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.DerivativePipelineAggregator;
import org.renameme.search.aggregations.pipeline.EwmaModel;
import org.renameme.search.aggregations.pipeline.ExtendedStatsBucketParser;
import org.renameme.search.aggregations.pipeline.ExtendedStatsBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.ExtendedStatsBucketPipelineAggregator;
import org.renameme.search.aggregations.pipeline.HoltLinearModel;
import org.renameme.search.aggregations.pipeline.HoltWintersModel;
import org.renameme.search.aggregations.pipeline.InternalBucketMetricValue;
import org.renameme.search.aggregations.pipeline.InternalDerivative;
import org.renameme.search.aggregations.pipeline.InternalExtendedStatsBucket;
import org.renameme.search.aggregations.pipeline.InternalPercentilesBucket;
import org.renameme.search.aggregations.pipeline.InternalSimpleValue;
import org.renameme.search.aggregations.pipeline.InternalStatsBucket;
import org.renameme.search.aggregations.pipeline.LinearModel;
import org.renameme.search.aggregations.pipeline.MaxBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.MaxBucketPipelineAggregator;
import org.renameme.search.aggregations.pipeline.MinBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.MinBucketPipelineAggregator;
import org.renameme.search.aggregations.pipeline.MovAvgModel;
import org.renameme.search.aggregations.pipeline.MovAvgPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.MovAvgPipelineAggregator;
import org.renameme.search.aggregations.pipeline.MovFnPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.MovFnPipelineAggregator;
import org.renameme.search.aggregations.pipeline.PercentilesBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.PercentilesBucketPipelineAggregator;
import org.renameme.search.aggregations.pipeline.PipelineAggregator;
import org.renameme.search.aggregations.pipeline.SerialDiffPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.SerialDiffPipelineAggregator;
import org.renameme.search.aggregations.pipeline.SimpleModel;
import org.renameme.search.aggregations.pipeline.StatsBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.StatsBucketPipelineAggregator;
import org.renameme.search.aggregations.pipeline.SumBucketPipelineAggregationBuilder;
import org.renameme.search.aggregations.pipeline.SumBucketPipelineAggregator;
import org.renameme.search.aggregations.support.ValuesSourceRegistry;
import org.renameme.search.fetch.FetchPhase;
import org.renameme.search.fetch.FetchSubPhase;
import org.renameme.search.fetch.subphase.FetchDocValuesPhase;
import org.renameme.search.fetch.subphase.ExplainPhase;
import org.renameme.search.fetch.subphase.FetchFieldsPhase;
import org.renameme.search.fetch.subphase.FetchScorePhase;
import org.renameme.search.fetch.subphase.FetchSourcePhase;
import org.renameme.search.fetch.subphase.MatchedQueriesPhase;
import org.renameme.search.fetch.subphase.FetchScorePhase;
import org.renameme.search.fetch.subphase.ScriptFieldsPhase;
import org.renameme.search.fetch.subphase.SeqNoPrimaryTermPhase;
import org.renameme.search.fetch.subphase.FetchVersionPhase;
import org.renameme.search.fetch.subphase.highlight.FastVectorHighlighter;
import org.renameme.search.fetch.subphase.highlight.HighlightPhase;
import org.renameme.search.fetch.subphase.highlight.Highlighter;
import org.renameme.search.fetch.subphase.highlight.PlainHighlighter;
import org.renameme.search.fetch.subphase.highlight.UnifiedHighlighter;
import org.renameme.search.rescore.QueryRescorerBuilder;
import org.renameme.search.rescore.RescorerBuilder;
import org.renameme.search.sort.FieldSortBuilder;
import org.renameme.search.sort.GeoDistanceSortBuilder;
import org.renameme.search.sort.ScoreSortBuilder;
import org.renameme.search.sort.ScriptSortBuilder;
import org.renameme.search.sort.SortBuilder;
import org.renameme.search.sort.SortValue;
import org.renameme.search.suggest.Suggest;
import org.renameme.search.suggest.SuggestionBuilder;
import org.renameme.search.suggest.completion.CompletionSuggestion;
import org.renameme.search.suggest.completion.CompletionSuggestionBuilder;
import org.renameme.search.suggest.phrase.Laplace;
import org.renameme.search.suggest.phrase.LinearInterpolation;
import org.renameme.search.suggest.phrase.PhraseSuggestion;
import org.renameme.search.suggest.phrase.PhraseSuggestionBuilder;
import org.renameme.search.suggest.phrase.SmoothingModel;
import org.renameme.search.suggest.phrase.StupidBackoff;
import org.renameme.search.suggest.term.TermSuggestion;
import org.renameme.search.suggest.term.TermSuggestionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static org.renameme.index.query.CommonTermsQueryBuilder.COMMON_TERMS_QUERY_DEPRECATION_MSG;

/**
 * Sets up things that can be done at search time like queries, aggregations, and suggesters.
 */
public class SearchModule {
    public static final Setting<Integer> INDICES_MAX_CLAUSE_COUNT_SETTING = Setting.intSetting("indices.query.bool.max_clause_count",
            1024, 1, Integer.MAX_VALUE, Setting.Property.NodeScope);

    private final boolean transportClient;
    private final Map<String, Highlighter> highlighters;
    private final ParseFieldRegistry<MovAvgModel.AbstractModelParser> movingAverageModelParserRegistry = new ParseFieldRegistry<>(
            "moving_avg_model");

    private final List<FetchSubPhase> fetchSubPhases = new ArrayList<>();

    private final Settings settings;
    private final List<NamedWriteableRegistry.Entry> namedWriteables = new ArrayList<>();
    private final List<NamedXContentRegistry.Entry> namedXContents = new ArrayList<>();
    private final ValuesSourceRegistry valuesSourceRegistry;

    /**
     * Constructs a new SearchModule object
     *
     * NOTE: This constructor should not be called in production unless an accurate {@link Settings} object is provided.
     *       When constructed, a static flag is set in Lucene {@link BooleanQuery#setMaxClauseCount} according to the settings.
     * @param settings Current settings
     * @param transportClient Is this being constructed in the TransportClient or not
     * @param plugins List of included {@link SearchPlugin} objects.
     */
    public SearchModule(Settings settings, boolean transportClient, List<SearchPlugin> plugins) {
        this.settings = settings;
        this.transportClient = transportClient;
        registerSuggesters(plugins);
        highlighters = setupHighlighters(settings, plugins);
        registerScoreFunctions(plugins);
        registerQueryParsers(plugins);
        registerRescorers(plugins);
        registerSorts();
        registerValueFormats();
        registerSignificanceHeuristics(plugins);
        this.valuesSourceRegistry = registerAggregations(plugins);
        registerMovingAverageModels(plugins);
        registerPipelineAggregations(plugins);
        registerFetchSubPhases(plugins);
        registerSearchExts(plugins);
        registerShapes();
        registerIntervalsSourceProviders();
        namedWriteables.addAll(SortValue.namedWriteables());
    }

    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return namedWriteables;
    }

    public List<NamedXContentRegistry.Entry> getNamedXContents() {
        return namedXContents;
    }

    public ValuesSourceRegistry getValuesSourceRegistry() {
        return valuesSourceRegistry;
    }

    /**
     * Returns the {@link Highlighter} registry
     */
    public Map<String, Highlighter> getHighlighters() {
        return highlighters;
    }

    /**
     * The registry of {@link MovAvgModel}s.
     */
    public ParseFieldRegistry<MovAvgModel.AbstractModelParser> getMovingAverageModelParserRegistry() {
        return movingAverageModelParserRegistry;
    }

    private ValuesSourceRegistry registerAggregations(List<SearchPlugin> plugins) {
        ValuesSourceRegistry.Builder builder = new ValuesSourceRegistry.Builder();
        registerAggregation(new AggregationSpec(AvgAggregationBuilder.NAME, AvgAggregationBuilder::new, AvgAggregationBuilder.PARSER)
            .addResultReader(InternalAvg::new)
            .setAggregatorRegistrar(AvgAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(WeightedAvgAggregationBuilder.NAME, WeightedAvgAggregationBuilder::new,
            WeightedAvgAggregationBuilder.PARSER).addResultReader(InternalWeightedAvg::new)
            .setAggregatorRegistrar(WeightedAvgAggregationBuilder::registerUsage), builder);
        registerAggregation(new AggregationSpec(SumAggregationBuilder.NAME, SumAggregationBuilder::new, SumAggregationBuilder.PARSER)
            .addResultReader(InternalSum::new)
            .setAggregatorRegistrar(SumAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(MinAggregationBuilder.NAME, MinAggregationBuilder::new, MinAggregationBuilder.PARSER)
                .addResultReader(InternalMin::new)
                .setAggregatorRegistrar(MinAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(MaxAggregationBuilder.NAME, MaxAggregationBuilder::new, MaxAggregationBuilder.PARSER)
                .addResultReader(InternalMax::new)
                .setAggregatorRegistrar(MaxAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(StatsAggregationBuilder.NAME, StatsAggregationBuilder::new, StatsAggregationBuilder.PARSER)
            .addResultReader(InternalStats::new)
            .setAggregatorRegistrar(StatsAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(ExtendedStatsAggregationBuilder.NAME, ExtendedStatsAggregationBuilder::new,
            ExtendedStatsAggregationBuilder.PARSER)
                .addResultReader(InternalExtendedStats::new)
                .setAggregatorRegistrar(ExtendedStatsAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(ValueCountAggregationBuilder.NAME, ValueCountAggregationBuilder::new,
                ValueCountAggregationBuilder.PARSER)
                    .addResultReader(InternalValueCount::new)
                    .setAggregatorRegistrar(ValueCountAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(PercentilesAggregationBuilder.NAME, PercentilesAggregationBuilder::new,
                PercentilesAggregationBuilder::parse)
                    .addResultReader(InternalTDigestPercentiles.NAME, InternalTDigestPercentiles::new)
                    .addResultReader(InternalHDRPercentiles.NAME, InternalHDRPercentiles::new)
                    .setAggregatorRegistrar(PercentilesAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(PercentileRanksAggregationBuilder.NAME, PercentileRanksAggregationBuilder::new,
                PercentileRanksAggregationBuilder::parse)
                        .addResultReader(InternalTDigestPercentileRanks.NAME, InternalTDigestPercentileRanks::new)
                        .addResultReader(InternalHDRPercentileRanks.NAME, InternalHDRPercentileRanks::new)
                        .setAggregatorRegistrar(PercentileRanksAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(MedianAbsoluteDeviationAggregationBuilder.NAME,
            MedianAbsoluteDeviationAggregationBuilder::new, MedianAbsoluteDeviationAggregationBuilder.PARSER)
                .addResultReader(InternalMedianAbsoluteDeviation::new)
                .setAggregatorRegistrar(MedianAbsoluteDeviationAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(CardinalityAggregationBuilder.NAME, CardinalityAggregationBuilder::new,
                CardinalityAggregationBuilder.PARSER).addResultReader(InternalCardinality::new)
            .setAggregatorRegistrar(CardinalityAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(GlobalAggregationBuilder.NAME, GlobalAggregationBuilder::new,
                GlobalAggregationBuilder::parse).addResultReader(InternalGlobal::new), builder);
        registerAggregation(new AggregationSpec(MissingAggregationBuilder.NAME, MissingAggregationBuilder::new,
                MissingAggregationBuilder.PARSER)
                    .addResultReader(InternalMissing::new)
                    .setAggregatorRegistrar(MissingAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(FilterAggregationBuilder.NAME, FilterAggregationBuilder::new,
                FilterAggregationBuilder::parse).addResultReader(InternalFilter::new), builder);
        registerAggregation(new AggregationSpec(FiltersAggregationBuilder.NAME, FiltersAggregationBuilder::new,
                FiltersAggregationBuilder::parse).addResultReader(InternalFilters::new), builder);
        registerAggregation(new AggregationSpec(AdjacencyMatrixAggregationBuilder.NAME, AdjacencyMatrixAggregationBuilder::new,
                AdjacencyMatrixAggregationBuilder::parse).addResultReader(InternalAdjacencyMatrix::new), builder);
        registerAggregation(new AggregationSpec(SamplerAggregationBuilder.NAME, SamplerAggregationBuilder::new,
                SamplerAggregationBuilder::parse)
                    .addResultReader(InternalSampler.NAME, InternalSampler::new)
                    .addResultReader(UnmappedSampler.NAME, UnmappedSampler::new),
            builder);
        registerAggregation(new AggregationSpec(DiversifiedAggregationBuilder.NAME, DiversifiedAggregationBuilder::new,
                DiversifiedAggregationBuilder.PARSER).setAggregatorRegistrar(DiversifiedAggregationBuilder::registerAggregators)
                    /* Reuses result readers from SamplerAggregator*/, builder);
        registerAggregation(new AggregationSpec(TermsAggregationBuilder.NAME, TermsAggregationBuilder::new,
                TermsAggregationBuilder.PARSER)
                    .addResultReader(StringTerms.NAME, StringTerms::new)
                    .addResultReader(UnmappedTerms.NAME, UnmappedTerms::new)
                    .addResultReader(LongTerms.NAME, LongTerms::new)
                    .addResultReader(DoubleTerms.NAME, DoubleTerms::new)
            .setAggregatorRegistrar(TermsAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(RareTermsAggregationBuilder.NAME, RareTermsAggregationBuilder::new,
                RareTermsAggregationBuilder.PARSER)
                    .addResultReader(StringRareTerms.NAME, StringRareTerms::new)
                    .addResultReader(UnmappedRareTerms.NAME, UnmappedRareTerms::new)
                    .addResultReader(LongRareTerms.NAME, LongRareTerms::new)
                    .setAggregatorRegistrar(RareTermsAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(SignificantTermsAggregationBuilder.NAME, SignificantTermsAggregationBuilder::new,
                SignificantTermsAggregationBuilder::parse)
                    .addResultReader(SignificantStringTerms.NAME, SignificantStringTerms::new)
                    .addResultReader(SignificantLongTerms.NAME, SignificantLongTerms::new)
                    .addResultReader(UnmappedSignificantTerms.NAME, UnmappedSignificantTerms::new)
                    .setAggregatorRegistrar(SignificantTermsAggregationBuilder::registerAggregators),  builder);
        registerAggregation(new AggregationSpec(SignificantTextAggregationBuilder.NAME, SignificantTextAggregationBuilder::new,
                SignificantTextAggregationBuilder::parse), builder);
        registerAggregation(new AggregationSpec(RangeAggregationBuilder.NAME, RangeAggregationBuilder::new,
                RangeAggregationBuilder.PARSER)
                    .addResultReader(InternalRange::new)
                    .setAggregatorRegistrar(RangeAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(DateRangeAggregationBuilder.NAME, DateRangeAggregationBuilder::new,
                DateRangeAggregationBuilder.PARSER)
                    .addResultReader(InternalDateRange::new)
                    .setAggregatorRegistrar(DateRangeAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(IpRangeAggregationBuilder.NAME, IpRangeAggregationBuilder::new,
                IpRangeAggregationBuilder.PARSER)
                    .addResultReader(InternalBinaryRange::new)
                    .setAggregatorRegistrar(IpRangeAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(HistogramAggregationBuilder.NAME, HistogramAggregationBuilder::new,
                HistogramAggregationBuilder.PARSER)
                    .addResultReader(InternalHistogram::new)
                    .setAggregatorRegistrar(HistogramAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(DateHistogramAggregationBuilder.NAME, DateHistogramAggregationBuilder::new,
                DateHistogramAggregationBuilder.PARSER)
                    .addResultReader(InternalDateHistogram::new)
                    .setAggregatorRegistrar(DateHistogramAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(AutoDateHistogramAggregationBuilder.NAME, AutoDateHistogramAggregationBuilder::new,
                AutoDateHistogramAggregationBuilder.PARSER)
                    .addResultReader(InternalAutoDateHistogram::new)
                    .setAggregatorRegistrar(AutoDateHistogramAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(VariableWidthHistogramAggregationBuilder.NAME,
                VariableWidthHistogramAggregationBuilder::new,
                VariableWidthHistogramAggregationBuilder.PARSER)
                    .addResultReader(InternalVariableWidthHistogram::new)
                    .setAggregatorRegistrar(VariableWidthHistogramAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(GeoDistanceAggregationBuilder.NAME, GeoDistanceAggregationBuilder::new,
                GeoDistanceAggregationBuilder::parse)
                    .addResultReader(InternalGeoDistance::new)
                    .setAggregatorRegistrar(GeoDistanceAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(GeoHashGridAggregationBuilder.NAME, GeoHashGridAggregationBuilder::new,
                GeoHashGridAggregationBuilder.PARSER)
                    .addResultReader(InternalGeoHashGrid::new)
                    .setAggregatorRegistrar(GeoHashGridAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(GeoTileGridAggregationBuilder.NAME, GeoTileGridAggregationBuilder::new,
                GeoTileGridAggregationBuilder.PARSER)
                    .addResultReader(InternalGeoTileGrid::new)
                    .setAggregatorRegistrar(GeoTileGridAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(NestedAggregationBuilder.NAME, NestedAggregationBuilder::new,
                NestedAggregationBuilder::parse).addResultReader(InternalNested::new), builder);
        registerAggregation(new AggregationSpec(ReverseNestedAggregationBuilder.NAME, ReverseNestedAggregationBuilder::new,
                ReverseNestedAggregationBuilder::parse).addResultReader(InternalReverseNested::new), builder);
        registerAggregation(new AggregationSpec(TopHitsAggregationBuilder.NAME, TopHitsAggregationBuilder::new,
                TopHitsAggregationBuilder::parse).addResultReader(InternalTopHits::new), builder);
        registerAggregation(new AggregationSpec(GeoBoundsAggregationBuilder.NAME, GeoBoundsAggregationBuilder::new,
                GeoBoundsAggregationBuilder.PARSER)
                    .addResultReader(InternalGeoBounds::new)
                    .setAggregatorRegistrar(GeoBoundsAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(GeoCentroidAggregationBuilder.NAME, GeoCentroidAggregationBuilder::new,
                GeoCentroidAggregationBuilder.PARSER)
                    .addResultReader(InternalGeoCentroid::new)
                    .setAggregatorRegistrar(GeoCentroidAggregationBuilder::registerAggregators), builder);
        registerAggregation(new AggregationSpec(ScriptedMetricAggregationBuilder.NAME, ScriptedMetricAggregationBuilder::new,
                ScriptedMetricAggregationBuilder.PARSER).addResultReader(InternalScriptedMetric::new), builder);
        registerAggregation(
            new AggregationSpec(CompositeAggregationBuilder.NAME, CompositeAggregationBuilder::new, CompositeAggregationBuilder.PARSER)
                .addResultReader(InternalComposite::new)
                .setAggregatorRegistrar(CompositeAggregationBuilder::registerAggregators),
            builder
        );
        registerFromPlugin(plugins, SearchPlugin::getAggregations, (agg) -> this.registerAggregation(agg, builder));

        // after aggs have been registered, see if there are any new VSTypes that need to be linked to core fields
        registerFromPlugin(plugins, SearchPlugin::getAggregationExtentions,
            (registrar) -> {if (registrar != null) {registrar.accept(builder);}});

        return builder.build();
    }

    private void registerAggregation(AggregationSpec spec, ValuesSourceRegistry.Builder builder) {
        if (false == transportClient) {
            namedXContents.add(new NamedXContentRegistry.Entry(BaseAggregationBuilder.class, spec.getName(), (p, c) -> {
                String name = (String) c;
                return spec.getParser().parse(p, name);
            }));
        }
        namedWriteables.add(
                new NamedWriteableRegistry.Entry(AggregationBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
        for (Map.Entry<String, Writeable.Reader<? extends InternalAggregation>> t : spec.getResultReaders().entrySet()) {
            String writeableName = t.getKey();
            Writeable.Reader<? extends InternalAggregation> internalReader = t.getValue();
            namedWriteables.add(new NamedWriteableRegistry.Entry(InternalAggregation.class, writeableName, internalReader));
        }
        Consumer<ValuesSourceRegistry.Builder> register = spec.getAggregatorRegistrar();
        if (register != null) {
            register.accept(builder);
        } else {
            // Register is typically handling usage registration, but for the older aggregations that don't use register, we
            // have to register usage explicitly here.
            builder.registerUsage(spec.getName().getPreferredName());
        }
    }

    private void registerPipelineAggregations(List<SearchPlugin> plugins) {
        registerPipelineAggregation(new PipelineAggregationSpec(
                DerivativePipelineAggregationBuilder.NAME,
                DerivativePipelineAggregationBuilder::new,
                DerivativePipelineAggregator::new,
                DerivativePipelineAggregationBuilder::parse)
                    .addResultReader(InternalDerivative::new));
        registerPipelineAggregation(new PipelineAggregationSpec(
                MaxBucketPipelineAggregationBuilder.NAME,
                MaxBucketPipelineAggregationBuilder::new,
                MaxBucketPipelineAggregator::new,
                MaxBucketPipelineAggregationBuilder.PARSER)
                    // This bucket is used by many pipeline aggreations.
                    .addResultReader(InternalBucketMetricValue.NAME, InternalBucketMetricValue::new));
        registerPipelineAggregation(new PipelineAggregationSpec(
                MinBucketPipelineAggregationBuilder.NAME,
                MinBucketPipelineAggregationBuilder::new,
                MinBucketPipelineAggregator::new,
                MinBucketPipelineAggregationBuilder.PARSER)
                    /* Uses InternalBucketMetricValue */);
        registerPipelineAggregation(new PipelineAggregationSpec(
                AvgBucketPipelineAggregationBuilder.NAME,
                AvgBucketPipelineAggregationBuilder::new,
                AvgBucketPipelineAggregator::new,
                AvgBucketPipelineAggregationBuilder.PARSER)
                    // This bucket is used by many pipeline aggreations.
                    .addResultReader(InternalSimpleValue.NAME, InternalSimpleValue::new));
        registerPipelineAggregation(new PipelineAggregationSpec(
                SumBucketPipelineAggregationBuilder.NAME,
                SumBucketPipelineAggregationBuilder::new,
                SumBucketPipelineAggregator::new,
                SumBucketPipelineAggregationBuilder.PARSER)
                    /* Uses InternalSimpleValue */);
        registerPipelineAggregation(new PipelineAggregationSpec(
                StatsBucketPipelineAggregationBuilder.NAME,
                StatsBucketPipelineAggregationBuilder::new,
                StatsBucketPipelineAggregator::new,
                StatsBucketPipelineAggregationBuilder.PARSER)
                    .addResultReader(InternalStatsBucket::new));
        registerPipelineAggregation(new PipelineAggregationSpec(
                ExtendedStatsBucketPipelineAggregationBuilder.NAME,
                ExtendedStatsBucketPipelineAggregationBuilder::new,
                ExtendedStatsBucketPipelineAggregator::new,
                new ExtendedStatsBucketParser())
                    .addResultReader(InternalExtendedStatsBucket::new));
        registerPipelineAggregation(new PipelineAggregationSpec(
                PercentilesBucketPipelineAggregationBuilder.NAME,
                PercentilesBucketPipelineAggregationBuilder::new,
                PercentilesBucketPipelineAggregator::new,
                PercentilesBucketPipelineAggregationBuilder.PARSER)
                    .addResultReader(InternalPercentilesBucket::new));
        registerPipelineAggregation(new PipelineAggregationSpec(
                MovAvgPipelineAggregationBuilder.NAME,
                MovAvgPipelineAggregationBuilder::new,
                MovAvgPipelineAggregator::new,
                (XContentParser parser, String name) ->
                    MovAvgPipelineAggregationBuilder.parse(movingAverageModelParserRegistry, name, parser)
                )/* Uses InternalHistogram for buckets */);
        registerPipelineAggregation(new PipelineAggregationSpec(
                CumulativeSumPipelineAggregationBuilder.NAME,
                CumulativeSumPipelineAggregationBuilder::new,
                CumulativeSumPipelineAggregator::new,
                CumulativeSumPipelineAggregationBuilder.PARSER));
        registerPipelineAggregation(new PipelineAggregationSpec(
                BucketScriptPipelineAggregationBuilder.NAME,
                BucketScriptPipelineAggregationBuilder::new,
                BucketScriptPipelineAggregator::new,
                BucketScriptPipelineAggregationBuilder.PARSER));
        registerPipelineAggregation(new PipelineAggregationSpec(
                BucketSelectorPipelineAggregationBuilder.NAME,
                BucketSelectorPipelineAggregationBuilder::new,
                BucketSelectorPipelineAggregator::new,
                BucketSelectorPipelineAggregationBuilder::parse));
        registerPipelineAggregation(new PipelineAggregationSpec(
                BucketSortPipelineAggregationBuilder.NAME,
                BucketSortPipelineAggregationBuilder::new,
                BucketSortPipelineAggregator::new,
                BucketSortPipelineAggregationBuilder::parse));
        registerPipelineAggregation(new PipelineAggregationSpec(
                SerialDiffPipelineAggregationBuilder.NAME,
                SerialDiffPipelineAggregationBuilder::new,
                SerialDiffPipelineAggregator::new,
                SerialDiffPipelineAggregationBuilder::parse));
        registerPipelineAggregation(new PipelineAggregationSpec(
                MovFnPipelineAggregationBuilder.NAME,
                MovFnPipelineAggregationBuilder::new,
                MovFnPipelineAggregator::new,
                MovFnPipelineAggregationBuilder.PARSER));

        registerFromPlugin(plugins, SearchPlugin::getPipelineAggregations, this::registerPipelineAggregation);
    }

    private void registerPipelineAggregation(PipelineAggregationSpec spec) {
        if (false == transportClient) {
            namedXContents.add(new NamedXContentRegistry.Entry(BaseAggregationBuilder.class, spec.getName(),
                    (p, c) -> spec.getParser().parse(p, (String) c)));
        }
        namedWriteables.add(
                new NamedWriteableRegistry.Entry(PipelineAggregationBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
        if (spec.getAggregatorReader() != null) {
            namedWriteables.add(new NamedWriteableRegistry.Entry(
                PipelineAggregator.class, spec.getName().getPreferredName(), spec.getAggregatorReader()));
        }
        for (Map.Entry<String, Writeable.Reader<? extends InternalAggregation>> resultReader : spec.getResultReaders().entrySet()) {
            namedWriteables
                    .add(new NamedWriteableRegistry.Entry(InternalAggregation.class, resultReader.getKey(), resultReader.getValue()));
        }
    }

    private void registerShapes() {
        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            namedWriteables.addAll(GeoShapeType.getShapeWriteables());
        }
    }

    private void registerRescorers(List<SearchPlugin> plugins) {
        registerRescorer(new RescorerSpec<>(QueryRescorerBuilder.NAME, QueryRescorerBuilder::new, QueryRescorerBuilder::fromXContent));
        registerFromPlugin(plugins, SearchPlugin::getRescorers, this::registerRescorer);
    }

    private void registerRescorer(RescorerSpec<?> spec) {
        if (false == transportClient) {
            namedXContents.add(new NamedXContentRegistry.Entry(RescorerBuilder.class, spec.getName(), (p, c) -> spec.getParser().apply(p)));
        }
        namedWriteables.add(new NamedWriteableRegistry.Entry(RescorerBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
    }

    private void registerSorts() {
        namedWriteables.add(new NamedWriteableRegistry.Entry(SortBuilder.class, GeoDistanceSortBuilder.NAME, GeoDistanceSortBuilder::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SortBuilder.class, ScoreSortBuilder.NAME, ScoreSortBuilder::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SortBuilder.class, ScriptSortBuilder.NAME, ScriptSortBuilder::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SortBuilder.class, FieldSortBuilder.NAME, FieldSortBuilder::new));
    }

    private <T> void registerFromPlugin(List<SearchPlugin> plugins, Function<SearchPlugin, List<T>> producer, Consumer<T> consumer) {
        for (SearchPlugin plugin : plugins) {
            for (T t : producer.apply(plugin)) {
                consumer.accept(t);
            }
        }
    }

    public static void registerSmoothingModels(List<Entry> namedWriteables) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(SmoothingModel.class, Laplace.NAME, Laplace::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SmoothingModel.class, LinearInterpolation.NAME, LinearInterpolation::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SmoothingModel.class, StupidBackoff.NAME, StupidBackoff::new));
    }

    private void registerSuggesters(List<SearchPlugin> plugins) {
        registerSmoothingModels(namedWriteables);

        registerSuggester(new SuggesterSpec<>(TermSuggestionBuilder.SUGGESTION_NAME,
            TermSuggestionBuilder::new, TermSuggestionBuilder::fromXContent, TermSuggestion::new));

        registerSuggester(new SuggesterSpec<>(PhraseSuggestionBuilder.SUGGESTION_NAME,
            PhraseSuggestionBuilder::new, PhraseSuggestionBuilder::fromXContent, PhraseSuggestion::new));

        registerSuggester(new SuggesterSpec<>(CompletionSuggestionBuilder.SUGGESTION_NAME,
            CompletionSuggestionBuilder::new, CompletionSuggestionBuilder::fromXContent, CompletionSuggestion::new));

        registerFromPlugin(plugins, SearchPlugin::getSuggesters, this::registerSuggester);
    }

    private void registerSuggester(SuggesterSpec<?> suggester) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(
                SuggestionBuilder.class, suggester.getName().getPreferredName(), suggester.getReader()));
        namedXContents.add(new NamedXContentRegistry.Entry(SuggestionBuilder.class, suggester.getName(),
                suggester.getParser()));

        namedWriteables.add(new NamedWriteableRegistry.Entry(
            Suggest.Suggestion.class, suggester.getName().getPreferredName(), suggester.getSuggestionReader()
        ));
    }

    private Map<String, Highlighter> setupHighlighters(Settings settings, List<SearchPlugin> plugins) {
        NamedRegistry<Highlighter> highlighters = new NamedRegistry<>("highlighter");
        highlighters.register("fvh",  new FastVectorHighlighter(settings));
        highlighters.register("plain", new PlainHighlighter());
        highlighters.register("unified", new UnifiedHighlighter());
        highlighters.extractAndRegister(plugins, SearchPlugin::getHighlighters);

        return unmodifiableMap(highlighters.getRegistry());
    }

    private void registerScoreFunctions(List<SearchPlugin> plugins) {
        // ScriptScoreFunctionBuilder has it own named writable because of a new script_score query
        namedWriteables.add(new NamedWriteableRegistry.Entry(
            ScriptScoreFunctionBuilder.class, ScriptScoreFunctionBuilder.NAME,  ScriptScoreFunctionBuilder::new));
        registerScoreFunction(new ScoreFunctionSpec<>(ScriptScoreFunctionBuilder.NAME, ScriptScoreFunctionBuilder::new,
                ScriptScoreFunctionBuilder::fromXContent));

        registerScoreFunction(
                new ScoreFunctionSpec<>(GaussDecayFunctionBuilder.NAME, GaussDecayFunctionBuilder::new, GaussDecayFunctionBuilder.PARSER));
        registerScoreFunction(new ScoreFunctionSpec<>(LinearDecayFunctionBuilder.NAME, LinearDecayFunctionBuilder::new,
                LinearDecayFunctionBuilder.PARSER));
        registerScoreFunction(new ScoreFunctionSpec<>(ExponentialDecayFunctionBuilder.NAME, ExponentialDecayFunctionBuilder::new,
                ExponentialDecayFunctionBuilder.PARSER));
        registerScoreFunction(new ScoreFunctionSpec<>(RandomScoreFunctionBuilder.NAME, RandomScoreFunctionBuilder::new,
                RandomScoreFunctionBuilder::fromXContent));
        registerScoreFunction(new ScoreFunctionSpec<>(FieldValueFactorFunctionBuilder.NAME, FieldValueFactorFunctionBuilder::new,
                FieldValueFactorFunctionBuilder::fromXContent));

        //weight doesn't have its own parser, so every function supports it out of the box.
        //Can be a single function too when not associated to any other function, which is why it needs to be registered manually here.
        namedWriteables.add(new NamedWriteableRegistry.Entry(ScoreFunctionBuilder.class, WeightBuilder.NAME, WeightBuilder::new));

        registerFromPlugin(plugins, SearchPlugin::getScoreFunctions, this::registerScoreFunction);
    }

    private void registerScoreFunction(ScoreFunctionSpec<?> scoreFunction) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(
                ScoreFunctionBuilder.class, scoreFunction.getName().getPreferredName(), scoreFunction.getReader()));
        // TODO remove funky contexts
        namedXContents.add(new NamedXContentRegistry.Entry(
                ScoreFunctionBuilder.class, scoreFunction.getName(),
                (XContentParser p, Object c) -> scoreFunction.getParser().fromXContent(p)));
    }

    private void registerValueFormats() {
        registerValueFormat(DocValueFormat.BOOLEAN.getWriteableName(), in -> DocValueFormat.BOOLEAN);
        registerValueFormat(DocValueFormat.DateTime.NAME, DocValueFormat.DateTime::new);
        registerValueFormat(DocValueFormat.Decimal.NAME, DocValueFormat.Decimal::new);
        registerValueFormat(DocValueFormat.GEOHASH.getWriteableName(), in -> DocValueFormat.GEOHASH);
        registerValueFormat(DocValueFormat.GEOTILE.getWriteableName(), in -> DocValueFormat.GEOTILE);
        registerValueFormat(DocValueFormat.IP.getWriteableName(), in -> DocValueFormat.IP);
        registerValueFormat(DocValueFormat.RAW.getWriteableName(), in -> DocValueFormat.RAW);
        registerValueFormat(DocValueFormat.BINARY.getWriteableName(), in -> DocValueFormat.BINARY);
        registerValueFormat(DocValueFormat.UNSIGNED_LONG_SHIFTED.getWriteableName(), in -> DocValueFormat.UNSIGNED_LONG_SHIFTED);
    }

    /**
     * Register a new ValueFormat.
     */
    private void registerValueFormat(String name, Writeable.Reader<? extends DocValueFormat> reader) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(DocValueFormat.class, name, reader));
    }

    private void registerSignificanceHeuristics(List<SearchPlugin> plugins) {
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(ChiSquare.NAME, ChiSquare::new, ChiSquare.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(GND.NAME, GND::new, GND.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(JLHScore.NAME, JLHScore::new, JLHScore.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(
            MutualInformation.NAME, MutualInformation::new, MutualInformation.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(PercentageScore.NAME, PercentageScore::new, PercentageScore.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(ScriptHeuristic.NAME, ScriptHeuristic::new, ScriptHeuristic.PARSER));

        registerFromPlugin(plugins, SearchPlugin::getSignificanceHeuristics, this::registerSignificanceHeuristic);
    }

    private <T extends SignificanceHeuristic> void registerSignificanceHeuristic(SignificanceHeuristicSpec<?> spec) {
        namedXContents.add(new NamedXContentRegistry.Entry(
            SignificanceHeuristic.class, spec.getName(), p -> spec.getParser().apply(p, null)));
        namedWriteables.add(new NamedWriteableRegistry.Entry(
            SignificanceHeuristic.class, spec.getName().getPreferredName(), spec.getReader()));
    }

    private void registerMovingAverageModels(List<SearchPlugin> plugins) {
        registerMovingAverageModel(new SearchExtensionSpec<>(SimpleModel.NAME, SimpleModel::new, SimpleModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(LinearModel.NAME, LinearModel::new, LinearModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(EwmaModel.NAME, EwmaModel::new, EwmaModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(HoltLinearModel.NAME, HoltLinearModel::new, HoltLinearModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(HoltWintersModel.NAME, HoltWintersModel::new, HoltWintersModel.PARSER));

        registerFromPlugin(plugins, SearchPlugin::getMovingAverageModels, this::registerMovingAverageModel);
    }

    private void registerMovingAverageModel(SearchExtensionSpec<MovAvgModel, MovAvgModel.AbstractModelParser> movAvgModel) {
        movingAverageModelParserRegistry.register(movAvgModel.getParser(), movAvgModel.getName());
        namedWriteables.add(
                new NamedWriteableRegistry.Entry(MovAvgModel.class, movAvgModel.getName().getPreferredName(), movAvgModel.getReader()));
    }

    private void registerFetchSubPhases(List<SearchPlugin> plugins) {
        registerFetchSubPhase(new ExplainPhase());
        registerFetchSubPhase(new FetchDocValuesPhase());
        registerFetchSubPhase(new ScriptFieldsPhase());
        registerFetchSubPhase(new FetchSourcePhase());
        registerFetchSubPhase(new FetchFieldsPhase());
        registerFetchSubPhase(new FetchVersionPhase());
        registerFetchSubPhase(new SeqNoPrimaryTermPhase());
        registerFetchSubPhase(new MatchedQueriesPhase());
        registerFetchSubPhase(new HighlightPhase(highlighters));
        registerFetchSubPhase(new FetchScorePhase());

        FetchPhaseConstructionContext context = new FetchPhaseConstructionContext(highlighters);
        registerFromPlugin(plugins, p -> p.getFetchSubPhases(context), this::registerFetchSubPhase);
    }

    private void registerSearchExts(List<SearchPlugin> plugins) {
        registerFromPlugin(plugins, SearchPlugin::getSearchExts, this::registerSearchExt);
    }

    private void registerSearchExt(SearchExtSpec<?> spec) {
        namedXContents.add(new NamedXContentRegistry.Entry(SearchExtBuilder.class, spec.getName(), spec.getParser()));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SearchExtBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
    }

    private void registerFetchSubPhase(FetchSubPhase subPhase) {
        Class<?> subPhaseClass = subPhase.getClass();
        if (fetchSubPhases.stream().anyMatch(p -> p.getClass().equals(subPhaseClass))) {
            throw new IllegalArgumentException("FetchSubPhase [" + subPhaseClass + "] already registered");
        }
        fetchSubPhases.add(requireNonNull(subPhase, "FetchSubPhase must not be null"));
    }

    private void registerQueryParsers(List<SearchPlugin> plugins) {
        registerQuery(new QuerySpec<>(MatchQueryBuilder.NAME, MatchQueryBuilder::new, MatchQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchPhraseQueryBuilder.NAME, MatchPhraseQueryBuilder::new, MatchPhraseQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchPhrasePrefixQueryBuilder.NAME, MatchPhrasePrefixQueryBuilder::new,
                MatchPhrasePrefixQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MultiMatchQueryBuilder.NAME, MultiMatchQueryBuilder::new, MultiMatchQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(NestedQueryBuilder.NAME, NestedQueryBuilder::new, NestedQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(DisMaxQueryBuilder.NAME, DisMaxQueryBuilder::new, DisMaxQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(IdsQueryBuilder.NAME, IdsQueryBuilder::new, IdsQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchAllQueryBuilder.NAME, MatchAllQueryBuilder::new, MatchAllQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(QueryStringQueryBuilder.NAME, QueryStringQueryBuilder::new, QueryStringQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(BoostingQueryBuilder.NAME, BoostingQueryBuilder::new, BoostingQueryBuilder::fromXContent));
        BooleanQuery.setMaxClauseCount(INDICES_MAX_CLAUSE_COUNT_SETTING.get(settings));
        registerQuery(new QuerySpec<>(BoolQueryBuilder.NAME, BoolQueryBuilder::new, BoolQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TermQueryBuilder.NAME, TermQueryBuilder::new, TermQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TermsQueryBuilder.NAME, TermsQueryBuilder::new, TermsQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(FuzzyQueryBuilder.NAME, FuzzyQueryBuilder::new, FuzzyQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(RegexpQueryBuilder.NAME, RegexpQueryBuilder::new, RegexpQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(RangeQueryBuilder.NAME, RangeQueryBuilder::new, RangeQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(PrefixQueryBuilder.NAME, PrefixQueryBuilder::new, PrefixQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(WildcardQueryBuilder.NAME, WildcardQueryBuilder::new, WildcardQueryBuilder::fromXContent));
        registerQuery(
                new QuerySpec<>(ConstantScoreQueryBuilder.NAME, ConstantScoreQueryBuilder::new, ConstantScoreQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanTermQueryBuilder.NAME, SpanTermQueryBuilder::new, SpanTermQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanNotQueryBuilder.NAME, SpanNotQueryBuilder::new, SpanNotQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanWithinQueryBuilder.NAME, SpanWithinQueryBuilder::new, SpanWithinQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanContainingQueryBuilder.NAME, SpanContainingQueryBuilder::new,
                SpanContainingQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(FieldMaskingSpanQueryBuilder.NAME, FieldMaskingSpanQueryBuilder::new,
                FieldMaskingSpanQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanFirstQueryBuilder.NAME, SpanFirstQueryBuilder::new, SpanFirstQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanNearQueryBuilder.NAME, SpanNearQueryBuilder::new, SpanNearQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanGapQueryBuilder.NAME, SpanGapQueryBuilder::new, SpanGapQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanOrQueryBuilder.NAME, SpanOrQueryBuilder::new, SpanOrQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MoreLikeThisQueryBuilder.NAME, MoreLikeThisQueryBuilder::new,
                MoreLikeThisQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(WrapperQueryBuilder.NAME, WrapperQueryBuilder::new, WrapperQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(new ParseField(CommonTermsQueryBuilder.NAME).withAllDeprecated(COMMON_TERMS_QUERY_DEPRECATION_MSG),
                CommonTermsQueryBuilder::new, CommonTermsQueryBuilder::fromXContent));
        registerQuery(
                new QuerySpec<>(SpanMultiTermQueryBuilder.NAME, SpanMultiTermQueryBuilder::new, SpanMultiTermQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(FunctionScoreQueryBuilder.NAME, FunctionScoreQueryBuilder::new,
                FunctionScoreQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(ScriptScoreQueryBuilder.NAME, ScriptScoreQueryBuilder::new, ScriptScoreQueryBuilder::fromXContent));
        registerQuery(
                new QuerySpec<>(SimpleQueryStringBuilder.NAME, SimpleQueryStringBuilder::new, SimpleQueryStringBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TypeQueryBuilder.NAME, TypeQueryBuilder::new, TypeQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(ScriptQueryBuilder.NAME, ScriptQueryBuilder::new, ScriptQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(GeoDistanceQueryBuilder.NAME, GeoDistanceQueryBuilder::new, GeoDistanceQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(GeoBoundingBoxQueryBuilder.NAME, GeoBoundingBoxQueryBuilder::new,
                GeoBoundingBoxQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(GeoPolygonQueryBuilder.NAME, GeoPolygonQueryBuilder::new, GeoPolygonQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(ExistsQueryBuilder.NAME, ExistsQueryBuilder::new, ExistsQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchNoneQueryBuilder.NAME, MatchNoneQueryBuilder::new, MatchNoneQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TermsSetQueryBuilder.NAME, TermsSetQueryBuilder::new, TermsSetQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(IntervalQueryBuilder.NAME, IntervalQueryBuilder::new, IntervalQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(DistanceFeatureQueryBuilder.NAME, DistanceFeatureQueryBuilder::new,
            DistanceFeatureQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(MatchBoolPrefixQueryBuilder.NAME, MatchBoolPrefixQueryBuilder::new, MatchBoolPrefixQueryBuilder::fromXContent));

        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            registerQuery(new QuerySpec<>(GeoShapeQueryBuilder.NAME, GeoShapeQueryBuilder::new, GeoShapeQueryBuilder::fromXContent));
        }

        registerFromPlugin(plugins, SearchPlugin::getQueries, this::registerQuery);
    }

    private void registerIntervalsSourceProviders() {
        namedWriteables.addAll(getIntervalsSourceProviderNamedWritables());
    }

    public static List<NamedWriteableRegistry.Entry> getIntervalsSourceProviderNamedWritables() {
        return unmodifiableList(Arrays.asList(
                new NamedWriteableRegistry.Entry(IntervalsSourceProvider.class, IntervalsSourceProvider.Match.NAME,
                        IntervalsSourceProvider.Match::new),
                new NamedWriteableRegistry.Entry(IntervalsSourceProvider.class, IntervalsSourceProvider.Combine.NAME,
                        IntervalsSourceProvider.Combine::new),
                new NamedWriteableRegistry.Entry(IntervalsSourceProvider.class, IntervalsSourceProvider.Disjunction.NAME,
                        IntervalsSourceProvider.Disjunction::new),
                new NamedWriteableRegistry.Entry(IntervalsSourceProvider.class, IntervalsSourceProvider.Prefix.NAME,
                        IntervalsSourceProvider.Prefix::new),
                new NamedWriteableRegistry.Entry(IntervalsSourceProvider.class, IntervalsSourceProvider.Wildcard.NAME,
                        IntervalsSourceProvider.Wildcard::new),
                new NamedWriteableRegistry.Entry(IntervalsSourceProvider.class, IntervalsSourceProvider.Fuzzy.NAME,
                        IntervalsSourceProvider.Fuzzy::new)));
    }

    private void registerQuery(QuerySpec<?> spec) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(QueryBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
        namedXContents.add(new NamedXContentRegistry.Entry(QueryBuilder.class, spec.getName(),
                (p, c) -> spec.getParser().fromXContent(p)));
    }

    public FetchPhase getFetchPhase() {
        return new FetchPhase(fetchSubPhases);
    }
}
