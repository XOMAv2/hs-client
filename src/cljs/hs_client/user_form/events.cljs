(ns hs-client.user-form.events
  (:require [re-frame.core :as rf]
            [hs-client.user-form.db :as db]
            [hs-client.user-form.cofx :as cofx]
            [hs-common.specs :as ss]
            [hs-common.helpers :as help]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [reitit.frontend.controllers]
            [hs-client.user-form.interceptors :as interceptors]
            [hs-client.user-form.effects :as effects]
            [cljs.spec.alpha :as s]))

(rf/reg-event-db
 ::initialize-db
 [interceptors/check-spec]
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
 [interceptors/check-spec]
 (fn [db [_ new-match]]
   (let [old-match (:route-match db)
         controllers (reitit.frontend.controllers/apply-controllers
                      (:controllers old-match) new-match)
         new-match (assoc new-match :controllers controllers)]
     (assoc db :route-match new-match))))

(rf/reg-event-db
 ::update-user-form
 [interceptors/check-spec]
 (fn [db [_ form-path err-path key val]]
   (let [valid? (s/valid? (db/user-form-maps key) val)
         db (if valid?
              (help/dissoc-in db (conj err-path key))
              (assoc-in db (conj err-path key) "Проверьте правильность заполнения поля."))
         db (assoc-in db (conj form-path key) val)]
     db)))

(rf/reg-event-db
 ::panel-loading
 [interceptors/check-spec]
 (fn [db [_ panel loading?]]
   (assoc-in db [:panels panel :loading] loading?)))

(rf/reg-event-fx
 ::add-form-submit
 [(rf/inject-cofx ::cofx/api-url) interceptors/check-spec]
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
                             :on-failure [::add-form-submit-error]}]]
              [[::effects/alert "Неверные данные пользователя."]])]
     (conj {:db db} (when fx {:fx fx})))))

(rf/reg-event-fx
 ::add-user-success
 (fn [_ _]
   {:fx [[:dispatch [::panel-loading :add-user false]]
         [::effects/alert "Пользователь успешно добавлен."]]}))

(rf/reg-event-fx
 ::add-form-submit-error
 (fn [_ [_ resp]]
   {:fx [[:dispatch [::panel-loading :add-user false]]
         [:dispatch [::http-error resp]]]}))

(rf/reg-event-fx
 ::http-error
 [interceptors/check-spec]
 (fn [{:keys [db]} [_ resp]]
   {:db (assoc db :server-error true)
    :fx [[::effects/console-log resp]
         [::effects/alert "При обращении к серверу произошла ошибка.\nОбновите страницу."]]}))

(rf/reg-event-fx
 ::load-users
 [(rf/inject-cofx ::cofx/api-url)]
 (fn [{:keys [api-url]} _]
   {:fx [[:dispatch [::panel-loading :all-users true]]
         [:http-xhrio {:method :get
                       :uri (str api-url "api/v1/users")
                       :response-format help/kebabed-json-response-format
                       :on-success [::users-loading-success]
                       :on-failure [::load-users-error]}]]}))

(defn ->yyyy-mm-dd [date]
  (apply str (take 10 date)))

(rf/reg-event-fx
 ::users-loading-success
 [interceptors/check-spec]
 (fn [{:keys [db]} [_ users]]
   (let [users (map #(update % :birthday ->yyyy-mm-dd) users)]
     {:db (assoc-in db [:panels :all-users :users] users)
      :fx [[:dispatch [::panel-loading :all-users false]]]})))

(rf/reg-event-fx
 ::load-users-error
 (fn [_ [_ resp]]
   {:fx [[:dispatch [::http-error resp]
          :dispatch [::panel-loading :all-users false]]]}))

(rf/reg-event-fx
 ::delete-user-request
 [(rf/inject-cofx ::cofx/api-url) interceptors/check-spec]
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
 [interceptors/check-spec]
 (fn [{:keys [db]} [_ user-id resp]]
   {:db (update-in db [:panels :all-users :users] #(->> %
                                                        (map (fn [user] (if (= (:id user) user-id)
                                                                          (dissoc user :deleting)
                                                                          user)))
                                                        (vec)))
    :fx [[:dispatch [::http-error resp]]]}))

(rf/reg-event-fx
 ::delete-user-success
 [interceptors/check-spec]
 (fn [{:keys [db]} [_ {user-id :id}]]
   (let [edit-user-id (-> db :panels :edit-user :user-id)]
     {:db (update-in db [:panels :all-users :users]
                     #(->> %
                           (remove (fn [user] (= (:id user) user-id)))
                           (vec)))
      :fx [(when (= edit-user-id user-id) [:dispatch [::reset-edit-panel nil]])
           [::effects/console-log "Пользователь успешно удалён"]]})))

