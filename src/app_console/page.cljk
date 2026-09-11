(ns app-console.page
  (:require [mokuroku.catalog :as catalog]
            [mokuroku-ui.core :as mui]))

(def view-opts
  {:columns [:message :timestamp :level]
   :formatters {:level name}
   :noun "messages"
   :search-placeholder "Search messages"
   :empty-title "No messages"
   ;; Never "no errors" -- the reader is always looking at a window of an
   ;; unbounded log, and stating absence over a slice as absence overall is
   ;; the way a log viewer lies.
   :empty-body "Nothing in the retrieved window matches the current filter."
   :badge (fn [it]
            (case (:level (:item/attrs it))
              :fault "Fault"
              :error "Error"
              nil))
   :title "Console"
   :description "Log messages, newest first."})

(defn render [cat] (mui/->page (catalog/view cat) view-opts))
(defn render-html [cat] (mui/->html (catalog/view cat) view-opts))
