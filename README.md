# Service Spy
Service Spy is a tool for intercepting HTTP and HTTPS traffic and log the
requests and responses. The Service Spy also exposes an administrative
interface for log inspection and for configuration.

## Introduction

Service spy is deployed as a stand-alone server and will listen to the
configured HTTP and HTTPS endpoints for incoming requests. Any requests that
are issued to the endpoints are proxied to a matching backend endpoint. The
request and the response is logged.

## How to use it

Start it using Docker:

    docker run -d -p 8080:80 -p 8443:443 -p 8000:8080 pesan/service-spy

Visit `http://<dockerhost>:8000` for the administrative user interface. Use
the ports 8080/8443 for accessing the proxy.

### Configuration

The application accepts the configuration properties listed below:

    Property                             Default                Comment
    --------------------------------------------------------------------------------------------
    server.port                          9900                   Administrative interface port number
    actions.limit                        200                    The number of actions to keep in memory

    default.url                          http://localhost:8080  Default URL value
    default.pattern                      /.*                    Default pattern value
    
    proxy.mappings.url                   ${default.url}         DEPRECATED: replace with proxy.server.<server-name>.mappings.url
    proxy.mappings.pattern               ${default.pattern}     DEPRECATED: replace with proxy.server.<server-name>.mappings.pattern
    
    proxy.servers.<server-name>          -                      Server definition, see below
    
    
    Server definition property           Default                Comment
    --------------------------------------------------------------------------------------------
    .mappings[].url                      -                      Proxy target URL
    .mappings[].pattern                  -                      Proxy path match regexp pattern
    
    .host                                0.0.0.0                Proxy server listener host
    .port                                -                      Proxy server listener port number
    
    .ssl                                 false                  SSL listener if set to true
    
    .jksKeystore                         -                      Path to Java keystore (SSL only)
    .jksPassword                         -                      Java keystore password (SSL only)

    .pemCertPath                         default cert           Path to PEM formatted certificate (SSL only)
    .pemKeyPath                          default key            Path to PEM formatted key (SSL only)

    .pfxKeystore                         -                      Path to PFX keystore (SSL only)
    .pfxPassword                         -                      PFX keystore password (SSL only)
    

These properties can be applied on the command line with a double dash, for example:

    docker run -d pesan/service-spy --actions.limit=150 --proxy.severs.http.mappings[0].url=http://www.example.com

The can also be listed in an `application.yml` file mounted into the `/opt/service-spy` directory
of the container. Example of config file:

    actions.limit: 200
    proxy.servers:
      http8080:
        port: 8080
        mappings:
        - url: http://test1.example.com
          pattern: /test1/.*
        - url: http://test2.example.com
          pattern: /test2/.*

**Example.** How to configure TLS certificate:

Generate the key and the certificate:

    openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 30 -nodes

Mount the key and the certificate into the container and reference them with
`pemCertPath` and `pemKeyPath`:

    docker run -d \
        -p 8000:80 -p 8443:443 -p 8080:8080 \
        -v "/path/to/local/keys/:/keys:ro" \
        pesan/service-spy \
            --proxy.servers.https.pemCertPath=/keys/cert.pem \
            --proxy.servers.https.pemKeyPath=/keys/key.pem

Note that if an SSL listener is not explicitly given a key and certificate, a default key
and self-signed certificate will be used.

## How to do development

Clone and build the project:

    git clone https://github.com/pesan/service-spy.git
    cd service-spy
    mvn clean package

Start it:

    java -jar target/service-spy-*.jar

## Changelog

### 1.9
 - Add possibility to configure mappings per proxy server listener

### 1.8.2
 - Fix request path URI display
 - Fix server description text
 - Fix unintended initially open content dropdown

### 1.8.1
 - Add possibility to change content type
 - Remove trust store configuration from API

### 1.8
 - Add better support for content types: image, json, xml, plain and binary
 - Various UI improvements

### 1.7.1
 - Add listen host
 - Add server configuration UI

### 1.7
 - Add https support
 - Prevent double entries in case of server timeout

### 1.6.1
 - Fix bug with unstable server synchronization

### 1.6
 - Add better support for REST

### 1.5
 - Initial public release
