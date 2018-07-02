/**
 * Copyright 2018 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dynatrace.openkit.core.communication;

import com.dynatrace.openkit.core.SessionImpl;
import com.dynatrace.openkit.protocol.HTTPClient;
import com.dynatrace.openkit.protocol.StatusResponse;
import com.dynatrace.openkit.providers.HTTPClientProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BeaconSendingFlushSessionsStateTest {


    private BeaconSendingContext mockContext;
    private SessionWrapper mockSession1Open;
    private SessionWrapper mockSession2Open;
    private SessionWrapper mockSession3Closed;

    @Before
    public void setUp() {

        mockSession1Open = mock(SessionWrapper.class);
        mockSession2Open = mock(SessionWrapper.class);
        mockSession3Closed = mock(SessionWrapper.class);
        when(mockSession1Open.getSession()).thenReturn(mock(SessionImpl.class));
        when(mockSession2Open.getSession()).thenReturn(mock(SessionImpl.class));
        when(mockSession3Closed.getSession()).thenReturn(mock(SessionImpl.class));
        when(mockSession1Open.isDataSendingAllowed()).thenReturn(true);
        when(mockSession2Open.isDataSendingAllowed()).thenReturn(true);
        when(mockSession3Closed.isDataSendingAllowed()).thenReturn(true);

        StatusResponse mockResponse = mock(StatusResponse.class);

        HTTPClient mockHttpClient = mock(HTTPClient.class);
        when(mockHttpClient.sendStatusRequest()).thenReturn(mockResponse);

        mockContext = mock(BeaconSendingContext.class);
        when(mockContext.getHTTPClient()).thenReturn(mockHttpClient);
        when(mockContext.getAllOpenAndConfiguredSessions()).thenReturn(Arrays.asList(mockSession1Open, mockSession2Open));
        when(mockContext.getAllFinishedAndConfiguredSessions()).thenReturn(Arrays.asList(mockSession3Closed,
            mockSession2Open, mockSession1Open));
    }

    @Test
    public void toStringReturnsTheStateName() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();

        // then
        assertThat(target.toString(), is(equalTo("FlushSessions")));
    }

    @Test
    public void aBeaconSendingFlushSessionsStateIsNotATerminalState() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();

        // verify that BeaconSendingCaptureOffState is not a terminal state
        assertThat(target.isTerminalState(), is(false));
    }

    @Test
    public void aBeaconSendingFlushSessionsStateHasTerminalStateBeaconSendingTerminalState() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();

        AbstractBeaconSendingState terminalState = target.getShutdownState();
        // verify that terminal state is BeaconSendingTerminalState
        assertThat(terminalState, is(instanceOf(BeaconSendingTerminalState.class)));
    }

    @Test
    public void aBeaconSendingFlushSessionsStateTransitionsToTerminalStateWhenDataIsSent() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();

        // when
        target.doExecute(mockContext);

        // verify transition to terminal state
        verify(mockContext, times(1)).setNextState(org.mockito.Matchers.any(BeaconSendingTerminalState.class));
    }

    @Test
    public void aBeaconSendingFlushSessionsClosesOpenSessions() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();

        // when
        target.doExecute(mockContext);

        // verify that open sessions are closed
        verify(mockSession1Open, times(1)).end();
        verify(mockSession2Open, times(1)).end();
    }

    @Test
    public void aBeaconSendingFlushSessionStateSendsAllOpenAndClosedBeacons() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();

        // when
        target.doExecute(mockContext);

        // verify that beacons are sent
        verify(mockSession1Open, times(1)).sendBeacon(org.mockito.Matchers.any(HTTPClientProvider.class));
        verify(mockSession2Open, times(1)).sendBeacon(org.mockito.Matchers.any(HTTPClientProvider.class));
        verify(mockSession3Closed, times(1)).sendBeacon(org.mockito.Matchers.any(HTTPClientProvider.class));
    }

    @Test
    public void aBeaconSendingFlushSessionStateDoesNotSendIfSendingIsNotAllowed() {

        // given
        BeaconSendingFlushSessionsState target = new BeaconSendingFlushSessionsState();
        when(mockSession1Open.isDataSendingAllowed()).thenReturn(false);
        when(mockSession2Open.isDataSendingAllowed()).thenReturn(false);
        when(mockSession3Closed.isDataSendingAllowed()).thenReturn(false);

        // when
        target.doExecute(mockContext);

        // verify that beacons are not sent, but cleared
        verify(mockSession1Open, times(0)).sendBeacon(org.mockito.Matchers.any(HTTPClientProvider.class));
        verify(mockSession2Open, times(0)).sendBeacon(org.mockito.Matchers.any(HTTPClientProvider.class));
        verify(mockSession3Closed, times(0)).sendBeacon(org.mockito.Matchers.any(HTTPClientProvider.class));
        verify(mockSession1Open, times(1)).clearCapturedData();
        verify(mockSession2Open, times(1)).clearCapturedData();
        verify(mockSession3Closed, times(1)).clearCapturedData();
    }
}
