; -*- mode: clojure; -*-
; vim: filetype=clojure

; Riemann kafka consumer - It pulls metrics from Kafka

;
; Many thanks to https://github.com/vitorbrandao
;
;

(require '[clojure.string :as str])
(import (ch.qos.logback.classic Level))

(def BOOTSTRAP_SERVERS (System/getenv "BOOTSTRAP_SERVERS"))
(def RIEMANN_CONSUMER_LOG_LEVEL (System/getenv "RIEMANN_CONSUMER_LOG_LEVEL"))
(def HOSTNAME (str "riemann-consumer-" (str/replace (.getHostName (java.net.InetAddress/getLocalHost)) #"\.eu-west-2\.compute\.internal" "")))

; Set a custom log level read from the RIEMANN_CONSUMER_LOG_LEVEL environment variable
(logging/init {:console? true})
(logging/set-level (Level/toLevel RIEMANN_CONSUMER_LOG_LEVEL))
(logging/set-level "riemann.core", (Level/toLevel RIEMANN_CONSUMER_LOG_LEVEL))
(logging/set-level "riemann.graphite", (Level/toLevel RIEMANN_CONSUMER_LOG_LEVEL))
(logging/set-level "riemann.kafka", (Level/toLevel RIEMANN_CONSUMER_LOG_LEVEL))

; Expire old events from the index every 5 seconds.
(periodically-expire 5)

(let [index (index)]
  ; Inbound events will be passed to these streams:
  (streams
    (default :ttl 60
      ; Index all events immediately.
      index
)))

(def graph
  (graphite {:host "graphite"}))

; Pull from Kafka topic "metrics"
; For a complete list of producer configuration options see https://kafka.apache.org/documentation#consumerconfigs
(kafka-consumer {
  :consumer.config {
    :bootstrap.servers BOOTSTRAP_SERVERS
    :client.id HOSTNAME
    :group.id "metrics"
    :security.protocol "SSL"
    :ssl.client.auth "none"
    :ssl.truststore.location "/etc/ssl/certs/java/cacerts"
  }
  :topics ["metrics"]})

; Stream to graphite
(streams
  graph
)
