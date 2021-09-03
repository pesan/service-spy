package org.github.pesan.tools.servicespy.proxy;

public class ProxyService {

    /*private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    private final Vertx vertx;
    private final ActionService actionService;
    private final HttpClientBindings proxyClients;
    private final HttpServerBindings proxyServers;
    private final Clock clock;

    public ProxyService(Vertx vertx, ActionService actionService, HttpClientBindings proxyClients, HttpServerBindings proxyServers, Clock clock) {
        this.vertx = vertx;
        this.actionService = actionService;
        this.proxyClients = proxyClients;
        this.proxyServers = proxyServers;
        this.clock = clock;
    }

    //@PostConstruct
    public void init() {
        vertx.deployVerticle(this);
    }

    @Override
    public void start() {
        proxyServers.stream().forEach(this::startServer);
    }

    private void startServer(Binding server) {
        server.getServer()
                .requestHandler(serverRequest -> handleRequest(server.getMappings(), serverRequest))
                .listen(listenHandlerForServer(server));
    }

    private void handleRequest(List<ProxyServer.Mapping> mappings, HttpServerRequest serverRequest) {
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        ByteArrayOutputStream sent = new ByteArrayOutputStream();

        RequestContext context = new RequestContext(serverRequest, getClockTime());

        try {
            URL backendUrl = getBackendUrl(mappings, context);
            HttpClientRequest clientRequest = null;
            createClientRequest(serverRequest, backendUrl)
                    .map(responseHandler(context, sent, received, backendUrl)
                            .exceptionHandler(responseExceptionHandler(context, backendUrl, sent))
                            .setChunked(true)
                    ));

            clientRequest.headers().setAll(serverRequest.headers());
            serverRequest.handler(data -> {
                write(sent, data);
                clientRequest.write(data);
            });
            serverRequest.endHandler(v -> clientRequest.end());

        } catch (NoMappingException e) {
            logger.warn(e.getMessage(), e);
            serverRequest.response().close();
            actionService.log(null,
                    RequestDataEntry.fromContext(context, e),
                    ResponseDataEntry.empty(null, getClockTime()));
        }
    }

    private Future<HttpClientRequest> createClientRequest(HttpServerRequest serverRequest, URL backendUrl) {
        return proxyClients.getByScheme(backendUrl.getProtocol())
                .request(serverRequest.method(), getPort(backendUrl), backendUrl.getHost(), serverRequest.uri());
    }

    private int getPort(URL backendUrl) {
        int port = backendUrl.getPort();
        return port != -1 ? port : backendUrl.getDefaultPort();
    }

    private URL getBackendUrl(List<ProxyServer.Mapping> mappings, RequestContext context) {
        return createURL(mappings.stream()
                .filter(ProxyServer.Mapping::isActive)
                .filter(mapping -> mapping.getPattern().matcher(context.getRequestPath()).matches())
                .map(mapping -> mapping.getUrl() + context.getRequestPathWithQuery())
                .findFirst()
                .orElseThrow(() -> new NoMappingException(context.getRequestPath())));
    }

    private Handler<Throwable> responseExceptionHandler(RequestContext context, URL backendUrl, ByteArrayOutputStream sent) {
        return throwable -> {
            actionService.log(
                    null,
                    RequestDataEntry.fromContext(context, sent),
                    null//new ResponseExceptionEntry(backendUrl, fromThrowable(throwable), getClockTime())
            );
            context.getResponse().close();
        };
    }

    private Handler<HttpClientResponse> responseHandler(RequestContext context, ByteArrayOutputStream sent, ByteArrayOutputStream received, URL backendUrl) {
        return clientResponse -> {
            HttpServerResponse serverResponse = context.getResponse()
                    .setChunked(true)
                    .setStatusCode(clientResponse.statusCode());
            serverResponse.headers().setAll(clientResponse.headers());
            clientResponse.handler(data -> {
                write(received, data);
                serverResponse.write(data);
            }).exceptionHandler(throwable -> {
                actionService.log(
                        null,
                        RequestDataEntry.fromContext(context, sent),
                        null); //new ResponseExceptionEntry(backendUrl, fromThrowable(throwable), getClockTime()));
                serverResponse.close();
            }).endHandler(v -> {
                actionService.log(
                        null,
                        RequestDataEntry.fromContext(context, sent),
                        new ResponseDataEntry(clientResponse.statusCode(), clientResponse.getHeader(HttpHeaders.CONTENT_TYPE), backendUrl, parseHeaders(clientResponse.headers()), received.toByteArray(), getClockTime()));
                serverResponse.end();
            });
        };
    }

    private static Map<String, List<String>> parseHeaders(MultiMap headers) {
        return headers.entries().stream().collect(
                groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    }

    private Handler<AsyncResult<HttpServer>> listenHandlerForServer(Binding server) {
        return result -> {
            String name = server.getName();
            String host = server.getHost();
            int port = server.getPort();
            if (result.succeeded()) {
                logger.info("Proxy server '{}' listening on {}:{} with {} mapping(s)", name, host, port, server.getMappings().size());
            } else {
                logger.error("Unable to start '{}' proxy server on {}:{}", name, host, port, result.cause());
            }
        };
    }

    private LocalDateTime getClockTime() {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    private void write(OutputStream stream, Buffer buffer) {
        try {
            stream.write(buffer.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL createURL(String backendUrl) {
        try {
            return new URL(backendUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    } */
}