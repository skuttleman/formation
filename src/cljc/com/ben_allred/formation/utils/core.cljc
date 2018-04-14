(ns com.ben-allred.formation.utils.core)

(defn deep-into [val-1 val-2]
  (if (or (map? val-1) (map? val-2))
    (merge-with deep-into val-1 val-2)
    (distinct (concat val-1 val-2))))
