package org.github.pesan.tools.servicespy.config;

import org.github.pesan.tools.servicespy.config.ConfigValue.PrimitiveConfigValue;
import org.github.pesan.tools.servicespy.util.GenReference;
import org.github.pesan.tools.servicespy.util.GenTransform;
import org.quicktheories.core.Gen;
import org.quicktheories.generators.Generate;

import java.util.function.UnaryOperator;

import static java.util.function.UnaryOperator.identity;
import static org.github.pesan.tools.servicespy.config.ConfigValue.listValue;
import static org.github.pesan.tools.servicespy.config.ConfigValue.recordValue;
import static org.quicktheories.generators.Generate.byteArrays;
import static org.quicktheories.generators.Generate.bytes;
import static org.quicktheories.generators.SourceDSL.booleans;
import static org.quicktheories.generators.SourceDSL.doubles;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.lists;
import static org.quicktheories.generators.SourceDSL.maps;
import static org.quicktheories.generators.SourceDSL.strings;

public class ConfigValues {

    private static final Gen<PrimitiveConfigValue> INTEGERS = integers().all().map(ConfigValue::numberValue);
    private static final Gen<PrimitiveConfigValue> DOUBLES = doubles().any().map(ConfigValue::numberValue);
    private static final Gen<PrimitiveConfigValue> BOOLEANS = booleans().all().map(ConfigValue::booleanValue);
    private static final Gen<PrimitiveConfigValue> STRINGS = strings().basicLatinAlphabet().ofLengthBetween(0, 8).map(ConfigValue::stringValue);
    private static final Gen<PrimitiveConfigValue> BINARIES = byteArrays(integers()
            .between(0, 10), bytes((byte) 0x80, (byte) 0x7f, (byte) 0x00))
            .map(ConfigValue::binaryValue);

    private static final Gen<String> KEYS = strings().basicLatinAlphabet().ofLengthBetween(1, 10);

    public static Gen<ConfigValue> configValues() {
        return configValues(
                primitiveValues(),
                ConfigValues::listValues,
                ConfigValues::recordValues
        );
    }

    public static Gen<ConfigValue> configValues(
            Gen<PrimitiveConfigValue> primitives,
            UnaryOperator<Gen<ConfigValue>> lists,
            UnaryOperator<Gen<ConfigValue>> records
    ) {
        Gen<ConfigValue> configValueGen = primitives.map(identity());

        GenReference<ConfigValue> recordValues = GenReference.create();
        GenReference<ConfigValue> listValues = GenReference.create();

        Gen<ConfigValue> values = configValueGen
                .mix(recordValues, 1)
                .mix(listValues, 1);

        listValues.set(lists.apply(values));
        recordValues.set(records.apply(values));

        return values;
    }

    public static Gen<ConfigValue> listValues(Gen<ConfigValue> values) {
        return lists().of(values).ofSizeBetween(0, 10).map(elements -> listValue(elements));
    }

    public static Gen<ConfigValue> recordValues(Gen<ConfigValue> values) {
        return maps().of(KEYS, values).ofSizeBetween(0, 10).map(fields -> recordValue(fields));
    }

    public static <T> Gen<T> primitiveValues(GenTransform<PrimitiveConfigValue, T> transform) {
        return Generate.oneOf(
                transform.apply(INTEGERS),
                transform.apply(DOUBLES),
                transform.apply(BOOLEANS),
                transform.apply(BINARIES),
                transform.apply(STRINGS)
        );
    }

    public static Gen<PrimitiveConfigValue> primitiveValues() {
        return primitiveValues(GenTransform.id());
    }
}
