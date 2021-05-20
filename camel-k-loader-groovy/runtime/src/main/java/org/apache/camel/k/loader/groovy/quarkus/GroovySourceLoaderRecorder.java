/*
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
package org.apache.camel.k.loader.groovy.quarkus;

import java.io.Reader;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.StartupStep;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.dsl.groovy.common.GroovyDSL;
import org.apache.camel.dsl.groovy.GroovyRoutesBuilderLoader;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.spi.StartupStepRecorder;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

@Recorder
public class GroovySourceLoaderRecorder {
    public RuntimeValue<RoutesBuilderLoader> createLoader() {
        return new RuntimeValue<>(new Loader());
    }

    public static class Loader extends GroovyRoutesBuilderLoader {
        public static final String EXTENSION = "groovy";

        private StartupStepRecorder recorder;

        @Override
        protected void doBuild() throws Exception {
            super.doBuild();

            if (getCamelContext() != null) {
                this.recorder = getCamelContext().adapt(ExtendedCamelContext.class).getStartupStepRecorder();
            }
        }

        @ManagedAttribute(description = "Supported file extension")
        @Override
        public String getSupportedExtension() {
            return EXTENSION;
        }


        @Override
        public RoutesBuilder loadRoutesBuilder(Resource resource) throws Exception {
            StartupStep step = recorder != null
                ? recorder.beginStep(GroovyRoutesBuilderLoader.class, resource.getLocation(), "Compiling RouteBuilder")
                : null;

            try {
                return EndpointRouteBuilder.loadEndpointRoutesBuilder(resource, this::load);
            } finally {
                if (recorder != null) {
                    recorder.endStep(step);
                }
            }
        }

        private void load(Reader reader, EndpointRouteBuilder builder) {
            ImportCustomizer ic = new ImportCustomizer();
            ic.addStarImports("org.apache.camel");
            ic.addStarImports("org.apache.camel.spi");

            CompilerConfiguration cc = new CompilerConfiguration();
            cc.addCompilationCustomizers(ic);
            cc.setScriptBaseClass(DelegatingScript.class.getName());

            ClassLoader cl = builder.getContext().getApplicationContextClassLoader() != null
                ? builder.getContext().getApplicationContextClassLoader()
                : Thread.currentThread().getContextClassLoader();

            GroovyShell sh = new GroovyShell(cl, new Binding(), cc);
            DelegatingScript script = (DelegatingScript) sh.parse(reader);

            // set the delegate target
            script.setDelegate(new GroovyDSL(builder));
            script.run();
        }
    }
}
