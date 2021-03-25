(ns hs-client.user-form.effects
  (:require [reitit.frontend.easy]
            [re-frame.core :as rf]))

(rf/reg-fx
 ::alert
 (fn [msg]
   (js/alert msg)))

(rf/reg-fx
 ::console-log
 (fn [msg]
   (.log js/console msg)))

(rf/reg-fx
 ::change-route
 (fn [[route-name path-params]]
   (reitit.frontend.easy/push-state route-name path-params)))