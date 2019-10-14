/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.knative.http;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class KnativeHttpMain {
    public static void main(String[] args) throws Exception {
        DefaultCamelContext context = new DefaultCamelContext();

        try {
            context.disableJMX();
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("knative-http:http://0.0.0.0:8080?filter.headerName=CE-Source&filter.headerValue=CH1")
                        .convertBodyTo(String.class)
                        .to("log:ch-11?showAll=true&multiline=true")
                        .setBody().constant("Hello from CH1");
                    from("knative-http:http://0.0.0.0:8080?filter.headerName=CE-Source&filter.headerValue=CH2")
                        .convertBodyTo(String.class)
                        .to("log:ch-2?showAll=true&multiline=true")
                        .setBody().constant("Hello from CH2");
                }
            });

            context.start();

            Thread.sleep(Integer.MAX_VALUE);
        } finally {
            context.stop();
        }

    }
}