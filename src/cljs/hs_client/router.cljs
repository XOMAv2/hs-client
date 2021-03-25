(ns hs-client.router
  (:require [reitit.frontend]
            [reitit.coercion.spec]
            [reitit.coercion]
            [re-frame.core :as rf]
            [hs-client.user-form.events :as events]))

(def routes
  [["/users/" {:name :all-route
               :controllers [{:start (fn [& _]
                                       (.log js/console "Entering all-route")
                                       (.log js/console "Loading users...")
                                       (rf/dispatch [::events/load-users]))}]}]
   ["/users/add" {:name :add-route
                  :controllers [{:start (fn [& _]
                                          (.log js/console "Entering add-route"))}]}]
   ["/users/:id/edit" {:name :edit-route
                       :parameters {:path {:id int?}}
                       :controllers [{:parameters {:path [:id]}
                                      :start (fn [identity]
                                               (.log js/console "Entering edit-route")
                                               (.log js/console (-> identity :path :id)))}]}]])

(def router
  (reitit.frontend/router routes {:data {:coercion reitit.coercion.spec/coercion}
                                  :conflicts nil
                                  :compile reitit.coercion/compile-request-coercers}))

#_(let [match (reitit.core/match-by-path router "/users/")]
  (assoc match :coerced-params (reitit.coercion/coerce! match)))