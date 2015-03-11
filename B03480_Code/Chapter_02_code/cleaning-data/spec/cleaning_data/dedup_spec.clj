
(ns cleaning-data.dedup-spec
  (:use [cleaning-data.dedup]
        [speclj.core]))

(def data
  {:mulder {:given-name "Fox"
            :surname "Mulder"}
   :molder {:given-name "Fox"
            :surname "Molder"}
   :mulder2 {:given-name "fox"
             :surname "mulder"}
   :scully {:given-name "Dana"
            :surname "Scully"}
   :scully2 {:given-name "Dan"
             :surname "Scully"}
   :doggett {:given-name "John"
             :surname "Doggett"}
   :reyes {:given-name "Monica"
           :surname "Reyes"}
   :skinner {:given-name "Walter"
             :surname "Skinner"}})

(def a {:given-name "Eric"
        :surname "Rochester"
        :languages 1
        :widgets 13})
(def b {:given-name "Erick"
        :surname "Rodchester"
        :color 'blue})
(def c {:given-name "Emma"
        :surname "Rochester"
        :color 'yellow})
(def d {:given-name "Elsa"
        :surname "Rochester"
        :color 'blue})
(def e {:given-name "Eri"
        :surname "Rochester"
        :color 'red})

(binding [*fuzzy-max-diff* 1]
  (describe
    "fuzzy="
    (it "should return true for strings one character off."
        (should (fuzzy= "Eric" "Erick")))
    (it "should return true for strings with less than 10% difference."
        (should (fuzzy= "Eric Rochester" "Erick Rochester"))
        (should (fuzzy= "Eric Richard Rochester" "Erick Richard Rodchester")))
    (it "should return false for strings with more than 10% difference."
        (should (not (fuzzy= "Eric Rochester" "Erick Rodcheste")))
        (should (not (fuzzy= "Eric Richard Rochester" "Erick Richart Rodchester")))))

  (describe
    "records-match"
    (it "should return true if one ID field matches."
        (should (records-match :given-name a b)))
    (it "should return true if two ID fields match."
        (should (records-match [:given-name :surname] a b)))
    (it "should return false if one ID field does not match."
        (should (not (records-match :given-name a c))))
    (it "should return false if two ID fields do not match."
        (should (not (records-match [:given-name :surname] a c))))
    (it "should return false even if a non-ID field matches."
        (should (not (records-match :given-name b d)))))

  (describe
    "pair-all"
    (it "should match each element with every other element."
        (should= (list [:a :b] [:a :c] [:a :d]
                       [:b :c] [:b :d]
                       [:c :d])
                 (pair-all [:a :b :c :d]))))

  (describe
    "merge-duplicates"
    (it "should return [] for the 1st value if all are duplicates."
        (should= [] (first (merge-duplicates :given-name [a b]))))
    (it "should return [] for the 2nd value if there are no duplicates."
        (should= [] (second (merge-duplicates :given-name [a c d]))))
    (it "should merge duplicates with no overlapping keys."
        (let [merged (first (second (merge-duplicates :given-name [a b c d])))]
          (should (and (= 1 (:languages merged))
                       (= 13 (:widgets merged))
                       (= 'blue (:color merged))))))
    (it "should combine overlapping keys into a set."
        (let [merged (first (second (merge-duplicates :given-name [a b c d])))]
          (should (and (= #{"Eric" "Erick"} (:given-name merged))
                       (= #{"Rochester" "Rodchester"} (:surname merged))))))
    (it "should combine three duplicates into one."
        (let [merged (first (second (merge-duplicates :given-name [a b c d e])))]
          (should= {:given-name #{"Eric" "Erick" "Eri"}
                    :surname #{"Rochester" "Rodchester"}
                    :languages 1
                    :widgets 13
                    :color #{'blue 'red}}
                   merged))))

  (run-specs))

