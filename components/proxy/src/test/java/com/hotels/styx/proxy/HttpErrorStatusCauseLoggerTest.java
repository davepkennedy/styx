/*
  Copyright (C) 2013-2019 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.proxy;

import com.hotels.styx.api.LiveHttpRequest;
import com.hotels.styx.common.format.HttpMessageFormatter;
import com.hotels.styx.support.matchers.LoggingTestSupport;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;

import static ch.qos.logback.classic.Level.ERROR;
import static com.hotels.styx.api.HttpResponseStatus.BAD_GATEWAY;
import static com.hotels.styx.api.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static com.hotels.styx.support.matchers.LoggingEventMatcher.loggingEvent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class HttpErrorStatusCauseLoggerTest {

    private static final String FORMATTED_REQUEST = "request";

    private HttpErrorStatusCauseLogger httpErrorStatusCauseLogger;
    private LoggingTestSupport loggingTestSupport;

    @Mock
    private HttpMessageFormatter httpMessageFormatter;

    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(httpMessageFormatter.formatRequest(any(LiveHttpRequest.class))).thenReturn(FORMATTED_REQUEST);
    }

    @BeforeMethod
    public void setUp() {
        loggingTestSupport = new LoggingTestSupport(HttpErrorStatusCauseLogger.class);
        httpErrorStatusCauseLogger = new HttpErrorStatusCauseLogger(httpMessageFormatter);
    }

    @AfterMethod
    public void removeAppender() {
        loggingTestSupport.stop();
    }

    @Test
    public void logsThrowables() {
        Exception exception = new Exception("This is just a test");

        httpErrorStatusCauseLogger.proxyErrorOccurred(INTERNAL_SERVER_ERROR, exception);

        assertThat(loggingTestSupport.log(), hasItem(
                loggingEvent(
                        ERROR,
                        "Failure status=\"500 Internal Server Error\"",
                        "java.lang.Exception",
                        "This is just a test")));
    }

    @Test
    public void logsThrowablesWithStatus5xxExcluding500WithoutStackTrace() {
        Exception exception = new Exception("This is just a test");

        httpErrorStatusCauseLogger.proxyErrorOccurred(BAD_GATEWAY, exception);

        assertThat(loggingTestSupport.log(), hasItem(
                loggingEvent(
                        ERROR,
                        "Failure status=\"502 Bad Gateway\", exception=\"java.lang.Exception.*This is just a test.*\"")));
    }

    @Test
    public void logsInternalServerErrorWithRequest() {
        LiveHttpRequest request = LiveHttpRequest.get("/foo").build();
        Exception exception = new Exception("This is just a test");

        httpErrorStatusCauseLogger.proxyErrorOccurred(request, InetSocketAddress.createUnresolved("localhost", 80), INTERNAL_SERVER_ERROR, exception);

        assertThat(loggingTestSupport.log(), hasItem(
                loggingEvent(
                        ERROR,
                        "Failure status=\"500 Internal Server Error\" during request=" + FORMATTED_REQUEST + ", clientAddress=localhost:80",
                        "java.lang.Exception",
                        "This is just a test")));
    }

    @Test
    public void logsOtherExceptionsWithoutRequest() {
        LiveHttpRequest request = LiveHttpRequest.get("/foo").build();
        Exception exception = new Exception("This is just a test");

        httpErrorStatusCauseLogger.proxyErrorOccurred(request, InetSocketAddress.createUnresolved("127.0.0.1", 0), BAD_GATEWAY, exception);

        assertThat(loggingTestSupport.log(), hasItem(
                loggingEvent(
                        ERROR,
                        "Failure status=\"502 Bad Gateway\", exception=\"java.lang.Exception.*This is just a test.*\"")));
    }

}