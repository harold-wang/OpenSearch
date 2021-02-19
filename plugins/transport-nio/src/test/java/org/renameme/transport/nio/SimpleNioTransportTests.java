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

package org.renameme.transport.nio;

import org.renameme.Version;
import org.renameme.action.ActionListener;
import org.renameme.bootstrap.JavaVersion;
import org.renameme.cluster.node.DiscoveryNode;
import org.renameme.common.io.stream.NamedWriteableRegistry;
import org.renameme.common.network.NetworkService;
import org.renameme.common.settings.ClusterSettings;
import org.renameme.common.settings.Settings;
import org.renameme.common.transport.TransportAddress;
import org.renameme.common.util.MockPageCacheRecycler;
import org.renameme.core.internal.io.IOUtils;
import org.renameme.core.internal.net.NetUtils;
import org.renameme.indices.breaker.NoneCircuitBreakerService;
import org.renameme.test.transport.MockTransportService;
import org.renameme.test.transport.StubbableTransport;
import org.renameme.transport.AbstractSimpleTransportTestCase;
import org.renameme.transport.ConnectTransportException;
import org.renameme.transport.ConnectionProfile;
import org.renameme.transport.TcpChannel;
import org.renameme.transport.TcpTransport;
import org.renameme.transport.TestProfiles;
import org.renameme.transport.Transport;
import org.renameme.transport.nio.NioGroupFactory;
import org.renameme.transport.nio.NioTcpChannel;
import org.renameme.transport.nio.NioTransport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Collections;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class SimpleNioTransportTests extends AbstractSimpleTransportTestCase {

    @Override
    protected Transport build(Settings settings, final Version version, ClusterSettings clusterSettings, boolean doHandshake) {
        NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
        NetworkService networkService = new NetworkService(Collections.emptyList());
        return new NioTransport(settings, version, threadPool, networkService, new MockPageCacheRecycler(settings),
            namedWriteableRegistry, new NoneCircuitBreakerService(), new NioGroupFactory(settings, logger)) {

            @Override
            public void executeHandshake(DiscoveryNode node, TcpChannel channel, ConnectionProfile profile,
                                         ActionListener<Version> listener) {
                if (doHandshake) {
                    super.executeHandshake(node, channel, profile, listener);
                } else {
                    listener.onResponse(version.minimumCompatibilityVersion());
                }
            }
        };
    }

    public void testConnectException() throws UnknownHostException {
        try {
            serviceA.connectToNode(new DiscoveryNode("C", new TransportAddress(InetAddress.getByName("localhost"), 9876),
                emptyMap(), emptySet(),Version.CURRENT));
            fail("Expected ConnectTransportException");
        } catch (ConnectTransportException e) {
            assertThat(e.getMessage(), containsString("connect_exception"));
            assertThat(e.getMessage(), containsString("[127.0.0.1:9876]"));
            Throwable cause = e.getCause();
            assertThat(cause, instanceOf(IOException.class));
        }
    }

    public void testDefaultKeepAliveSettings() throws IOException {
        assumeTrue("setting default keepalive options not supported on this platform",
            (IOUtils.LINUX || IOUtils.MAC_OS_X) &&
                JavaVersion.current().compareTo(JavaVersion.parse("11")) >= 0);
        try (MockTransportService serviceC = buildService("TS_C", Version.CURRENT, Settings.EMPTY);
             MockTransportService serviceD = buildService("TS_D", Version.CURRENT, Settings.EMPTY)) {
            serviceC.start();
            serviceC.acceptIncomingRequests();
            serviceD.start();
            serviceD.acceptIncomingRequests();

            try (Transport.Connection connection = serviceC.openConnection(serviceD.getLocalDiscoNode(), TestProfiles.LIGHT_PROFILE)) {
                assertThat(connection, instanceOf(StubbableTransport.WrappedConnection.class));
                Transport.Connection conn = ((StubbableTransport.WrappedConnection) connection).getConnection();
                assertThat(conn, instanceOf(TcpTransport.NodeChannels.class));
                TcpTransport.NodeChannels nodeChannels = (TcpTransport.NodeChannels) conn;
                for (TcpChannel channel : nodeChannels.getChannels()) {
                    assertFalse(channel.isServerChannel());
                    checkDefaultKeepAliveOptions(channel);
                }

                assertThat(serviceD.getOriginalTransport(), instanceOf(TcpTransport.class));
                for (TcpChannel channel : getAcceptedChannels((TcpTransport) serviceD.getOriginalTransport())) {
                    assertTrue(channel.isServerChannel());
                    checkDefaultKeepAliveOptions(channel);
                }
            }
        }
    }

    private void checkDefaultKeepAliveOptions(TcpChannel channel) throws IOException {
        assertThat(channel, instanceOf(NioTcpChannel.class));
        NioTcpChannel nioChannel = (NioTcpChannel) channel;
        SocketChannel socketChannel = nioChannel.getRawChannel();
        assertThat(socketChannel.supportedOptions(), hasItem(NetUtils.getTcpKeepIdleSocketOptionOrNull()));
        Integer keepIdle = socketChannel.getOption(NetUtils.getTcpKeepIdleSocketOptionOrNull());
        assertNotNull(keepIdle);
        assertThat(keepIdle, lessThanOrEqualTo(500));
        assertThat(socketChannel.supportedOptions(), hasItem(NetUtils.getTcpKeepIntervalSocketOptionOrNull()));
        Integer keepInterval = socketChannel.getOption(NetUtils.getTcpKeepIntervalSocketOptionOrNull());
        assertNotNull(keepInterval);
        assertThat(keepInterval, lessThanOrEqualTo(500));
    }
}
