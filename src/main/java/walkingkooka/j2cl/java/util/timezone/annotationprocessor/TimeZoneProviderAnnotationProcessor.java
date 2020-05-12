/*
 * Copyright 2019 Miroslav Pokorny (github.com/mP1)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package walkingkooka.j2cl.java.util.timezone.annotationprocessor;

import walkingkooka.collect.set.Sets;
import walkingkooka.j2cl.locale.WalkingkookaLanguageTag;
import walkingkooka.j2cl.locale.annotationprocessor.LocaleAwareAnnotationProcessor;
import walkingkooka.text.printer.IndentingPrinter;

import java.io.DataOutput;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

public final class TimeZoneProviderAnnotationProcessor extends LocaleAwareAnnotationProcessor {
    @Override
    protected Set<String> additionalArguments() {
        return Sets.of(ZONEIDS_ANNOTATION_PROCESSOR_OPTION);
    }

    /**
     * The annotation processor option that has the csv list of time zone ids selectors.
     */
    private final static String ZONEIDS_ANNOTATION_PROCESSOR_OPTION = "walkingkooka.j2cl.java.util.TimeZone";

    @Override
    protected void generate(final Set<String> languageTags,
                            final Function<String, String> arguments,
                            final DataOutput data,
                            final IndentingPrinter comments) throws Exception {
        TimeZoneProviderTool.generate(languageTags,
                TimeZoneProviderTool.timezoneIds(arguments.apply(ZONEIDS_ANNOTATION_PROCESSOR_OPTION)),
                data,
                comments);
    }

    @Override
    protected String generatedClassName() {
        return "walkingkooka.j2cl.java.util.timezone.TimeZoneProvider";
    }
}
