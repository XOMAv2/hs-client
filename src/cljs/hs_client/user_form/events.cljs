(ns hs-client.user-form.events
  (:require [re-frame.core :as rf]
            [hs-client.user-form.db :as db]
            [hs-client.user-form.cofx :as cofx]
            [hs-common.specs :as ss]
            [hs-common.helpers :as help]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [reitit.frontend.controllers]
            [hs-client.user-form.effects :as effects]
            [cljs.spec.alpha :as s]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

; Для смены URI нужно отправлять это событие. Его аргуент - квалифицированное имя route'а.
(rf/reg-event-fx
 ::change-route
 (fn [_ [_ route-name path-params]]
   {::effects/change-route [route-name path-params]}))

; Это событие не нужно вызывать напрямую из views. Оно вызываться при смене URI.
(rf/reg-event-db
 ::on-navigate
 (fn [db [_ new-match]]
   (let [old-match (:route-match db)
         controllers (reitit.frontend.controllers/apply-controllers
                      (:controllers old-match) new-match)
         new-match (assoc new-match :controllers controllers)]
     (assoc db :route-match new-match))))

(rf/reg-event-db
 ::update-user-form
 (fn [db [_ form-path err-path key val]]
   (let [valid? (s/valid? (db/user-form-maps key) val)
         db (if valid?
              (help/dissoc-in db (conj err-path key))
              (assoc-in db (conj err-path key) "Проверьте правильность заполнения поля."))
         db (assoc-in db (conj form-path key) val)]
     db)))

(rf/reg-event-db
 ::panel-loading
 (fn [db [_ panel loading?]]
   (assoc-in db [:panels panel :loading] loading?)))

(rf/reg-event-fx
 ::add-form-submit
 [(rf/inject-cofx ::cofx/api-url)]
 (fn [{:keys [db api-url]} _]
   (let [user-form (-> db :panels :add-user :user-form)
         valid? (s/valid? ::ss/user-form user-form)
         db (assoc-in db [:panels :add-user :show-errors] true)
         fx (if valid?
              [[:dispatch [::panel-loading :add-user true]]
               [:http-xhrio {:method :post
                             :uri (str api-url "api/v1/users")
                             :params user-form
                             :format (ajax/json-request-format)
                             :response-format help/kebabed-json-response-format
                             :on-success [::add-user-success]
                             :on-failure [::http-error]}]]
              [[::effects/alert "Неверные данные пользователя."]])]
     (conj {:db db} (when fx {:fx fx})))))

(rf/reg-event-fx
 ::http-error
 (fn [_ [_ resp]]
   {:fx [[::effects/console-log resp]
         [::effects/alert "При обращении к серверу произошла ошибка.\nОбновите страницу."]
         [:dispatch [::panel-loading :add-user false]]]}))

(rf/reg-event-fx
 ::load-users
 [(rf/inject-cofx ::cofx/api-url)]
 (fn [{:keys [api-url]} _]
   {:fx [[:dispatch [::panel-loading :all-users true]]
         [:http-xhrio {:method :get
                       :uri (str api-url "api/v1/users")
                       :response-format help/kebabed-json-response-format
                       :on-success [::users-loading-success]
                       :on-failure [::http-error]}]]}))

(rf/reg-event-fx
 ::users-loading-success
 (fn [{:keys [db]} [_ users]]
   {:db (assoc-in db [:panels :all-users :users] users)
    :fx [[:dispatch [::panel-loading :all-users false]]]}))

(rf/reg-event-fx
 ::add-user-success
 (fn [{:keys [db]} [_ users]]
   {:db (assoc-in db [:panels :all-users :users] users)
    :fx [[:dispatch [::panel-loading :add-user false]]
         [::effects/alert "Пользователь успешно добавлен."]]}))

(rf/reg-event-fx
 ::delete-user-request
 [(rf/inject-cofx ::cofx/api-url)]
 (fn [{:keys [db api-url]} [_ user-id]]
   {:db (update-in db [:panels :all-users :users] #(->> %
                                                        (map (fn [user]
                                                               (conj user
                                                                     (when (= (:id user) user-id)
                                                                       {:deleting true}))))
                                                        (vec)))
    :fx [[:http-xhrio {:method :delete
                       :uri (str api-url "api/v1/users/" user-id)
                       :body ""
                       :response-format help/kebabed-json-response-format
                       :on-success [::delete-user-success]
                       :on-failure [::delete-user-error user-id]}]]}))

(rf/reg-event-fx
 ::delete-user-error
 (fn [{:keys [db]} [_ user-id resp]]
   {:db (update-in db [:panels :all-users :users] #(->> %
                                                        (map (fn [user] (if (= (:id user) user-id)
                                                                          (dissoc user :deleting)
                                                                          user)))
                                                        (vec)))
    :fx [[:dispatch [::http-error resp]]]}))

(rf/reg-event-fx
 ::delete-user-success
 (fn [{:keys [db]} [_ {user-id :id}]]
   {:db (update-in db [:panels :all-users :users] #(->> %
                                                        (remove (fn [user] (= (:id user) user-id)))
                                                        (vec)))
    :fx [[::effects/console-log "Пользователь успешно удалён"]]}))

(rf/reg-event-fx
 ::start-editing
 (fn [{:keys [db]} [_ user]]
   (let [user-id (:id user)
         db (assoc-in db [:panels :edit-user] {:user-id user-id
                                               :user-form (dissoc user :id)
                                               :user-form-errors nil
                                               :show-errors false
                                               :loading false})
         fx [[:dispatch [::change-route :edit-route {:id user-id}]]]]
     {:db db
      :fx fx})))