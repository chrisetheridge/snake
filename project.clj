(defproject snake "0.1.0-SNAPSHOT"
  :description "Snake: S3 interop for Clojure."
  :url "https://github.com/chris-etheridge/snake"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.amazonaws/aws-java-sdk-s3 "1.9.39" :exclusions [joda-time]]])
