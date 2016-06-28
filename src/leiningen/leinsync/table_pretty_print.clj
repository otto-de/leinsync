(ns leiningen.leinsync.table-pretty-print
  (:require [leiningen.core.main :as m]))

(defn- print-table
  ([rows with-extra-seperator-line] (print-table (keys (first rows)) rows with-extra-seperator-line))
  ([ks rows with-extra-seperator-line]
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
       (m/info)
       (m/info (fmt-row "| " " | " " |" (zipmap ks ks)))
       (m/info (fmt-row "|-" "-+-" "-|" (zipmap ks spacers)))
       (doseq [row rows]
         (m/info (fmt-row "| " " | " " |" row))
         (if with-extra-seperator-line
           (m/info (fmt-row "|-" "---" "-|" (zipmap ks spacers)))))
       (m/info (fmt-row "|-" "---" "-|" (zipmap ks spacers)))))))

(defn print-compact-table [rows]
  (print-table rows false))

(defn print-full-table [rows]
  (print-table rows true))
