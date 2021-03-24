(ns hs-client.user-form.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::add-form
 (fn [db _]
   (-> db :panels :add-user :user-form)))

(rf/reg-sub
 ::add-form-show-errors
 (fn [db _]
   (-> db :panels :add-user :show-errors)))

(rf/reg-sub
 ::add-form-errors
 (fn [db _]
   (-> db :panels :add-user :user-form-errors)))

(rf/reg-sub
 ::add-form-visible-errors
 :<- [::add-form-show-errors]
 :<- [::add-form-errors]
 (fn [[show? errors] _]
   (when show? errors)))

(rf/reg-sub
 ::add-panel-loading
 (fn [db _]
   (-> db :panels :add-user :loading)))

(rf/reg-sub
 ::users
 (fn [db _]
   (-> db :panels :all-users :users)))

(rf/reg-sub
 ::current-panel
 (fn [db _]
   (:current-panel db)))

(rf/reg-sub
 ::edit-form
 (fn [db _]
   (-> db :panels :edit-user :user-form)))

(rf/reg-sub
 ::edit-user-id
 (fn [db _]
   (-> db :panels :edit-user :user-id)))

(rf/reg-sub
 ::edit-form-show-errors
 (fn [db _]
   (-> db :panels :edit-user :show-errors)))

(rf/reg-sub
 ::edit-form-errors
 (fn [db _]
   (-> db :panels :edit-user :user-form-errors)))

(rf/reg-sub
 ::edit-form-visible-errors
 :<- [::edit-form-show-errors]
 :<- [::edit-form-errors]
 (fn [[show? errors] _]
   (when show? errors)))