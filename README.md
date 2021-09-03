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

    docker run -d -p 9080:80 -p 9443:443 -p 8080:8080 pesan/service-spy

Visit `http://<dockerhost>:8080` for the administrative user interface. Use
the ports 0080/9443 for accessing the proxy.

### Configuration

The application accepts the configuration properties listed below:

#### Service configuration

 Property                     | Default                     | Comment
------------------------------|-----------------------------|---------
`server.port`                 | `9900`                      | Administrative interface port number
`actions.limit`               | `200`                       | The number of actions to keep in memory
`proxy.servers.<server-name>` | `http` and `https` sections | Proxy server definition, see below
    
#### Proxy server definition configuration
    
 Property             | Default        | Comment
----------------------|----------------|---------
`.mappings[].url`     | _n/a_          | Proxy target URL
`.mappings[].pattern` | _n/a_          | Proxy path match regexp pattern
`.host`               | `0.0.0.0`      | Proxy server listener host
`.port`               | _n/a_          | Proxy server listener port number
`.ssl`                | `false`        | SSL listener if set to true
`.jksKeystore`        | _n/a_          | Path to Java keystore (SSL only)
`.jksPassword`        | _n/a_          | Java keystore password (SSL only)
`.pemCertPath`        | _default cert_ | Path to PEM formatted certificate (SSL only)
`.pemKeyPath`         | _default key_  | Path to PEM formatted key (SSL only)
`.pfxKeystore`        | _n/a_          | Path to PFX keystore (SSL only)
`.pfxPassword`        | _n/a_          | PFX keystore password (SSL only)
    

The configuration can be overriden by an `application.yml` file mounted into the `/opt/service-spy` directory
of the container.

**Example.** Layout of a configuration file override:

    proxy.servers:
      http8080:
        port: 8080
        mappings:
        - url: http://test1.example.com
          pattern: /test1/.*
        - url: http://test2.example.com
          pattern: /test2/.*

This will add a third proxy server alongside the default ones (`http` and `https`).

**Example.** How to configure TLS certificate:

Generate the key and the certificate:

    openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 30 -nodes

Mount the key and the certificate into the container and reference them from
the configuration override file.

File *application.yml*:

    proxy.servers:
      https:
        pemCertPath: /keys/cert.pem
        pemKeyPath: /keys/key.pem

Run command:

    docker run -d \
        -p 9080:80 -p 9443:443 -p 8080:8080 \
        -v "/path/to/local/keys/:/keys:ro" \
        pesan/service-spy

Note that if an SSL listener is not explicitly given a key and certificate, a default key
and self-signed certificate will be used.

## How to do development

Clone and build the project:

    git clone https://github.com/pesan/service-spy.git
    cd service-spy
    mvn clean package

Start the frontend:

    (cd frontend; npm install; npm start)

Start the backend:

    java -jar target/service-spy-*-fat.jar

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