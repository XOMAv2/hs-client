(ns hs-client.user-form.interceptors
  (:require [cljs.spec.alpha :as s]
            [hs-client.user-form.db :as db]
            [re-frame.core :as rf]))

(defn- check-and-throw
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec
  (rf/after (partial check-and-throw ::db/db)))