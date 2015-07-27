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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Ehringer
 */
public class OpsMetricsMetric {

    private static final Logger LOG = LoggerFactory.getLogger(OpsMetricsMetric.class);

    private static final Pattern METRIC_NAME_PATTERN = Pattern
            .compile("^org.cloudfoundry/deployment=(.*)/job=(.*)/index=(\\d+)/ip=(.*)$");

    private String deployment;
    private String job;
    private String index;
    private String ip;
    private String attribute;
    private float value;
    private String cfInstanceName;
    private Map<String, Object> nestedAttributes = new HashMap<String, Object>();

    public String getType() {
        String type = "cf_vm_metrics";
        if ("untitled_dev".equalsIgnoreCase(deployment)) {
            type = "cf_elastic_runtime_metrics";
        }
        return type;
    }

    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("platform_instance", cfInstanceName);
        attributes.put("deployment", deployment);
        attributes.put("job", job);
        attributes.put("index", index);
        attributes.put("ip", ip);
        attributes.put("attribute", attribute);
        attributes.put("value", value);
        attributes.putAll(nestedAttributes);
        return attributes;
    }

    public static OpsMetricsMetric from(JmxMetric jmxMetric, String cfInstanceName) {
        return from(jmxMetric.getName(), jmxMetric.getValue(), jmxMetric.getValueType(), cfInstanceName);
    }
    
    public static OpsMetricsMetric from(String name, Number value, String valueType, String cfInstanceName) {
        OpsMetricsMetric metric = new OpsMetricsMetric();
        metric.cfInstanceName = cfInstanceName;
        
        String metricName = name.replaceAll(":", "/").replaceAll(",", "/");
        Matcher matcher = METRIC_NAME_PATTERN.matcher(metricName);
        if (matcher.find()) {
            metric.deployment = matcher.group(1);
            metric.job = matcher.group(2);
            metric.index = matcher.group(3);
            metric.ip = matcher.group(4);
            metric.attribute = valueType;
            metric.value = value.floatValue();
            metric.parseAttributeName(valueType);
        } else {
            LOG.error("Unknown metric named '{}'", name);
        }
        return metric;
    }

    /**
     * Examples: router.responses[component=app,dea_index=7,status=3xx]
     * router.responses
     */
    private void parseAttributeName(String name) {
        Pattern pattern = Pattern.compile("^(.*?)(\\[(.*)\\])?$");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            nestedAttributes.put("attribute", matcher.group(1));
            if (matcher.group(3) != null) {
                parseNestedAttributes(matcher.group(3));
            }
        }
    }

    /**
     * Example: component=app,dea_index=7,status=3xx
     */
    private void parseNestedAttributes(String attributeList) {
        for (String attr : attributeList.split(",")) {
            String[] tuple = attr.split("=");
            if (tuple.length == 2) {
                nestedAttributes.put(tuple[0], tuple[1]);
            }
        }
    }

    @Override
    public String toString() {
        return "OpsMetricsMetric [getType=" + getType() + ", attributes=" + getAttributes() + "]";
    }

}
