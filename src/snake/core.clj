(ns snake.core
  (:require [clojure.string :as string])
  (:import
   [java.io ByteArrayInputStream]
   [com.amazonaws.auth BasicAWSCredentials InstanceProfileCredentialsProvider]
   [com.amazonaws.services.s3 AmazonS3Client]
   [java.io File InputStream FileInputStream FileOutputStream Writer OutputStreamWriter]
   [com.amazonaws.services.s3.model
    ObectMetadata
    S3Object
    PutObjectRequest
    CannedAccessControlList
    AmazonS3Exception
    ListObjectsRequest]))
