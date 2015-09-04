/*******************************************************************************
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
 ******************************************************************************/
/*global use, properties, resource, sightly*/
use(function () {
    'use strict';

    var test = {};

    test.text = properties.get('text') ||  resource.getPath();
    test.tag = properties.get('tag') || null;
    if (test.tag != null) {
        test.startTag = '<' + test.tag + '>';
        test.endTag = '</' + test.tag + '>';
    }
    test.includeChildren = properties.get('includeChildren') || false;
    if (test.includeChildren) {
        test.children = sightly.resource.getChildren();
    }

    return test;
});