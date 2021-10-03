(ns rdd.grand-central.validation.db-spec
  (:require [clojure.spec.alpha :as s]))

(s/def :measurement/yield double?)
(s/def :measurement/quantity double?)
(s/def :currency.usd/cost double?)
