(ns hs-client.user-form.db
  (:require [cljs.spec.alpha :as s]
            [hs-common.specs :as ss]
            [hs-common.helpers :as help]))

(s/def ::loading boolean?)
(s/def ::show-errors boolean?)
(s/def ::user-id ::ss/->int)

(def user-form-keys (set (help/spec->keys ::ss/user-form)))
(def user-form-maps (help/spec->map ::ss/user-form))

(s/def ::user-form-errors (s/nilable (s/map-of user-form-keys ::ss/non-empty-string)))

(s/def ::all-users-panel (s/keys :req-un [::ss/users
                                          ::loading]))

(s/def ::add-user-panel (s/keys :req-un [::ss/user-form
                                         ::user-form-errors
                                         ::show-errors
                                         ::loading]))

(s/def ::edit-user-panel (s/keys :req-un [::user-id
                                          ::ss/user-form
                                          ::ss/user-form-errors
                                          ::show-errors
                                          ::loading]))

(s/def ::db (s/keys :req-un [::all-users-panel
                             ::add-user-panel
                             ::edit-user-panel]))

(def default-db
  {:current-panel :all-users-panel
   :panels {:all-users {:users []
                        :loading false}
            :add-user {:user-form {:fullname ""
                                   :sex "x"
                                   :birthday ""
                                   :address ""
                                   :policy-number ""}
                       :user-form-errors {:fullname "Проверьте правильность заполнения поля."
                                          :birthday "Проверьте правильность заполнения поля."
                                          :address "Проверьте правильность заполнения поля."
                                          :policy-number "Проверьте правильность заполнения поля."}
                       :show-errors false
                       :loading false}
            :edit-user {:user-id 3
                        :user-form {:fullname ""
                                    :sex "x"
                                    :birthday ""
                                    :address ""
                                    :policy-number ""}
                        :user-form-errors {:fullname "Проверьте правильность заполнения поля."
                                           :birthday "Проверьте правильность заполнения поля."
                                           :address "Проверьте правильность заполнения поля."
                                           :policy-number "Проверьте правильность заполнения поля."}
                        :show-errors false
                        :loading false}}})