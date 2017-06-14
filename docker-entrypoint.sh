#!/bin/bash

set -e

if [[ -z "$JAVA_XMS" ]]; then
    case "${EXPEDIA_DEPLOYED_ENVIRONMENT}" in
    int)        JAVA_XMS=512m ;;
    stress)     JAVA_XMS=512m ;;
    prod)       JAVA_XMS=512m ;;
    prod-p)     JAVA_XMS=512m ;;
    dev|test|*) JAVA_XMS=512m ;;
    esac
fi
if [[ -z "$JAVA_XMX" ]]; then
    case "${EXPEDIA_DEPLOYED_ENVIRONMENT}" in
    int)        JAVA_XMX=1024m ;;
    stress)     JAVA_XMX=1024m ;;
    prod)       JAVA_XMX=1024m ;;
    prod-p)     JAVA_XMX=1024m ;;
    dev|test|*) JAVA_XMX=1024m ;;
    esac
fi

if [ -n "$EXPEDIA_DNS_ZONE" -a -n "$AWS_REGION" ]; then
    # Assume running in ECS
    [ -z "$HOST_IP" ] && export HOST_IP="$(curl http://169.254.169.254/latest/meta-data/local-ipv4/)"
    [ -z "$HOST_PORT" ] && export HOST_PORT=48570
    [ -z "$HOST_SECURE_PORT" ] && export HOST_SECURE_PORT=48571
else
    # Assume running in Docker Machine
    [ -z "$HOST_IP" ] && export HOST_IP=192.168.99.100
fi

JAVA_OPTS="${JAVA_OPTS} \
-server \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-Xmx${JAVA_XMX} \
-Xms${JAVA_XMS} \
-Djava.security.egd=file:/dev/./urandom \
-Djdk.xml.entityExpansionLimit=0 \
-Djdk.xml.elementAttributeLimit=0 \
-Djdk.xml.maxOccur=0 \
-Djdk.xml.totalEntitySizeLimit=0 \
-Xdebug -Xrunjdwp:server=y,transport=dt_socket,suspend=n,address=8000 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.port=1098  \
-Dcom.sun.management.jmxremote.rmi.port=1098 \
-Dapplication.environment=${EXPEDIA_ENVIRONMENT} \
-DactiveVersionFileLocation=${APP_HOME}/active.txt \
-Dapplication.home=${APP_HOME} \
-Dspring.profiles.active=${EXPEDIA_DEPLOYED_ENVIRONMENT},${EXPEDIA_DEPLOYED_ENVIRONMENT}-${AWS_REGION} \
-DAWS_REGION=${AWS_REGION} \
-DACTIVE_VERSION=${ACTIVE_VERSION}"


if [ "$1" = 'service' ]; then

    echo $ACTIVE_VERSION > ${APP_HOME}/active.txt

    exec java ${JAVA_OPTS} -jar "${APP_BIN}/cs-media-service-ng.jar" ${APP_ARGS}
fi

exec "$@"
