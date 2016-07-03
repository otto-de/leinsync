(ns leiningen.leinsync.table-pretty-print
  (:require [leiningen.core.main :as m]
            [clojure.string :as str]))

(defn print-table
  ([rows with-extra-seperator-line log-fn]
   (let [ks (->> rows
                 (map keys)
                 (distinct)
                 (reduce concat)
                 (distinct))]
     (print-table ks rows with-extra-seperator-line log-fn)))
  ([ks rows with-extra-seperator-line log-fn]
   (when (seq rows)
     (let [widths (map
                   (fn [k]
                     (apply max (count (str k)) (map #(count (str (get % k))) rows)))
                   ks)
           spacers (map #(apply str (repeat % "-")) widths)
           fmts (map #(str "%" % "s") widths)
           fmt-row (fn [leader divider trailer row]
                     (str leader
                          (apply str (interpose divider
                                                (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                                  (format fmt (str col)))))
                          trailer))]
       (log-fn "\n")
       (log-fn (fmt-row "| " " | " " |" (zipmap ks ks)))
       (log-fn (fmt-row "|-" "-+-" "-|" (zipmap ks spacers)))
       (doseq [row rows]
         (log-fn (fmt-row "| " " | " " |" row))
         (if with-extra-seperator-line
           (log-fn (fmt-row "|-" "---" "-|" (zipmap ks spacers)))))
       (log-fn "\n")))))

(defn print-compact-table [rows]
  (print-table rows false m/info))

(defn print-full-table [rows]
  (print-table rows true m/info))