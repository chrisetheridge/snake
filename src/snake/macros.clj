(ns snake.macros
  (:require [snake.core]))

(def doc-string? #(string? (first %)))

(defmacro defs3
  "Defines a function that can access S3. Ensures
  that the S3 client is available before running the body."
  [name & args]
  (let [doc-string    (when (doc-string? args) (first args))
        [args & body] (if (doc-string? args) (rest args) args)]
    `(defn ~name {:doc ~doc-string} ~args
       (assert (not (nil? (snake.core/s3-client))) "S3 client is nil! Failing.")
       ~@body)))

(comment

  (macroexpand '(defs3 a-test-fn "some fn" [an-arg] (+ 1 2)))

  (defs3 a-test-fn "some fn" [an-arg] (+ 1 2))

  )
