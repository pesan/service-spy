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
    stream.timeout                       864000000              Stream connection timeout (milliseconds)

    default.url                          http://localhost:8080  Default URL value
    default.pattern                      /.*                    Default pattern value

    proxy.mappings[0].url                ${default.url}         The first proxied URL (shown under Config)
    proxy.mappings[0].pattern            ${default.pattern}     The first regexp path pattern

    proxy.servers.http.port              9000                   HTTP proxy port number
    proxy.servers.https.port             9443                   HTTPS proxy port number

    proxy.servers.https.jksKeystore      N/A                    Path to Java keystore
    proxy.servers.https.jksPassword      N/A                    Java keystore password

    proxy.servers.https.pemCertPath      default cert           Path to PEM formatted certificate
    proxy.servers.https.pemKeyPath       default key            Path to PEM formatted key

    proxy.servers.https.pfxKeystore      N/A                    Path to PFX keystore
    proxy.servers.https.pfxPassword      N/A                    PFX keystore password

These properties can be applied on the command line with a double dash, for example:

    docker run -d pesan/service-spy --actions.limit=150 --default.url=https://example.com

The can also be listed in an `application.yml` file mounted into the root of the container.
Example of config file:

    actions.limit: 200
    proxy.mappings:
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

