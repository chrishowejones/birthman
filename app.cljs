(ns app
  (:require ["moment$default" :as moment]
            ["inquirer$default" :as inquirer]
            [promesa.core :as p]
            ["mongodb$default" :as mongodb]
            [nbb.core :refer [await]]))


(def password js/process.env.BMAN_DB_PASS)
(def user (or js/process.env.BMAN_DB_USER "nbb"))

(def db-uri (str "mongodb+srv://" user ":" password "@birthdaydb.vxgoe88.mongodb.net/?retryWrites=true&w=majority"))

(def client (mongodb/MongoClient. db-uri))

(defn write-birthday [name day month]
 (p/let [conn (.connect client)
         db (.db conn "birthdaydb")
         collection (.collection db "birthdays")
         response (.insertOne collection #js {:name name
                                              :day (str day)
                                              :month month})
         _ (.close conn)]
   response))

(comment

   (await (write-birthday "Tom" 18 "January"))
  )

(def questions (clj->js
                [{:name "name"
                  :type "input"
                  :message "Who's birthday is it?"}
                 {:name "day"
                  :type "number"
                  :message "What day is their birthday?"
                  :validate (fn [v]
                              (< 0 v 31))}
                 {:name "month"
                  :type "list"
                  :choices (moment/months)}]))



(defn create-birthday-entry []
  (p/let [_answers (inquirer/prompt questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [name day month]} answers]
    (prn "Saving birthday for " name day month)
    (write-birthday name day month)))

(defn find-birthday-entries [day month]
  (println "Finding birthdays" day month)
  (p/let [query #js {:month month :day (str day)}
          conn (.connect client)
          db (.db conn "birthdaydb")
          collection (.collection db "birthdays")
          response (.toArray (.find collection query))

          _ (.close conn)]
    response))

(comment
  (await (find-birthday-entries 4 "December"))

  )

(defn list-birthdays []
  (p/let [month (.format (moment) "MMMM")
          day (.date (moment))
          _entries (find-birthday-entries day month)
          entries (js->clj _entries :keywordize-keys true)]
    (run! (fn [{:keys [name]}]
           (println "It's" (str name "'s") "birthday today"))
          entries)))

(comment
  (await (list-birthdays))
  )

(cond
  (= (first *command-line-args*) "list") (list-birthdays)
  :else (create-birthday-entry))
