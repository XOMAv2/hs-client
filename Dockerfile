FROM clojure
WORKDIR /usr/src/app
COPY . .
COPY config.edn ./target
CMD java -jar ./target/hs-client.jar -h