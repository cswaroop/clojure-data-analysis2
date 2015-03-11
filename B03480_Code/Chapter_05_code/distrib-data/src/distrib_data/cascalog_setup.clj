
;;;; This is for recipe 05.01, 05.03, 05.06, 05.07, 05.09, 05.10.

(ns distrib-data.cascalog-setup
  (:require (cascalog
              [workflow :as w]
              [ops :as c]
              [vars :as v])
            [clojure.string :as string])
  (:use cascalog.api))

(comment
  (require '[clojure.string :as string])
  (require '(cascalog [workflow :as w] [ops :as c] [vars :as v]))
  (use 'cascalog.api)
  )

(def input-data
  [{:given-name "Susan", :surname "Forman", :doctors [1]}
   {:given-name "Barbara", :surname "Wright", :doctors [1]}
   {:given-name "Ian", :surname "Chesterton", :doctors [1]}
   {:given-name "Vicki", :surname nil, :doctors [1]}
   {:given-name "Steven", :surname "Taylor", :doctors [1]}
   {:given-name "Katarina", :surname nil, :doctors [1]}
   {:given-name "Sara", :surname "Kingdom", :doctors [1]}
   {:given-name "Dodo", :surname "Chaplet", :doctors [1]}
   {:given-name "Polly", :surname nil, :doctors [1 2]}
   {:given-name "Ben", :surname "Jackson", :doctors [1 2]}
   {:given-name "Jamie", :surname "McCrimmon", :doctors [2]}
   {:given-name "Victoria", :surname "Waterfield", :doctors [2]}
   {:given-name "Zoe", :surname "Heriot", :doctors [2]}
   {:given-name "Brigadier", :surname "Lethbridge-Stewart", :doctors [2]}
   {:given-name "Liz", :surname "Shaw", :doctors [3]}
   {:given-name "Jo", :surname "Grant", :doctors [3]}
   {:given-name "Sarah Jane", :surname "Smith", :doctors [3 4 10]}
   {:given-name "Harry", :surname "Sullivan", :doctors [4]}
   {:given-name "Leela", :surname nil, :doctors [4]}
   {:given-name "K-9 Mark I", :surname nil, :doctors [4]}
   {:given-name "K-9 Mark II", :surname nil, :doctors [4]}
   {:given-name "Romana", :surname nil, :doctors [4]}
   {:given-name "Adric", :surname nil, :doctors [4 5]}
   {:given-name "Nyssa", :surname nil, :doctors [4 5]}
   {:given-name "Tegan", :surname "Jovanka", :doctors [4 5]}
   {:given-name "Vislor", :surname "Turlough", :doctors [5]}
   {:given-name "Kamelion", :surname nil, :doctors [5]}
   {:given-name "Peri", :surname "Brown", :doctors [5 6]}
   {:given-name "Melanie", :surname "Bush", :doctors [6 7]}
   {:given-name "Ace", :surname nil, :doctors [7]}
   {:given-name "Grace", :surname "Holloway", :doctors [8]}
   {:given-name "Rose", :surname "Tyler", :doctors [9 10]}
   {:given-name "Adam", :surname "Mitchell", :doctors [9]}
   {:given-name "Jack", :surname "Harkness", :doctors [9 10]}
   {:given-name "Mickey", :surname "Smith", :doctors [10]}
   {:given-name "Donna", :surname "Noble", :doctors [10]}
   {:given-name "Martha", :surname "Jones", :doctors [10]}
   {:given-name "Astrid", :surname "Peth", :doctors [10]}
   {:given-name "Jackson", :surname "Lake", :doctors [10]}
   {:given-name "Rosita", :surname "Farisi", :doctors [10]}
   {:given-name "Christina", :surname "de Souza", :doctors [10]}
   {:given-name "Adelaide", :surname "Brooke", :doctors [10]}
   {:given-name "Wilfred", :surname "Mott", :doctors [10]}
   {:given-name "Amy", :surname "Pond", :doctors [11]}
   {:given-name "Rory", :surname "Williams", :doctors [11]}
   {:given-name "River", :surname "Song", :doctors [11]}
   {:given-name "Craig", :surname "Owens", :doctors [11]}])

(def companion (map string/lower-case
                    (map :given-name input-data)))
(def full-name
  (map (fn [{:keys [given-name surname]}]
         [(string/lower-case given-name)
          (string/trim
            (string/join \space [given-name surname]))])
       input-data))
(def doctor
  (mapcat #(map (fn [d] [(string/lower-case (:given-name %)) d])
                (:doctors %))
          input-data))

(def actor
  [[1 "William Hartnell" "1963–66"]
   [2 "Patrick Troughton" "1966–69"]
   [3 "Jon Pertwee" "1970–74"]
   [4 "Tom Baker" "1974–81"]
   [5 "Peter Davison" "1981–84"]
   [6 "Colin Baker" "1984–86"]
   [7 "Sylvester McCoy" "1987–89, 1996"]
   [8 "Paul McGann" "1996"]
   [9 "Christopher Eccleston" "2005"]
   [10 "David Tennant" "2005–10"]
   [11 "Matt Smith" "2010–present"]])

(comment
  ;; Simple queries (05.01)
  (?<- (stdout) [?companion] (companion ?companion))
  (?<- (stdout) [?name] (full-name _ ?name))

  ;; 05.03
  (?<- (stdout) [?n ?actor ?period]
       (actor ?n ?actor ?period) (<= ?n 5))
  (?<- (stdout) [?companion] (doctor ?companion ?n) (= ?n 10))

  ;; curl -O http://people.virginia.edu/~err8n/companions.txt
  ;; hadoop fs -put companions.txt /tmp/companions.txt
  ;; lein uberjar
  ;; (Now, to work around case-insensitivity on the Mac OS. Blargh:)
  ;; zip -d target/distrib-data-0.1.0-standalone.jar META-INF/LICENSE
  ;; hadoop jar target/distrib-data-0.1.0-standalone.jar clojure.main
  ;; For 05.04.
  (?<- (stdout) [?line]
       ((hfs-textline "hdfs://localhost:9000/tmp/companions.txt")
          :> ?line))

  (defmapop split-comma [string] (string/split string #","))
  (def hdfs-data-file 
          "hdfs://localhost:9000/user/err8n/va-survey/all_160_in_51.P35.csv")
  (?<- (stdout)
       [?name ?pop100 ?hu100 ?families]
       ((hfs-textline hdfs-data-file)
          ?line)
       (split-comma ?line :>
                    _ _ _ _ _ _ _ _
                    ?name ?pop100 ?hu100 _ _ ?families _))

  ;;; For 05.06.
  ;; Joins
  (?<- (stdout) [?name ?dr] (full-name ?c ?name) (doctor ?c ?dr))
  (?<- (stdout)
       [?name ?dr ?actor ?tenure]
       (full-name ?c ?name) (doctor ?c ?dr)
       (actor ?dr ?actor ?tenure))

  ;; Queries
  (?<- (stdout) [?name]
       (full-name ?c ?name) (doctor ?c ?dr)
       (>= ?dr 9))
  (?<- (stdout)
       [?name ?modern]
       (full-name ?c ?name) (doctor ?c ?dr)
       (>= ?dr 9 :> ?modern))

  ;;; For 05.07.
  ;; Aggregates
  (?<- (stdout) [?count] (companion _) (c/count ?count))
  (?<- (stdout)
       [?name ?count]
       (full-name ?c ?name) (doctor ?c _)
       (c/count ?count))

  ;; Custom functions. 05.10.
  (defmapcatop split [string] (string/split string #"\s+"))
  (?<- (stdout)
       [?name ?count]
       (full-name _ ?name) (split ?name :> ?split) (c/count ?count))

  (deffilterop is-even? [x] (even? x))
  (?<- (stdout)
       [?companion ?dr-count]
       (doctor ?companion _)
       (c/count ?dr-count)
       (is-even? ?dr-count))

  (defbufferop count-chars [strings]
    [(reduce + 0 (mapcat #(map count %) strings))])
  (?<- (stdout)
       [?dr ?companion-chars]
       (doctor ?c ?dr)
       (full-name ?c ?name)
       (count-chars ?name :> ?companion-chars))

  (defaggregateop mean-count
                  ([] [0 0])
                  ([[n total] string] [(inc n) (+ total (count string))])
                  ([[n total]] [(float (/ total n))]))
  (?<- (stdout)
       [?dr ?companion-chars]
       (doctor ?c ?dr)
       (full-name ?c ?name)
       (mean-count ?name :> ?companion-chars))

  (defn mean-init ([x] [1 (count x)]))
  (defn mean-step ([n1 t1 n2 t2] [(+ n1 n2) (+ t1 t2)]))
  (defparallelagg
    mean-count-p
    :init-var #'mean-init
    :combine-var #'mean-step)
  (?<- (stdout)
       [?dr ?companion-chars]
       (doctor ?c ?dr)
       (full-name ?c ?name)
       (mean-count-p ?name :> ?n ?total)
       (div ?total ?n :> ?companion-chars))

  ; Use this for 05.09.
  (defmapop split-range [date-range]
    (let [[from to] (string/split (str date-range) #"\u2013" 2)]
      [from (if (nil? to) from to)]))

  (?<- (stdout)
       [?n ?name ?from ?to]
       (actor ?n ?name ?range)
       (split-range ?range :> ?from ?to))

  )


