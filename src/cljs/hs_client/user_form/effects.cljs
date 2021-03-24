(ns hs-client.user-form.effects
  (:require [re-frame.core :as rf]))

(rf/reg-fx
 ::alert
 (fn [msg]
   (js/alert msg)))

(rf/reg-fx
 ::console-log
 (fn [msg]
   (.log js/console msg)))
