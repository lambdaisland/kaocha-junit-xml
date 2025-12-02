# Unreleased

## Added

## Fixed

* Reporting test cases marked with "pending" Kaocha tag as "skipped" in JUnit XML output (Issue #20)

## Changed

# 1.17.101 (2022-11-09 / 95067b2)

## Fixed

* Fix Cljdoc build.

# 1.16.98 (2022-07-26 / d50528d)

## Fixed

- Fix `--junit-xml-add-location-metadata` command line flag

# 1.15.95 (2022-07-26 / de6d134)

## Added

- Added a flag to include file location metadata as attributes on testcases, à la
  pytest.

# 0.0.76 (2020-07-21 / 397a3c1)

## Added

- Added a flag to omit `system-out` from the generated junit.xml file, (thanks @ondrs)

# 0.0-70 (2019-03-30 / 0377e39)

## Fixed

- `<` and `>` are now escaped in XML attributes

# 0.0-63 (2019-02-15 / 5243781)

## Fixed

- Fix potential NullPointerException

# 0.0-57 (2019-02-15 / 3804cb7)

## Fixed

- Addressed a warning.

# 0.0-53 (2019-01-28 / 69d2e2f)

## Fixed

- Render non-leaf test types (e.g. clojure.test / ns) if they contain failures
  or errors (e.g. load errors).

# 0.0-50 (2018-12-28 / d44f155)

## Changed

- The rendering of errors and failures has been made more in line with what
  consumers expect, with a one-line message attribute, and with full multiline
  output as a text element.
- Each failure and error is now output as a separate XML element, rather than
  being grouped into a single element for each type.
- Rendering of errors (exceptions) will look for a
  `:kaocha.report/error-message` and `:kaocha.report/error-type`, before falling
  back to calling `.getMessage` / `.getClass` on the Exception. This is for
  cases like ClojureScript where the error is originating and captured outside
  the JVM.

## Fixed

- Fixed an issue in the code that strips out ANSI escape sequences, to prevent
  it from eating up some of the input.

# 0.0-47 (2018-12-07 / db418fa)

## Fixed

- Detect "leaf" test types through Kaocha's hierarchy functions, this fixes
  compatibility with `kaocha-cucumber`

# 0.0-43 (2018-12-05 / 311587e)

## Fixed

- Address cljdoc analysis error preventing the docs to build

# 0.0-39 (2018-12-05 / 1507bab)

## Fixed

- Automatically create parent directory of output file if it doesn't exist

## Changed

- Encode single quote as the more widely understood `&apos;` rather than `&#27;`

# 0.0-31 (2018-11-20 / 060108f)

## Fixed

- Make XML output strictly conform to the JUnit XML schema ([#2](https://github.com/lambdaisland/kaocha-junit-xml/issues/2))

## Changed

- Strip escape characters in text node, they are not valid XML
- Strip ANSI color codes
- Number of skipped tests and number of assertions are no longer reported. While
  some sources seem to suggest they are part of the JUnit XML format, they are
  not part of the schema, and so hinder validation.

# 0.0-27 (2018-11-17 / a7f8432)

## Fixed

- Fix entity escaping of text nodes and attribute values in output XML ([#1](https://github.com/lambdaisland/kaocha-junit-xml/issues/1))

# 0.0-18 (2018-11-05 / 83a953b)

## Changed

- error elements now contain the full stack trace as a child element, and only
  the short message as a message attribute

# 0.0-13 (2018-11-01 / a22889b)

## Fixed

- Make target file configurable in tests.edn

# 0.0-7 (2018-10-31 / 163d219)

First release.