(rf/reg-event-fx
 ::load-edit-user
 [(rf/inject-cofx ::cofx/api-url) interceptors/check-spec]
 (fn [{:keys [db api-url]} [_ user-id]]
   (let [curr-user-id (-> db :panels :edit-user :user-id)
         curr-user-form (-> db :panels :edit-user :user-form)]
     (if (and (= curr-user-id user-id) (seq curr-user-form))
       {}
       {:db (assoc-in db [:panels :edit-user] {:user-id user-id
                                               :user-form nil
                                               :user-form-errors nil
                                               :show-errors false
                                               :loading false
                                               :fetching true})
        :fx [[:http-xhrio {:method :get
                           :uri (str api-url "api/v1/users/" user-id)
                           :response-format help/kebabed-json-response-format
                           :on-success [::reset-edit-panel]
                           :on-failure [::load-edit-user-error]}]]}))))

(rf/reg-event-fx
 ::load-edit-user-error
 [interceptors/check-spec]
 (fn [{:keys [db]} [_ resp]]
   {:db (assoc-in db [:panels :edit-user :fetching] false)
    :fx [(if (= 404 (:status resp))
           [::effects/alert (str "Не удалось найти редактируемого пользователя.\n"
                                 "Возможно он был удалён.")]
           [:dispatch [::http-error resp]])]}))

(rf/reg-event-fx
 ::start-editing
 (fn [_ [_ user]]
   {:fx [[:dispatch [::reset-edit-panel user]]
         [:dispatch [::change-route :edit-route {:id (:id user)}]]]}))

(rf/reg-event-db
 ::reset-edit-panel
 [interceptors/check-spec]
 (fn [db [_ user]]
   (let [user (update user :birthday ->yyyy-mm-dd)]
     (assoc-in db [:panels :edit-user] {:user-id (:id user)
                                        :user-form (dissoc user :id)
                                        :user-form-errors nil
                                        :show-errors false
                                        :loading false
                                        :fetching false}))))

(rf/reg-event-fx
 ::edit-form-submit
 [(rf/inject-cofx ::cofx/api-url) interceptors/check-spec]
 (fn [{:keys [db api-url]} _]
   (let [user-form (-> db :panels :edit-user :user-form)
         user-id (-> db :panels :edit-user :user-id)
         valid? (s/valid? ::ss/user-form user-form)
         db (assoc-in db [:panels :edit-user :show-errors] true)
         fx (if valid?
              [[:dispatch [::panel-loading :edit-user true]]
               [:http-xhrio {:method :patch
                             :uri (str api-url "api/v1/users/" user-id)
                             :params user-form
                             :format (ajax/json-request-format)
                             :response-format help/kebabed-json-response-format
                             :on-success [::edit-user-success]
                             :on-failure [::edit-form-submit-error]}]]
              [[::effects/alert "Неверные данные пользователя."]])]
     (conj {:db db} (when fx {:fx fx})))))

(rf/reg-event-fx
 ::edit-user-success
 (fn [_ _]
   {:fx [[:dispatch [::panel-loading :edit-user false]]
         [::effects/alert "Пользователь успешно отредактирован."]]}))

(rf/reg-event-fx
 ::edit-form-submit-error
 (fn [_ [_ resp]]
   {:fx (if (= 404 (:status resp))
          [[:dispatch [::reset-edit-panel nil]]
           [::effects/alert (str "Не удалось найти редактируемого пользователя.\n"
                                 "Возможно он был удалён.")]]
          [[:dispatch [::panel-loading :edit-user false]]
           [:dispatch [::http-error resp]]])}))
