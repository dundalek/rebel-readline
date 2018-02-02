(ns rebel-readline.commands
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as string]
   [rebel-readline.jline-api :as api :refer [attr-str]]
   [rebel-readline.tools.syntax-highlight :as syn]
   [rebel-readline.tools.colors :as col]
   [rebel-readline.service.core :as srv])
  (:import
   [org.jline.utils AttributedStringBuilder AttributedString AttributedStyle]
   [org.jline.reader LineReader]))

(defmulti command first)
(defmulti command-doc identity)

(defmethod command :default [[com]]
  (println "No command" (pr-str com) "found."))

(defmethod command-doc :repl/toggle-indent [_]
  "Toggle auto indenting on and off.")

(defmethod command :repl/toggle-indent [_]
  (srv/apply-to-config update :indent #(not %))
  (if (:indent (srv/config))
    (println "Indenting on!")
    (println "Indenting off!")))

(defmethod command-doc :repl/toggle-highlight [_]
  "Toggle readline syntax highlighting on and off.  See
`:repl/toggle-color` if you want to turn color off completely.")

(defmethod command :repl/toggle-highlight [_]
  (srv/apply-to-config update :highlight #(not %))
  (if (:highlight (srv/config))
    (println "Highlighting on!")
    (println "Highlighting off!")))

(defmethod command-doc :repl/toggle-eldoc [_]
  "Toggle the auto display of function signatures on and off.")

(defmethod command :repl/toggle-eldoc [_]
  (srv/apply-to-config update :eldoc #(not %))
  (if (:eldoc (srv/config))
    (println "Eldoc on!")
    (println "Eldoc off!")))

(defmethod command-doc :repl/toggle-completion [_]
  "Toggle the completion functionality on and off.")

(defmethod command :repl/toggle-completion [_]
  (srv/apply-to-config update :completion #(not %))
  (if (:completion (srv/config))
    (println "Completion on!")
    (println "Completion off!")))

(defmethod command-doc :repl/toggle-color [_]
  "Toggle ANSI text coloration on and off.")

(defmethod command :repl/toggle-color [_]
  (let [{:keys [color-theme backup-color-theme]}
        (srv/config)]
    (cond
      (and (nil? color-theme)
           (some? backup-color-theme)
           (col/color-themes backup-color-theme))
      (srv/apply-to-config assoc :color-theme backup-color-theme)
      (nil? color-theme)
      (srv/apply-to-config assoc :color-theme :dark-screen-theme)
      (some? color-theme)
      (do
        (srv/apply-to-config assoc :backup-color-theme color-theme)
        (srv/apply-to-config dissoc :color-theme)))))

(defmethod command-doc :repl/set-color-theme [_]
  (str "Change the color theme to one of the available themes:"
       (System/getProperty "line.separator")
       (with-out-str
         (pprint (keys col/color-themes)))))

(defmethod command :repl/set-color-theme [[_ new-theme]]
  (let [new-theme (keyword new-theme)]
    (if-not (col/color-themes new-theme)
      (println
       (str (pr-str new-theme) " is not a known color theme, please choose one of:"
            (System/getProperty "line.separator")
            (with-out-str
              (pprint (keys col/color-themes)))))
      (srv/apply-to-config assoc :color-theme new-theme))))

;; TODO this should be here the underlying repl should handle this
;; or consider a cross repl solution that works
;; maybe something you can put in service core interface
(defmethod command-doc :repl/quit [_]
  "Quits the REPL. This may only work in certain contexts.")

(defmethod command :repl/quit [_]
  (println "Bye!")
  ;; request exit
  (throw (ex-info "Exit Request" {:request-exit true})))

(defn handle-command [command-str]
  (let [cmd? 
        (try (read-string (str "[" command-str "]"))
             (catch Throwable e
               []))]
    (if (and (keyword? (first cmd?))
             (= "repl" (namespace (first cmd?))))
      (do (command cmd?) true)
      false)))

(defn all-commands []
  (filter #(= (namespace %) "repl")
   (keys (.getMethodTable command))))

(defmethod command-doc :repl/help [_]
  "Prints the documentation for all available commands.")

(defmethod command :repl/help [_]
  (println
   (.toAnsi
    (apply
     attr-str
     (AttributedString. "Available Commands:" (.bold AttributedStyle/DEFAULT))
     (System/getProperty "line.separator")
     (keep
      #(when-let [doc (command-doc %)]
         (attr-str
          " "
          (AttributedString. (prn-str %)
                             (.underline (col/fg-color AttributedStyle/CYAN)))
          (string/join
           (System/getProperty "line.separator")
           (map (fn [x] (str "     " x))
                (string/split-lines doc)))
          (System/getProperty "line.separator")))
      (sort (all-commands)))))))

#_ (require 'rebel-readline.service.impl.local-clojure-service)
#_(binding [srv/*service* (rebel-readline.service.impl.local-clojure-service/create)]
    (handle-command ":repl/toggle-ind")
    (handle-command ":repl/toggle-indent")
    (srv/config)
    
  )