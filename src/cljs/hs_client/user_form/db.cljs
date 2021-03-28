(ns hs-client.user-form.db
  (:require [cljs.spec.alpha :as s]
            [hs-common.specs :as ss]
            [hs-common.helpers :as help]))

(s/def ::loading boolean?)
(s/def ::show-errors boolean?)
(def user-form-keys (set (help/spec->keys ::ss/user-form)))
(def user-form-maps (help/spec->map ::ss/user-form))
(s/def ::user-form-errors (s/nilable (s/map-of user-form-keys ::ss/non-empty-string)))

(s/def ::deliting boolean?)
(s/def ::user (s/and ::ss/user
                     (s/keys :opt-un [::deleting])))
(s/def ::users (s/coll-of ::user :into []))
(s/def ::all-users-panel (s/keys :req-un [::users
                                          ::loading]))

(s/def ::add-user-panel (s/keys :req-un [::ss/user-form
                                         ::user-form-errors
                                         ::show-errors
                                         ::loading]))

(s/def ::user-id ::ss/->int)
(s/def ::fetching boolean?)
(s/def ::edit-user-panel (s/keys :req-un [::user-id
                                          ::ss/user-form
                                          ::user-form-errors
                                          ::show-errors
                                          ::loading
                                          ::fetching]))

(s/def ::path string?)
(s/def ::parameters any?)
(s/def ::name keyword?)
(s/def ::data (s/keys :req-un [::name]))
(s/def ::route-match (s/nilable (s/keys :req-un [::path
                                                 ::parameters
                                                 ::data])))

(s/def ::panels (s/keys :req-un [::all-users
                                 ::add-user
                                 ::edit-user]))

(s/def ::db (s/keys :req-un [::route-match
                             ::panels]))

(def default-db
  {:route-match nil
   :panels {:all-users {:users []
                        :loading false}
            :add-user {:user-form {:fullname ""
                                   :sex ""
                                   :birthday ""
                                   :address ""
                                   :policy-number ""}
                       :user-form-errors {:fullname "Проверьте правильность заполнения поля."
                                          :sex "Проверьте правильность заполнения поля."
                                          :birthday "Проверьте правильность заполнения поля."
                                          :address "Проверьте правильность заполнения поля."
                                          :policy-number "Проверьте правильность заполнения поля."}
                       :show-errors false
                       :loading false}
            :edit-user {:user-id nil
                        :user-form nil
                        :user-form-errors nil
                        :show-errors false
                        :loading false
                        :fetching false}}})