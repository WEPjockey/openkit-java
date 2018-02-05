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

package com.dynatrace.openkit.core;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;

import com.dynatrace.openkit.api.OpenKitConstants;
import com.dynatrace.openkit.protocol.Beacon;

/**
 * Tests the {@link WebRequestTracerBaseImpl} implementation having knowledge of the code.
 */
public class WebRequestTracerURLConnectionTest {

    @Test
    public void testConstructors() {
        // create test environment
        final Beacon beacon = mock(Beacon.class);
        final ActionImpl action = mock(ActionImpl.class);
        final URLConnection urlConnection = mock(URLConnection.class);

        // test the constructor call
        final WebRequestTracerURLConnection conn = new WebRequestTracerURLConnection(beacon, action, urlConnection);
        assertThat(conn, not(nullValue()));

        // verify the correct methods being called
        verify(beacon, times(1)).createSequenceNumber();
        verify(beacon, times(1)).createTag(eq(action), eq(0));
    }

    @Test
    public void testConnectionTagging() throws MalformedURLException, IOException {
        // create test environment
        final Beacon beacon = mock(Beacon.class);
        final String tag = "Some tag";
        when(beacon.createTag(isA(ActionImpl.class), anyInt())).thenReturn(tag);
        final ActionImpl action = mock(ActionImpl.class);
        final URLConnection urlConnection = new URL("http://example.com").openConnection();

        // Check that the tag is not yet set
        String existingTag = urlConnection.getRequestProperty(OpenKitConstants.WEBREQUEST_TAG_HEADER);
        assertThat(existingTag, is(nullValue()));

        // test the constructor call
        final WebRequestTracerURLConnection conn = new WebRequestTracerURLConnection(beacon, action, urlConnection);
        assertThat(conn, not(nullValue()));

        // verify that the tag is now set
        existingTag = urlConnection.getRequestProperty(OpenKitConstants.WEBREQUEST_TAG_HEADER);
        assertThat(existingTag, not(nullValue()));
        assertThat(existingTag, is(tag));
    }
}
