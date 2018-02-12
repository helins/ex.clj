# Ex

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/ex.svg)](https://clojars.org/dvlopt/ex)

Describing java exceptions in clojure data structures is useful for a variety of
use cases such as logging or sharing exceptions over the network.

## Usage

Read the [API](https://dvlopt.github.io/doc/ex/).

All functions are fully specified and checked with clojure.spec.

In short :

```clj
(require '[dvlopt.ex :as ex])


;; An exception with a cause, the kind of thing you can catch

(def example-exception
     (Exception. "Something bad happened"
                 (ex-info "Takes into account clojure's ExceptionInfo"
                          {:some :data})))


;; Now, let us translate this exception into pure data
(ex/exception example-exception)
```

## License

Copyright © 2018 Adam Helinski

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
