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
import walkingkooka.HashCodeEqualsDefinedTesting2;
import walkingkooka.ToStringTesting;
import walkingkooka.compare.ComparableTesting2;
import walkingkooka.reflect.ClassTesting2;
import walkingkooka.reflect.JavaVisibility;

public final class TimeZoneProviderToolCalendarTest implements ClassTesting2<TimeZoneProviderToolCalendar>,
        HashCodeEqualsDefinedTesting2<TimeZoneProviderToolCalendar>,
        ComparableTesting2<TimeZoneProviderToolCalendar>,
        ToStringTesting<TimeZoneProviderToolCalendar> {

    private final static int FIRST = 1;
    private final static int MINIMAL = 5;

    @Test
    public void testCompareLess() {
        this.compareToAndCheckLess(TimeZoneProviderToolCalendar.with(FIRST + 1, MINIMAL));
    }

    @Test
    public void testCompareLess2() {
        this.compareToAndCheckLess(TimeZoneProviderToolCalendar.with(FIRST, MINIMAL + 1));
    }

    @Test
    public void testCompareSorted() {
        final TimeZoneProviderToolCalendar a = TimeZoneProviderToolCalendar.with(FIRST, MINIMAL);
        final TimeZoneProviderToolCalendar b = TimeZoneProviderToolCalendar.with(FIRST, MINIMAL + 1);
        final TimeZoneProviderToolCalendar c = TimeZoneProviderToolCalendar.with(FIRST, MINIMAL + 5);
        final TimeZoneProviderToolCalendar d = TimeZoneProviderToolCalendar.with(FIRST + 1, MINIMAL);

        this.compareToArraySortAndCheck(d, c, a, b, a, b, c, d);
    }

    @Test
    public void testToString() {
        this.toStringAndCheck(this.createComparable().toString(),
                "firstDayOfWeek=1 minimalDaysInFirstWeek=5");
    }

    @Override
    public TimeZoneProviderToolCalendar createComparable() {
        return TimeZoneProviderToolCalendar.with(FIRST, MINIMAL);
    }

    @Override
    public Class<TimeZoneProviderToolCalendar> type() {
        return TimeZoneProviderToolCalendar.class;
    }

    @Override
    public JavaVisibility typeVisibility() {
        return JavaVisibility.PACKAGE_PRIVATE;
    }
}
