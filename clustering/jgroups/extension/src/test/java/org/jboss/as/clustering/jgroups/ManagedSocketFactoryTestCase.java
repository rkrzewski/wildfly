/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.jgroups;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import org.jboss.as.network.ManagedServerSocketFactory;
import org.jboss.as.network.ManagedSocketFactory;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Paul Ferraro
 */
public class ManagedSocketFactoryTestCase {

    private final SocketBindingManager manager = mock(SocketBindingManager.class);

    private final SocketFactory subject = new org.jboss.as.clustering.jgroups.ManagedSocketFactory(this.manager, Map.of("known-service", new SocketBinding("binding", 0, false, null, 0, null, this.manager, List.of())));

    @After
    public void destroy() {
        reset(this.manager);
    }

    @Test
    public void createSocket() throws IOException {
        this.createSocket("known-service", "binding");
        this.createSocket("unknown-service", "unknown-service");
    }

    private void createSocket(String serviceName, String bindingName) throws IOException {
        ManagedSocketFactory factory = mock(ManagedSocketFactory.class);
        Socket socket = mock(Socket.class);

        when(this.manager.getSocketFactory()).thenReturn(factory);
        when(factory.createSocket(bindingName)).thenReturn(socket);

        try (Socket result = this.subject.createSocket(serviceName)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());
            verify(socket, never()).connect(any());
        }
        reset(socket);

        InetAddress connectAddress = InetAddress.getLocalHost();
        int connectPort = 1;

        try (Socket result = this.subject.createSocket(serviceName, connectAddress, connectPort)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
        reset(socket);

        try (Socket result = this.subject.createSocket(serviceName, connectAddress.getHostName(), connectPort)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
        reset(socket);

        InetAddress bindAddress = InetAddress.getLoopbackAddress();
        int bindPort = 2;

        try (Socket result = this.subject.createSocket(serviceName, connectAddress, connectPort, bindAddress, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedBindAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedBindAddress.capture());
            InetSocketAddress bindSocketAddress = capturedBindAddress.getValue();
            assertEquals(bindAddress, bindSocketAddress.getAddress());
            assertEquals(bindPort, bindSocketAddress.getPort());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
        reset(socket);

        try (Socket result = this.subject.createSocket(serviceName, connectAddress.getHostName(), connectPort, bindAddress, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedBindAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedBindAddress.capture());
            InetSocketAddress bindSocketAddress = capturedBindAddress.getValue();
            assertEquals(bindAddress, bindSocketAddress.getAddress());
            assertEquals(bindPort, bindSocketAddress.getPort());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
    }

    @Test
    public void createServerSocket() throws IOException {
        this.createServerSocket("known-service", "binding");
        this.createServerSocket("unknown-service", "unknown-service");
    }

    private void createServerSocket(String serviceName, String bindingName) throws IOException {
        ManagedServerSocketFactory factory = mock(ManagedServerSocketFactory.class);
        ServerSocket socket = mock(ServerSocket.class);

        when(this.manager.getServerSocketFactory()).thenReturn(factory);
        when(factory.createServerSocket(bindingName)).thenReturn(socket);

        try (ServerSocket result = this.subject.createServerSocket(serviceName)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());
        }
        reset(socket);

        int bindPort = 1;

        try (ServerSocket result = this.subject.createServerSocket(serviceName, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedAddress.capture(), eq(SocketFactory.DEFAULT_BACKLOG));
            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket);

        int backlog = 10;

        try (ServerSocket result = this.subject.createServerSocket(serviceName, bindPort, backlog)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedAddress.capture(), eq(backlog));
            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket);

        InetAddress bindAddress = InetAddress.getLoopbackAddress();

        try (ServerSocket result = this.subject.createServerSocket(serviceName, bindPort, backlog, bindAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedAddress.capture(), eq(backlog));
            InetSocketAddress address = capturedAddress.getValue();
            assertEquals(bindAddress, address.getAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket);
    }

    @Test
    public void createDatagramSocket() throws IOException {
        this.createDatagramSocket("known-service", "binding");
        this.createDatagramSocket("unknown-service", "unknown-service");
    }

    private void createDatagramSocket(String serviceName, String bindingName) throws IOException {
        DatagramSocket socket = mock(DatagramSocket.class);

        when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(0, address.getPort());
        }
        reset(socket, this.manager);

        int bindPort = 1;
        when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        InetAddress bindAddress = InetAddress.getLocalHost();
        when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, bindPort, bindAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());

            InetSocketAddress address = capturedAddress.getValue();
            assertSame(bindAddress, address.getAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        when(this.manager.createDatagramSocket(eq(bindingName))).thenReturn(socket);

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, null)) {
            assertSame(socket, result);
        }
        reset(socket, this.manager);

        SocketAddress socketAddress = new InetSocketAddress(bindAddress, bindPort);
        when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, socketAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<SocketAddress> capturedAddress = ArgumentCaptor.forClass(SocketAddress.class);
            verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());

            assertSame(socketAddress, capturedAddress.getValue());
        }
    }

    @Test
    public void createMulticastSocket() throws IOException {
        this.createMulticastSocket("known-service", "binding");
        this.createMulticastSocket("unknown-service", "unknown-service");
    }

    private void createMulticastSocket(String serviceName, String bindingName) throws IOException {
        MulticastSocket socket = mock(MulticastSocket.class);

        when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(0, address.getPort());
        }
        reset(socket, this.manager);

        int bindPort = 1;
        when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        InetAddress bindAddress = InetAddress.getLocalHost();
        when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, bindPort, bindAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());

            InetSocketAddress address = capturedAddress.getValue();
            assertSame(bindAddress, address.getAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        when(this.manager.createMulticastSocket(eq(bindingName))).thenReturn(socket);

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, null)) {
            assertSame(socket, result);
        }
        reset(socket, this.manager);

        SocketAddress socketAddress = new InetSocketAddress(bindAddress, bindPort);
        when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, socketAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<SocketAddress> capturedAddress = ArgumentCaptor.forClass(SocketAddress.class);
            verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());

            assertSame(socketAddress, capturedAddress.getValue());
        }
    }

    @Test
    public void closeSocket() throws IOException {

        Socket socket = mock(Socket.class);

        this.subject.close(socket);

        verify(socket).close();
    }

    @Test
    public void closeServerSocket() throws IOException {

        ServerSocket socket = mock(ServerSocket.class);

        this.subject.close(socket);

        verify(socket).close();
    }

    @Test
    public void closeDatagramSocket() {

        DatagramSocket socket = mock(DatagramSocket.class);

        this.subject.close(socket);

        verify(socket).close();
    }

    @Test
    public void closeMulticastSocket() {

        MulticastSocket socket = mock(MulticastSocket.class);

        this.subject.close(socket);

        verify(socket).close();
    }
}
