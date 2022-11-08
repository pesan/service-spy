package org.github.pesan.tools.servicespy.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.github.pesan.tools.servicespy.config.ConfigValue.binaryValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.booleanValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.listValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.numberValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.recordValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.stringValue;

class YamlConfigTest {
    @Test
    void testStringPrimitives() {
        Optional<ConfigValue> configValue = parseConfig("""
                single text
                """);

        assertThat(configValue).hasValue(stringValue("single text"));
        assertThat(configValue.flatMap(value -> YamlConfig.toObject(String.class, value).toOptional()))
                .hasValue("single text");
    }

    @Test
    void testIntegerField() {
        record IntFieldRecord(int intField) {
        }
        Optional<ConfigValue> configValue = parseConfig("""
                intField: 1
                """);

        assertThat(configValue).hasValue(
                recordValue(
                        "intField", numberValue(1)
                ));
        assertThat(configValue.flatMap(value -> YamlConfig.toObject(IntFieldRecord.class, value).toOptional()))
                .hasValue(new IntFieldRecord(1));
    }

    @Test
    void textMixedListField() {
        record MixedListFieldRecord(List<Object> mixedList) {
        }
        Optional<ConfigValue> configValue = parseConfig("""
                mixedList:
                  - 1
                  - "2"
                  - true
                """);

        assertThat(configValue).hasValue(recordValue(
                "mixedList", listValue(
                        numberValue(1),
                        stringValue("2"),
                        booleanValue(true)
                )));
        assertThat(configValue.flatMap(value -> YamlConfig.toObject(MixedListFieldRecord.class, value).toOptional()))
                .hasValue(new MixedListFieldRecord(List.of(1, "2", true)));
    }

    @Test
    void testOptional() {
        record OptionalListRecord(List<Integer> listWithEmpty, Optional<String> fieldWithEmpty) {
        }
        Optional<ConfigValue> configValue = parseConfig("""
                listWithEmpty:
                  - 1
                  -
                fieldWithEmpty:
                """);

        assertThat(configValue).hasValue(recordValue(
                "listWithEmpty", listValue(
                        numberValue(1)
                )
        ));
        assertThat(configValue.flatMap(value -> YamlConfig.toObject(OptionalListRecord.class, value).toOptional()))
                .hasValue(new OptionalListRecord(List.of(1), Optional.empty()));
    }

    @Test
    void testBinaryField() {
        record BinaryFieldRecord(byte[] binaryField) {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof BinaryFieldRecord that && Arrays.equals(binaryField, that.binaryField);
            }
        }
        Optional<ConfigValue> configValue = parseConfig("""
                binaryField: !!binary %s
                """.formatted(base64("ABC")));

        assertThat(configValue).hasValue(recordValue(
                "binaryField", binaryValue("ABC".getBytes(UTF_8))
        ));

        assertThat(configValue.flatMap(value -> YamlConfig.toObject(BinaryFieldRecord.class, value).toOptional()))
                .hasValue(new BinaryFieldRecord("ABC".getBytes(UTF_8)));
    }

    @Test
    void testPrimitiveProperties() {
        assertThat(stringValue("text").asText()).isEqualTo("text");
        assertThat(stringValue("123").asInteger().getAsInt()).isEqualTo(123);
        assertThat(stringValue("text").asInteger()).isEmpty();
        assertThat(stringValue("text").asDouble()).isEmpty();
        assertThat(stringValue("text").isTrue()).isFalse();
        assertThat(stringValue("\"").asDecoded()).isEmpty();

        assertThat(numberValue(1).asText()).isEqualTo("1");
        assertThat(numberValue(1).asInteger().getAsInt()).isEqualTo(1);
        assertThat(numberValue(1.23).asInteger().getAsInt()).isEqualTo(1);
        assertThat(numberValue(1.23).asDouble().getAsDouble()).isEqualTo(1.23);

        assertThat(binaryValue("ABC".getBytes()).asDecoded()).hasValue("ABC".getBytes());
        assertThat(binaryValue("ABC".getBytes()).asText()).isEqualTo(base64("ABC"));

        assertThat(booleanValue(false).isTrue()).isFalse();
        assertThat(booleanValue(false).asText()).isEqualTo("false");
        assertThat(booleanValue(true).isTrue()).isTrue();
        assertThat(booleanValue(true).asText()).isEqualTo("true");
    }


    @Test
    void testOverride() {
        assertThat(
                recordValue(Map.of(
                        "A", stringValue("abc"),
                        "B", stringValue("def")
                )).overrideWith(
                        recordValue(Map.of(
                                "A", stringValue("123"),
                                "C", stringValue("456")
                        ))
                ))
                .isEqualTo(
                        recordValue(Map.of(
                                "A", stringValue("123"),
                                "B", stringValue("def"),
                                "C", stringValue("456")
                        )));
    }

    @Test
    void testOverrideList() {
        assertThat(
                listValue(
                        recordValue(Map.of(
                                "name", stringValue("myname"),
                                "value", stringValue("abc"),
                                "enabled", booleanValue(true))
                        ),
                        recordValue(Map.of(
                                "id", stringValue("myid"),
                                "value", stringValue("abc"),
                                "enabled", booleanValue(true))
                        ),
                        recordValue(Map.of(
                                "id", stringValue("abc"),
                                "value", stringValue("def"))
                        ),
                        numberValue(123)
                ).overrideWith(
                        recordValue(Map.of(
                                "myname", recordValue("value", stringValue("123")),
                                "myid", recordValue("enabled", booleanValue(false)),
                                "unmatched", recordValue(Map.of("unmatched", stringValue("id"), "value", stringValue("def")))
                        ))))
                .isEqualTo(listValue(
                        recordValue(Map.of(
                                "name", stringValue("myname"),
                                "value", stringValue("123"),
                                "enabled", booleanValue(true))
                        ),
                        recordValue(Map.of(
                                "id", stringValue("myid"),
                                "value", stringValue("abc"),
                                "enabled", booleanValue(false))
                        ),
                        recordValue(Map.of(
                                "id", stringValue("abc"),
                                "value", stringValue("def"))
                        ),
                        numberValue(123)
                ));
    }

    private static Optional<ConfigValue> parseConfig(String s) {
        return YamlConfig.fromYaml(new ByteArrayInputStream(s.getBytes(UTF_8))).toOptional();
    }

    private static String base64(String src) {
        return Base64.getEncoder().encodeToString(src.getBytes(UTF_8));
    }
}