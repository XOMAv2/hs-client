(ns hs-client.user-form.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [hs-client.user-form.subs :as subs]
            [hs-client.user-form.events :as events]
            [reitit.frontend.easy :refer [href]]
            [hs-common.helpers :as help]
            [hs-client.config :refer [debug?]]))

#_(defn with-err
  "Функция дополняет атрибуты элемента el ссылкой на div с ошибкой и добавляет этот div, если строка
   err-msg не пуста.
   els вставляются между el и элементом с ошибкой."
  [err-msg el & els]
  (let [[tag attrs] el
        attrs (when (map? attrs) attrs)
        body (if attrs
               (rest (rest el))
               (rest el))
        error? (seq err-msg)
        error-id (name (gensym))]
    `[:<>
      [~tag ~(cond-> attrs
               error? (assoc :class "is-invalid"
                             :aria-describedby error-id)) ~@body]
      ~@els
      ~(when error? [:div.invalid-feedback {:id error-id} err-msg])]))

(defn err-wrapper
  "Функция дополняет атрибуты элемента el ссылкой на div с ошибкой и добавляет этот div, если строка
   err-msg не пуста.
   els вставляются между el и элементом с ошибкой."
  [err-msg el & els]
  (let [[tag attrs] el
        attrs (when (map? attrs) attrs)
        body (if attrs
               (rest (rest el))
               (rest el))
        error? (seq err-msg)
        error-id (name (gensym))]
    [:<>
     [tag (cond-> attrs
            error? (assoc :class "is-invalid"
                          :aria-describedby error-id)) (help/seq-indexed body)]
     (help/seq-indexed els)
     (when error? [:div.invalid-feedback {:id error-id} err-msg])]))

(defn user-form
  [{:keys [user errors form-path err-path]} & comps]
  (let [event-val (fn [event] (-> event .-target .-value))
        updater (fn [field event] (dispatch [::events/update-user-form
                                             form-path err-path
                                             field (event-val event)]))]
    [:div.card
     [:div.card-body
      [:form {:on-change #(when debug? (.log js/console {:user user
                                                         :errors errors}))
              :on-submit #(.preventDefault %)}
     ; ФИО
       [:div.mb-3
        [:label.form-label {:for "fullname-input"} "Полное имя"]
        (err-wrapper (:fullname errors)
                     [:input#fullname-input.form-control {:type "text"
                                                          :placeholder "Фамилия Имя Отчество"
                                                          :value (:fullname user)
                                                          :on-change #(updater :fullname %)}])]
     ; Пол
       [:fieldset.mb-3 {:on-change #(updater :sex %)}
        [:legend.col-form-label.pt-0 "Пол"]
        (let [err-attrs (when (:sex errors)
                          {:class "is-invalid"
                           :aria-describedby "radio-error"})]
          [:<>
           [:div.form-check
            [:input#sex-male.form-check-input (merge err-attrs
                                                     {:value "m"
                                                      :default-checked (= (:sex user) "m")
                                                      :name "sex-radio"
                                                      :type "radio"})]
            [:label.form-check-label {:for "sex-male"} "Мужской"]]
           [:div.form-check
            [:input#sex-female.form-check-input (merge err-attrs
                                                       {:value "f"
                                                        :default-checked (= (:sex user) "f")
                                                        :name "sex-radio"
                                                        :type "radio"})]
            [:label.form-check-label {:for "sex-female"} "Женский"]]
           [:div.form-check
            [:input#sex-other.form-check-input (merge err-attrs
                                                      {:value "x"
                                                       :default-checked (= (:sex user) "x")
                                                       :name "sex-radio"
                                                       :type "radio"})]
            [:label.form-check-label {:for "sex-other"} "Другой"]
            (when (:sex errors)
              [:div#radio-error.invalid-feedback (:sex errors)])]])]
     ; Дата рождения
       [:div.mb-3
        [:label.form-label {:for "birthday-input"} "Дата рождения"]
        (err-wrapper (:birthday errors)
                     [:input#birthday-input.form-control {:type "date"
                                                          :value (:birthday user)
                                                          :on-change #(updater :birthday %)}])]
     ; Адрес
       [:div.mb-3
        [:label.form-label {:for "address-select"} "Адрес проживания"]
        (err-wrapper (:address errors)
                     [:select#address-select.form-select {:value (:address user)
                                                          :on-change #(updater :address %)}
                      [:option {:value ""} "Не указан"]
                      [:option {:value "Москва"} "Москва"]
                      [:option {:value "Санкт-Петербург"} "Санкт-Петербург"]
                      [:option {:value "Мытищи"} "Мытищи"]])]
     ; Номер полиса
       [:div.mb-3
        [:label.form-label {:for "policy-input"} "Номер полиса"]
        (err-wrapper (:policy-number errors)
                     [:input#policy-input.form-control
                      {:aria-describedby "policy-help"
                       :type "text"
                       :value (:policy-number user)
                       :on-change #(updater :policy-number %)}]
                     [:div#policy-help.form-text "Ваш номер полиса не будет передан третьим лицам."])]

       (help/seq-indexed comps)]]]))

(defn add-button []
  (let [loading? @(subscribe [::subs/add-panel-loading])]
    [:div.d-flex.justify-content-end
     [:button.btn.btn-outline-primary {:type "submit"
                                       :disabled loading?
                                       :on-click #(dispatch [::events/add-form-submit])}
      [:span.spinner-border.spinner-border-sm.me-2 {:style {:display (when (not loading?)
                                                                       "none")}}]
      "Добавить"]]))

(defn edit-button []
  (let [loading? @(subscribe [::subs/edit-panel-loading])]
    [:div.d-flex.justify-content-end
     [:button.btn.btn-outline-primary {:type "submit"
                                       :disabled loading?
                                       :on-click #(dispatch [::events/edit-form-submit])}
      [:span.spinner-border.spinner-border-sm.me-2 {:style {:display (when (not loading?)
                                                                       "none")}}]
      "Изменить"]]))

(defn user-item [user edit-disabled?]
  (let [sex-map {\m "мужской"
                 \f "женский"
                 \x "другой"}]
    [:li.list-group-item.list-group-item-action
     [:div.d-flex.justify-content-between
      [:h5.mb-1 (:fullname user)]
      [:div
       [:button.btn.btn-link.btn-sm {:aria-label "Редактировать данные пользователя"
                                     :disabled edit-disabled?
                                     :on-click #(dispatch [::events/start-editing user])
                                     :type "button"}
        [:i.bi.bi-pencil]]
       [:button.btn.btn-link.btn-sm {:aria-label "Удалить пользователя"
                                     :disabled (:deleting user)
                                     :on-click #(dispatch [::events/delete-user-request (:id user)])
                                     :type "button"}
        (if (:deleting user)
          [:span.spinner-border.spinner-border-sm]
          [:i.bi.bi-x-circle])]]]
     [:p.mb-1 [:span.fw-lighter "Пол: "] (sex-map (:sex user))]
     [:p.mb-1 [:span.fw-lighter "Дата рождения: "] (apply str (take 10 (:birthday user)))]
     [:p.mb-1 [:span.fw-lighter "Адрес проживания: "] (:address user)]
     [:p.mb-1 [:span.fw-lighter "Номер полиса: "] (:policy-number user)]]))

(defn one-el-list [& el-body]
  [:ul.list-group
   [:li.list-group-item.list-group-item-action
    [:p.my-2
     [:<> el-body]]]])

(defn user-list []
  (let [users @(subscribe [::subs/users])
        edit-fetching? @(subscribe [::subs/edit-user-fetching])]
    (if (empty? users)
      [one-el-list "Список пользователей пуст."]
      [:ul.list-group
       (for [user users] ^{:key (gensym)} [user-item user edit-fetching?])])))

(defn redirection-panel []
  (dispatch [::events/change-route :all-route])
  [:div.py-3 {:style {:width "36rem"}}
   [one-el-list
    ^{:key (gensym)} [:span.spinner-border.spinner-border-sm.me-2]
    "Перенаправление на список всех пользователей..."]])

(defn all-users-panel []
  (let [loading? @(subscribe [::subs/users-loading])]
    [:div.py-3.vh-100 {:style {:width "36rem"}}
     [:div.d-flex.flex-column.h-100
      (when loading? [:div.mb-3
                      [one-el-list
                       ^{:key (gensym)} [:span.spinner-border.spinner-border-sm.me-2]
                       "Обновление списка пользователей..."]])
      [:div.flex-grow-1.overflow-auto
       [user-list]]]]))

(defn add-user-panel []
  (let [user @(subscribe [::subs/add-form])
        errors @(subscribe [::subs/add-form-visible-errors])]
    [:div.py-3 {:style {:width "36rem"}}
     [user-form {:user user
                 :errors errors
                 :form-path [:panels :add-user :user-form]
                 :err-path [:panels :add-user :user-form-errors]}
      [add-button]]]))

(defn edit-user-panel []
  (let [user @(subscribe [::subs/edit-form])
        errors @(subscribe [::subs/edit-form-visible-errors])
        fetching? @(subscribe [::subs/edit-user-fetching])]
    [:div.py-3 {:style {:width "36rem"}}
     (cond
       fetching? [one-el-list
                  ^{:key (gensym)} [:span.spinner-border.spinner-border-sm.me-2]
                  "Загрузка редактируемого пользователя..."]
       (seq user) [user-form {:user user
                              :errors errors
                              :form-path [:panels :edit-user :user-form]
                              :err-path [:panels :edit-user :user-form-errors]}
                   [edit-button]]
       :else [redirection-panel])]))

(defn navigation-panel []
  (let [match @(subscribe [::subs/route-match])
        edit-user-id @(subscribe [::subs/edit-user-id])
        curr-route-name (-> match :data :name)
        with-nav-attrs (fn [attrs [route-name route-params]]
                         (merge attrs
                                {:href (href route-name route-params)}
                                (when (= curr-route-name route-name)
                                  {:class "active"
                                   :aria-current "page"})))
          not-found? @(subscribe [::subs/edit-user-not-found])]
    [:div.d-flex.align-items-start
     [:nav.nav.flex-column.nav-pills.m-3
      [:a.nav-link.text-nowrap.text-center.link-dark (with-nav-attrs
                                                       {}
                                                       [:all-route])
       "Все пользователи"]
      [:a.nav-link.text-nowrap.text-center.link-dark (with-nav-attrs
                                                       {}
                                                       [:add-route])
       "Создание" [:br] "нового пользователя"]
      [:a.nav-link.text-nowrap.text-center.link-dark (with-nav-attrs
                                                       {:style {:display (when not-found? "none")}}
                                                       [:edit-route {:id edit-user-id}])
       "Редактирование" [:br] "пользователя"]]
     [:div.tab.content
      [:div.tab-pane {:role "tabpanel"}
       (case curr-route-name
         :all-route [all-users-panel]
         :add-route [add-user-panel]
         :edit-route [edit-user-panel]
         [redirection-panel])]]]))