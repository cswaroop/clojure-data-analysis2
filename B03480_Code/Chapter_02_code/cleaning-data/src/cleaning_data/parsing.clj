
(ns cleaning-data.parsing
  (:use [protoflex.parse]))

#_
(use 'protoflex.parse)

(defrecord FastaData
  [defline gene-seq])

(defn <| [l r]
  (let [l-output (l)]
    (r)
    l-output))

(defn nl []
  (chr-in #{\newline \return}))

(defn defline []
  (chr \>)
  (<| #(read-to-re #"[\n\r]+") nl))

(defn acid-code []
  (chr-in #{\A \B \C \D \E \F \G \H \I \K \L \M
            \N \P \Q \R \S \T \U \V \W \X \Y \Z
            \- \*}))

(defn acid-code-line []
  (<| #(multi+ acid-code) #(attempt nl)))

(defn fasta []
  (ws?)
  (let [dl (defline)
        gls (apply str (flatten (multi+ acid-code-line)))]
    {:defline dl, :gene-seq gls}))

(defn multi-fasta []
  (<| #(multi+ fasta)
      ws?))

(defn parse-fasta [input]
  (parse multi-fasta input
         :eof false :auto-trim false))

