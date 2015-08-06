# Cloud Foundry New Relic Insights Metric Drain

This project forwards metrics from the [Ops Metrics][e] component in [Pivotal Cloud Foundry][d] into [New Relic Insights][c].

## Environment Variables For Configuration

### Required

| Key | Description
| --- | -----------
| `NEW_RELIC_LICENSE_KEY` | The New Relic license key for your account.
| `OPSMETRICS_HOST` | The ip of your Ops Metrics instance.
| `OPSMETRICS_USERNAME` | Username for the Ops Metrics instance.
| `OPSMETRICS_PASSWORD` | Password for the Ops Metrics instance.

### Optional

| Key | Description
| --- | -----------
| `CF_INSTANCE_NAME` | An identifier to distinguish between multiple CF environment reporting metrics to the same New Relic account. Reported with all events as `platform_instance`. Default is `default`
| `COLLECTION_INTERVAL` | The frequency in milliseconds in which metrics are reported. Default is `30000`.
| `DATASTORES` | A comma separated list of datastores to report metrics on. Used with `VSPHERE_HOST`.
| `VSPHERE_HOST` | The vSphere host used to lookup vSphere metrics when deployed on a vSphere infrastructure.
| `VSPHERE_PASSWORD` | Used with `VSPHERE_HOST` to give vSphere access.
| `VSPHERE_USERNAME` | Used with `VSPHERE_HOST` to give vSphere access.

## Metrics Format

All metrics are reported as individual Insights events. Each event consists of a name and a collection of attributes.

### Elastic Runtime Metric Attributes

Event name: `cf_elastic_runtime_metrics`

All Elastic Runtime events will have the following attributes. The examples below use the Ops Metric JMX metric:
* Name: `org.cloudfoundry:deployment=untitled_dev,job=Router,index=1,ip=10.18.115.254`
* Attribute: `router.responses[component=app,dea_index=4,status=5xx]`
* Value: `45.0`

| Name | Description/Example
| ---- | -----------
| `platform_instance` | Value specified in the `CF_INSTANCE_NAME` environment variable.
| `deployment` | `untitled_dev`
| `job` | `Router`
| `index` | `1`
| `ip` | `10.18.115.254`
| `attribute` | `router.responses`
| `value` | `45.0`

Some attributes have additional metrics embedded within the name. In the example above, in addition to reporting 
`router.responses` as the `attribute`, `component`, `dea_index`, and `status` are also reported. The specifics will vary
by JMX metric exposed by Ops Metrics.

| Name | Description/Example
| ---- | -----------
| `component` | `app`
| `dea_index` | `4`
| `status` | `5xx`


### VM Metric Attributes

Event name: `cf_vm_metrics`

All VM events will have the following attributes. The examples below use the Ops Metric JMX metric:
* Name: org.cloudfoundry:deployment=cf-ff8aaad5ee70d9fd796b,job=dea-partition-e4843d06a9805bbe56ae,index=9,ip=null
* Attribute: system.cpu.user
* Value: 0.2

| Name | Description/Example
| ---- | -----------
| `platform_instance` | Value specified in the `CF_INSTANCE_NAME` environment variable.
| `deployment` | `cf-ff8aaad5ee70d9fd796b`
| `job` | `dea-partition-e4843d06a9805bbe56ae`
| `index` | `9`
| `ip` | `null`
| `attribute` | `system.cpu.user`
| `value` | `0.2`

### vSphere Metric Attributes

Event name: `cf_iaas_metrics`

#### Datastore metrics

Only vSphere Datastore metrics are reported at this time. 

All Datastore events will have the following attributes. 

| Name | Description/Example
| ---- | -----------
| `platform_instance` | Value specified in the `CF_INSTANCE_NAME` environment variable.
| `type` | `datastore` (constant value)
| `name` | Datastore name
| `capacity` | Total capacity of datastore in GB
| `free_space` | Free space in GB
| `uncommitted` | Uncommitted space in GB

## Example Insights Queries

```
SELECT average(value), max(value) from cf_vm_metrics where deployment LIKE 'cf-%' and job LIKE 'router-%' and attribute = 'system.cpu.user' TIMESERIES

SELECT average(value), max(value) from cf_vm_metrics where deployment LIKE 'cf-%' and job LIKE 'dea-%' and attribute = 'system.cpu.user' TIMESERIES

SELECT uniqueCount(ip) as 'VMs' from cf_elastic_runtime_metrics

SELECT sum(value) FROM cf_elastic_runtime_metrics WHERE job = 'Router' and attribute = 'router.requests_per_sec' TIMESERIES AUTO
```

If you have multiple CF environments denoted by `CF_INSTANCE_NAME`, you can target metrics of a specific environment explictly by using the 
`platform_instance` attribute or by using facets (e.g. `FACET platform_instance`)

## Compatibility

This project has been tested against PCF 1.4 and 1.5.

## Deployment

In order to automate the deployment process as much as possible, the project contains a Cloud Foundry [manifest][b]. Update the manifest as required for your environment.  To deploy run the following commands:

```bash
mvn clean package
cf push
```

## Developing
The project is set up as a Maven project and doesn't have any special requirements beyond that.

## License
The project is released under version 2.0 of the [Apache License][a].

[a]: http://www.apache.org/licenses/LICENSE-2.0
[b]: manifest.yml
[c]: http://newrelic.com/insights
[d]: http://pivotal.io/platform-as-a-service/pivotal-cloud-foundry
[e]: http://docs.pivotal.io/pivotalcf/customizing/use-metrics.html
