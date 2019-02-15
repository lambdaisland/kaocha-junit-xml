# lambdaisland/kaocha-junit-xml

<!-- badges -->
[![CircleCI](https://circleci.com/gh/lambdaisland/kaocha-junit-xml.svg?style=svg)](https://circleci.com/gh/lambdaisland/kaocha-junit-xml) [![cljdoc badge](https://cljdoc.org/badge/lambdaisland/kaocha-junit-xml)](https://cljdoc.org/d/lambdaisland/kaocha-junit-xml) [![Clojars Project](https://img.shields.io/clojars/v/lambdaisland/kaocha-junit-xml.svg)](https://clojars.org/lambdaisland/kaocha-junit-xml) [![codecov](https://codecov.io/gh/lambdaisland/kaocha-junit-xml/branch/master/graph/badge.svg)](https://codecov.io/gh/lambdaisland/kaocha-junit-xml)
<!-- /badges -->

[Kaocha](https://github.com/lambdaisland/kaocha) plugin to generate a JUnit XML version of the test results.

## Usage

- Add kaocha-junit-xml as a dependency

``` clojure
;; deps.edn
{:aliases
 {:test
  {:extra-deps {lambdaisland/kaocha {...}
                lambdaisland/kaocha-junit-xml {:mvn/version "0.0-57"}}}}}
```

or

``` clojure
;; project.clj
(defproject ,,,
  :dependencies [,,,
                 [lambdaisland/kaocha-junit-xml "0.0-57"]])
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

## Requirements

Requires at least Kaocha 0.0-306 and Clojure 1.9.

## CircleCI

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

<!-- license-epl -->
## License

Copyright &copy; 2018 Arne Brasseur

Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt
<!-- /license-epl -->
