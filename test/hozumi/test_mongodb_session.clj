(ns hozumi.test-mongodb-session
  (:use [clojure.test]
	[ring.middleware.session.store]
	[hozumi.mongodb-session])
  (:require [somnium.congomongo :as congo]))

(def test-db-host "127.0.0.1")
(def test-db "hozumi-test-mongodb-sessions")

(defn server-fixture [f]
  (congo/mongo! :db test-db :host test-db-host)
  (f)
  (congo/drop-database! test-db))

(use-fixtures :each server-fixture)

(deftest read-not-exist
  (let [store (mongodb-store)]
    (is (read-session store "non-existent")
	{})))

(deftest session-create
  (let [store    (mongodb-store)
	sess-key (write-session store nil {:foo "bar"})
	entity   (read-session store sess-key)]
    (is (not (nil? sess-key)))
    (is (and (:_id entity) (:_date entity)))
    (is (= (dissoc entity :_id :_date)
	   {:foo "bar"}))))

(deftest session-update
  (let [store     (mongodb-store)
	sess-key  (write-session store nil {:foo "bar"})
	sess-key* (write-session store sess-key {:bar "baz"})
	entity    (read-session store sess-key*)]
    (is (= sess-key sess-key*))
    (is (and (:_id entity) (:_date entity)))
    (is (= (dissoc entity :_id :_date)
	   {:bar "baz"}))))

(deftest session-auto-key-change
  (let [store     (mongodb-store {:auto-key-change? true})
	sess-key  (write-session store nil {:foo "bar"})
	sess-key* (write-session store sess-key {:bar "baz"})
	entity    (read-session store sess-key*)]
    (is (not= sess-key sess-key*))
    (is (and (:_id entity) (:_date entity)))
    (is (= (dissoc entity :_id :_date)
	   {:bar "baz"}))))

(deftest session-delete
  (let [store    (mongodb-store)
	sess-key (write-session store nil {:foo "bar"})]
    (is (nil? (delete-session store sess-key)))
    (is (= (read-session store sess-key)
	   {}))))