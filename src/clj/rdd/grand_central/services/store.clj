(ns rdd.grand-central.services.store
  (:require [datomic.client.api :as d]
            [clojure.edn :as edn]
            [rdd.grand-central.validation.db-spec :as db-spec]
            [clojure.spec.alpha :as s]
            [clojure.repl :refer [doc]]
            [spec-coerce.core :as sc]
            [rdd.grand-central.db.core :as db-core]))

(defn conn
  []
  @db-core/db-conn)

(defn db
  []
  (d/db (conn)))

(defn transact
  [tx-data]
  (d/transact (conn) {:tx-data tx-data}))

(defn update-recipe-line-item-quantity
  [uuid quantity]
  (transact [[:db/add [:recipe-line-item/uuid uuid] :measurement/quantity (double quantity)]]))

(defn update-recipe-line-item
  [{:keys [uuid quantity uom]}]
  (let [tx-data (cond-> []
                  quantity (conj [:db/add [:recipe-line-item/uuid uuid] :measurement/quantity (double quantity)])
                  uom (conj [:db/add [:recipe-line-item/uuid uuid] :measurement/uom [:uom/code uom]]))]
    (transact tx-data)))

(defn get-items
  []
  (flatten (d/q '[:find (pull ?eid [*
                                    {:measurement/uom [:uom/uuid]
                                     :composite/contains [:recipe-line-item/uuid]}])
                  :where
                  [?eid :item/uuid ?uuid]]
                (db))))

(defn get-recipe-line-items
  []
  (flatten (d/q '[:find (pull ?eid [*
                                    {:measurement/uom [:uom/uuid]
                                     :recipe-line-item/item [:item/uuid]
                                     :recipe-line-item/company-item [:company-item/uuid]}])
                  :where
                  [?eid :recipe-line-item/uuid ?uuid]]
                (db))))

(defn get-uoms
  []
  (flatten (d/q '[:find (pull ?eid [*])
                  :where
                  [?eid :uom/uuid ?uuid]]
                (db))))

(defn get-companies
  []
  (flatten (d/q '[:find (pull ?eid [* {:company/company-items [:company-item/uuid]}])
                  :where
                  [?eid :company/uuid ?uuid]]
                (db))))

(defn get-conversions
  []
  (flatten (d/q '[:find (pull ?eid [* {:conversion/from [:uom/uuid]} {:conversion/to [:uom/uuid]}])
                  :where
                  [?eid :conversion/uuid ?uuid]]
                (db))))

(defn get-company-items
  []
  (flatten (d/q '[:find (pull ?eid [*
                                    {:company-item/item [:item/uuid]}
                                    {:company-item/quotes [:quote/uuid]}
                                    {:uom/conversions [:conversion/uuid]}])
                  :where
                  [?eid :company-item/uuid ?uuid]]
                (db))))

(defn get-quotes
  []
  (flatten (d/q '[:find (pull ?eid [* {:measurement/uom [:uom/uuid]}])
                  :where
                  [?eid :quote/uuid ?uuid]]
                (db))))

(defn coerce
  [datom]
  (let [[db-fn eid attr value] datom
        coerced (sc/coerce attr value)]
    [db-fn eid attr coerced]))

(defn transact-from-remote!
  [tx-data]
  (let [coerced-tx-data (map coerce tx-data)]
    (d/transact (conn) {:tx-data coerced-tx-data})))

(defn initial-data
  "Load initial data"
  []
  {:items (get-items)
   :recipe-line-items (get-recipe-line-items)
   :uoms (get-uoms)
   :companies (get-companies)
   :conversions (get-conversions)
   :company-items (get-company-items)
   :quotes (get-quotes)})

#_(defn item->tree
    [name]
    (d/pull (db) '[* {:measurement/uom [:uom/code]
                      :cost/_item [:cost/uuid
                                   :measurement/quantity
                                   {:cost/item [:item/uuid]}
                                   {:measurement/uom [:uom/code]}]
                      :composite/contains ...}] [:item/name name]))

