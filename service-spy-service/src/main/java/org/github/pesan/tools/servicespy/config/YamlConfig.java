package org.github.pesan.tools.servicespy.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.github.pesan.tools.servicespy.features.dashboard.config.Result;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

public class YamlConfig {

    private static final YAMLMapper YAML_MAPPER = (YAMLMapper) new YAMLMapper(new YAMLFactory())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new Jdk8Module());

    public static Result<ConfigValue, Exception> fromYaml(String text) {
        return fromYaml(new ByteArrayInputStream(text.getBytes(UTF_8)));
    }

    public static Result<ConfigValue, Exception> fromYaml(InputStream stream) {
        try {
            return Result.fromOptional(
                    toConfigValue(YAML_MAPPER.readTree(new String(stream.readAllBytes()))),
                    new IllegalArgumentException("could not produce config from input"));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static Result<String, Exception> toYaml(ConfigValue value) {
        try {
            return Result.success(YAML_MAPPER.writeValueAsString(toNode(value)));
        } catch (JsonProcessingException e) {
            return Result.error(e);
        }
    }

    public static <T> Result<T, Exception> toObject(Class<T> type, ConfigValue value) {
        try {
            return Result.success(YAML_MAPPER.convertValue(toNode(value), type));
        } catch (RuntimeException e) {
            return Result.error(e);
        }
    }

    public static JsonNode toNode(ConfigValue value) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        return value.<JsonNode>handle(
                primary -> switch (primary.type()) {
                    case NUMBER ->
                            primary.asText().indexOf('.') >= 0
                            ? factory.numberNode(Double.parseDouble(primary.asText()))
                            : factory.numberNode(Integer.parseInt(primary.asText()));
                    case STRING -> factory.textNode(primary.asText());
                    case BINARY -> factory.binaryNode(primary.asDecoded().orElseThrow());
                    case BOOLEAN -> factory.booleanNode(primary.isTrue());
                },
                elements -> factory.arrayNode().addAll(elements),
                fields -> factory.objectNode().setAll(fields)
        );
    }

    private static Optional<ConfigValue> toConfigValue(JsonNode node) {
        return switch (node.getNodeType()) {
            case OBJECT -> Optional.of(new ConfigValue.RecordValue(stream(node.fields())
                    .flatMap(entry -> toConfigValue(entry.getValue()).map(v -> Map.entry(entry.getKey(), v)).stream())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));
            case ARRAY -> Optional.of(new ConfigValue.ListValue(
                    stream(node.elements())
                            .flatMap(element -> toConfigValue(element).stream())
                            .toList()));
            case NUMBER -> Optional.of(ConfigValue.numberValue(node.numberValue()));
            case STRING -> Optional.of(ConfigValue.stringValue(node.asText()));
            case BOOLEAN -> Optional.of(ConfigValue.booleanValue(node.asBoolean()));
            case BINARY -> {
                try {
                    yield Optional.of(ConfigValue.binaryValue(node.binaryValue()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            case MISSING, NULL -> Optional.empty();
            case POJO -> throw new RuntimeException(node.getNodeType().toString() + " not supported");
        };
    }

    private static <T> Stream<T> stream(Iterator<T> it) {
        return StreamSupport.stream(((Iterable<T>) () -> it).spliterator(), false);
    }

    public static <T> T convert(Class<T> type, Object o) {
        return YAML_MAPPER.convertValue(o, type);
    }
}
