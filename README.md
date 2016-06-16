# Service Spy
Service Spy a tool that will intercept HTTP and HTTPS traffic and log the requests and responses. The Service Spy also exposes an administrative interface for log inspection and for configuration.

## Introduction

Service spy is deployed as a stand-alone server and will listen to the configured HTTP and HTTPS endpoints for incoming requests. Any request that is issued to the endpoints are proxied to a matching backend endpoint. The request and the response is logged.

## How to use it

Start it using Docker:

    docker run -d -p 8000:80 -p 8443:443 -p 8080:8080 pesan/service-spy

Visit `http://<dockerhost>:8080` for the administrative user interface. Use the ports 8000/8443 for accessing the proxy.

### Configuration

The application accepts the configuration properties listed below:

    Property                             Default         Comment
    actions.limit                        200             The number of actions to keep in memory

    proxy.servers.https.jksKeystore      N/A             Path to Java keystore
    proxy.servers.https.jksPassword      N/A             Java keystore password

    proxy.servers.https.pemCertPath      default cert    Path to PEM formatted certificate
    proxy.servers.https.pemKeyPath       default key     Path to PEM formatted key

    proxy.servers.https.pfxKeystore      N/A             Path to PFX keystore
    proxy.servers.https.pfxPassword      N/A             PFX keystore password

**Example.** How to configure TLS certificate:

Generate the key:

    openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 30 -nodes

Mount the key into the container and reference them with `pemCertPath` and `pemKeyPath`:

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

