(ns hs-client.server
  (:require [hs-client.handler :refer [handler]]
            [ring.adapter.jetty :refer [run-jetty]]
            [aero.core :as aero])
  (:gen-class))

(defn -main [& args]
  (let [flag-map {"-h" {:profile :heroku}
                  "-d" {:profile :dev}}
        profile (flag-map (first args))
        config (aero/read-config "config.edn" profile)]
    (run-jetty handler (:server config))))
