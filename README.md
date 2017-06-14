[Primer](https://primer.tools.expedia.com/dashboard/cs-media-service-ng) |
[Banzai](http://banzai.tools.expedia.com/#/workflow/cs-media-service-ng) |
Jenkins:
*master* [![Master Build Status](https://primer.builds.tools.expedia.com/buildStatus/icon?job=cs-media-service-ng-master)](https://primer.builds.tools.expedia.com/job/cs-media-service-ng-master/), 
*all* [![All Build Status](https://primer.builds.tools.expedia.com/buildStatus/icon?job=cs-media-service-ng-all)](https://primer.builds.tools.expedia.com/job/cs-media-service-ng-all/), 
*Fortity* [![Fortity Status](https://primer.builds.tools.expedia.com/buildStatus/icon?job=cs-media-service-ng-fortifyScan)](https://primer.builds.tools.expedia.com/job/cs-media-service-ng-fortifyScan/)

**Table of Contents**

* [Environments](#environments)
* [Tools](#tools)
* [Resources](#resources)
* [Security](#security)
* [Develop](#develop)


## Environments<a id="environments"></a>

| Environment | Endpoints |
|-------------|-----------|
| Test | [cs-media-service-ng.**us-west-2**.test.expedia.com](https://cs-media-service-ng.us-west-2.test.expedia.com) |
| Integration | [cs-media-service-ng.**us-west-2**.int.expedia.com](https://cs-media-service-ng.us-west-2.int.expedia.com) |
| Stress | [cs-media-service-ng.**us-west-2**.stress.expedia.com](https://cs-media-service-ng.us-west-2.stress.expedia.com) |
| Prod Non-PCI | [cs-media-service-ng.**us-west-2**.prod.expedia.com](https://cs-media-service-ng.us-west-2.prod.expedia.com) |
| Prod PCI | [cs-media-service-ng.**us-west-2**.prod-p.expedia.com](https://cs-media-service-ng.us-west-2.prod-p.expedia.com) |

## Tools<a id="tools"></a>

###### Pipeline
* [Primer Dashboard](https://primer.tools.expedia.com/dashboard/cs-media-service-ng)
* [Banzai Workflow](http://banzai.tools.expedia.com/#/workflow/cs-media-service-ng)

###### Logs
* Kibana:
  [Test](http://kibana.us-west-2.test.expedia.com/app/kibana#/discover?_g=\(refreshInterval:\(display:Off,pause:!f,value:0\),time:\(from:now-15m,mode:quick,to:now\)\)&_a=\(columns:!\(log,image\),index:'logstash-*',interval:auto,query:\(query_string:\(analyze_wildcard:!t,query:'container_name:%22*cs-media-service-ng%22'\)\),sort:!\('@timestamp',desc\)\));
  [Production PCI](http://kibana.us-west-2.prod-p.expedia.com/app/kibana#/discover?_g=\(refreshInterval:\(display:Off,pause:!f,value:0\),time:\(from:now-15m,mode:quick,to:now\)\)&_a=\(columns:!\(log,image\),index:'logstash-*',interval:auto,query:\(query_string:\(analyze_wildcard:!t,query:'container_name:%22*cs-media-service-ng%22'\)\),sort:!\('@timestamp',desc\)\))
* Splunk:
  [Test](https://splunk.us-west-2.test.expedia.com/en-US/app/search/flashtimeline?q=search%20index=app%20sourcetype%3Dcs-media-service-ng*%20earliest%3D-60m);
  [Integration](https://splunk.us-west-2.int.expedia.com/en-US/app/search/flashtimeline?q=search%20index=app%20sourcetype%3Dcs-media-service-ng*%20earliest%3D-60m);
  [Stress](https://splunk.us-west-2.stress.expedia.com/en-US/app/search/flashtimeline?q=search%20index=app%20sourcetype%3Dcs-media-service-ng*%20earliest%3D-60m);
  [Production](https://splunk.us-west-1.prod.expedia.com/en-US/app/search/flashtimeline?q=search%20index=app%20sourcetype%3Dcs-media-service-ng*%20earliest%3D-60m);
  [Production PCI](https://splunk.us-west-2.prod-p.expedia.com/en-US/app/search/flashtimeline?q=search%20index=app%20sourcetype%3Dcs-media-service-ng*%20earliest%3D-60m)

###### Monitoring
* Spring Boot Admin:
  [Test, Integration & Stress](https://cs-spring-boot-admin-service.us-west-2.int.expedia.com);
  [Production](https://cs-spring-boot-admin-service.us-west-2.prod.expedia.com);
  [Production PCI](https://cs-spring-boot-admin-service.us-west-2.prod-p.expedia.com)
* Grafana Dashboards:
  [Test](https://hubble.prod.expedia.com/dashboard/db/ewe-global-monitoring-springboot-app?var-Application=cs-media-service-ng&var-Prefix=ewe&from=now-1h&to=now&var-Datasource=aws_ewetest&var-Env=test-us-west-2&var-Server=%24__all);
  [Integration](https://hubble.prod.expedia.com/dashboard/db/ewe-global-monitoring-springboot-app?var-Application=cs-media-service-ng&var-Prefix=ewe&from=now-1h&to=now&var-Datasource=aws_ewetest&var-Env=int-us-west-2&var-Server=%24__all);
  [Stress](https://hubble.prod.expedia.com/dashboard/db/ewe-global-monitoring-springboot-app?var-Application=cs-media-service-ng&var-Prefix=ewe&from=now-1h&to=now&var-Datasource=aws_ewetest&var-Env=stress-us-west-2&var-Server=%24__all);
  [Production](https://hubble.prod.expedia.com/dashboard/db/ewe-global-monitoring-springboot-app?var-Application=cs-media-service-ng&var-Prefix=ewe&from=now-1h&to=now&var-Datasource=aws_prod_ee&var-Env=prod-us-west-2&var-Server=%24__all);
  [Production PCI](https://hubble.prod.expedia.com/dashboard/db/ewe-global-monitoring-springboot-app?var-Application=cs-media-service-ng&var-Prefix=ewe&from=now-1h&to=now&var-Datasource=aws_prod_p_ee&var-Env=prod-p-us-west-2&var-Server=%24__all);
* [Docker Registry Repository](https://docker-registry.tools.expedia.com/repositories/library/cs-media-service-ng/details)

###### Security
* [Fortify SSC Portal](https://ssc.idxlab.expedmz.com/ssc/html/ssc/index.jsp#!/search/versions?q=cs-media-service-ng)
* Vault Web Admin: [Test](https://vault-web.test.expedia.com); [Production](https://vault-web.prod.expedia.com); [Production PCI](https://vault-web.prod-p.expedia.com)

## Resources<a id="resources"></a>

Development resources and support information can be found in [DEVELOP.md](DEVELOP.md#resources).

### Deployment and Infrastructure Configuration

Deployment and infrastructure configuration can be found in [.primer/](.primer/README.md) folder.

## Security<a id="security"></a>

Security information can be found in [DEVELOP.md](DEVELOP.md#security).

## Develop<a id="develop"></a>

Development information can be found in [DEVELOP.md](DEVELOP.md).

### Build and Run<a id="build-run"></a>

```bash
mvn clean install && \
docker build -t cs-media-service-ng . && \
docker run --rm -p 8080:8080 -p 8443:8443 \
-e "ACTIVE_VERSION=$(git rev-parse HEAD)" \
-e SPRING_BOOT_ADMIN_CLIENT_ENABLED=false \
-e "EXPEDIA_ENVIRONMENT=test" \
-e "EXPEDIA_DEPLOYED_ENVIRONMENT=test" \
cs-media-service-ng
```
**Note**: If you don't have Expedia Maven development setup, copy the `settings.xml` to `~/.m2/` folder.
