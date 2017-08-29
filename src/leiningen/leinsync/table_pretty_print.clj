(ns leiningen.leinsync.table-pretty-print
  (:require [leiningen.core.main :as m]
            [clojure.string :as str]))

(defn get-formatter [keys widths]
  (fn [left middle right row]
    (str left
         (str/join (interpose middle
                              (for [[col fmt]
                                    (map vector
                                         (map #(get row %) keys)
                                         (map #(str "%" % "s") widths))]
                                (format fmt (str col)))))
         right)))

(defn max-width [rows key]
  (apply max
         (count (str key))
         (map #(count (str (get % key))) rows)))

(defn pretty-print-table
  [rows with-extra-separator-line log-fn]
  (let [keys (->> (map keys rows)
                  (distinct)
                  (reduce concat)
                  (distinct))
        widths (map (partial max-width rows) keys)
        spacers (map #(str/join (repeat % "-")) widths)
        formatter (get-formatter keys widths)]
    (when (seq rows)
      (log-fn "\n")
      (log-fn (formatter "| " " | " " |" (zipmap keys keys)))
      (log-fn (formatter "|-" "-+-" "-|" (zipmap keys spacers)))
      (doseq [row rows]
        (log-fn (formatter "| " " | " " |" row))
        (if with-extra-separator-line
          (log-fn (formatter "|-" "---" "-|" (zipmap keys spacers)))))
      (if-not with-extra-separator-line
        (log-fn (formatter "|-" "---" "-|" (zipmap keys spacers))))
      (log-fn "\n"))))

(defn print-compact-table [rows]
  (pretty-print-table rows false m/info))

(defn print-full-table [rows]
  (pretty-print-table rows true m/info))