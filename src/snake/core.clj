(ns cognician.server.s3
  (:require
   [clojure.string :as string]
   [cognician.config :as config]
   [cognician.log :as log])
  (:import
   [java.io ByteArrayInputStream]
   [com.amazonaws.auth BasicAWSCredentials InstanceProfileCredentialsProvider]
   [com.amazonaws.services.s3 AmazonS3Client]
   [java.io File InputStream FileInputStream FileOutputStream Writer OutputStreamWriter]
   [com.amazonaws.services.s3.model
    ObjectMetadata S3Object PutObjectRequest CannedAccessControlList AmazonS3Exception
    ListObjectsRequest]))


(def DEFAULT_BUCKET "")


(defn s3-client []
  (if (and ""
           "")
    (AmazonS3Client. (BasicAWSCredentials. "" ""))
    (AmazonS3Client. (InstanceProfileCredentialsProvider. true))))


(defn copy-object
  ([bucket src-key dest-key]
   (copy-object bucket src-key bucket dest-key))
  ([src-bucket src-key dest-bucket dest-key]
   (.copyObject (s3-client) src-bucket src-key dest-bucket dest-key)))


(defn object-summary->map [object-listing]
  (map (fn [summary]
         {:metadata {:content-length (.getSize summary)
                     :etag           (.getETag summary)
                     :last-modified  (.getLastModified summary)}
          :bucket   (.getBucketName summary)
          :key      (.getKey summary)})
       (.getObjectSummaries object-listing)))


(defn object-listing->map [listing]
  {:bucket          (.getBucketName listing)
   :objects         (object-summary->map listing)
   :prefix          (.getPrefix listing)
   :common-prefixes (seq (.getCommonPrefixes listing))
   :truncated?      (.isTruncated listing)
   :max-keys        (.getMaxKeys listing)
   :marker          (.getMarker listing)
   :next-marker     (.getNextMarker listing)})


(defn list-objects [bucket {:keys [delimeter prefix] :as opts}]
  (let [req (doto (ListObjectsRequest.)
              (.setBucketName bucket)
              (.setDelimiter delimeter)
              (.setPrefix prefix))]
    (log/info "s3/list-objects " {:bucket bucket :opts opts})
    (-> (.listObjects (s3-client) req)
        (object-listing->map))))


(defn object-exists? [bucket key]
  (try
    (.getObjectMetadata (s3-client) bucket key)
    true
    (catch Exception e
      false)))


(defn inc-key [key]
  (let [m (re-matches #"^(.*?-?)(\d+)?(\..+)$" key)]
    (str (second m)
         (if-let [num (nth m 2)]
           (-> num Integer. inc)
           "-1")
         (last m))))


(defn unique-key [bucket key]
  (if (object-exists? bucket key)
    (unique-key bucket (inc-key key))
    key))


(defn filename->content-type [key]
  (case (->> key string/lower-case (re-matches #".*\.(.+)$") last)
    "woff"  "application/font-woff"
    "woff2" "application/font-woff2"
    "ttf"   "application/x-font-ttf"
    "eot"   "application/vnd.ms-fontobject"
    "svg"   "image/svg+xml"
    "css"   "text/css"
    "jpg"   "image/jpeg"
    "jpe"   "image/jpeg"
    "jpeg"  "image/jpeg"
    "png"   "image/png"
    "gif"   "image/gif"
    "txt"   "text/css"
    nil))


(defprotocol PutValueType
  "Converts `x` to a value type."
  (->put-value [x]))


(extend-protocol PutValueType
  InputStream
  (->put-value [x] x)

  File
  (->put-value [x] (FileInputStream. x))

  String
  (->put-value [x] (ByteArrayInputStream. x)))


(defn put-object [bucket key content-type file]
  (let [input   (->put-value file)
        meta    (doto (ObjectMetadata.)
                  (.setCacheControl (str "public, max-age " (* 60 60 24 31)))
                  (.setContentType content-type)
                  (.setContentLength (.available input)))
        request (PutObjectRequest. bucket key input meta)]
    (.setCannedAcl request CannedAccessControlList/PublicRead)
    (.putObject (s3-client) request)))
