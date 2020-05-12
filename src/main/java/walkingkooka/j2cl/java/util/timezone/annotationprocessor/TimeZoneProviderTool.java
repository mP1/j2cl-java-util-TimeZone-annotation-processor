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

import walkingkooka.collect.map.Maps;
import walkingkooka.collect.set.Sets;
import walkingkooka.j2cl.java.io.string.StringDataInputDataOutput;
import walkingkooka.j2cl.locale.TimeZoneDisplay;
import walkingkooka.j2cl.locale.WalkingkookaLanguageTag;
import walkingkooka.j2cl.locale.annotationprocessor.LocaleAwareAnnotationProcessor;
import walkingkooka.text.LineEnding;
import walkingkooka.text.printer.IndentingPrinter;
import walkingkooka.text.printer.Printer;
import walkingkooka.text.printer.Printers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * This tool prints to {@link DataOutput} all data to make {@link TimeZone#getDisplayName()} work.
 * <pre>
 * int timeZoneIdCount
 *
 * for each timeZoneId
 *     String timeZoneId
 *     int rawOffset
 *
 *     String default shortDisplayText
 *     String default shortDisplayTextDaylight
 *     String default longDisplayText
 *     String default longDisplayTextDaylight
 *
 *     int display to locales count
 *
 *     for each display to locales
 *
 *         String shortDisplayText
 *         String shortDisplayTextDaylight
 *         String longDisplayText
 *         String longDisplayTextDaylight
 *
 *         int locale count
 *         for each locale
 *             String locale language tag
 *         end
 *     end
 * end
 * </pre>
 */
public final class TimeZoneProviderTool {

    public static void main(final String[] args) throws Exception {
        try (final Printer printer = Printers.sysOut()) {
            final StringBuilder data = new StringBuilder();

            final TimeZoneProviderTool tool = new TimeZoneProviderTool(WalkingkookaLanguageTag.locales("*"),
                    timezoneIds("*"),
                    StringDataInputDataOutput.output(data::append),
                    LocaleAwareAnnotationProcessor.comments(printer));
            tool.generate0();

            String left = data.toString();
            int i = 0;
            do {
                printer.print(printer.lineEnding());
                if (left.length() < 160) {
                    printer.print(left);
                    break;
                }
                printer.print(left.substring(0, 160));
                left = left.substring(160);

                i += 160;
            } while (i < 4096);

            printer.print(LineEnding.SYSTEM);
            printer.print("data size: " + data.length());
            printer.print(LineEnding.SYSTEM);

            try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                try (final GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                    gzip.write(data.toString().getBytes(Charset.defaultCharset()));
                    gzip.flush();
                }

                printer.print("gzip size: " + bytes.toByteArray().length);
                printer.print(LineEnding.SYSTEM);
            }

            //printer.print(CharSequences.quoteAndEscape(data));
            printer.flush();
        }
    }

    /**
     * Uses the filter to select all or a subset of available zone ids.
     */
    static Set<String> timezoneIds(final String filter) {
        final Predicate<String> predicate = WalkingkookaLanguageTag.filter(filter);
        return Arrays.stream(TimeZone.getAvailableIDs())
                .filter(predicate)
                .collect(Collectors.toCollection(Sets::sorted));
    }

    static void generate(final Set<String> languageTags,
                         final Set<String> timezoneIds,
                         final DataOutput data,
                         final IndentingPrinter comments) throws Exception {
        new TimeZoneProviderTool(languageTags.stream()
                .map(Locale::forLanguageTag)
                .collect(Collectors.toCollection(TimeZoneProviderTool::localeSet)),
                timezoneIds,
                data,
                comments)
                .generate0();
    }

    private static Set<Locale> localeSet() {
        return Sets.sorted(TimeZoneProviderTool::compareLocaleLanguageTag);
    }

    private static int compareLocaleLanguageTag(final Locale left, final Locale right) {
        return left.toLanguageTag().compareTo(right.toLanguageTag());
    }


    private TimeZoneProviderTool(final Set<Locale> locales,
                                 final Set<String> timezoneIds,
                                 final DataOutput data,
                                 final IndentingPrinter comments) {
        super();
        this.locales = locales;
        this.timezoneIds = timezoneIds;
        this.data = data;
        this.comments = comments;
    }

    private void generate0() throws Exception {
        this.comments.lineStart();
        this.comments.print("Timezone ids: " + this.timezoneIds.size() + ", locales: " + this.locales.size());

        this.data.writeInt(this.timezoneIds.size());

        for (final String zoneId : this.timezoneIds) {
            generateZoneId(zoneId);
        }
    }

    private void generateZoneId(final String zoneId) throws Exception {
        final Set<Locale> locales = this.locales;
        final DataOutput data = this.data;
        final IndentingPrinter comments = this.comments;

        // timeZoneId
        comments.lineStart();
        comments.print(zoneId);
        data.writeUTF(zoneId);

        final TimeZone timeZone = TimeZone.getTimeZone(zoneId);

        // rawOffset
        final int rawOffset = timeZone.getRawOffset();
        comments.lineStart();
        comments.print("rawOffset: " + rawOffset);
        data.writeInt(rawOffset);

        final Map<TimeZoneDisplay, Set<Locale>> displayToLocales = Maps.sorted();

        for (final Locale locale : locales) {
            final TimeZoneDisplay display = display(zoneId, locale);
            Set<Locale> displayLocales = displayToLocales.get(display);
            if (null == displayLocales) {
                displayLocales = localeSet();
                displayToLocales.put(display, displayLocales);
            }
            displayLocales.add(locale);
        }

        // find the most popular display
        int mostDisplayLocaleCounts = -1;
        TimeZoneDisplay mostDisplay = null;

        final Map<String, TimeZoneDisplay> localesStringToDisplay = Maps.sorted(); // sorts locales to display alphabetically for comments

        for (final Entry<TimeZoneDisplay, Set<Locale>> displayAndLocales : displayToLocales.entrySet()) {
            final TimeZoneDisplay display = displayAndLocales.getKey();
            final Set<Locale> displayLocales = displayAndLocales.getValue();
            final int count = displayLocales.size();

            if (count > mostDisplayLocaleCounts) {
                if (count > mostDisplayLocaleCounts) {
                    mostDisplayLocaleCounts = count;
                    mostDisplay = display;
                }
            }

            localesStringToDisplay.put(displayLocales.stream()
                            .map(Locale::toLanguageTag)
                            .collect(Collectors.joining(", ")),
                    display);
        }

        localesStringToDisplay.forEach((l, d) -> {
            comments.lineStart();
            comments.print(l + "=" + d);
        });

        // the display with the most locales will be removed.
        displayToLocales.remove(mostDisplay);

        // write the display text for the most popular locales
        mostDisplay.write(data);

        // write all other display and locales
        data.writeInt(displayToLocales.size());
        for (final Entry<TimeZoneDisplay, Set<Locale>> displayAndLocales : displayToLocales.entrySet()) {
            displayAndLocales.getKey().write(data);

            final Set<Locale> displayLocales = displayAndLocales.getValue();
            data.writeInt(displayLocales.size());
            for (final Locale locale : displayLocales) {
                data.writeUTF(locale.toLanguageTag());
            }
        }

        comments.lineStart();
        comments.print(comments.lineEnding());
    }

    private static TimeZoneDisplay display(final String zoneId,
                                           final Locale locale) {
        final TimeZone timeZone = TimeZone.getTimeZone(zoneId);
        return TimeZoneDisplay.with(timeZone.getDisplayName(false, TimeZone.SHORT, locale),
                timeZone.getDisplayName(true, TimeZone.SHORT, locale),
                timeZone.getDisplayName(false, TimeZone.LONG, locale),
                timeZone.getDisplayName(true, TimeZone.LONG, locale));
    }

    private final Set<Locale> locales;
    private final Set<String> timezoneIds;
    private final DataOutput data;
    private final IndentingPrinter comments;
}
