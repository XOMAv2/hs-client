(ns hs-client.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [hs-client.user-form.events :as events]
            [hs-client.user-form.views :as views]
            [reitit.frontend]
            [reitit.frontend.easy]
            [reitit.coercion.spec]
            [reitit.coercion]
            [hs-client.router :refer [router]]
            [hs-client.config :as config]))

;; DEBUG: ошибка, возникающая при вводе URI руками в поисковой строке.
;; TODO: разбиение на компоненты
;; DEGUB: валидация полей заданных при инициализации формы
;; TODO: валидация db с использование спек.
;; TODO: уведомления

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/navigation-panel] root-el)))

(defn -main []
  (rf/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (reitit.frontend.easy/start! router
                               (fn [match]
                                 (when match
                                   (let [parameters (reitit.coercion/coerce! match)
                                         ; Приведение параметров почему-то осуществляется даже без
                                         ; вызова функции coerce!
                                         match (assoc match :parameters parameters)]
                                     (rf/dispatch [::events/on-navigate match]))))
                               {:use-fragment false})
  (mount-root))