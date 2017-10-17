(ns snake.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [com.amazonaws.auth BasicAWSCredentials InstanceProfileCredentialsProvider]
           com.amazonaws.services.s3.AmazonS3Client
           [com.amazonaws.services.s3.model CannedAccessControlList ListObjectsRequest ObjectMetadata PutObjectRequest]
           [java.io ByteArrayInputStream File FileInputStream InputStream]))

(def *s3-creds
  (atom {:access-key nil
         :secret-key nil}))

(defn setup!
  "Sets up the S3 credentials map. This *must* be used if you are
   not going to be using AWS instance profiles.

   access-key = S3 Access Key
   secret-key = S3 Secret key"
  [access-key secret-key]
  (reset! *s3-creds {:access-key access-key
                     :secret-key secret-key})
  @*s3-creds)

(defn s3-client
  "Returns the current AWS S3 client."
  []
  (cond
    (and (:access-key @*s3-creds)
         (:secret-key @*s3-creds))
    (AmazonS3Client. (BasicAWSCredentials. (:access-key @*s3-creds)
                                           (:secret-key @*s3-creds)))
    :else (AmazonS3Client. (InstanceProfileCredentialsProvider. true))))

(defn copy
  "Copies an object from `source bucket` to `destination bucket`.

   If only supplied 1 bucket, the object will be copied to that bucket.

   Required args:
   src-bucket  = name of the source bucket
   src-key     = key of the source object
   dest-bucket = name of the destination bucket
   dest-key    = key of the destination object"
  ([bucket src-key dest-key]
   (copy bucket src-key bucket dest-key))
  ([src-bucket src-key dest-bucket dest-key]
   (.copyObject (s3-client) src-bucket src-key dest-bucket dest-key)))

(defn- object-summary->map [^ListObjectsRequest object-listing]
  (map (fn [summary]
         {:metadata {:content-length (.getSize summary)
                     :etag           (.getETag summary)
                     :last-modified  (.getLastModified summary)}
          :bucket   (.getBucketName summary)
          :key      (.getKey summary)})
       (.getObjectSummaries object-listing)))

(defn- object-listing->map [listing]
  {:bucket          (.getBucketName listing)
   :objects         (object-summary->map listing)
   :prefix          (.getPrefix listing)
   :common-prefixes (seq (.getCommonPrefixes listing))
   :truncated?      (.isTruncated listing)
   :max-keys        (.getMaxKeys listing)
   :marker          (.getMarker listing)
   :next-marker     (.getNextMarker listing)})

(defn list-objects
  "Lists objects in the bucket.

   Optionally takes in a map containing list options.

   Required args:
   bucket     = name of the bucket to list
   :delimeter = path delimter for the bucket
   :prefix    = prefix of the file. e.g. \"foo\" would expand to
                my-bucket/foo/bar.bz"
  [bucket {prefix :prefix delimeter :delimeter
           :or    {prefix    ""
                   delimeter "/"}}]
  (let [req (doto (ListObjectsRequest.)
              (.setBucketName bucket)
              (.setDelimiter delimeter)
              (.setPrefix prefix))]
    (-> (.listObjects (s3-client) req)
        (object-listing->map))))

(defn object-exists?
  "Returns whether an object exists in the bucket or not.

   Required args:
   bucket = name of bucket to check
   key    = object to find"
  [bucket key]
  (try
    (.getObjectMetadata (s3-client) bucket key)
    true
    (catch Exception e
      false)))