(comment
  (get-items)
  (get-recipe-line-items)
  ;; => ({:db/id 96757023244450,
  ;;      :recipe-line-item/uuid "yUyKzE-NtGm-_9VWSIp-Z",
  ;;      :recipe-line-item/item #:item{:uuid "bMZHGcJJh2zispw2j-gox"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "s8-BdhZP88dPhBaKfSYN7"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "HH66_A1pKyREkChKfYuJ6"},
  ;;      :meta/position 1286742750677284}
  ;;     {:db/id 96757023244451,
  ;;      :recipe-line-item/uuid "oKrBTEYiX8WqIZFpHH19M",
  ;;      :recipe-line-item/item #:item{:uuid "PSySyvnZqHFXn5qyr8Tz8"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "EOGEtr3Sverg6nONMK7we"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "HH66_A1pKyREkChKfYuJ6"},
  ;;      :meta/position 2573485501354568}
  ;;     {:db/id 96757023244452,
  ;;      :recipe-line-item/uuid "D630OEfNfwemNv2xw1-gj",
  ;;      :recipe-line-item/item #:item{:uuid "KJU29o8XzNFiddLOnXhPQ"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "4Dkq6FYuC41vB9fgwu9Vf"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "HH66_A1pKyREkChKfYuJ6"},
  ;;      :meta/position 3860228252031853}
  ;;     {:db/id 96757023244453,
  ;;      :recipe-line-item/uuid "Q-BCo2vNQOR0eua__oauq",
  ;;      :recipe-line-item/item #:item{:uuid "b9NOS92v8FxNKDnokFTdc"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "nsO1zVQqK95brtQWokBZs"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "HH66_A1pKyREkChKfYuJ6"},
  ;;      :meta/position 5146971002709137}
  ;;     {:db/id 96757023244454,
  ;;      :recipe-line-item/uuid "BK4OOtQ_HrK3yUrCoa2fF",
  ;;      :recipe-line-item/item #:item{:uuid "_sVQTnYwcDXs2eMuu9Uk4"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "762wXrZHsJlT3yEr6Kcra"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "HH66_A1pKyREkChKfYuJ6"},
  ;;      :meta/position 6433713753386422}
  ;;     {:db/id 96757023244455,
  ;;      :recipe-line-item/uuid "m6hXXTWqNLMW-3QPNvDCz",
  ;;      :recipe-line-item/item #:item{:uuid "jeKQM9OY9rnYRKlTt0sRu"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "UXpulM8FwJeIPEmiCPIzB"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "HH66_A1pKyREkChKfYuJ6"},
  ;;      :meta/position 7720456504063706}
  ;;     {:db/id 96757023244456,
  ;;      :recipe-line-item/uuid "R-TGp8Tuf2jhSLD29lrHW",
  ;;      :recipe-line-item/item #:item{:uuid "-CkOmBUwPqTNg7bsqG2Nk"},
  ;;      :measurement/quantity 1000.0,
  ;;      :measurement/uom #:uom{:uuid "kgGALZbLHZxGWLfVb5XE1"},
  ;;      :meta/position 2251799813685247}
  ;;     {:db/id 96757023244457,
  ;;      :recipe-line-item/uuid "QvTVNnHWw0LU5RUdeJpsf",
  ;;      :recipe-line-item/item #:item{:uuid "NRbdyIs7yqIE0OaL4Vpkp"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "lyUKwqRH5xWW2pJVcTCuM"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "XFbqocZfWw75FtPj8i05_"},
  ;;      :meta/position 4503599627370495}
  ;;     {:db/id 96757023244458,
  ;;      :recipe-line-item/uuid "EYDhhabOyL1s1ngGL9AkV",
  ;;      :recipe-line-item/item #:item{:uuid "jeKQM9OY9rnYRKlTt0sRu"},
  ;;      :recipe-line-item/company-item #:company-item{:uuid "UXpulM8FwJeIPEmiCPIzB"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "kgGALZbLHZxGWLfVb5XE1"},
  ;;      :meta/position 6755399441055743})

  ;; => ({:db/id 74766790688895,
  ;;      :recipe-line-item/uuid "IokuDmoZN71t8Vpr9yFCg",
  ;;      :recipe-line-item/item #:item{:uuid "dObUcn1iBPn8mKNAUU-0p"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "vDzXXrpF2A9qWBHtEWdM-"},
  ;;      :meta/position 1286742750677284}
  ;;     {:db/id 74766790688896,
  ;;      :recipe-line-item/uuid "Gl3hX4rL36d6y2VtALMDt",
  ;;      :recipe-line-item/item #:item{:uuid "B0lSqA_d12RFv0N-0xd1h"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "vDzXXrpF2A9qWBHtEWdM-"},
  ;;      :meta/position 2573485501354568}
  ;;     {:db/id 74766790688897,
  ;;      :recipe-line-item/uuid "NSMIDOq1aoK7Jc6rTTctq",
  ;;      :recipe-line-item/item #:item{:uuid "Ix4XfgYpTjArGWo2H_apc"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "vDzXXrpF2A9qWBHtEWdM-"},
  ;;      :meta/position 3860228252031853}
  ;;     {:db/id 74766790688898,
  ;;      :recipe-line-item/uuid "xuXD0OZZ5WGbzZPJ1hHNb",
  ;;      :recipe-line-item/item #:item{:uuid "z7Ivu6KjVJT4GKRJlHm7K"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "vDzXXrpF2A9qWBHtEWdM-"},
  ;;      :meta/position 5146971002709137}
  ;;     {:db/id 74766790688899,
  ;;      :recipe-line-item/uuid "WcGkKTd11TScD1wYlISYh",
  ;;      :recipe-line-item/item #:item{:uuid "sitgzQR_Cpa_tGo5AFmwC"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "vDzXXrpF2A9qWBHtEWdM-"},
  ;;      :meta/position 6433713753386422}
  ;;     {:db/id 74766790688900,
  ;;      :recipe-line-item/uuid "VM1FQgyhZjg6aAARQSB0D",
  ;;      :recipe-line-item/item #:item{:uuid "7xEpudE0HvfN2jQN-fzuV"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "vDzXXrpF2A9qWBHtEWdM-"},
  ;;      :meta/position 7720456504063706}
  ;;     {:db/id 74766790688901,
  ;;      :recipe-line-item/uuid "yCy0rMxV-OFXQFhN4-sew",
  ;;      :recipe-line-item/item #:item{:uuid "bptlxfcE_xvyYh8aZ9X78"},
  ;;      :measurement/quantity 1000.0,
  ;;      :measurement/uom #:uom{:uuid "VndKDhzT-AZBqHs811PYE"},
  ;;      :meta/position 2251799813685247}
  ;;     {:db/id 74766790688902,
  ;;      :recipe-line-item/uuid "7GR6e6Rrxf0oLb4eqlFzV",
  ;;      :recipe-line-item/item #:item{:uuid "6HOB4XFBGbLLC6T6xivL3"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "JQiZlnapKZbbrjpv7xwDQ"},
  ;;      :meta/position 4503599627370495}
  ;;     {:db/id 74766790688903,
  ;;      :recipe-line-item/uuid "us6JvJjmqZsNvpX-WXdAw",
  ;;      :recipe-line-item/item #:item{:uuid "7xEpudE0HvfN2jQN-fzuV"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "VndKDhzT-AZBqHs811PYE"},
  ;;      :meta/position 6755399441055743})

  ;; => ()



  ;; => Execution error (IllegalArgumentException) at datomic.client.api.protocols/fn$G (protocols.clj:72).
  ;;    No implementation of method: :db of protocol: #'datomic.client.api.protocols/Connection found for class: nil

  ;; => ({:db/id 4611681620380876927,
  ;;      :recipe-line-item/uuid "UabF0nwZo3KXcGX9226_k",
  ;;      :recipe-line-item/item #:item{:uuid "uaoc9bC_utmh6Uy8KYII3"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 1286742750677284}
  ;;     {:db/id 4611681620380876928,
  ;;      :recipe-line-item/uuid "7vP3d_bG-C19u4nx2zvaM",
  ;;      :recipe-line-item/item #:item{:uuid "2SJ3Gez06WX4HfKqw6LE3"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 2573485501354568}
  ;;     {:db/id 4611681620380876929,
  ;;      :recipe-line-item/uuid "Tbinhms4rJhGLdMyvlM9L",
  ;;      :recipe-line-item/item #:item{:uuid "v4RyKyvWd3PKG52id4BLj"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 3860228252031853}
  ;;     {:db/id 4611681620380876930,
  ;;      :recipe-line-item/uuid "stq63PArHTIUQ_qGNLf3y",
  ;;      :recipe-line-item/item #:item{:uuid "Sj2r_EzDPhSzoD1DgORVZ"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 5146971002709137}
  ;;     {:db/id 4611681620380876931,
  ;;      :recipe-line-item/uuid "eHeySLXMM8M50XEXhvsEW",
  ;;      :recipe-line-item/item #:item{:uuid "6DHfUgBE2RKjdiSWFEiqU"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 6433713753386422}
  ;;     {:db/id 4611681620380876932,
  ;;      :recipe-line-item/uuid "0EneYlH20fSwpTUhK1LG0",
  ;;      :recipe-line-item/item #:item{:uuid "-gUmPHsBgyV69NAMBFlCD"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 7720456504063706}
  ;;     {:db/id 4611681620380876933,
  ;;      :recipe-line-item/uuid "LYc1MknExFpy6kiVMy2pA",
  ;;      :recipe-line-item/item #:item{:uuid "63rdM8btv1p-aO7TjMPq6"},
  ;;      :measurement/quantity 1000.0,
  ;;      :measurement/uom #:uom{:uuid "IHWLov37WPatBNNZRWKcN"},
  ;;      :meta/position 2251799813685247}
  ;;     {:db/id 4611681620380876934,
  ;;      :recipe-line-item/uuid "Xotq1WnVqPUM0LZJWodTU",
  ;;      :recipe-line-item/item #:item{:uuid "S2bv3SV_MR0V9ke7QQ2YD"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "dT1mu7vrKvM8eeyu1wYwq"},
  ;;      :meta/position 4503599627370495}
  ;;     {:db/id 4611681620380876935,
  ;;      :recipe-line-item/uuid "gvyG_-gVbJ93AKp0AuaC2",
  ;;      :recipe-line-item/item #:item{:uuid "-gUmPHsBgyV69NAMBFlCD"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "IHWLov37WPatBNNZRWKcN"},
  ;;      :meta/position 6755399441055743})

  ;; => ({:db/id 4611681620380876927,
  ;;      :recipe-line-item/uuid "UabF0nwZo3KXcGX9226_k",
  ;;      :recipe-line-item/item #:item{:uuid "uaoc9bC_utmh6Uy8KYII3"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 1286742750677284}
  ;;     {:db/id 4611681620380876928,
  ;;      :recipe-line-item/uuid "7vP3d_bG-C19u4nx2zvaM",
  ;;      :recipe-line-item/item #:item{:uuid "2SJ3Gez06WX4HfKqw6LE3"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 2573485501354568}
  ;;     {:db/id 4611681620380876929,
  ;;      :recipe-line-item/uuid "Tbinhms4rJhGLdMyvlM9L",
  ;;      :recipe-line-item/item #:item{:uuid "v4RyKyvWd3PKG52id4BLj"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 3860228252031853}
  ;;     {:db/id 4611681620380876930,
  ;;      :recipe-line-item/uuid "stq63PArHTIUQ_qGNLf3y",
  ;;      :recipe-line-item/item #:item{:uuid "Sj2r_EzDPhSzoD1DgORVZ"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 5146971002709137}
  ;;     {:db/id 4611681620380876931,
  ;;      :recipe-line-item/uuid "eHeySLXMM8M50XEXhvsEW",
  ;;      :recipe-line-item/item #:item{:uuid "6DHfUgBE2RKjdiSWFEiqU"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 6433713753386422}
  ;;     {:db/id 4611681620380876932,
  ;;      :recipe-line-item/uuid "0EneYlH20fSwpTUhK1LG0",
  ;;      :recipe-line-item/item #:item{:uuid "-gUmPHsBgyV69NAMBFlCD"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 7720456504063706}
  ;;     {:db/id 4611681620380876933,
  ;;      :recipe-line-item/uuid "LYc1MknExFpy6kiVMy2pA",
  ;;      :recipe-line-item/item #:item{:uuid "63rdM8btv1p-aO7TjMPq6"},
  ;;      :measurement/quantity 1000.0,
  ;;      :measurement/uom #:uom{:uuid "IHWLov37WPatBNNZRWKcN"},
  ;;      :meta/position 2251799813685247}
  ;;     {:db/id 4611681620380876934,
  ;;      :recipe-line-item/uuid "Xotq1WnVqPUM0LZJWodTU",
  ;;      :recipe-line-item/item #:item{:uuid "S2bv3SV_MR0V9ke7QQ2YD"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "dT1mu7vrKvM8eeyu1wYwq"},
  ;;      :meta/position 4503599627370495}
  ;;     {:db/id 4611681620380876935,
  ;;      :recipe-line-item/uuid "gvyG_-gVbJ93AKp0AuaC2",
  ;;      :recipe-line-item/item #:item{:uuid "-gUmPHsBgyV69NAMBFlCD"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "IHWLov37WPatBNNZRWKcN"},
  ;;      :meta/position 6755399441055743})

  ;; => ({:db/id 4611681620380876927,
  ;;      :recipe-line-item/uuid "UabF0nwZo3KXcGX9226_k",
  ;;      :recipe-line-item/item #:item{:uuid "uaoc9bC_utmh6Uy8KYII3"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 1286742750677284}
  ;;     {:db/id 4611681620380876928,
  ;;      :recipe-line-item/uuid "7vP3d_bG-C19u4nx2zvaM",
  ;;      :recipe-line-item/item #:item{:uuid "2SJ3Gez06WX4HfKqw6LE3"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 2573485501354568}
  ;;     {:db/id 4611681620380876929,
  ;;      :recipe-line-item/uuid "Tbinhms4rJhGLdMyvlM9L",
  ;;      :recipe-line-item/item #:item{:uuid "v4RyKyvWd3PKG52id4BLj"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 3860228252031853}
  ;;     {:db/id 4611681620380876930,
  ;;      :recipe-line-item/uuid "stq63PArHTIUQ_qGNLf3y",
  ;;      :recipe-line-item/item #:item{:uuid "Sj2r_EzDPhSzoD1DgORVZ"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 5146971002709137}
  ;;     {:db/id 4611681620380876931,
  ;;      :recipe-line-item/uuid "eHeySLXMM8M50XEXhvsEW",
  ;;      :recipe-line-item/item #:item{:uuid "6DHfUgBE2RKjdiSWFEiqU"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 6433713753386422}
  ;;     {:db/id 4611681620380876932,
  ;;      :recipe-line-item/uuid "0EneYlH20fSwpTUhK1LG0",
  ;;      :recipe-line-item/item #:item{:uuid "-gUmPHsBgyV69NAMBFlCD"},
  ;;      :measurement/quantity 1.0,
  ;;      :measurement/uom #:uom{:uuid "t_JfU1udQ3_KGMsG2c_vU"},
  ;;      :meta/position 7720456504063706}
  ;;     {:db/id 4611681620380876933,
  ;;      :recipe-line-item/uuid "LYc1MknExFpy6kiVMy2pA",
  ;;      :recipe-line-item/item #:item{:uuid "63rdM8btv1p-aO7TjMPq6"},
  ;;      :measurement/quantity 1000.0,
  ;;      :measurement/uom #:uom{:uuid "IHWLov37WPatBNNZRWKcN"},
  ;;      :meta/position 2251799813685247}
  ;;     {:db/id 4611681620380876934,
  ;;      :recipe-line-item/uuid "Xotq1WnVqPUM0LZJWodTU",
  ;;      :recipe-line-item/item #:item{:uuid "S2bv3SV_MR0V9ke7QQ2YD"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "dT1mu7vrKvM8eeyu1wYwq"},
  ;;      :meta/position 4503599627370495}
  ;;     {:db/id 4611681620380876935,
  ;;      :recipe-line-item/uuid "gvyG_-gVbJ93AKp0AuaC2",
  ;;      :recipe-line-item/item #:item{:uuid "-gUmPHsBgyV69NAMBFlCD"},
  ;;      :measurement/quantity 2.0,
  ;;      :measurement/uom #:uom{:uuid "IHWLov37WPatBNNZRWKcN"},
  ;;      :meta/position 6755399441055743})

  (get-uoms)
  (get-companies)
  (get-conversions)
  (get-quotes)
  (get-company-items)

  (initial-data)

  (tap> (initial-data))

  ;; 
  )