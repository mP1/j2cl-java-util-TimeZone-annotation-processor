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
import walkingkooka.j2cl.locale.annotationprocessor.LocaleAwareAnnotationProcessor;
import walkingkooka.j2cl.locale.annotationprocessor.LocaleAwareAnnotationProcessorTool;
import walkingkooka.text.CharSequences;
import walkingkooka.text.printer.IndentingPrinter;

import java.io.DataOutput;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class TimeZoneProviderAnnotationProcessor extends LocaleAwareAnnotationProcessor {
    @Override
    protected Set<String> additionalArguments() {
        return Sets.of(
                SELECTED_ZONEIDS,
                DEFAULT_TIMEZONE
        );
    }

    // timeZoneFilter .................................................................................................

    private String timeZoneFilter(final Function<String, String> arguments) {
        return arguments.apply(SELECTED_ZONEIDS);
    }

    private static Set<String> timeZones(final String timeZoneFilter) {
        try {
            return TimeZoneProviderTool.timezoneIds(timeZoneFilter);
        } catch (final Exception fail) {
            throw new IllegalArgumentException(fail.getMessage() + " " + URL);
        }
    }

    /**
     * The annotation processor option that has the csv list of time zone ids selectors.
     */
    private final static String SELECTED_ZONEIDS = "walkingkooka.j2cl.java.util.TimeZone";

    private final static String URL = "(https://github.com/mP1/j2cl-java-util-TimeZone-annotation-processor)";

    // default..........................................................................................................

    @Override
    protected Optional<String> defaultValue(final Set<String> selectedLocales,
                                            final Function<String, String> arguments) {
        final String defaultTimeZone = arguments.apply(DEFAULT_TIMEZONE);
        final String timeZoneFilter = this.timeZoneFilter(arguments);

        if (false == timeZones(timeZoneFilter).contains(defaultTimeZone)) {
            throw new IllegalArgumentException(
                    "Default timezone " +
                            CharSequences.quoteAndEscape(defaultTimeZone) +
                            " not in selected timeZone " +
                            CharSequences.quoteAndEscape(timeZoneFilter)
            );
        }
        return Optional.of(defaultTimeZone);
    }

    private final static String DEFAULT_TIMEZONE = "walkingkooka.j2cl.java.util.TimeZone.DEFAULT";

    // generate.........................................................................................................

    @Override
    protected String generate(final String filter,
                              final Set<String> languageTags,
                              final Function<String, String> arguments,
                              final DataOutput data,
                              final IndentingPrinter comments) throws Exception {
        final String timeZoneFilter = timeZoneFilter(arguments);
        final Set<String> timeZones = timeZones(timeZoneFilter);

        TimeZoneProviderTool.generate(
                LocaleAwareAnnotationProcessorTool.toLocales(languageTags),
                timeZones,
                data,
                comments
        );

        return LocaleAwareAnnotationProcessorTool.extractSummary(
                languageTags.size(),
                "Locale",
                filter) +
                ", " +
                LocaleAwareAnnotationProcessorTool.extractSummary(
                        timeZones.size(),
                        "TimeZone",
                        timeZoneFilter
                );
    }
}
