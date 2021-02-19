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

package org.renameme.index.reindex;

import org.renameme.action.ActionListener;
import org.renameme.action.support.ActionFilters;
import org.renameme.action.support.HandledTransportAction;
import org.renameme.client.Client;
import org.renameme.client.ParentTaskAssigningClient;
import org.renameme.cluster.service.ClusterService;
import org.renameme.common.inject.Inject;
import org.renameme.common.io.stream.Writeable;
import org.renameme.index.reindex.BulkByScrollResponse;
import org.renameme.index.reindex.BulkByScrollTask;
import org.renameme.index.reindex.DeleteByQueryAction;
import org.renameme.index.reindex.DeleteByQueryRequest;
import org.renameme.script.ScriptService;
import org.renameme.tasks.Task;
import org.renameme.threadpool.ThreadPool;
import org.renameme.transport.TransportService;


public class TransportDeleteByQueryAction extends HandledTransportAction<DeleteByQueryRequest, BulkByScrollResponse> {

    private final ThreadPool threadPool;
    private final Client client;
    private final ScriptService scriptService;
    private final ClusterService clusterService;

    @Inject
    public TransportDeleteByQueryAction(ThreadPool threadPool, ActionFilters actionFilters, Client client,
                                        TransportService transportService, ScriptService scriptService, ClusterService clusterService) {
        super(DeleteByQueryAction.NAME, transportService, actionFilters,
            (Writeable.Reader<DeleteByQueryRequest>) DeleteByQueryRequest::new);
        this.threadPool = threadPool;
        this.client = client;
        this.scriptService = scriptService;
        this.clusterService = clusterService;
    }

    @Override
    public void doExecute(Task task, DeleteByQueryRequest request, ActionListener<BulkByScrollResponse> listener) {
        BulkByScrollTask bulkByScrollTask = (BulkByScrollTask) task;
        BulkByScrollParallelizationHelper.startSlicedAction(request, bulkByScrollTask, DeleteByQueryAction.INSTANCE, listener, client,
            clusterService.localNode(),
            () -> {
                ParentTaskAssigningClient assigningClient = new ParentTaskAssigningClient(client, clusterService.localNode(),
                    bulkByScrollTask);
                new AsyncDeleteByQueryAction(bulkByScrollTask, logger, assigningClient, threadPool, request, scriptService,
                    listener).start();
            }
        );
    }
}
