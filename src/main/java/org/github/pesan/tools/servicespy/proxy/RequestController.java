package org.github.pesan.tools.servicespy.proxy;

import org.github.pesan.tools.servicespy.admin.RequestLogEntry;
import org.github.pesan.tools.servicespy.common.ActionService;
import org.github.pesan.tools.servicespy.common.ProxyConfig;
import org.github.pesan.tools.servicespy.proxy.transform.StreamTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.MatchableDocumentTransformer;
import org.github.pesan.tools.servicespy.proxy.transform.matching.misc.StringTransformerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.toList;
import static org.github.pesan.tools.servicespy.proxy.transform.matching.MatchingTransformer.matches;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class RequestController {
    private @Autowired ProxyService proxy;
    private @Autowired ActionService actionService;

    private @Autowired StringTransformerFactory stringFactory;
    private @Autowired ProxyConfig proxyConfig;

    private StreamTransformer getResponseTransformer() {
        if (proxyConfig.getResponseTransforms().isEmpty()) {
            return StreamTransformer.PASS_THROUGH;
        }

        return stringFactory.createMatchingTransformer(
                proxyConfig.getResponseTransforms().stream()
                        .filter(ProxyConfig.RegexpTransform::isActive)
                        .map(t -> new MatchableDocumentTransformer<String>(matches(t.getServicePattern()), (s, c) ->
                                s.replaceAll(t.getMatchPattern(), t.getReplacement())
                        )).collect(toList())
        );
    }

    private StreamTransformer getRequestTransformer() {
        if (proxyConfig.getRequestTransforms().isEmpty()) {
            return StreamTransformer.PASS_THROUGH;
        }

        return stringFactory.createMatchingTransformer(
                proxyConfig.getRequestTransforms().stream()
                        .filter(ProxyConfig.RegexpTransform::isActive)
                        .map(t -> new MatchableDocumentTransformer<String>(matches(t.getServicePattern()), (s, c) ->
                                s.replaceAll(t.getMatchPattern(), t.getReplacement())
                        )).collect(toList())
        );
    }

    @RequestMapping(value = "/**", method = POST)
    public Callable<Void> handle(HttpServletRequest request, HttpServletResponse response) throws IOException, XMLStreamException, TransformerConfigurationException {
        String requestPath = (String)request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String backendUrl = proxyConfig.getMappings().stream()
                .filter(ProxyConfig.Mapping::isActive)
                .filter(m -> m.getPattern().matcher(requestPath).matches())
                .map(x -> x.getUrl() + requestPath)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no mapping for " + requestPath));
        RequestLogEntry logEntry = actionService.beginRequest(UUID.randomUUID().toString(), requestPath, new URL(backendUrl));
        request.setAttribute("requestLog", logEntry);
        return () -> {
            ByteArrayOutputStream requestInStream = new ByteArrayOutputStream();
            ByteArrayOutputStream requestOutStream = new ByteArrayOutputStream();

            ByteArrayOutputStream responseInStream = new ByteArrayOutputStream();
            ByteArrayOutputStream responseOutStream = new ByteArrayOutputStream();

            StreamTransformer requestTransformer = new SpyingTransformer(getRequestTransformer(), requestInStream, requestOutStream);
            StreamTransformer responseTransformer = new SpyingTransformer(getResponseTransformer(), responseInStream, responseOutStream);

            ProxyResponse proxyResponse = proxy.doProxy(requestPath, requestTransformer, responseTransformer, new ProxyRequest(
                    request.getInputStream(),
                    response.getOutputStream(),
                    request.getContentLength(),
                    request.getContentType(),
                    request.getRequestURI(),
                    backendUrl
            ));

            response.setStatus(proxyResponse.getStatus());
            response.setContentLength((int) proxyResponse.getContentLength());
            response.setContentType(proxyResponse.getContentType());

            actionService.endRequest(logEntry, requestInStream.toString(), requestOutStream.toString(),
                    responseInStream.toString(), responseOutStream.toString());
            return null;
        };
    }

    @ExceptionHandler(Exception.class)
    public void exceptionHandler(HttpServletRequest request, Exception exception) throws Exception {
        exception.printStackTrace(System.out);
        actionService.endRequest(
                ((RequestLogEntry) request.getAttribute("requestLog")),
                exception
        );
    }
}
