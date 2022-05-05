# lambdaisland/kaocha-junit-xml

<!-- badges -->
[![CircleCI](https://circleci.com/gh/lambdaisland/kaocha-junit-xml.svg?style=svg)](https://circleci.com/gh/lambdaisland/kaocha-junit-xml) [![cljdoc badge](https://cljdoc.org/badge/lambdaisland/kaocha-junit-xml)](https://cljdoc.org/d/lambdaisland/kaocha-junit-xml) [![Clojars Project](https://img.shields.io/clojars/v/lambdaisland/kaocha-junit-xml.svg)](https://clojars.org/lambdaisland/kaocha-junit-xml)
<!-- /badges -->

[Kaocha](https://github.com/lambdaisland/kaocha) plugin to generate a JUnit XML version of the test results.

<!-- opencollective -->

&nbsp;

<img align="left" src="https://github.com/lambdaisland/open-source/raw/master/artwork/lighthouse_readme.png">

&nbsp;

## Support Lambda Island Open Source

kaocha-junit-xml is part of a growing collection of quality Clojure libraries and
tools released on the Lambda Island label. If you are using this project
commercially then you are expected to pay it forward by
[becoming a backer on Open Collective](http://opencollective.com/lambda-island#section-contribute),
so that we may continue to enjoy a thriving Clojure ecosystem.

&nbsp;

&nbsp;

<!-- /opencollective -->

## Usage

- Add kaocha-junit-xml as a dependency

``` clojure
;; deps.edn
{:aliases
 {:test
  {:extra-deps {lambdaisland/kaocha {...}
                lambdaisland/kaocha-junit-xml {:mvn/version "0.0.76"}}}}}
```

or

``` clojure
;; project.clj
(defproject ,,,
  :dependencies [,,,
                 [lambdaisland/kaocha-junit-xml "0.0.76"]])
```

- Enable the plugin and set an output file

``` clojure
;; tests.edn
#kaocha/v1
{:plugins [:kaocha.plugin/junit-xml]
 :kaocha.plugin.junit-xml/target-file "junit.xml"}
```

Or from the CLI

``` shell
bin/kaocha --plugin kaocha.plugin/junit-xml --junit-xml-file junit.xml
```

Optionally you can omit captured output from junit.xml

``` clojure
;; tests.edn
#kaocha/v1
{:plugins [:kaocha.plugin/junit-xml]
 :kaocha.plugin.junit-xml/target-file      "junit.xml"
 :kaocha.plugin.junit-xml/omit-system-out? true}
```

Or from the CLI

``` shell
bin/kaocha --plugin kaocha.plugin/junit-xml --junit-xml-file junit.xml --junit-xml-omit-system-out
```

## Requirements

Requires at least Kaocha 0.0-306 and Clojure 1.9.

## Examples

### CircleCI

One of the services that can use this output is CircleCI. Your
`.circleci/config.yml` could look like this:

``` yml
version: 2
jobs:
  build:
    docker:
      - image: circleci/clojure:tools-deps-1.9.0.394
    steps:
      - checkout
      - run: mkdir -p test-results/kaocha
      - run: bin/kaocha --plugin kaocha.plugin/junit-xml --junit-xml-file test-results/kaocha/results.xml
      - store_test_results:
          path: test-results
```

### GitHub Actions

The following `.github/workflows/build.yml` configuration will create annotations 
for test failures on files of the relevant commit/PR:

```yml
name: Build
on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    container:
      image: clojure:openjdk-8-tools-deps-1.11.1.1113
    steps:
      - uses: actions/checkout@v2
      - name: test
        run: |
          bin/kaocha \
            --plugin kaocha.plugin/junit-xml \
            --junit-xml-file junit.xml \
            --add-location-metadata
      - name: Annotate failure
        if: failure()
        uses: mikepenz/action-junit-report@41a3188dde10229782fd78cd72fc574884dd7686
        with:
          report_paths: junit.xml
```


## Caveats

For timing information (timestamp and running time) this plugin relies on the
`kaocha.plugin/profiling` plugin. If the plugin is not present then a running
time of 0 will be reported.

For output capturing the `kaocha.plugin/capture-output` must be present. If it
is not present `<system-out>` will always be empty.

## Resources

It was hard to find a definitive source of the Ant Junit XML format. I mostly
went with [this page](http://llg.cubic.org/docs/junit/) for documentation.

For information on how to configure CircleCI to use this information, see
[store_test_results](https://circleci.com/docs/2.0/configuration-reference/#store_test_results).

After reports that the output was not compatible with Azure Devops Pipeline the
output was changed to adhere to [this schema](https://github.com/windyroad/JUnit-Schema/blob/49e95a79cc0bfba7961aaf779710a43a4d3f96bd/JUnit.xsd).

### Gitlab

Configuring Gitlab to parse JUnit XML is easy; just add a `report` artifact that
points to the XML file:

```yaml
test:
  only:
    -tags
  script:
    - make test
  artifacts:
    reports:
      junit: junit.xml
```

See the [Gitlab documentation on reports using
JUnit](https://docs.gitlab.com/ce/ci/junit_test_reports.html) for more information.

<!-- contributing -->
## Contributing

Everyone has a right to submit patches to kaocha-junit-xml, and thus become a contributor.

Contributors MUST

- adhere to the [LambdaIsland Clojure Style Guide](https://nextjournal.com/lambdaisland/clojure-style-guide)
- write patches that solve a problem. Start by stating the problem, then supply a minimal solution. `*`
- agree to license their contributions as EPL 1.0.
- not break the contract with downstream consumers. `**`
- not break the tests.

Contributors SHOULD

- update the CHANGELOG and README.
- add tests for new functionality.

If you submit a pull request that adheres to these rules, then it will almost
certainly be merged immediately. However some things may require more
consideration. If you add new dependencies, or significantly increase the API
surface, then we need to decide if these changes are in line with the project's
goals. In this case you can start by [writing a pitch](https://nextjournal.com/lambdaisland/pitch-template),
and collecting feedback on it.

`*` This goes for features too, a feature needs to solve a problem. State the problem it solves, then supply a minimal solution.

`**` As long as this project has not seen a public release (i.e. is not on Clojars)
we may still consider making breaking changes, if there is consensus that the
changes are justified.
<!-- /contributing -->

<!-- license -->
## License

Copyright &copy; 2018-2020 Arne Brasseur and contributors

Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt
<!-- /license -->