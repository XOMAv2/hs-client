(ns hs-client.user-form.events
  (:require [re-frame.core :as rf]
            [hs-client.user-form.db :as db]
            [hs-client.user-form.cofx :as cofx]
            [hs-common.specs :as ss]
            [hs-common.helpers :as help]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [hs-client.user-form.effects :as effects]
            [cljs.spec.alpha :as s]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-db
 ::change-panel
 (fn [db [_ val]]
   (assoc db :current-panel val)))

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
 (fn [_ [_ msg]]
   {:fx [[::effects/alert msg]
         [:dispatch [::panel-loading :add-user false]]]}))

(rf/reg-event-fx
 ::load-users
 [(rf/inject-cofx ::cofx/api-url)]
 (fn [{:keys [db api-url]} _]
   {:fx [[:http-xhrio {:method :get
                       :uri (str api-url "api/v1/users")
                       :response-format help/kebabed-json-response-format
                       :on-success [::users-loading-success]
                       :on-failure [::http-error]}]]}))

(rf/reg-event-fx
 ::users-loading-success
 (fn [{:keys [db]} [_ users]]
   {:db (assoc-in db [:panels :all-users :users] users)
    :dispatch [::panel-loading :all-users false]}))

(rf/reg-event-fx
 ::add-user-success
 (fn [{:keys [db]} [_ users]]
   {:db (assoc-in db [:panels :all-users :users] users)
    :fx [[:dispatch [::panel-loading :add-user false]]
         [::effects/alert "Пользователь успешно добавлен."]]}))

(rf/reg-event-fx
 ::delete-user-request
 [(rf/inject-cofx ::cofx/api-url)]
 (fn [{:keys [api-url]} [_ id]]
   {:fx [[:http-xhrio {:method :delete
                       :uri (str api-url "api/v1/users/" id)
                       :body ""
                       :response-format help/kebabed-json-response-format
                       :on-success [::delete-user-success]
                       :on-failure [::http-error]}]]}))

(rf/reg-event-fx
 ::delete-user-success
 (fn [{:keys [db]} [_ user]]
   {:db (update-in db [:panels :all-users :users] #(vec (remove (fn [el] (= el user)) %)))
    :fx [[::effects/console-log "Пользователь успешно удалён"]]}))

(rf/reg-event-db
 ::start-editing
 (fn [db [_ user]]
   (assoc-in db [:panels :edit-user] {:user-id (:id user)
                                      :user-form (dissoc user :id)
                                      :user-form-errors nil
                                      :show-errors false
                                      :loading false})))