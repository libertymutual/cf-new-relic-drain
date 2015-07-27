/*
 * Copyright 2015, Liberty Mutual Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmig.cf.metrics.newrelic;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.newrelic.agent.deps.com.google.common.collect.Lists;
import com.newrelic.agent.deps.com.google.common.collect.Maps;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.InventoryNavigator;

/**
 * @author David Ehringer (n0119737)
 */
@Component
@ConditionalOnBean(InventoryNavigator.class)
public class VSphereMetricsForwarder {

    private static final Logger LOG = LoggerFactory.getLogger(VSphereMetricsForwarder.class);

    private final InventoryNavigator inventoryNavigator;
    private final Insights insights;
    private final String cfInstanceName;
    private final List<String> datastores;

    @Autowired
    public VSphereMetricsForwarder(InventoryNavigator inventoryNavigator,
            @Value("${cf.instance.Name:default}") String cfInstanceName,
            @Value("${datastores:}") String[] datastores) {
        this.inventoryNavigator = inventoryNavigator;
        this.cfInstanceName = cfInstanceName;
        this.insights = NewRelic.getAgent().getInsights();
        this.datastores = Lists.newArrayList(datastores);
    }

    @Scheduled(fixedRateString = "${collection.interval:30000}")
    public void forwardMetricsToInsights() throws InvalidProperty, RuntimeFault, RemoteException {
        long start = startTiming();
        int datastoreCount = 0;
        for (String name : datastores) {
            Datastore datastore = (Datastore) inventoryNavigator.searchManagedEntity("Datastore",
                    name);
            if (datastore != null) {
                Map<String, Object> attributes = Maps.newHashMap();
                attributes.put("platform_instance", cfInstanceName);
                attributes.put("type", "datastore");
                attributes.put("name", name);
                attributes.put("capacity", bytesToGb(datastore.getSummary().getCapacity()));
                attributes.put("free_space", bytesToGb(datastore.getSummary().getFreeSpace()));
                attributes.put("uncommitted", bytesToGb(datastore.getSummary().getUncommitted()));
                insights.recordCustomEvent("cf_iaas_metrics", attributes);
                datastoreCount++;
            } else {
                LOG.error("Unable to find datastore {}", name);
            }
        }
        endTiming(start, datastoreCount);
    }

    private Object bytesToGb(long bytes) {
        return bytes / 1024 / 1024 / 1024;
    }

    private long startTiming() {
        LOG.info("Starting vSphere metrics collection");
        return System.currentTimeMillis();
    }

    private void endTiming(long start, int datastoreCount) {
        LOG.info("vSphere metrics collection completed in {} ms for {} datastores.",
                (System.currentTimeMillis() - start), datastoreCount);
    }
}
