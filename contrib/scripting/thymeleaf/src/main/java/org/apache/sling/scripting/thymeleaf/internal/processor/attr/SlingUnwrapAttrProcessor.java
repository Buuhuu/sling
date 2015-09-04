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
package org.apache.sling.scripting.thymeleaf.internal.processor.attr;

public final class SlingUnwrapAttrProcessor extends SlingNodePropertyAttrProcessor {

    public static final int ATTR_PRECEDENCE = 99;

    public static final String ATTR_NAME = "unwrap";

    public static final String NODE_PROPERTY_NAME = String.format("%s.%s", PREFIX, ATTR_NAME);

    public SlingUnwrapAttrProcessor() {
        super(ATTR_NAME);
    }

    @Override
    public int getPrecedence() {
        return ATTR_PRECEDENCE;
    }

    @Override
    protected String getNodePropertyName() {
        return NODE_PROPERTY_NAME;
    }

}
