(ns app-console.source
  "The `log/read` seam. Read-only by construction."
  (:require [app-console.model :as model]
            [mokuroku.source :as source]))

(defrecord LogSource [scope read-fn]
  source/ISource
  (-descriptor [_] (model/descriptor scope))
  (-fetch [_] (model/listing->items (read-fn scope))))

(defn log-source [scope read-fn] (->LogSource scope read-fn))
(defn fixture-source [scope entries] (log-source scope (constantly entries)))

(def denied
  {:log/state :denied :log/capability model/capability :log/entries []})

(defn granted [entries]
  {:log/state :granted :log/capability model/capability :log/entries (vec entries)})

(defn denied? [r] (= :denied (:log/state r)))

(def truncated
  "The provider returned a window, not the whole log.

  Logs are unbounded; every real reader is looking at a slice. Saying so is
  the difference between `no errors in the last hour` and `no errors`."
  {:log/state :truncated :log/capability model/capability})

(defn truncated? [r] (= :truncated (:log/state r)))
