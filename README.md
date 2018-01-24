# Ex

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/ex.svg)](https://clojars.org/dvlopt/ex)

Describing java exceptions in clojure data structures is useful for a variety of
use cases such as logging or sharing exceptions over the network.

## Usage

Read the [API](https://dvlopt.github.io/doc/ex/).

All functions are fully spec'ed and checked with clojure.spec.

In short :

```clj
(require '[dvlopt.ex :as ex])


(ex/exception (Exception. "Something bad happened"
                          (ex-info "Takes into account ExceptionInfo's"
                                   {:some :data})))
```

## License

Copyright © 2018 Adam Helinski

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
