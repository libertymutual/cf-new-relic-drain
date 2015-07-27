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
package com.lmig.cf.metrics.opsmetrics;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.lmig.cf.metrics.opsmetrics.OpsMetricsMetric;

/**
 * @author David Ehringer
 */
public class OpsMetricsMetricTest {

    @Test
    public void vmMetrics(){
        String rawMetricName = "org.cloudfoundry:deployment=cf-ff8aaad5ee70d9fd796b,job=dea-partition-e4843d06a9805bbe56ae,index=9,ip=null";
        Number rawMetricValue = 0.2;
        String rawMetricType = "system.cpu.user";
        
        OpsMetricsMetric metric= OpsMetricsMetric.from(rawMetricName, rawMetricValue, rawMetricType, "sandbox");

        assertThat(metric.getAttributes().get("platform_instance"), is("sandbox"));
        assertThat(metric.getAttributes().get("value"), is(rawMetricValue.floatValue()));
        assertThat(metric.getType(), is("cf_vm_metrics"));
        assertThat(metric.getAttributes().get("ip"), is("null"));
        assertThat(metric.getAttributes().get("index"), is("9"));
        assertThat(metric.getAttributes().get("attribute"), is("system.cpu.user"));
        assertThat(metric.getAttributes().get("job"), is("dea-partition-e4843d06a9805bbe56ae"));
        assertThat(metric.getAttributes().get("deployment"), is("cf-ff8aaad5ee70d9fd796b"));
    }

    @Test
    public void elastricRuntimeDeaMetric(){
        String rawMetricName = "org.cloudfoundry:deployment=untitled_dev,job=DEA,index=10,ip=10.187.112.67";
        Number rawMetricValue = 0.0;
        String rawMetricType = "log_count[level=fatal,stack=lucid64]";
        
        OpsMetricsMetric metric= OpsMetricsMetric.from(rawMetricName, rawMetricValue, rawMetricType, "sandbox");

        assertThat(metric.getAttributes().get("platform_instance"), is("sandbox"));
        assertThat(metric.getAttributes().get("value"), is(rawMetricValue.floatValue()));
        assertThat(metric.getType(), is("cf_elastic_runtime_metrics"));
        assertThat(metric.getAttributes().get("ip"), is("10.187.112.67"));
        assertThat(metric.getAttributes().get("index"), is("10"));
        assertThat(metric.getAttributes().get("stack"), is("lucid64"));
        assertThat(metric.getAttributes().get("level"), is("fatal"));
        assertThat(metric.getAttributes().get("attribute"), is("log_count"));
        assertThat(metric.getAttributes().get("job"), is("DEA"));
        assertThat(metric.getAttributes().get("deployment"), is("untitled_dev"));
    }

    @Test
    public void elasticRuntimeRouterMetric(){
        String rawMetricName = "org.cloudfoundry:deployment=untitled_dev,job=Router,index=1,ip=10.187.115.254";
        Number rawMetricValue = 45.0;
        String rawMetricType = "router.responses[component=app,dea_index=4,status=5xx]";
        
        OpsMetricsMetric metric= OpsMetricsMetric.from(rawMetricName, rawMetricValue, rawMetricType, "sandbox");

        assertThat(metric.getAttributes().get("platform_instance"), is("sandbox"));
        assertThat(metric.getAttributes().get("value"), is(rawMetricValue.floatValue()));
        assertThat(metric.getType(), is("cf_elastic_runtime_metrics"));
        assertThat(metric.getAttributes().get("ip"), is("10.187.115.254"));
        assertThat(metric.getAttributes().get("index"), is("1"));
        assertThat(metric.getAttributes().get("component"), is("app"));
        assertThat(metric.getAttributes().get("dea_index"), is("4"));
        assertThat(metric.getAttributes().get("status"), is("5xx"));
        assertThat(metric.getAttributes().get("attribute"), is("router.responses"));
        assertThat(metric.getAttributes().get("job"), is("Router"));
        assertThat(metric.getAttributes().get("deployment"), is("untitled_dev"));
    }
    
}
