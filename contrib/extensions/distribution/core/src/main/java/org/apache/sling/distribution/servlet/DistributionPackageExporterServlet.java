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
package org.apache.sling.distribution.servlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.transport.impl.HttpTransportUtils;
import org.apache.sling.distribution.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle fetching of distribution content.
 */
@SlingServlet(resourceTypes = DistributionResourceTypes.EXPORTER_RESOURCE_TYPE, methods = "POST")
public class DistributionPackageExporterServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());



    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String operation = request.getParameter("operation");

        try {
            if ("delete".equals(operation)) {

                deletePackage(request, response);

            } else if ("fetch".equals(operation)) {

                exportOnePackage(request, response, false);

            } else {

                exportOnePackage(request, response, true);
            }

        } catch (Throwable t) {
            response.setStatus(503);
            log.error("error while exporting package", t);
        }
    }


    protected void exportOnePackage(SlingHttpServletRequest request, SlingHttpServletResponse response, boolean delete)
            throws ServletException, IOException {

        DistributionPackageExporter distributionPackageExporter = request
                .getResource()
                .adaptTo(DistributionPackageExporter.class);

        final long start = System.currentTimeMillis();

        response.setContentType(ContentType.APPLICATION_OCTET_STREAM.toString());

        DistributionRequest distributionRequest = RequestUtils.fromServletRequest(request);
        ResourceResolver resourceResolver = request.getResourceResolver();

        int consumed = 0;
        int fetched = 0;
        try {
            // get all items
            List<DistributionPackage> distributionPackages = distributionPackageExporter.exportPackages(resourceResolver, distributionRequest);
            fetched = distributionPackages.size();

            if (distributionPackages.size() > 0) {
                log.info("{} package(s) available for fetching", distributionPackages.size());

                for (DistributionPackage distributionPackage : distributionPackages) {
                    if (distributionPackage != null) {
                        consumed++;
                        InputStream inputStream = null;
                        int bytesCopied = -1;
                        try {
                            response.addHeader(HttpTransportUtils.HEADER_DISTRIBUTION_ORIGINAL_ID, distributionPackage.getId());

                            inputStream = distributionPackage.createInputStream();

                            bytesCopied = IOUtils.copy(inputStream, response.getOutputStream());
                        } finally {
                            IOUtils.closeQuietly(inputStream);
                        }

                        String packageId = distributionPackage.getId();
                        if (delete) {
                            // delete the package permanently
                            distributionPackage.delete();
                        }


                        // everything ok
                        response.setStatus(200);
                        log.info("exported package {} was sent (and deleted={}), bytes written {}", new Object[] { packageId, delete, bytesCopied });
                    } else {
                        log.warn("fetched a null package");
                    }
                }

                long end = System.currentTimeMillis();
                log.info("Processed distribution export request in {} ms: : consumed {} of {}", new Object[]{end - start, consumed, fetched});
            } else {
                response.setStatus(204);
                log.debug("nothing to fetch");
            }

        } catch (Throwable e) {
            response.setStatus(503);
            log.error("error while exporting package", e);
        }
    }



    void deletePackage(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        DistributionPackageExporter distributionPackageExporter = request
                .getResource()
                .adaptTo(DistributionPackageExporter.class);

        ResourceResolver resourceResolver = request.getResourceResolver();


        String id = request.getParameter("id");

        DistributionPackage distributionPackage = distributionPackageExporter.getPackage(resourceResolver, id);

        if (distributionPackage != null) {
            distributionPackage.delete();
            log.info("exported package {} was deleted", distributionPackage.getId());

            response.setStatus(200);
        } else {
            response.setStatus(204);
            log.debug("nothing to delete {}", id);
        }


    }

}
