/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.amq;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO:
 *
 * @author Dirk Rudolph <dirk.rudolph@netcentric.biz>
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ActiveMQBrokerServiceFactoryService.Config.class)
public class ActiveMQBrokerServiceFactoryService {

    @ObjectClassDefinition(name = "Apache Sling Active MQ BrokerService Factory",
            description = "BrokerService factory for Active MQ")
    public @interface Config {
        // Where the broker is configured out of the box, the shutdown hook must be disabled.
        // so that the deactivate method can perform the shutdown.
        // This assumes that OSGi does shutdown properly.
        @AttributeDefinition(name = "Broker URI", description = "The URI to the broker.")
        String jms_brokerUri();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQBrokerServiceFactoryService.class);
    private BrokerService brokerService;

    @Activate
    public void activate(ActiveMQConnectionFactoryService.Config config) {
        // this is to prevent xbean failing to load classpath resources (spring sucks)
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            BrokerFactory.setStartDefault(false); // disable default autostart

            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            brokerService = BrokerFactory.createBroker(config.jms_brokerUri(), false);
            // always start the BrokerService async to not block FelixStartLevel thread
            brokerService.setStartAsync(true);
            // and always without shutdownHook
            brokerService.setUseShutdownHook(false);
            // and definitively never exit system on shutdown
            brokerService.setSystemExitOnShutdown(false);
            // finally start the brokerService
            brokerService.start();
        } catch (final Exception ex) {
            LOGGER.error("Failed to create and start broker {}", config.jms_brokerUri(), ex);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Deactivate
    public void deactivate() {
        if (brokerService != null) {
            try {
                brokerService.stop();
            } catch (final Exception ex) {
                LOGGER.error("Failed to tear down broker {}", brokerService.getBrokerName(), ex);
            } finally {
                brokerService = null;
            }
        }
    }
}
