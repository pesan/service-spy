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
    
    .ssl                                 false                  SSL listener
    
    .jksKeystore                         N/A                    (SSL) Path to Java keystore
    .jksPassword                         N/A                    (SSL) Java keystore password

    .pemCertPath                         default cert           (SSL) Path to PEM formatted certificate
    .pemKeyPath                          default key            (SSL) Path to PEM formatted key

    .pfxKeystore                         N/A                    (SSL) Path to PFX keystore
    .pfxPassword                         N/A                    (SSL) PFX keystore password
    

These properties can be applied on the command line with a double dash, for example:

    docker run -d pesan/service-spy --actions.limit=150

The can also be listed in an `application.yml` file mounted into the root of the container.
Example of config file:

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

## How to do development

Clone and build the project:

    git clone https://github.com/pesan/service-spy.git
    cd service-spy
    mvn clean package

Start it:

    java -jar target/service-spy-*.jar

