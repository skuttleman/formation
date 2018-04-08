(ns com.ben-allred.formation.test.utils.spies
  (:refer-clojure :exclude [reset!]))

(defn create
  ([] (create (constantly nil)))
  ([f]
   (let [calls (atom [])
         responder (atom f)]
     (with-meta (fn [& args]
                  (swap! calls conj args)
                  (apply @responder args))
                {::calls     calls
                 ::responder responder
                 ::initial-f f}))))

(defn calls [spy]
  (when-let [calls (::calls (meta spy))]
    @calls))

(defn called? [spy]
  (boolean (seq (calls spy))))

(def never-called? (complement called?))

(defn called-with? [spy & args]
  (->> spy
       (calls)
       (some (partial = args))
       (boolean)))

(def never-called-with? (complement called-with?))

(defn reset! [& spies]
  (doseq [spy spies
          :let [md (meta spy)
                calls (::calls md)
                responder (::responder md)]]
    (when calls
      (clojure.core/reset! calls []))
    (when responder
      (clojure.core/reset! responder (::initial-f md)))))

(defn respond-with! [spy f]
  (when-let [responder (::responder (meta spy))]
    (clojure.core/reset! responder f)))