(defn- inc-key [key]
  (let [m (re-matches #"^(.*?-?)(\d+)?(\..+)$" key)]
    (str (second m)
         (if-let [num (nth m 2)]
           (-> num Integer. inc)
           "-1")
         (last m))))

(defn unique-key [bucket key]
  "Returns a unqiue variant of the key supplied, for a bucket.

   Required args:
   bucket = name of bucket to use
   key    = unique varient of the key to return

   Example:

   (unique-key \"foo\") => foo ; foo did not exist
   (unique-key \"foo\") => foo-1 ; foo exists"
  (if (object-exists? bucket key)
    (unique-key bucket (inc-key key))
    key))

(defn filename->content-type [key]
  "Returns the content-type from the filename. Only infers the filename based
   on the extension.

   Required args:
   key = filename of the file."
  (case (->> key string/lower-case (re-matches #".*\.(.+)$") last)
    "woff"  "application/font-woff"
    "woff2" "application/font-woff2"
    "ttf"   "application/x-font-ttf"
    "eot"   "application/vnd.ms-fontobject"
    "svg"   "image/svg+xml"
    "css"   "text/css"
    "jpg"   "image/jpeg"
    "html"  "text/html"
    "jpe"   "image/jpeg"
    "jpeg"  "image/jpeg"
    "png"   "image/png"
    "gif"   "image/gif"
    "txt"   "text/css"
    nil))

(defn generate-metadata
  "Generates a metadata map, given the filename and data."
  [filename data]
  {:content-type   (filename->content-type filename)
   :content-length (count data)})

(defprotocol PutValueType
  "Converts `x` to a value type appropriate to `put` into a bucket."
  (->put-value [x]))

(extend-protocol PutValueType
  InputStream
  (->put-value [x] x)

  File
  (->put-value [x] (FileInputStream. x))

  String
  (->put-value [x] (ByteArrayInputStream. (.getBytes x))))

(defn put-object
  "Puts an object into a bucket.

   Required args:
   bucket       = destination bucket for the object
   key          = key for the resulting object
   content-type = content type of the resulting file
   file         = the file to put

   `java.io.InputStream`, `java.io.File`, and `String` is supported as values.

   For supported content types, look at `filename->content-type`."
  [bucket key content-type file]
  (let [input   (->put-value file)
        meta    (doto (ObjectMetadata.)
                  (.setCacheControl (str "public, max-age " (* 60 60 24 31)))
                  (.setContentType content-type)
                  (.setContentLength (.available input)))
        request (PutObjectRequest. bucket key input meta)]
    (.setCannedAcl request CannedAccessControlList/PublicRead)
    (.putObject (s3-client) request)))

(defn get-object [bucket key]
  "Gets an object by bucket and key.

   Required args:
   bucket = bucket the key is in.
   key    = the key of the object."
  (-> (s3-client)
      (.getObject bucket key)
      (.getObjectContent)
      (slurp)))

(defn get-object-input-stream [bucket key]
  "Gets an object's input stream by bucket and key.

   Required args:
   bucket = bucket the key is in.
   key    = the key of the object."
  (-> (s3-client)
      (.getObject bucket key)
      (.getObjectContent)))

(defn upload!
  "Uploads a given file, with given filename, to the given bucket.

   Required args:
   bucket   = destination bucket for the object
   filename = name of the resulting file
   file     = actual file to upload

   Optional args:
   overwrite? = whether or not to overwrite, in the instance
   of the file already existing. Defaults to `false`.

   Returns the resulting filename."
  [bucket filename file & {:as options}]
  (let [content-type (filename->content-type filename)
        filename     (if (and options (:overwrite? options))
                       filename
                       (unique-key bucket filename))]
    (put-object bucket filename content-type file)
    filename))

(defn download-folder
  "Download `local-path` to local disk.
   Prefix specifies prefix the bucket to list. e.g. \"foo\" would expand
   my-bucket/foo/"
  [bucket local-path prefix]
  (doseq [{path                  :key
           {len :content-length} :metadata} (:objects (list-objects bucket {:prefix prefix}))
          :when                             (pos? len)
          :let                              [dest (str local-path "/" path)]]
    (io/make-parents dest)
    (with-open [raw-stream (:content (get-object bucket path))
                in         (io/input-stream raw-stream)]
      (io/copy in (io/file dest)))))

(defn url-for-bucket-and-key
  "Returns the URL for the given S3 bucket and key.

   Required args:
   bucket = bucket with the object
   key    = object key / name"
  [bucket key]
  (str "https://" bucket ".s3.amazonaws.com/" key))

;; Deprecated functions.

(defn ^:deprecated copy-object
  "Copies an object from `source bucket` to `destination bucket`.

   If only supplied 1 bucket, the object will be copied to that

   Required args:
   src-bucket  = name of the source bucket
   src-key     = key of the source object
   dest-bucket = name of the destination bucket
   dest-key    = key of the destination object"
  ([bucket src-key dest-key]
   (copy-object bucket src-key bucket dest-key))
  ([src-bucket src-key dest-bucket dest-key]
   (.copyObject (s3-client) src-bucket src-key dest-bucket dest-key)))
