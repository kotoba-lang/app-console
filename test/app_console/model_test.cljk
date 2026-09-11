(ns app-console.model-test
  (:require [app-console.model :as model]
            [app-console.page :as page]
            [app-console.source :as source]
            [clojure.test :refer [deftest is testing]]
            [design-quality.audit :as dq]
            [mokuroku.catalog :as catalog]
            [mokuroku.item :as item]))

(def entries
  [{:timestamp 1000 :seq 1 :level :info :subsystem "kernel" :process "launchd"
    :message "booted"}
   {:timestamp 1000 :seq 2 :level :error :subsystem "net" :process "netd"
    :message "link down"}
   {:timestamp 1200 :seq 3 :level :warn :subsystem "net" :process "netd"
    :message "retrying"}
   {:timestamp 1300 :seq 4 :level :debug :subsystem "app" :process "nbb"
    :message "tick"}])

(defn- cat-of [es]
  (catalog/refresh (catalog/catalog (source/fixture-source "All Messages" es)
                                    model/default-query)))

(deftest colliding-timestamps-stay-two-entries
  ;; A burst of log lines can share a millisecond. An id that collides means
  ;; two entries become one row, silently losing whichever arrived second.
  (is (= 4 (count (:result/items (catalog/result (cat-of entries))))))
  (let [ids (set (map :item/id (model/listing->items entries)))]
    (is (= 4 (count ids)))
    (is (contains? ids [1000 1]))
    (is (contains? ids [1000 2]))))

(deftest severity-is-ranked-not-spelled
  ;; Alphabetically :error < :info < :warn, which reads as a severity order
  ;; and is not one.
  (is (< (model/level-rank :info) (model/level-rank :warn)))
  (is (< (model/level-rank :warn) (model/level-rank :error)))
  (is (< (model/level-rank :error) (model/level-rank :fault)))

  (testing "filtering for warn-and-worse uses the rank"
    (let [c (catalog/set-query (cat-of entries)
                               (assoc model/default-query
                                      :query/filters [(model/at-least :warn)]))
          msgs (set (map :item/label (:result/items (catalog/result c))))]
      (is (= #{"link down" "retrying"} msgs))
      (is (not (contains? msgs "booted")) "info is below warn")
      (is (not (contains? msgs "tick")) "debug is below warn"))))

(deftest an-unknown-level-is-kept-not-coerced
  ;; Pretending an unknown severity is informational is how a fault gets
  ;; filtered out of view.
  (let [it (model/entry->item {:timestamp 1 :seq 1 :level :panic :message "?"})]
    (is (= :panic (item/attr it :level)))
    (is (= -1 (item/attr it :severity)))
    (is (model/unknown-level? it))
    (testing "and it does not pass a warn-and-worse filter by accident"
      (let [c (catalog/set-query
               (catalog/refresh
                (catalog/catalog (source/fixture-source "x" [{:timestamp 1 :seq 1
                                                              :level :panic :message "?"}])))
               (assoc model/default-query :query/filters [(model/at-least :warn)]))]
        (is (empty? (:result/items (catalog/result c)))
            "unranked, so it is excluded rather than silently promoted")))))

(deftest newest-first-with-sequence-breaking-ties
  (is (= ["tick" "retrying" "link down" "booted"]
         (mapv :item/label (:result/items (catalog/result (cat-of entries)))))))

(deftest the-viewer-cannot-mutate
  (let [c (catalog/select (cat-of entries) [1000 2])]
    (is (= #{:copy-path :export}
           (set (map :command/id (:view/commands (catalog/view c))))))
    (doseq [destructive [:trash :quit :eject]]
      (is (= :source-does-not-accept (:proposal/refused (catalog/propose c destructive)))
          (str destructive " must not be offered by a log viewer")))))

(deftest a-window-is-not-the-whole-log
  (is (source/truncated? source/truncated))
  (is (not (source/denied? source/truncated)))
  (is (= "log/read" (:log/capability source/truncated))))

(deftest window-meets-the-design-quality-floor
  (let [pages {"log" (page/render (cat-of entries))
               "filtered" (page/render (catalog/search (cat-of entries) "net"))
               "awaiting-grant" (page/render
                                 (catalog/catalog (source/fixture-source "All Messages" [])
                                                  model/default-query))}
        {:keys [overall pages] :as report} (dq/audit pages {:extra-axes dq/extra-axes})]
    (println "design-quality: aggregate" overall)
    (is (>= overall 98.0) (pr-str (:findings report)))
    (doseq [[nm r] pages] (is (>= (:overall r) 98.0) nm))))
