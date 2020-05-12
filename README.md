[![Build Status](https://travis-ci.com/mP1/j2cl-java-util-TimeZone-annotation-processor.svg?branch=master)](https://travis-ci.com/mP1/j2cl-java-util-TimeZone-annotation-processor.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/mP1/j2cl-java-util-TimeZone-annotation-processor/badge.svg?branch=master)](https://coveralls.io/github/mP1/j2cl-java-util-TimeZone-annotation-processor?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/mP1/j2cl-java-util-TimeZone-annotation-processor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/j2cl-java-util-TimeZone-annotation-processor/context:java)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/mP1/j2cl-java-util-TimeZone-annotation-processor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/j2cl-java-util-TimeZone-annotation-processor/alerts/)



# j2cl java-util-TimeZone-annotation-processor

An annotation processor that generates the `TimeZoneProvider` used by the emulated `java.util.TimeZone` in 
[j2cl-java-util-TimeZone](https://travis-ci.com/mP1/j2cl-java-util-TimeZone), to the `java.util.TimeZone#getDisplay` methods in javascript
after building with [j2cl-maven-plugin](https://travis-ci.com/mP1/j2cl-maven-plugin).

To select which locales are included set the `walkingkooka.j2cl.java.util.Locale` annotation processor argument.

```text
-Awalkingkooka.j2cl.java.util.Locale=EN*
-Awalkingkooka.j2cl.java.util.TimeZone=Australia*
```

This selects all locales starting with `EN` and Australian timezones.

For more details [click here](https://github.com/mP1/j2cl-locale)



## Unsupported features.

See [j2cl-java-util-TimeZone](https://travis-ci.com/mP1/j2cl-java-util-TimeZone) for a more comprehensive summary.



## Getting the source

You can either download the source using the "ZIP" button at the top
of the github page, or you can make a clone using git:

```
git clone git://github.com/mP1/j2cl-java-util-TimeZone-annotation-processor.git
```
