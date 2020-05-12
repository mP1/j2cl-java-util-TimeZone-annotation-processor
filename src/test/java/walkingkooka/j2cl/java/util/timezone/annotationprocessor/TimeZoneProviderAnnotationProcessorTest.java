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
import walkingkooka.reflect.ClassTesting;
import walkingkooka.reflect.JavaVisibility;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TimeZoneProviderAnnotationProcessorTest implements ClassTesting<TimeZoneProviderAnnotationProcessor> {

    @Test
    public void testDefaultPublicConstructor() throws Exception {
        assertEquals(JavaVisibility.PUBLIC, JavaVisibility.of(TimeZoneProviderAnnotationProcessor.class.getConstructor()));
    }

    @Override
    public Class<TimeZoneProviderAnnotationProcessor> type() {
        return TimeZoneProviderAnnotationProcessor.class;
    }

    @Override
    public JavaVisibility typeVisibility() {
        return JavaVisibility.PUBLIC;
    }
}
