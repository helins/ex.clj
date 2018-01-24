(ns dvlopt.ex

  "Java exceptions as clojure data."

  {:author "Adam Helinski"}

  (:require [clojure.spec.alpha     :as s]
            [clojure.spec.gen.alpha :as gen]))




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




;;;;;;;;;;


(s/def ::string

  (s/and string?
         not-empty))


(s/def ::kind

  keyword?)


(s/def ::message

  (s/nilable ::string))


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
            64001))


(s/def ::element

  (s/keys :req-un [::class
                   ::method
                   ::native?]
          :opt-un [::file
                   ::line]))


(s/def ::stack-trace

  (s/coll-of ::element))


(s/def ::data

  map?)


(s/def ::exception

  (s/keys :req-un [::kind
                   ::message
                   ::stack-trace]
          :opt-un [::data
                   ::via]))


(s/def ::Throwable

  (s/with-gen exception?
              (fn make-gen []
                (gen/fmap (fn make-exception [[^String msg data cause]]
                            (if data
                              (ex-info msg
                                       data
                                       cause)
                              (Exception. msg
                                          cause)))
                          (s/gen (s/tuple ::string
                                          (s/nilable ::data)
                                          (s/nilable ::Throwable)))))))


(s/def ::StackTrace

  (s/with-gen (fn stack-trace? [x]
                (instance? -class--stack-trace
                           x))
              (fn make-gen []
                (gen/fmap (fn make-st [^Exception e]
                            (.getStackTrace e))
                          (s/gen ::Throwable)))))


(s/def ::StackTraceElement

  (s/with-gen (fn stack-trace-element? [x]
                (instance? StackTraceElement
                           x))
              (fn make-gen []
                (gen/fmap (fn make-ste [st]
                            (aget ^{:tag "[Ljava.lang.StackTraceElement;"} st
                                  (rand-int (count st))))
                          (s/gen ::StackTrace)))))




;;;;;;;;;;


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
  :ret  ::message)


(defn message
  
  "Gets the message of an exception."

  ^String

  [^Throwable e]

  (.getMessage e))




(s/fdef -stack-trace-element

  :args (s/cat :ste ::StackTraceElement)
  :ret  ::element)


(defn- -stack-trace-element

  "Describes a StackTraceElement.

   Do not contain a line number if the method is native."
  
  [^StackTraceElement ste]

  (let [native? (.isNativeMethod ste)
        ste'    {:class   (.getClassName  ste)
                 :method  (.getMethodName ste)
                 :native? native?}
        ste'2   (if-let [file (.getFileName ste)]
                  (assoc ste'
                         :file
                         file)
                  ste')
        ste'3   (if native?
                  ste'2
                  (assoc ste'2
                         :line
                         (.getLineNumber ste)))]
    ste'3))




(s/fdef stack-trace

  :args (s/cat :e ::Throwable)
  :ret  ::stack-trace)
        


(defn stack-trace

  "Gets and lazily describes a stack trace."

  [^Throwable e]

  (map -stack-trace-element
       (.getStackTrace e)))




(s/fdef -exception

  :args (s/cat :e ::Throwable)
  :ret  ::exception)


(defn- -exception

  "Describes an exception without diving into its causes."

  [^Throwable e]

  (let [e' {:kind        (kind        e)
            :message     (message     e)
            :stack-trace (stack-trace e)}]
    (if-let [data (ex-data e)]
      (assoc e'
             :data
             data)
      e')))




(s/fdef causes

  :args (s/cat :e ::Throwable)
  :ret  (s/coll-of ::exception))


(defn causes

  "Lazily describes the causes of an exception starting with the most direct one."

  [^Throwable e]

  (lazy-seq
    (when-let [cause (.getCause e)]
      (cons (-exception cause)
            (causes cause)))))




(s/fdef exception

  :args (s/cat :e ::Throwable)
  :ret  ::exception)


(defn exception

  "Describes an exception."

  [^Throwable e]

  (let [e' (-exception e)]
    (if (.getCause e)
      (assoc (-exception e)
             :via
             (causes e))
      e')))
