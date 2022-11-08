package org.github.pesan.tools.servicespy.config;

import org.github.pesan.tools.servicespy.config.ConfigValue.PrimitiveConfigValue;
import org.github.pesan.tools.servicespy.config.ConfigValue.RecordValue;
import org.github.pesan.tools.servicespy.features.dashboard.config.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.api.Pair;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.SourceDSL;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.github.pesan.tools.servicespy.config.ConfigValue.binaryValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.booleanValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.numberValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.recordValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.stringValue;
import static org.github.pesan.tools.servicespy.util.GenTransform.paired;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.byteArrays;
import static org.quicktheories.generators.Generate.bytes;
import static org.quicktheories.generators.SourceDSL.booleans;
import static org.quicktheories.generators.SourceDSL.doubles;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.strings;

class YamlConfigPropertiesTest {

    private static final Gen<PrimitiveConfigValue> INTEGERS = integers().all().map(ConfigValue::numberValue);
    private static final Gen<PrimitiveConfigValue> DOUBLES = doubles().any().map(ConfigValue::numberValue);
    private static final Gen<PrimitiveConfigValue> BOOLEANS = booleans().all().map(ConfigValue::booleanValue);
    private static final Gen<PrimitiveConfigValue> STRINGS = strings().basicLatinAlphabet().ofLengthBetween(0, 8).map(ConfigValue::stringValue);
    private static final Gen<PrimitiveConfigValue> BINARIES = byteArrays(integers()
            .between(0, 10), bytes((byte) 0x80, (byte) 0x7f, (byte) 0x00))
            .map(ConfigValue::binaryValue);

    private static final Gen<String> KEYS = strings().basicLatinAlphabet().ofLengthBetween(1, 10);

    @Test
    void testPrimitiveProperties() {
        qt().forAll(strings().allPossible().ofLengthBetween(0, 10))
                .check(str -> stringValue(str).asText().equals(str));

        qt().forAll(SourceDSL.doubles().any())
                .check(dbl -> numberValue(dbl).asDouble().orElseThrow() == dbl);

        qt().forAll(SourceDSL.doubles().any())
                .check(dbl -> (int) numberValue(dbl).asDouble().orElseThrow()
                        == numberValue(dbl).asInteger().orElseThrow());

        qt().forAll(integers().all())
                .check(integer -> numberValue(integer).asDouble().orElseThrow() == (double) integer);

        qt().forAll(byteArrays(integers().between(0, 10), bytes((byte) 0x80, (byte) 0x7F, (byte) 0x10)))
                .check(bytes -> Arrays.equals(binaryValue(bytes).asDecoded().orElseThrow(), bytes));

        qt().forAll(booleans().all())
                .check(bool -> booleanValue(bool).isTrue() == bool);

        qt().forAll(strings().allPossible().ofLengthBetween(0, 5))
                .assuming(str -> !"true".equals(str))
                .check(str -> !stringValue(str).isTrue());

    }

    @Test
    void testSerializationInvertableProperty() {
        qt().forAll(ConfigValues.configValues())
                .checkAssert(value ->
                        assertThat(YamlConfig.toYaml(value)
                                .flatMap(this::parseConfig)
                                .toOptional()
                                .orElseThrow()).isEqualTo(value));
    }

    @Test
    void testOverride() {
        qt().forAll(ConfigValues.primitiveValues(paired()))
                .checkAssert(pair ->
                        assertThat(pair._1.overrideWith(pair._2)).isEqualTo(pair._2)
                );

        qt().forAll(ConfigValues.listValues(ConfigValues.configValues()).zip(ConfigValues.listValues(ConfigValues.configValues()), Pair::of))
                .checkAssert(pair ->
                        assertThat(pair._1.overrideWith(pair._2)).isEqualTo(pair._2)
                );

        qt().forAll(ConfigValues.recordValues(ConfigValues.configValues()).zip(ConfigValues.recordValues(ConfigValues.configValues()), Pair::of))
                .checkAssert(pair ->
                        assertThat(((RecordValue) pair._1.overrideWith(pair._2)).fields())
                                .containsAllEntriesOf(((RecordValue) pair._2).fields())
                );

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

    private Result<ConfigValue, Exception> parseConfig(String s) {
        return YamlConfig.fromYaml(new ByteArrayInputStream(s.getBytes(UTF_8)));
    }
}