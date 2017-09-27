(ns leiningen.leinsync.program-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.project-reader :as pr]
            [leiningen.sync :as s])
  (:import (java.util.regex PatternSyntaxException)))

(deftest ^:unit test-split-string
  (is (= ["project1" "project2" "project3"]
         (u/split "project1,project2,project3")))

  (is (= ["de.otto.one.cool.ns1" "de.otto.one.cool.ns2"]
         (u/split "de.otto.one.cool.ns1,de.otto.one.cool.ns2"))))

(deftest ^:unit test-format-str
  (is (= "123  "
         (u/format-str "123" 5)))

  (is (= "123.."
         (u/format-str "1234567" 5)))

  (is (= "123456"
         (u/format-str "123456" 6))))

(deftest ^:unit find-command
  (let [m {:a (fn [x y] (+ x y))
           :b (fn [x y] (* x y))
           :c (fn [_ _] (throw RuntimeException))}
        commands (s/find-command {:a "" :b "" :c ""} m)]
    (is (= 3 (count commands)))))

(deftest ^:unit option->command
  (testing "find correct commands"
    (let [m {:a (fn [x y] (+ x y))
             :b (fn [x y] (* x y))}
          commands (s/option->command {:a "" :b ""} m)]
      (is (= 2 (count commands)))))

  (testing "returns fallback if no command found"
    (let [m {:default {:a ""}
             :a       (fn [_ x y] (+ x y))
             :b       (fn [_ x y] (* x y))}
          [fn-default & _] (s/option->command {} m)]
      (is (= 3 (fn-default 1 2))))))

(deftest ^:unit include-option-change-src-desc-test
  (testing "no change if have no include option"
    (let [source-project-desc (atom nil)
          target-project-name (atom nil)
          input "project-1,project-2"
          source-project {:ns-sync {:test-cmd   [["./lein.sh" "clean"]
                                                 ["./lein.sh" "test"]]
                                    :namespaces ["ns1" "ns2"]
                                    :resources  ["rc1" "rc2"]}}
          options {:a "command a opt"}
          sync-commands {:a (fn [_ target-project source]
                              (reset! source-project-desc source)
                              (reset! target-project-name target-project))}]
      (s/execute-program input source-project options sync-commands "example")

      (is (= {:ns-sync {:namespaces ["ns1" "ns2"]
                        :resources  ["rc1" "rc2"]
                        :test-cmd   [["./lein.sh" "clean"] ["./lein.sh" "test"]]}}
             @source-project-desc))

      (is (= ["project-1" "project-2"]
             @target-project-name))))

  (testing "change src project desc if have include option"
    (let [state (atom nil)
          input "project1,project2"
          source-project {:ns-sync {:test-cmd   [["./lein.sh" "clean"]
                                                 ["./lein.sh" "test"]]
                                    :namespaces ["ns1" "ns2"]
                                    :resources  ["rc1" "rc2"]}}
          options {:a                 "command a opt"
                   :include-namespace ["ns3" "ns4" "ns5"]
                   :include-resource  ["rs1" "rs2" "rs3"]}
          sync-commands {:a (fn [_ _ source] (reset! state source))}]
      (s/execute-program input source-project options sync-commands "")
      (is (= {:ns-sync {:namespaces ["ns3" "ns4" "ns5"]
                        :resources  ["rs1" "rs2" "rs3"]
                        :test-cmd   [["./lein.sh" "clean"] ["./lein.sh" "test"]]}}

             @state))))

  (testing "exception, no command has been executed"
    (let [state (atom nil)
          input "*-project"
          source-project {}
          options {:a "command a opt"}
          sync-commands {:a (fn [_ _ source] (reset! state source))}]
      (is (not @state)))))

(deftest ^:unit sub-str
  (is (= "abcd ..." (u/sub-str "abcde" 4)))
  (is (= "abcde" (u/sub-str "abcde" 7)))
  (is (= " ..." (u/sub-str "abcde" 0))))

(deftest ^:unit is-success?
  (is (true? (u/is-success? {:exit 0})))
  (is (false? (u/is-success? {:exit 1}))))

