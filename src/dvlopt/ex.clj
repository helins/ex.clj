(ns dvlopt.ex

  "Java exceptions as clojure data."

  {:author "Adam Helinski"}

  (:require [clojure.spec.alpha     :as s]
            [clojure.spec.gen.alpha :as gen]
            [dvlopt.void            :as void]))




;;;;;;;;;; Private


(def ^:private -class--stack-trace

  "Class for a java stack trace ?"

  (Class/forName "[Ljava.lang.StackTraceElement;"))




;;;;;;;;;;


(defn exception?

  "Is `x` an exception ?"

  [x]

  (instance? Throwable
             x))




;;;;;;;;;; Specs


(s/def ::string

  (s/and string?
         not-empty))


(s/def ::kind

  simple-keyword?)


(s/def ::message

  ::string)


(s/def ::class

  ::string)


(s/def ::method

  ::string)


(s/def ::native?

  boolean?)


(s/def ::file

  ::string)


(s/def ::line

  (s/int-in 0
            Long/MAX_VALUE))


(s/def ::element

  (s/and (s/keys :req [::class
                       ::method
                       ::native?]
                 :opt [::file
                       ::line])
         #(not (and (::native? %)
                    (contains? %
                               ::line)))))


(s/def ::stack-trace

  (s/coll-of ::element))


(s/def ::data

  map?)


(s/def ::exceptions

  (s/coll-of ::exception))


(s/def ::exception

  (s/keys :req [::kind
                ::stack-trace]
          :opt [::message
                ::data]))




;;;;;;;;;; Specs - Java objects


(s/def ::Throwable

  (s/with-gen exception?
              (fn gen []
                (gen/fmap (fn exception [[^String msg data cause]]
                            (if data
                              (ex-info msg
                                       data
                                       cause)
                              (Exception. msg
                                          cause)))
                          (s/gen (s/tuple string?
                                          (s/nilable ::data)
                                          (s/nilable ::Throwable)))))))


(s/def ::StackTrace

  (s/with-gen (fn stack-trace? [x]
                (instance? -class--stack-trace
                           x))
              (fn gen []
                (gen/fmap (fn st [^Exception e]
                            (.getStackTrace e))
                          (s/gen ::Throwable)))))


(s/def ::StackTraceElement

  (s/with-gen (fn stack-trace-element? [x]
                (instance? StackTraceElement
                           x))
              (fn gen []
                (gen/fmap (fn ste [st]
                            (aget ^{:tag "[Ljava.lang.StackTraceElement;"} st
                                  (rand-int (count st))))
                          (s/gen ::StackTrace)))))




;;;;;;;;;; API


(s/fdef kind

  :args (s/cat :e ::Throwable)
  :ret  keyword?)


(defn kind

  "Gets the type of the exception as a keyword."

  [^Throwable e]

  (keyword (.substring (str (.getClass e))
                       6)))




(s/fdef message

  :args (s/cat :e ::Throwable)
  :ret  (s/nilable ::message))


(defn message
  
  "Gets the message of an exception."

  ^String

  [^Throwable e]

  (not-empty (.getMessage e)))




(s/fdef -stack-trace-element

  :args (s/cat :ste ::StackTraceElement)
  :ret  ::element)


(defn- -stack-trace-element

  "Describes a StackTraceElement.

   Do not contain a line number if the method is native."
  
  [^StackTraceElement ste]

  (let [native? (.isNativeMethod ste)]
    (void/assoc-some {::class   (.getClassName  ste)
                      ::method  (.getMethodName ste)
                      ::native? native?}
                     ::file (.getFileName ste)
                     ::line (when-not native?
                              (.getLineNumber ste)))))




(s/fdef stack-trace

  :args (s/cat :e ::Throwable)
  :ret  ::stack-trace)
        


(defn stack-trace

  "Gets and lazily describes a stack trace."

  [^Throwable e]

  (map -stack-trace-element
       (.getStackTrace e)))




(s/fdef exception

  :args (s/cat :e ::Throwable)
  :ret  ::exception)


(defn exception

  "Describes an exception without any of its causes."

  [^Throwable e]

  (void/assoc-some {::kind        (kind e)
                    ::stack-trace (stack-trace e)}
                   ::message     (message e)
                   ::data        (ex-data e)))




(s/fdef exception-and-causes

  :args (s/cat :e (s/nilable ::Throwable))
  :ret  ::exceptions)


(defn exception-and-causes

  "Lazily describes an exception and its causes, starting with the most recent one in the call stack."

  [^Throwable e]

  (lazy-seq
    (when e
      (cons (exception e)
            (exception-and-causes (.getCause e))))))
