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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lmig.cf.metrics.opsmetrics.JmxMetric;
import com.lmig.cf.metrics.opsmetrics.OpsMetrics;
import com.lmig.cf.metrics.opsmetrics.OpsMetricsException;
import com.lmig.cf.metrics.opsmetrics.OpsMetricsMetric;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;

/**
 * @author David Ehringer
 */
@Component
public class OpsMetricsForwarder {

    private static final Logger LOG = LoggerFactory.getLogger(OpsMetricsForwarder.class);

    private final OpsMetrics opsMetrics;
    private final Insights insights;
    private final String cfInstanceName;

    @Autowired
    public OpsMetricsForwarder(OpsMetrics opsMetrics,
            @Value("${cf.instance.Name:default}") String cfInstanceName) {
        this.opsMetrics = opsMetrics;
        this.cfInstanceName = cfInstanceName;
        this.insights = NewRelic.getAgent().getInsights();
    }

    @Scheduled(fixedRateString = "${collection.interval:30000}")
    public void forwardOpsMetricsToInsights() {
        long start = startTiming();
        try {
            List<JmxMetric> jmxMetrics = opsMetrics.getMetrics();
            for (JmxMetric jmxMetric : jmxMetrics) {
                OpsMetricsMetric opsMetric = OpsMetricsMetric.from(jmxMetric, cfInstanceName);
                LOG.debug("Recording {}", opsMetric);
                insights.recordCustomEvent(opsMetric.getType(), opsMetric.getAttributes());
            }
            endTiming(start, jmxMetrics);
        } catch (OpsMetricsException e) {
            LOG.error("Unable to forward Ops Metrics metrics to New Relic Insights", e);
        }
    }

    private long startTiming() {
        LOG.info("Starting Ops Metrics metrics collection");
        return System.currentTimeMillis();
    }

    private void endTiming(long start, List<JmxMetric> jmxMetrics) {
        LOG.info("Ops Metrics metrics collection completed in {} ms. {} metrics reported.",
                (System.currentTimeMillis() - start), jmxMetrics.size());
    }
}
