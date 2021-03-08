i; -*- mode: clojure; -*-
; vim: filetype=clojure

; Riemann kafka producer - It pushes metrics to Kafka
(require '[clojure.string :as str])
(import (ch.qos.logback.classic Level))

(def BOOTSTRAP_SERVERS (System/getenv "BOOTSTRAP_SERVERS"))
(def RIEMANN_PRODUCER_LOG_LEVEL (System/getenv "RIEMANN_PRODUCER_LOG_LEVEL"))
(def HOSTNAME (str "riemann-producer-" (str/replace (.getHostName (java.net.InetAddress/getLocalHost)) #"\.eu-west-2\.compute\.internal" "")))

; Set a custom log level read from the RIEMANN_PRODUCER_LOG_LEVEL environment variable
(logging/init {:console? true})
(logging/set-level (Level/toLevel RIEMANN_PRODUCER_LOG_LEVEL))
(logging/set-level "riemann.core", (Level/toLevel RIEMANN_PRODUCER_LOG_LEVEL))
(logging/set-level "riemann.graphite", (Level/toLevel RIEMANN_PRODUCER_LOG_LEVEL))
(logging/set-level "riemann.kafka", (Level/toLevel RIEMANN_PRODUCER_LOG_LEVEL))

; Listen on the local interface over graphite (3003)
(let [host "0.0.0.0"]
  (graphite-server {:host host
                    :port 3003}
  )
)

; Expire old events from the index every 5 seconds.
(periodically-expire 5)

(let [index (index)]
  ; Inbound events will be passed to these streams:
  (streams
    (default :ttl 60
      ; Index all events immediately.
      index
)))

; For a complete list of producer configuration options see https://kafka.apache.org/documentation#producerconfigs
(def kafka-output (kafka {
  :bootstrap.servers BOOTSTRAP_SERVERS
  :client.id HOSTNAME
  :security.protocol "SSL"
  :ssl.client.auth "none"
  :ssl.truststore.location "/etc/ssl/certs/java/cacerts"
}))

; Stream to kafka topic "metrics"
(streams
  (kafka-output "metrics")
)
