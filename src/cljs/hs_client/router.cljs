(ns hs-client.router
  (:require [reitit.frontend]
            [reitit.coercion.spec]
            [reitit.coercion]))

; Хочу пользоваться событиями, вместо того чтобы помещать логику в контроллеры.

(def routes
  [["/users/" {:name ::all
               :controllers [{:start (constantly (.log js/console "Entering all"))}]}]
   ["/users/add" {:name ::add
                  :controllers [{:start (constantly (.log js/console "Entering add"))}]}]
   ["/users/:id/edit" {:name ::edit
                       :parameters {:path {:id int?}}
                       :controllers [{:parameters {:path [:id]}
                                      :start (fn [identity]
                                               (.log js/console "Entering edit")
                                               (.log js/console (-> identity :path :id)))}]}]])

(def router
  (reitit.frontend/router routes {:data {:coercion reitit.coercion.spec/coercion}
                                  :conflicts nil
                                  :compile reitit.coercion/compile-request-coercers}))

#_(let [match (reitit.core/match-by-path router "/users/")]
  (assoc match :coerced-params (reitit.coercion/coerce! match)))