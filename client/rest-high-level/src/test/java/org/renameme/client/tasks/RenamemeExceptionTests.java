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
package org.renameme.client.tasks;

import org.renameme.client.AbstractResponseTestCase;
import org.renameme.common.xcontent.XContentParser;
import org.renameme.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Collections;

public class RenamemeExceptionTests extends AbstractResponseTestCase<org.renameme.RenamemeException,
    org.renameme.client.tasks.RenamemeException> {

    @Override
    protected org.renameme.RenamemeException createServerTestInstance(XContentType xContentType) {
        IllegalStateException ies = new IllegalStateException("illegal_state");
        IllegalArgumentException iae = new IllegalArgumentException("argument", ies);
        org.renameme.RenamemeException exception = new org.renameme.RenamemeException("elastic_exception", iae);
        exception.addHeader("key","value");
        exception.addMetadata("es.meta","data");
        exception.addSuppressed(new NumberFormatException("3/0"));
        return exception;
    }

    @Override
    protected RenamemeException doParseToClientInstance(XContentParser parser) throws IOException {
        parser.nextToken();
        return RenamemeException.fromXContent(parser);
    }

    @Override
    protected void assertInstances(org.renameme.RenamemeException serverTestInstance, RenamemeException clientInstance) {

        IllegalArgumentException sCauseLevel1 = (IllegalArgumentException) serverTestInstance.getCause();
        RenamemeException cCauseLevel1 = clientInstance.getCause();

        assertTrue(sCauseLevel1 !=null);
        assertTrue(cCauseLevel1 !=null);

        IllegalStateException causeLevel2 = (IllegalStateException) serverTestInstance.getCause().getCause();
        RenamemeException cCauseLevel2 = clientInstance.getCause().getCause();
        assertTrue(causeLevel2 !=null);
        assertTrue(cCauseLevel2 !=null);


        RenamemeException cause = new RenamemeException(
            "Renameme exception [type=illegal_state_exception, reason=illegal_state]"
        );
        RenamemeException caused1 = new RenamemeException(
            "Renameme exception [type=illegal_argument_exception, reason=argument]",cause
        );
        RenamemeException caused2 = new RenamemeException(
            "Renameme exception [type=exception, reason=elastic_exception]",caused1
        );

        caused2.addHeader("key", Collections.singletonList("value"));
        RenamemeException supp = new RenamemeException(
            "Renameme exception [type=number_format_exception, reason=3/0]"
        );
        caused2.addSuppressed(Collections.singletonList(supp));

        assertEquals(caused2,clientInstance);

    }

}
