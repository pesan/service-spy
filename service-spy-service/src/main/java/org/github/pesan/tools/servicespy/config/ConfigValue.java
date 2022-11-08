package org.github.pesan.tools.servicespy.config;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public sealed interface ConfigValue {

    enum ValueType {
        NUMBER,
        STRING,
        BINARY,
        BOOLEAN
    }

    static ConfigValue recordValue(Map<String, ConfigValue> fields) {
        return new RecordValue(fields);
    }

    static ConfigValue recordValue(String key, ConfigValue value) {
        return recordValue(Map.of(key, value));
    }

    static ConfigValue listValue(List<ConfigValue> elements) {
        return new ListValue(elements);
    }

    static ConfigValue listValue(ConfigValue... elements) {
        return listValue(List.of(elements));
    }

    static PrimitiveConfigValue stringValue(String value) {
        return new PrimitiveConfigValue() {
            @Override
            public OptionalInt asInteger() {
                try {
                    return OptionalInt.of(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                    OptionalDouble optionalDouble = asDouble();
                    return optionalDouble.isPresent()
                            ? OptionalInt.of((int) optionalDouble.orElseThrow())
                            : OptionalInt.empty();
                }
            }

            @Override
            public OptionalDouble asDouble() {
                try {
                    return OptionalDouble.of(Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                    return OptionalDouble.empty();
                }
            }

            @Override
            public Optional<byte[]> asDecoded() {
                try {
                    return Optional.of(Base64.getDecoder().decode(value));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }

            @Override
            public String asText() {
                return value;
            }

            @Override
            public boolean isTrue() {
                return "true".equals(value);
            }

            @Override
            public ValueType type() {
                return ValueType.STRING;
            }

            @Override
            public String toString() {
                return asText();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof PrimitiveConfigValue that && that.asText().equals(asText());
            }

            @Override
            public int hashCode() {
                return asText().hashCode();
            }
        };
    }

    static PrimitiveConfigValue numberValue(Number value) {
        return new PrimitiveConfigValue() {
            @Override
            public OptionalInt asInteger() {
                return OptionalInt.of(value.intValue());
            }

            @Override
            public OptionalDouble asDouble() {
                return OptionalDouble.of(value.doubleValue());
            }

            @Override
            public String asText() {
                return value.toString();
            }

            @Override
            public ValueType type() {
                return ValueType.NUMBER;
            }

            @Override
            public String toString() {
                return asText();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof PrimitiveConfigValue that && that.asText().equals(asText());
            }

            @Override
            public int hashCode() {
                return asText().hashCode();
            }
        };
    }

    static PrimitiveConfigValue booleanValue(boolean value) {
        return new PrimitiveConfigValue() {
            @Override
            public String asText() {
                return value ? "true" : "false";
            }

            @Override
            public boolean isTrue() {
                return value;
            }

            @Override
            public ValueType type() {
                return ValueType.BOOLEAN;
            }

            @Override
            public String toString() {
                return asText();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof PrimitiveConfigValue that && that.asText().equals(asText());
            }

            @Override
            public int hashCode() {
                return asText().hashCode();
            }
        };
    }

    static PrimitiveConfigValue binaryValue(byte[] value) {
        return new PrimitiveConfigValue() {
            @Override
            public Optional<byte[]> asDecoded() {
                return Optional.of(value);
            }

            @Override
            public String asText() {
                return Base64.getEncoder().encodeToString(value);
            }

            @Override
            public ValueType type() {
                return ValueType.BINARY;
            }

            @Override
            public String toString() {
                return asText();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof PrimitiveConfigValue that && that.asText().equals(asText());
            }

            @Override
            public int hashCode() {
                return asText().hashCode();
            }
        };
    }

    non-sealed interface PrimitiveConfigValue extends ConfigValue {
        default OptionalInt asInteger() {
            return OptionalInt.empty();
        }

        default OptionalDouble asDouble() {
            return OptionalDouble.empty();
        }

        default Optional<byte[]> asDecoded() {
            return Optional.empty();
        }

        String asText();

        default boolean isTrue() {
            return false;
        }

        ValueType type();

        @Override
        default <T> T handle(Function<PrimitiveConfigValue, T> primitiveHandler, Function<List<T>, T> listHandler, Function<Map<String, T>, T> recordHandler) {
            return primitiveHandler.apply(this);
        }

        @Override
        default ConfigValue overrideWith(ConfigValue value) {
            return value;
        }
    }

    record ListValue(List<ConfigValue> elements) implements ConfigValue {
        @Override
        public <T> T handle(Function<PrimitiveConfigValue, T> primitiveHandler, Function<List<T>, T> listHandler, Function<Map<String, T>, T> recordHandler) {
            return listHandler.apply(stream()
                    .map(element -> element.handle(primitiveHandler, listHandler, recordHandler))
                    .toList());
        }

        @Override
        public ConfigValue overrideWith(ConfigValue value) {
            Set<String> idKeys = Set.of("id", "key", "name");

            if (value instanceof RecordValue rv) {
                Map<String, ConfigValue> fields = rv.fields();

                return listValue(stream()
                        .flatMap(element -> {
                            if (element instanceof RecordValue rv2) {
                                return rv2.stream()
                                        .flatMap(overrideField -> idKeys.stream()
                                                .flatMap(idKey ->
                                                        overrideField.getKey().equals(idKey) && overrideField.getValue() instanceof PrimitiveConfigValue idValue
                                                                ? Stream.ofNullable(fields.get(idValue.asText()))
                                                                : Stream.empty()
                                                )
                                        )
                                        .findFirst()
                                        .map(element::overrideWith)
                                        .or(() -> Optional.of(element))
                                        .stream();
                            }
                            return Stream.of(element);
                        })
                        .toList());
            }

            return value;
        }

        public Stream<ConfigValue> stream() {
            return elements.stream();
        }

        @Override
        public String toString() {
            return elements.toString();
        }
    }

    record RecordValue(Map<String, ConfigValue> fields) implements ConfigValue {
        @Override
        public <T> T handle(Function<PrimitiveConfigValue, T> primitiveHandler, Function<List<T>, T> listHandler, Function<Map<String, T>, T> recordHandler) {
            return recordHandler.apply(stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            field -> field.getValue().handle(primitiveHandler, listHandler, recordHandler)
                    )));
        }

        @Override
        public ConfigValue overrideWith(ConfigValue value) {
            if (value instanceof RecordValue record) {
                Map<String, ConfigValue> overrides = record.fields();
                return ConfigValue.recordValue(Stream.concat(
                                stream()
                                        .map(e -> overrides.containsKey(e.getKey())
                                                ? Map.entry(e.getKey(), e.getValue().overrideWith(overrides.get(e.getKey())))
                                                : e
                                        ),
                                overrides.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a))
                );
            }
            return this;
        }

        public Stream<Map.Entry<String, ConfigValue>> stream() {
            return fields.entrySet().stream();
        }

        @Override
        public String toString() {
            return stream()
                    .map(field -> field.getKey() + ": " + field.getValue())
                    .collect(joining(", ", "{", "}"));
        }
    }

    default boolean isEmpty() {
        return handle(primitive -> false, List::isEmpty, Map::isEmpty);
    }

    <T> T handle(Function<PrimitiveConfigValue, T> primitiveHandler,
                 Function<List<T>, T> listHandler,
                 Function<Map<String, T>, T> recordHandler);

    ConfigValue overrideWith(ConfigValue value);
}
