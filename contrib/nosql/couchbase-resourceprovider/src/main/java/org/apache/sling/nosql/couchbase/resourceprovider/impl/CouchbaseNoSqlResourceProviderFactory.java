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
package org.apache.sling.nosql.couchbase.resourceprovider.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.nosql.couchbase.client.CouchbaseClient;
import org.apache.sling.nosql.generic.adapter.MetricsNoSqlAdapterWrapper;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.resource.AbstractNoSqlResourceProviderFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.LoggerFactory;

/**
 * {@link ResourceProviderFactory} implementation that uses couchbase as
 * persistence.
 */
@Component(immediate = true, metatype = true,
    name="org.apache.sling.nosql.couchbase.resourceprovider.CouchbaseNoSqlResourceProviderFactory.factory.config",
    label = "Apache Sling NoSQL Couchbase Resource Provider Factory", 
    description = "Defines a resource provider factory with Couchbase persistence.", 
    configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service(value = ResourceProviderFactory.class)
@Property(name = "webconsole.configurationFactory.nameHint", 
    value = "Root paths: {" + CouchbaseNoSqlResourceProviderFactory.PROVIDER_ROOTS_PROPERTY + "}")
public final class CouchbaseNoSqlResourceProviderFactory extends AbstractNoSqlResourceProviderFactory {

    /**
     * Couchbase Client ID for Couchbase Resource Provider
     */
    public static final String COUCHBASE_CLIENT_ID = "sling-resourceprovider-couchbase";
    
    @Property(label = "Cache Key Prefix", description = "Prefix for caching keys.", value = CouchbaseNoSqlResourceProviderFactory.CACHE_KEY_PREFIX_DEFAULT)
    static final String CACHE_KEY_PREFIX_PROPERTY = "cacheKeyPrefix";
    private static final String CACHE_KEY_PREFIX_DEFAULT = "sling-resource:";

    @Property(label = "Root paths", description = "Root paths for resource provider.", cardinality = Integer.MAX_VALUE)
    static final String PROVIDER_ROOTS_PROPERTY = ResourceProvider.ROOTS;

    @Reference(target = "(" + CouchbaseClient.CLIENT_ID_PROPERTY + "=" + COUCHBASE_CLIENT_ID + ")")
    private CouchbaseClient couchbaseClient;

    @Reference
    private EventAdmin eventAdmin;

    private NoSqlAdapter noSqlAdapter;

    @Activate
    private void activate(ComponentContext componentContext, Map<String, Object> config) {
        String cacheKeyPrefix = PropertiesUtil.toString(config.get(CACHE_KEY_PREFIX_PROPERTY), CACHE_KEY_PREFIX_DEFAULT);
        NoSqlAdapter couchbaseAdapter = new CouchbaseNoSqlAdapter(couchbaseClient, cacheKeyPrefix);
        
        // enable call logging and metrics for {@link CouchbaseNoSqlAdapter}
        noSqlAdapter = new MetricsNoSqlAdapterWrapper(couchbaseAdapter, LoggerFactory.getLogger(CouchbaseNoSqlAdapter.class));
    }

    @Override
    protected NoSqlAdapter getNoSqlAdapter() {
        return noSqlAdapter;
    }

    @Override
    protected EventAdmin getEventAdmin() {
        return eventAdmin;
    }

}
