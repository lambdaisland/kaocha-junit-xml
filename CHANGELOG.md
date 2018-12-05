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
