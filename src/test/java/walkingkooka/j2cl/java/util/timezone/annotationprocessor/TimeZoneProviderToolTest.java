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

import org.junit.jupiter.api.Test;
import walkingkooka.collect.set.Sets;
import walkingkooka.j2cl.java.io.string.StringDataInputDataOutput;
import walkingkooka.j2cl.locale.TimeZoneDisplay;
import walkingkooka.j2cl.locale.TimeZoneOffsetProvider;
import walkingkooka.j2cl.locale.WalkingkookaLanguageTag;
import walkingkooka.reflect.ClassTesting;
import walkingkooka.reflect.JavaVisibility;
import walkingkooka.text.CharSequences;
import walkingkooka.text.Indentation;
import walkingkooka.text.printer.Printers;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TimeZoneProviderToolTest implements ClassTesting<TimeZoneProviderTool> {

    @Test
    public void testGenerateReadAndVerifyLocaleENAUTimeZoneIdAustralia() throws Exception {
        this.generateReadAndVerify("EN-AU", "Australia/*");
    }

    @Test
    public void testGenerateReadAndVerifyLocaleENAUFRTimeZoneIdAustralia() throws Exception {
        this.generateReadAndVerify("EN-AU,FR", "Australia/*");
    }

    @Test
    public void testGenerateReadAndVerifyLocaleENTimeZoneIdAustralia() throws Exception {
        this.generateReadAndVerify("EN", "Australia/*");
    }

    @Test
    public void testGenerateReadAndVerifyLocaleENWildcardTimeZoneIdAustralia() throws Exception {
        this.generateReadAndVerify("EN*", "Australia/*");
    }

    @Test
    public void testGenerateReadAndVerifyLocaleWildcardTimeZoneIdAct() throws Exception {
        this.generateReadAndVerify("*", "ACT");
    }

    @Test
    public void testGenerateReadAndVerifyAllLocalesAllTimeZoneIds() throws Exception {
        this.generateReadAndVerify("*", "*");
    }

    private void generateReadAndVerify(final String localeFilter,
                                       final String timeZoneIdFilter) throws Exception {
        final Set<Locale> selectedLocales = WalkingkookaLanguageTag.locales(localeFilter);
        final Set<String> timeZoneIds = TimeZoneProviderTool.timezoneIds(timeZoneIdFilter);

        assertNotEquals(0, selectedLocales.size(), "no locales");
        assertNotEquals(0, timeZoneIds.size(), "no timeZoneIds");

        final StringBuilder dataText = new StringBuilder();

        TimeZoneProviderTool.generate(selectedLocales,
                timeZoneIds,
                StringDataInputDataOutput.output(dataText::append),
                Printers.sink().indenting(Indentation.with("  ")));

        final DataInput data = StringDataInputDataOutput.input(dataText.toString());
        final int zoneIdCount = data.readInt();
        assertEquals(timeZoneIds.size(),
                zoneIdCount,
                "timeZone count timeZoneIdFilter: " + CharSequences.quoteAndEscape(timeZoneIdFilter));

        for (int i = 0; i < zoneIdCount; i++) {
            final String timeZoneId = data.readUTF();
            assertNotEquals("", timeZoneId, "timeZoneId");

            final TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
            assertNotEquals("",
                    timeZone,
                    () -> "timezoneId " + CharSequences.quoteAndEscape(timeZoneId) + " data: " + data.toString().substring(0, 50));

            // getRawOffset.............................................................................................
            final int rawOffset = data.readInt();
            assertEquals(timeZone.getRawOffset(),
                    rawOffset,
                    () -> "rawOffset for timeZoneId " + CharSequences.quoteAndEscape(timeZoneId) + " data: " + data.toString().substring(0, 50));

            // timeZoneOffset.............................................................................................
            final TimeZoneOffsetProvider offsets = TimeZoneOffsetProvider.read(rawOffset, data);

            // firstDayOfWeek, minimalDaysInFirstWeek -> locales........................................................
            final Set<Locale> localesWithDefault = Sets.ordered();
            localesWithDefault.addAll(selectedLocales);

            final int defaultFirstDayOfWeek = data.readInt();
            final int defaultMinimalDaysInFirstWeek = data.readInt();

            final int calendarToLocalesCount = data.readInt();
            for (int j = 0; j < calendarToLocalesCount; j++) {
                final Set<Locale> locales = readAndCheckLocales(data, (localeCount) -> "locale count " + localeCount + " < 0 for timeZoneId: " + CharSequences.quoteAndEscape(timeZoneId) + " data: " + data.toString().substring(0, 50));

                final int firstDayOfWeek = data.readInt();
                final int minimalDaysInFirstWeek = data.readInt();

                for (final Locale locale : locales) {
                    this.checkCalendarData(firstDayOfWeek, minimalDaysInFirstWeek, timeZone, locale);
                    localesWithDefault.remove(locale);
                }
            }

            for(final Locale locale : localesWithDefault) {
                this.checkCalendarData(defaultFirstDayOfWeek, defaultMinimalDaysInFirstWeek, timeZone, locale);
            }

            // default TimeZoneDisplay..................................................................................
            final TimeZoneDisplay most = TimeZoneDisplay.read(data);

            assertNotEquals("",
                    most.shortDisplayName,
                    () -> "shortDisplayName " + CharSequences.quoteAndEscape(most.shortDisplayName));
            assertNotEquals("",
                    most.shortDisplayNameDaylight,
                    () -> "shortDisplayNameDaylight " + CharSequences.quoteAndEscape(most.shortDisplayName));
            assertNotEquals("",
                    most.longDisplayName,
                    () -> "longDisplayName " + CharSequences.quoteAndEscape(most.longDisplayName));
            assertNotEquals("",
                    most.longDisplayNameDaylight,
                    () -> "longDisplayNameDaylight " + CharSequences.quoteAndEscape(most.longDisplayName));

            // display to locales.......................................................................................

            final int displayToLocalesCount = data.readInt();
            assertTrue(displayToLocalesCount >= 0,
                    () -> "locale displayToLocalesCount " + displayToLocalesCount + " < 0 for timeZoneId: " + CharSequences.quoteAndEscape(timeZoneId) + " data: " + data.toString().substring(0, 50));

            final Set<Locale> mostLocales = Sets.ordered();
            mostLocales.addAll(selectedLocales);

            for (int j = 0; j < displayToLocalesCount; j++) {
                final Set<Locale> locales = readAndCheckLocales(data, (localeCount) -> "locale count " + localeCount + " < 0 for timeZoneId: " + CharSequences.quoteAndEscape(timeZoneId) + " data: " + data.toString().substring(0, 50));

                final TimeZoneDisplay display = TimeZoneDisplay.read(data);

                for (final Locale locale : locales) {
                    checkDisplayName(timeZone, false, TimeZone.SHORT, locale, display.shortDisplayName);
                    checkDisplayName(timeZone, true, TimeZone.SHORT, locale, display.shortDisplayNameDaylight);
                    checkDisplayName(timeZone, false, TimeZone.LONG, locale, display.longDisplayName);
                    checkDisplayName(timeZone, true, TimeZone.LONG, locale, display.longDisplayNameDaylight);

                    mostLocales.remove(locale);
                }
            }

            // check the display for $mostLocales
            for (final Locale locale : mostLocales) {
                checkDisplayName(timeZone, false, TimeZone.SHORT, locale, most.shortDisplayName);
                checkDisplayName(timeZone, true, TimeZone.SHORT, locale, most.shortDisplayNameDaylight);
                checkDisplayName(timeZone, false, TimeZone.LONG, locale, most.longDisplayName);
                checkDisplayName(timeZone, true, TimeZone.LONG, locale, most.longDisplayNameDaylight);
            }
        }

        assertThrows(EOFException.class, () -> data.readBoolean());
    }

    private void checkCalendarData(final int firstDayOfWeek,
                                   final int minimalDaysInFirstWeek,
                                   final TimeZone timeZone,
                                   final Locale locale) {
        assertEquals(GregorianCalendar.getInstance(timeZone, locale).getFirstDayOfWeek(),
                firstDayOfWeek,
                () -> "firstDayOfWeek for timeZone: " + timeZone.getID() + " locale: " + locale);
        assertEquals(GregorianCalendar.getInstance(timeZone, locale).getMinimalDaysInFirstWeek(),
                minimalDaysInFirstWeek,
                () -> "minimalDaysInFirstWeek for timeZone: " + timeZone.getID() + " locale: " + locale);
    }

    private Set<Locale> readAndCheckLocales(final DataInput data,
                                            final Function<Integer, String> badLocaleCount) throws IOException {
        final int count = data.readInt();
        assertTrue(count >= 0,
                () -> badLocaleCount.apply(count));

        final Set<Locale> locales = Sets.ordered();
        for (int k = 0; k < count; k++) {
            locales.add(this.checkLocale(data.readUTF()));
        }

        return locales;
    }

    private Locale checkLocale(final String locale) {
        final Locale localeObject = Locale.forLanguageTag(locale);
        assertEquals(locale, localeObject.toLanguageTag(), "Bad locale");
        return localeObject;
    }

    private static void checkDisplayName(final TimeZone timeZone,
                                         final boolean daylight,
                                         final int style,
                                         final Locale locale,
                                         final String expected) {
        assertEquals(expected,
                timeZone.getDisplayName(daylight, style, locale),
                () -> "TimeZone " + CharSequences.quoteAndEscape(timeZone.getID()) + " daylight=" + daylight + " style=" + (TimeZone.SHORT == style ? "SHORT" : "LONG") + " locale: " + locale);
    }

    // ClassTesting.....................................................................................................

    @Override
    public Class<TimeZoneProviderTool> type() {
        return TimeZoneProviderTool.class;
    }

    @Override
    public JavaVisibility typeVisibility() {
        return JavaVisibility.PUBLIC;
    }
}
