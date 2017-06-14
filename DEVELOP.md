**Table of Contents**

* [Resources](#resources)
* [Support](#support)
* [Build and Run](#build-run)
* [Multi Environments and Regions Support](#multi-envs-regions)
* [Docker](#docker)
* [Security](#security)

## Resources<a id="resources"></a>

* [AWS Access](https://confluence/x/agMWK) - Get access to AWS Console and SSH onto EC2 instances;
* [AWS - SelfService Jenkins Jobs](https://confluence/x/QJv9JQ) - Create AWS resources, un/deploy, etc.;
* [IAM Role, User and Polocies Self Service](https://confluence/x/bjrmKg) - Tools for IAM role and policies, KMS encryption, etc.
* [ECS Canary / Blue-Green Release process](https://confluence/x/oybZIw)
* [Vault: AWS Dynamic secrets and Secure secret storage](https://confluence/x/E2PwK)
* [REST API Standard and Guidelines](https://confluence/x/tgO4GQ)

## Support<a id="support"></a>
* [Pipeline Engineering Support Portal](http://go/pipeline)
* [Cloud Forum](https://forum.tools.expedia.com/category/18/cloud?loggedin)
* [Developing Primer Applications with Docker on Amazon ECS](https://confluence/x/bgI3JQ)
* [Investigating Application Issues on Amazon ECS](https://confluence/x/0acMJ)

## Build and Run<a id="build-run"></a>

### Run as a "main" Java class

From your IDE, right-click on the "Application" class at the root of your Java package hierarchy, and run it directly. 
You should also be able to debug it as easily.

In IntelliJ, for Lombok annotations, you need to enable annotation processing, change the compiler from Ajc to Javac, 
and probably install the Lombok plugin.

Add the following line as VM option: `-Dspring.profiles.active=dev`


### As a Maven target

To run the application from command line, you may run one of the following commands after building your application:
```bash
mvn -Dspring.profiles.active=dev spring-boot:run
```

Open a browser and hit http://localhost:8080/ for service spec or http://localhost:8080/service/hello for sample API

### Generate Documentation

```bash
mvn -Dtest=ApiDocumentation test
```

### Template Update
You can update your project based on the latest version of the template by running this command:
```bash
./meka update
```
A branch named `template-update` will be created containing the proposed changes.
You can then review the changes using your IDE and keep what you need.

## Docker<a id="docker"></a>

For OS X setup instructions, see: https://ewegithub.sb.karmalab.net/EWE/docker

### How to build with Docker?

```bash
docker build -t cs-media-service-ng .
```

### How to run with Docker?

```bash
docker run --rm -e "ACTIVE_VERSION=$(git rev-parse HEAD)" -p 8080:8080 -p 8443:8443 cs-media-service-ng
```

You can optionally set these environment variables as well: `"APP_NAME=cs-media-service-ng" -e "EXPEDIA_DEPLOYED_ENVIRONMENT=dev"`.
To run in the background, use `docker run -d -e "ACTIVE_VERSION=$(git rev-parse HEAD)" -p 8080:8080 cs-media-service-ng` instead.

Open a browser and hit http://LOCAL_DOCKER_IP:8080 (e.g. http://192.168.99.100:8080)


## Multi Environments and Regions Support<a id="multi-envs-regions"></a>

To support different environments (*Prod*, *Prod PCI*) and different regions (*us-east-1*, *us-west-2*), create additional properties files.
This is useful when you need to configure different databases per environment and/or region.
By default, the template comes with:
* `application-prod.yml` - support for prod **NON** PCI
* `application-prod-p.yml` - support for prod PCI

To support prod PCI in *us-east-1* and *us-west-2*, create two additional files containing region specific properties.
* `application-prod-p-us-east-1.yml` - support Prod PCI in *us-east-1*
* `application-prod-p-us-west-2.yml` - support Prod PCI in *us-west-2*

## Security<a id="security"></a>

### How to enable Vault
Vault is supported out of the box, **but it's disabled by default**. First, on-board your application using the instruction on the
[Onboarding](https://confluence/display/POS/Onboarding+Applications+into+Vault) page.

To enable it, update the file `src/main/resources/bootstrap.yml` and set property `spring.cloud.vault.enabled: true`.
After enabled, the properties defined in Vault will be accessible through the current Spring way:
 * ConfigurationProperties
 * Environment
 * `@Value`

### How to support Vault on `dev` Profile
Run the following command to generate the token locally using your SEA account:
```bash
./meka vault -a
[INFO]: SEA\your_name password (will be masked): ************
```
The token will be put in file `.vault_token`.

### How to enable HTTPS
HTTPS is supported but disabled by default. Follow these instructions to enable it. 

Enable application HTTPS support by uncommenting configuration in `src/main/resources/application.yml` file between these lines:
```yaml
##  < HTTPS: Uncomment this section enable HTTPS support...
...
##  HTTPS >
```

Enable HTTPS on Spring Boot Admin Client Endpoints, changing `http://` to `https://` and `${HOST_PORT}` to `${HOST_SECURE_PORT}`:
```yml
spring:
  boot:
    admin:
      url: https://cs-spring-boot-admin-service.us-west-2.int.expedia.com
      auto-deregistration: true
      client:
        name: cs-media-service-ng
        enabled: false
        # Env variables HOST_IP and HOST_PORT are only available when running inside Docker.
        health-url: https://${HOST_IP}:${HOST_SECURE_PORT}/health      # <== Change here...
        service-url: https://${HOST_IP}:${HOST_SECURE_PORT}/           # <== ...and here...
        management-url: https://${HOST_IP}:${HOST_SECURE_PORT}/        # <== ...and here. Done!
```

Enable HTTPS on the ELB in `.primer/deployment.json` file.
```json
{
    "global": {
        "app_type": "cs-springboot",
        
        "app_ssl_port": 8443,
        "app_ssl_protocol": "HTTPS",
        "elb_ssl_protocol": "HTTPS",
        "loadbalancer": {
          "ping_protocol": "HTTPS",
          "http_enabled": false,
          "https_enabled": true
        }
    }
}
```

### How to create new SSL/TLS certificate
Use [ServiceNow](https://expedia.service-now.com/nav_to.do) to request a new certificate, going in Certificate Management / Request a New Certificate. 
Once the process is complete, you should be able to download a ZIP package containing the certificate material. 
More information available [here](https://expedia.service-now.com/kb_view.do?sysparm_article=KB0016390) or 
[here](https://confluence/dosearchsite.action?queryString=Requesting+a+New+SSL+Certificate).

### How to import SSL/TLS certificate to AWS
To upload your new certificate to AWS use these 
[jobs](https://confluence/display/PIP/AWS+%3A+SelfService+Jenkins+Jobs#AWS:SelfServiceJenkinsJobs-SSLCertificates), following instructions 
on corresponding job description. For example, uploading a certificate for Prod-PCI using package `cs-media-service-ng.prod-p.expedia.com.zip` 
downloaded from ServiceNow:

* **CERT_NAME**: `cs-media-service-ng.prod-p.expedia.com`;
* **cert.pem**: `cs-media-service-ng.prod-p.expedia.com.pem` file;
* **private-key.pem**: `netscaler.txt` file;
* **chain.pem**: `netscaler.txt` file.

### How to add SSL/TLS certificates to key store
Use the script `meka certificates` to add internally signed certificate to `src/main/resources/keystore.jks`. For example:

```bash
./meka certificates -c import \
    -s cs-media-service-ng.test.expedia.com.zip \
    -a cs-media-service-ng.test.expedia.com \
    -p KEY_STORE_PASSWORD
```
Here we are `import`ing the certificate package `cs-media-service-ng.test.expedia.com.zip` into default the key store, providing it's password with `-p`.
You will find it in `src/main/resources/application.yml` under property `server.ssl.key-store-password`.

Change the key alias use by the server in `src/main/resources/application.yml` under the key `server.ssl.key-alias` to chosen alias.

### How to change ELB HTTPS certificates 
Change the ELB HTTPS certificates in `.primer/infrastructure.json` file for the corresponding environment, region and Availability Zones. For example:

```json
{
  "eweprod-p": {
    "us-west-2a": {
      "elb_https_certificate_arn": "arn:aws:iam::408096535527:server-certificate/cs-media-service-ng.prod-p.expedia.com"
    },
    "us-west-2c": {
      "elb_https_certificate_arn": "arn:aws:iam::408096535527:server-certificate/cs-media-service-ng.prod-p.expedia.com"
    }
  }
}
```
More information about certificates can be found on this [page](https://forum.tools.expedia.com/topic/660/adding-global-https-certs-per-region-after-nov-30th)

### How to change key store password

```bash
./meka keystore -c password -P -W
```
This command will change the password on `src/main/resources/keystore.jks` with an newly auto generated password and update your application property 
`server.ssl.key-store-password` in configuration file `src/main/resources/application.yml`. If you would like to provide your own new password, either 
omit the `-W` parameter or use the `-w REPLACE_BY_NEW_KEY_STORE_PASSWORD` parameter instead.

```bash
./meka keystore -c password -P
[INFO]: New key store password: *********
```