(deftest ^:unit output-of
  (is (= "text" (u/output-of {:out "text"})))
  (is (= "a,b,c" (u/output-of {:out "a\nb\nc\n"} ","))))

(deftest ^:unit split-output-of
  (is (= ["a" "b" "c"] (u/split-output-of {:out "a\nb\nc\n"}))))

(deftest ^:unit error-of
  (is (= "text" (u/error-of {:err "text"})))
  (is (= "a,b,c" (u/error-of {:err "a\nb\nc\n"} ","))))

(deftest ^:unit yes-or-no
  (is (true? (u/yes-or-no "y")))
  (is (true? (u/yes-or-no "n")))
  (is (false? (u/yes-or-no "yn"))))

(deftest ^:unit is-number
  (is (false? (u/is-number 6 "6")))
  (is (true? (u/is-number 6 "5")))
  (is (false? (u/is-number 6 "7"))))

(deftest ^:unit lazy-contains?
  (is (true? (u/lazy-contains? (lazy-seq [1 2]) 1)))
  (is (false? (u/lazy-contains? (lazy-seq [1 2]) 3))))

(deftest ^:unit get-profiles
  (is (= #{"dev" "test" "uberjar"}
         (s/get-profiles {:profiles {:uberjar {}
                                     :test    {}
                                     :dev     {}}})))
  (is (= #{}
         (s/get-profiles {})))

  (is (s/usage {})))

(deftest ^:unit cli-option-test
  (testing "empty input"
    (let [input []
          source-project-desc {:ns-sync {}}
          {:keys [options arguments summary errors]} (s/parse-args {:ns-sync {}} input)]
      (is summary)
      (is (not errors))
      (is (= {} options))
      (is (= [] arguments))))

  (testing "happy case input"
    (let [input ["project1,project2" "--list" "diff" "-i" "ns1,ns2" "-j" "rs1,rs2" "--pull" "--push" "-d" "global"]
          source-project-desc {:ns-sync {}}
          {:keys [options arguments summary errors]} (s/parse-args {:ns-sync {}} input)]
      (is summary)
      (is (not errors))
      (is (= {:push              true
              :pull              true
              :list              :diff
              :deps              :global
              :include-namespace ["ns1" "ns2"]
              :include-resource  ["rs1" "rs2"]}
             options))
      (is (= ["project1,project2"] arguments)))))

(deftest ^:unit find-sync-projects-test
  (is (= #{"project-1" "project-2"}
         (s/find-sync-projects "example")))
  (is (= #{}
         (s/find-sync-projects "src")
         (s/find-sync-projects "non-sense"))))

(deftest ^:unit parse-project-search-input-test
  (is (= ["project-1-awesome" "project-2-awesome"]
         (s/parse-search-input ".*-awesome" #{"project-1-awesome" "project-2-awesome"})))

  (is (= []
         (s/parse-search-input "bull,shit" #{"project-1" "project-2"})))

  (is (= ["project-1" "project-2"]
         (s/parse-search-input "project-1,project-2" #{"project-1" "project-2"})))

  (is (= ["project-1" "project-2"]
         (s/parse-search-input "project-1,project-2" #{"project-1" "project-1-1" "project-2"})))

  (is (= ["project-1"]
         (s/parse-search-input "project-1,project-3" #{"project-1" "project-2"})))

  (is (= ["project-2" "project-1"]
         (s/parse-search-input "project-*" #{"project-1" "project-2"})))

  (is (= ["project-1-awesome" "project-2-awesome"]
         (s/parse-search-input ".*-awesome" #{"project-1-awesome" "project-2-awesome"})
         (s/parse-search-input ".*" #{"project-1-awesome" "project-2-awesome"})))

  (is (= ["project-1-awesome" "project-2"]
         (s/parse-search-input "project-1-awesome,project-2" #{"project-1-awesome" "project-1" "project-2"}))))

(deftest ^:unit ask-user-test
  (with-redefs-fn {#'u/capture-input (fn [prompt] "a answer")}
    #(is (= "a answer" (u/ask-user "a question")))))

(deftest ^:unit read-target-project-test
  (with-redefs-fn {#'pr/->target-project-path (fn [_p] "example/project-1")}
    #(is (= "project-1" (:name (pr/read-target-project-clj "project-1"))))))