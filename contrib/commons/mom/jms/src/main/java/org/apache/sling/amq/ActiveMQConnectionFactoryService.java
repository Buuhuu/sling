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

import javax.jms.ConnectionFactory;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.jms.ConnectionFactoryService;
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
 * Creates a ConnectionFactoryService that makes a pooled  JMS ConnectionFactory available to consumers. The implementation
 * of JMS is provided by ActiveMQ in this instances. If the component is left un-configured it will connection to vm://localhost:6161.
 * If no server is present at that address, the component will create a standalone ActiveMQ broker on startup. Without additional
 * configuration that AMQ Broker will operate standalone. With configuration it is possible to configure the broker to become
 * a member of a multi master AMQ Broker network. Alternatively if a dedicated AMQ Broker is required the jms.brokerUrl configuration
 * setting should be adjusted to point to that broker.
 * <p>
 * This component works OOTB and in Unit tests with no further action.
 * <p>
 * The jms.brokerUrl allows configuration of the broker in any way that ActiveMQ allows, including xbean and broker.
 * <p>
 * <p>
 * Available URI patterns.
 * <p>
 * xbean:org/apache/sling/amq/activemq.xml will result in the Broker searching for org/apache/sling/amq/activemq.xml in
 * the classpath and using that to configure the Borker, see http://activemq.apache.org/xml-configuration.html for details
 * of the format. See that location for an example of the default configuration.
 * <p>
 * <p>
 * <p>
 * broker:tcp://localhost:61616 will create a broker on localhost port 61616 using the URI configuration format.
 * See http://activemq.apache.org/broker-configuration-uri.html and http://activemq.apache.org/broker-uri.html for
 * details of the format.
 * <p>
 * properties:/foo/bar.properties uses a properties file as per http://activemq.apache.org/broker-properties-uri.html
 */
@Component(immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = ConnectionFactoryService.class)
@Designate(ocd = ActiveMQConnectionFactoryService.Config.class)
public class ActiveMQConnectionFactoryService implements ConnectionFactoryService {

    public static final String DEFAULT_BROKER_URI = "vm://localhost:61616?broker.useShutdownHook=false";

    @ObjectClassDefinition(name = "Apache Sling Active MQ Connection Factory",
            description = "Connection factory for Active MQ")
    public @interface Config {
        // Where the broker is configured out of the box, the shutdown hook must be disabled.
        // so that the deactivate method can perform the shutdown.
        // This assumes that OSGi does shutdown properly.
        @AttributeDefinition(name = "Connection URI", description = "The URI to the broker. You have to add ?create=false or in case of"
                + " failover:() protocol ?nested.create=false when jms_brokerConfigurationUri is set.")
        String jms_brokerUri() default DEFAULT_BROKER_URI;

        @AttributeDefinition(name = "Broker Configuration URI", description = "The URI to the broker configuration, leave empty when it "
                + "should be created from the brokerUri.")
        String jms_brokerConfigurationUri() default "";
    }

    private final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionFactoryService.class);
    private PooledConnectionFactory pooledConnectionFactory;
    private BrokerService brokerService;

    @Activate
    public void activate(Config config) {
        // this is to prevent xbean failing to load classpath resources (spring sucks)
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            BrokerFactory.setStartDefault(false); // disable default autostart
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            // if configured that has to happen before the ConnectionFactory gets created
            if (StringUtils.isNotEmpty(config.jms_brokerConfigurationUri())) {
                brokerService = BrokerFactory.createBroker(config.jms_brokerConfigurationUri(), false);
                // always start the BrokerService async to not block FelixStartLevel thread
                brokerService.setStartAsync(true);
                // and always without shutdownHook
                brokerService.setUseShutdownHook(false);
                // and definitively never exit system on shutdown
                brokerService.setSystemExitOnShutdown(false);
                // finally start the brokerService
                brokerService.start();
            }

            pooledConnectionFactory = new PooledConnectionFactory(config.jms_brokerUri());
            // When the bundle is configured to start a BrokerService using org.apache.sling.amq.ActiveMQBrokerServiceFactoryService startup
            // might hang here as the ConnectionFactory tries to establish a connection before the broker has been started yet.
            pooledConnectionFactory.setCreateConnectionOnStartup(false);
            pooledConnectionFactory.start();
        } catch (final Exception ex) {
            LOGGER.error("Failed to create and start broker {}", config.jms_brokerUri(), ex);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Deactivate
    public void deactivate() {
        LOGGER.info("Stopping ActiveMQ Pooled connection factory");
        pooledConnectionFactory.stop();
        pooledConnectionFactory = null;

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

    @Override
    public ConnectionFactory getConnectionFactory() {
        return pooledConnectionFactory;
    }
}