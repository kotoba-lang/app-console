(ns app-console.model
  "Console's domain: log entries.

  One capability, `log/read`, and it is read-only by construction — there is
  no command in this app that writes, signals, or deletes. A log viewer that
  could also mutate would be a different and much more dangerous program."
  (:require [mokuroku.item :as item]
            [mokuroku.source :as source]))

(def capability "log/read")

(def columns
  [(source/attribute :message "Message" :string)
   (source/attribute :timestamp "Time" :number)
   (source/attribute :level "Level" :string)
   (source/attribute :subsystem "Subsystem" :string)
   (source/attribute :process "Process" :string)
   ;; A sequence number orders entries that share a timestamp, but sorting by
   ;; it directly is just sorting by arrival, which the Time column already
   ;; says more honestly.
   (source/attribute :seq "Seq" :number false)])

(def commands
  #{:copy-path :export})

(def levels
  "Ordered from least to most severe, so `level-rank` can compare them.

  A string sort would put :error before :info before :warn alphabetically,
  which reads as a severity order and is not one."
  [:debug :info :notice :warn :error :fault])

(def level-rank (into {} (map-indexed (fn [i l] [l i])) levels))

(defn descriptor
  ([] (descriptor "All Messages"))
  ([scope]
   (source/descriptor
    {:id :app-console/log
     :item-kind :log-entry
     :label scope
     :capability capability
     :commands commands
     :attributes columns})))

(defn entry->item
  "The id is timestamp plus sequence. Timestamps collide — a burst of log
  lines can share a millisecond — and an id that collides means two entries
  become one row, silently losing whichever arrived second."
  [{:keys [timestamp seq level subsystem process message]}]
  (item/item [timestamp (or seq 0)]
             :log-entry
             (or message "")
             {:message (or message "")
              :timestamp timestamp
              :seq (or seq 0)
              ;; Sortable severity, not the keyword: alphabetical order over
              ;; level names is not severity order.
              :level level
              :severity (get level-rank level -1)
              :subsystem subsystem
              :process process}))

(defn listing->items [entries]
  (mapv entry->item entries))

(def newest-first [[:timestamp :desc] [:seq :desc]])

(def default-query
  {:query/sort newest-first :query/text "" :query/filters []})

(defn at-least
  "A filter for `level` and worse. Expressed against `:severity`, the numeric
  rank, because that is the only field where the comparison means what the
  user means."
  [level]
  [:severity :>= (get level-rank level -1)])

(defn unknown-level?
  "A provider may emit a level this app has never heard of. It is kept and
  ranked -1 rather than coerced to :info — pretending an unknown severity is
  informational is how a fault gets filtered out of view."
  [it]
  (neg? (item/attr it :severity 0)))
