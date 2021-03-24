(ns hs-client.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [hs-client.user-form.events :as events]
            [hs-client.user-form.views :as views]
            [reitit.frontend]
            [reitit.frontend.easy]
            [reitit.coercion.spec]
            [hs-client.config :as config]))

;; TODO: навигация
;; TODO: разбиение на компоненты
;; DEGUB: валидация полей заданных при инициализации формы
;; DEBUG: блокировка кнопки удаления до возвращения результата с сервера
;; TODO: уведомления
;; DEBUG: исправить формат даты в респонсе сервера
;; DEBUG: валидация select'ов
;; DEBUG: при удалении редактируемого пользователя необходимо завершать валидацию

(def routes
  [["/users"
    ["/" {:name ::users
          :controllers [{:start (fn [& params] (.log js/console "Entering edit"))
                         :stop  (fn [& params] (.log js/console "Leaving edit"))}]}]
    ["/add" {:name ::add
             :controllers [{:start (fn [& params] (.log js/console "Entering edit"))
                            :stop  (fn [& params] (.log js/console "Leaving edit"))}]}]
    ["/:id/edit" {:name ::edit
                  :parameters {:path {:id int?}}
                  :controllers [{:start (fn [& params] (.log js/console "Entering edit"))
                                 :stop  (fn [& params] (.log js/console "Leaving edit"))}]}]]
   ["/*" {:name ::not-found}]])

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
  (reitit.frontend.easy/start!
   (reitit.frontend/router routes {:data {:coercion reitit.coercion.spec/coercion}
                                   :conflicts nil})
   (fn [match] (.log js/console match))
   {:use-fragment false})
  (mount-root))