;; This is the example from the /README.md  Please keep me working.
;; to run, install the lein-exec plugin then:  lein exec -p examples/README.clj
(ns example-client
  (:require [clojure.tools.logging :as log]
            [puppetlabs.pcp.client :as client]
            [puppetlabs.pcp.message-v2 :as message]))

(defn cnc-request-handler
  [conn request]
  (log/info "cnc handler got message" request)
  (let [response (-> (message/make-message)
                     (assoc :target (:sender request)
                            :message_type "example/cnc_response")
                     (message/set-data {:response "Hello world"
                                        :request (:id request)}))]
    (client/send! conn response))
  (log/info "cnc handler sent response"))

(defn default-request-handler
  [conn request]
  (log/info "Default handler got message" request))

;; connecting with handlers and starting the WebSocket heartbeat thread
(def conn (client/connect
            {:server      "wss://localhost:8142/pcp/"
             :cert        "test-resources/ssl/certs/client03.example.com.pem"
             :private-key "test-resources/ssl/private_keys/client03.example.com.pem"
             :cacert      "test-resources/ssl/certs/ca.pem"}
           {"example/cnc_request" cnc-request-handler
            :default default-request-handler}))

(def timeout-ms (* 12 1000))

(client/wait-for-connection conn timeout-ms)

(client/start-heartbeat-thread conn)

(client/wait-for-association conn timeout-ms)

;; sending messages

(log/info "### sending example/any_schema")

(client/send! conn
              (-> (message/make-message)
                  (assoc :target "pcp://*/demo-client"
                         :message_type "example/any_schema")))

(log/info "### sending example/cnc_request")

(client/send! conn
              (-> (message/make-message)
                  (assoc :target "pcp://*/demo-client"
                         :message_type "example/cnc_request")
                  (message/set-data {:action "demo"})))

;; wait 5 seconds for things to resolve
(Thread/sleep (* 5 1000))

(client/close conn)
