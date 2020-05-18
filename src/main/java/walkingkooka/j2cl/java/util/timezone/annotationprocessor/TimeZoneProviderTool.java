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
import walkingkooka.j2cl.locale.TimeZoneCalendar;
import walkingkooka.j2cl.locale.TimeZoneDisplay;
import walkingkooka.j2cl.locale.TimeZoneOffsetProvider;
import walkingkooka.j2cl.locale.WalkingkookaLanguageTag;
import walkingkooka.j2cl.locale.annotationprocessor.LocaleAwareAnnotationProcessor;
import walkingkooka.j2cl.locale.annotationprocessor.LocaleAwareAnnotationProcessorTool;
import walkingkooka.text.LineEnding;
import walkingkooka.text.printer.IndentingPrinter;
import walkingkooka.text.printer.Printer;
import walkingkooka.text.printer.Printers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
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
 *     int default firstDayOfWeek
 *     int default minimalDaysInFirstWeek
 *
 *     group for each locales with common
           int locale count
 *         for each locale
 *             String locale language tag
 *         end
 *
 *         int firstdayofweek
 *         int minimaldaysinfirstweek
 *      end
 *
 *     String default shortDisplayText
 *     String default shortDisplayTextDaylight
 *     String default longDisplayText
 *     String default longDisplayTextDaylight
 *
 *     int display to locales count
 *
 *     for each display to locales
 *         int locale count
 *         for each locale
 *             String locale language tag
 *         end
 *
 *         String shortDisplayText
 *         String shortDisplayTextDaylight
 *         String longDisplayText
 *         String longDisplayTextDaylight
 *     end
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

    static void generate(final Set<Locale> locales,
                         final Set<String> timezoneIds,
                         final DataOutput data,
                         final IndentingPrinter comments) throws Exception {
        new TimeZoneProviderTool(locales,
                timezoneIds,
                data,
                comments)
                .generate0();
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
        final DataOutput data = this.data;
        final IndentingPrinter comments = this.comments;

        this.generateTimeZoneId(zoneId);
        final TimeZone timeZone = TimeZone.getTimeZone(zoneId);
        this.generateRawOffset(timeZone);

        this.generateGregorianCalendarData(timeZone);

        final Map<TimeZoneDisplay, Set<Locale>> displayToLocales = populateDisplayToLocales(zoneId);

        final TimeZoneDisplay mostDisplay = LocaleAwareAnnotationProcessorTool.findMostPopularLocaleKey(displayToLocales);

        this.generateCommentLocalesToDisplay(displayToLocales);

        // the display with the most locales will be removed.
        displayToLocales.remove(mostDisplay);

        generateDisplay(mostDisplay, "default ");
        this.generateDisplayToLocales(displayToLocales);

        comments.lineStart();
        comments.print(comments.lineEnding());
    }

    private void generateDisplayToLocales(final Map<TimeZoneDisplay, Set<Locale>> displayToLocales) throws IOException {
        final DataOutput data = this.data;
        final IndentingPrinter comments = this.comments;

        // write all other display and locales
        data.writeInt(displayToLocales.size());
        for (final Entry<TimeZoneDisplay, Set<Locale>> displayAndLocales : displayToLocales.entrySet()) {
            LocaleAwareAnnotationProcessorTool.generateLocales(displayAndLocales.getValue(),
                    data,
                    "locales",
                    comments);
            comments.indent();
            {
                generateDisplay(displayAndLocales.getKey(), "");
            }
            comments.outdent();
        }
    }

    private void generateDisplay(final TimeZoneDisplay display, final String prefix) throws IOException {
        final IndentingPrinter comments = this.comments;

        comments.lineStart();
        comments.print(prefix + "shortDisplayName: " + display.shortDisplayName);

        comments.lineStart();
        comments.print(prefix + "shortDisplayNameDaylight: " + display.shortDisplayNameDaylight);

        comments.lineStart();
        comments.print(prefix + "longDisplayName: " + display.longDisplayName);

        comments.lineStart();
        comments.print(prefix + "longDisplayNameDaylight " + display.longDisplayNameDaylight);

        display.write(this.data);
    }

    private void generateTimeZoneId(final String zoneId) throws IOException {
        this.comments.lineStart();
        this.comments.print(zoneId);
        this.data.writeUTF(zoneId);
    }

    private void generateRawOffset(final TimeZone timeZone) throws IOException {
        final int rawOffset = timeZone.getRawOffset();
        this.comments.lineStart();
        this.comments.print("rawOffset: " + rawOffset);
        this.data.writeInt(rawOffset);
    }

    /**
     * <pre>
     * for each locale
     *   int firstdayofweek
     *   int minimaldaysinfirstweek
     * </pre>
     **/
    private void generateGregorianCalendarData(final TimeZone timeZone) throws IOException {
        final Map<TimeZoneCalendar, Set<Locale>> calendarToLocales = LocaleAwareAnnotationProcessorTool.buildMultiLocaleMap(
                localeToTimeZoneProviderToolGregorianCalender(timeZone),
                this.locales);

        final DataOutput data = this.data;
        final IndentingPrinter comments = this.comments;

        TimeZoneOffsetProvider.collect(timeZone)
                .generate(data, comments);

        // find most popular and write that as a default.
        final TimeZoneCalendar most = LocaleAwareAnnotationProcessorTool.findMostPopularLocaleKey(calendarToLocales);
        most.generate(data, "default ", comments);

        calendarToLocales.remove(most);

        // other
        data.writeInt(calendarToLocales.size());
        for (final Entry<TimeZoneCalendar, Set<Locale>> calendarAndLocales : calendarToLocales.entrySet()) {
            LocaleAwareAnnotationProcessorTool.generateLocales(calendarAndLocales.getValue(),
                    data,
                    "locales",
                    comments);

            comments.indent();
            {
                calendarAndLocales.getKey()
                        .generate(data, "", comments);
            }
            comments.outdent();
        }
    }

    private static Function<Locale, TimeZoneCalendar> localeToTimeZoneProviderToolGregorianCalender(final TimeZone timeZone) {
        return locale -> TimeZoneCalendar.with((GregorianCalendar) GregorianCalendar.getInstance(timeZone, locale));
    }

    private void generateCommentLocalesToDisplay(final Map<TimeZoneDisplay, Set<Locale>> displayToLocales) {
        final Map<String, TimeZoneDisplay> localesStringToDisplay = Maps.sorted(); // sorts locales to display alphabetically for comments
        for (final Entry<TimeZoneDisplay, Set<Locale>> displayAndLocales : displayToLocales.entrySet()) {
            final TimeZoneDisplay display = displayAndLocales.getKey();
            final Set<Locale> displayLocales = displayAndLocales.getValue();
            localesStringToDisplay.put(displayLocales.stream()
                            .map(Locale::toLanguageTag)
                            .collect(Collectors.joining(", ")),
                    display);
        }

        final IndentingPrinter comments = this.comments;

        localesStringToDisplay.forEach((l, d) -> {
            comments.lineStart();
            comments.print(l + "=" + d);
        });
    }

    private Map<TimeZoneDisplay, Set<Locale>> populateDisplayToLocales(final String zoneId) {
        return LocaleAwareAnnotationProcessorTool.buildMultiLocaleMap(this.timeZoneDisplay(zoneId),
                this.locales);
    }

    private Function<Locale, TimeZoneDisplay> timeZoneDisplay(final String zoneId) {
        return locale -> {
            final TimeZone timeZone = TimeZone.getTimeZone(zoneId);
            return TimeZoneDisplay.with(timeZone.getDisplayName(false, TimeZone.SHORT, locale),
                    timeZone.getDisplayName(true, TimeZone.SHORT, locale),
                    timeZone.getDisplayName(false, TimeZone.LONG, locale),
                    timeZone.getDisplayName(true, TimeZone.LONG, locale));
        };
    }

    private final Set<Locale> locales;
    private final Set<String> timezoneIds;
    private final DataOutput data;
    private final IndentingPrinter comments;
}
