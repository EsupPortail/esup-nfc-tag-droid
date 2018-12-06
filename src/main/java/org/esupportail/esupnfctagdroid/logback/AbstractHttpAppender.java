/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupnfctagdroid.logback;

import org.apache.http.HttpResponse;
import org.esupportail.esupnfctagdroid.exceptions.NfcTagDroidException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public abstract class AbstractHttpAppender<E> extends UnsynchronizedAppenderBase<E> {


    public static final String DEFAULT_LISTENER_URI = "";
    public static final String HTTP_LISTENER_PROTOCOL = "http";
    public static final String HTTPS_LISTENER_PROTOCOL = "https";
    public static final String DEFAULT_HTTP_LISTENER_PORT = "80";
    public static final String DEFAULT_HTTPS_LISTENER_PORT = "443";

    public static final String DEFAULT_LAYOUT_PATTERN = "%level;%m%n";

    protected static final Charset UTF_8 = Charset.forName("UTF-8");

    protected String hostname;
    protected String uri;
    protected String protocol;
    protected String port;
    private String endpoint;

    protected Layout<E> layout;
    protected boolean layoutCreated = false;
    private String pattern;

    @Override
    public void start() {
        ensureLayout();

        if (!this.layout.isStarted()) {
            this.layout.start();
        }

        // set protocol first, so port can be detected
        ensureProtocol();

        ensurePort();

        if (this.hostname == null) {
            throw new NfcTagDroidException("Hostname of logback HttpAppender is null !");
        }

        if (this.uri == null) {
            this.uri = DEFAULT_LISTENER_URI;
        }
        buildEndpoint();

        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (this.layoutCreated) {
            this.layout.stop();
            this.layout = null;
            this.layoutCreated = false;
        }
    }

    protected void ensureProtocol() {
        if (this.protocol == null) {
            this.protocol = HTTP_LISTENER_PROTOCOL;
            return;
        }

        if (this.protocol != HTTP_LISTENER_PROTOCOL || this.protocol != HTTPS_LISTENER_PROTOCOL) {
            addError("Invalid protocol given. Must be set to 'http' or 'https', default to 'http'");
        }
    }

    protected void ensurePort() {
        if (this.port == null) {
            if (HTTPS_LISTENER_PROTOCOL.equals(getProtocol())) {
                this.port = DEFAULT_HTTPS_LISTENER_PORT;
            }
            this.port = DEFAULT_HTTP_LISTENER_PORT;
        }
    }

    protected final void ensureLayout() {
        if (this.layout == null) {
            this.layout = createLayout();
            this.layoutCreated = true;
        }
        if (this.layout != null) {
            Context context = this.layout.getContext();
            if (context == null) {
                this.layout.setContext(getContext());
            }
        }
    }

    protected Layout<E> createLayout() {
        PatternLayout layout = new PatternLayout();
        String pattern = getPattern();
        pattern = DEFAULT_LAYOUT_PATTERN;
        layout.setPattern(pattern);

        return (Layout<E>) layout;
    }

    protected String buildEndpoint() {
        StringBuilder result = new StringBuilder();

        String protocol = removePostfix(this.protocol, "://");
        String hostname = removePostfix(this.hostname, "/");
        String uri = removePostfix(this.uri, "/");

        if (uri != "") {
            uri = uri + '/';
        }

        endpoint = result.append(protocol).append("://").append(hostname).append(uri).toString();

        return endpoint;
    }

    protected String removePostfix(String s, String postfix) {
        if (s == null) return s;
        return s.substring(0, s.length() - (s.endsWith(postfix) ? postfix.length() : 0));
    }

    protected String cleanString(String cleaned) {
        if (cleaned != null) {
            cleaned = cleaned.trim();
        }
        if ("".equals(cleaned)) {
            cleaned = null;
        }

        return cleaned;
    }

    protected String readResponseBody(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";

        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = cleanString(protocol);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = cleanString(hostname);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = cleanString(uri);
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = cleanString(port);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Layout<E> getLayout() {
        return layout;
    }

    public void setLayout(Layout<E> layout) {
        this.layout = layout;
    }

}
