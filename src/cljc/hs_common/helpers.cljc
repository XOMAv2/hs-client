(ns hs-common.helpers
  (:require [clojure.walk :refer [postwalk]]
            [ajax.core :as ajax]
            [camel-snake-kebab.core :as csk]
            #?(:cljs [cljs.spec.alpha :as s]
               :clj [clojure.spec.alpha :as s])))

(defn change-keys-style
  "clj-struct - структура языка Clojure, стиль ключей в которой необходимо изменить.
   format-fn - функция форматирования ключа (для либы camel-snake-kebab это ->snake_case и т.д.).
   Изменяет стиль ключевых слов в структурах любой вложенности."
  [clj-struct format-fn]
  (postwalk #(if (keyword? %)
               (format-fn %)
               %)
            clj-struct))

(defn ->unqualify
  "Снимает кваливикатор с ключевого слова."
  [kw]
  (keyword (name kw)))

(defn spec->keys
  "spec-k - спека, кторая была определена формой (s/def spec-k (s/keys ...)).
   Получение всех ключей верхнего уровня из spec-k (и обязательных, и опциональных, и
   квалифицированных, и нет)."
  [spec-k]
  (let [form (s/form spec-k)
        params (apply hash-map (rest form))
        {:keys [req opt req-un opt-un]} params]
    (concat req opt (map ->unqualify opt-un) (map ->unqualify req-un))))

(defn spec->map
  "spec-k - спека, кторая была определена формой (s/def spec-k (s/keys ...)).
   Формирование мапы, ключами которой являются все ключи верхнего уровня из spec-k (и обязательные,
   и опциональные, и квалифицированные, и нет), а значениями - спеки, эти ключи валидирующие."
  [spec-k]
  (let [form (s/form spec-k)
        params (apply hash-map (rest form))
        {:keys [req opt req-un opt-un]} params
        keys (concat req opt (map ->unqualify opt-un) (map ->unqualify req-un))
        vals (concat req opt opt-un req-un)]
    (apply hash-map (mapcat #(vector % %2) keys vals))))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn seq-indexed
  "Создаёт последовательность индексированную через метаданные."
  [coll & {:keys [index-name] :or {index-name :key}}]
  (seq (map-indexed (fn [i e]
                      (try (with-meta e {index-name i})
                           (catch #?(:cljs js/Error :clj Exception) _ e))) coll)))

(def kebabed-json-response-format
  "Значение ключа :response-format мапы запроса в библиотеке cljs-ajax.
   Преобразует ответ сервера из JSON в мапу с ключами-кейвордами, отформатированными в kebab-case."
  (-> (ajax/json-response-format {:keywords? true})
      (update :read (fn [reader]
                      (fn [xhrio]
                        (change-keys-style (reader xhrio) csk/->kebab-case))))
      (update :description #(str "kebabed " %))))