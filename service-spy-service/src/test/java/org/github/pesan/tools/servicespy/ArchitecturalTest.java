package org.github.pesan.tools.servicespy;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "org.github.pesan.tools.servicespy", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitecturalTest {

    @ArchTest
    private ArchRule features_should_not_depend_on_each_other = slices()
                .matching("..features.(*)..")
                .should()
                .notDependOnEachOther();

    @ArchTest
    private ArchRule vertx =
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("io.vertx..");
}
