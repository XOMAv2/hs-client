(ns hs-client.user-form.cofx
  (:require [re-frame.core :as rf]))

(rf/reg-cofx
 ::api-url
 (fn [coeffects _]
   (assoc coeffects :api-url "https://hs-tt-server.herokuapp.com/")))