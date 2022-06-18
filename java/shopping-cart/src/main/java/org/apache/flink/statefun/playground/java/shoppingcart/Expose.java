
Filter changed files
 38  
java/shopping-cart/Dockerfile
@@ -0,0 +1,38 @@
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Build the functions code ...
FROM maven:3.6.3-jdk-11 AS builder
# TODO remove these commented lines and the jar; this is needed now only because we don't have the latest Java SDK published to Maven central yet
COPY statefun-sdk-java-3.0-SNAPSHOT.jar /usr/src/app/
RUN mvn install:install-file \
 -Dfile=/usr/src/app/statefun-sdk-java-3.0-SNAPSHOT.jar \
 -DgroupId=org.apache.flink \
 -DartifactId=statefun-sdk-java \
 -Dversion=3.0-SNAPSHOT \
 -Dpackaging=jar \
 -DgeneratePom=true
COPY pom.xml /usr/src/app/
# Build dependencies and cache this layer
RUN mvn -f /usr/src/app dependency:go-offline package -B
COPY src /usr/src/app/src
RUN mvn -f /usr/src/app/pom.xml clean package

# ... and run the web server!
FROM openjdk:8
WORKDIR /
COPY --from=builder /usr/src/app/target/shopping-cart*jar-with-dependencies.jar shopping-cart.jar
EXPOSE 1108
CMD java -jar shopping-cart.jar 
 76  
java/shopping-cart/README.md
@@ -0,0 +1,76 @@
# Shopping Cart Example with Docker Compose

This example demonstrates interaction between two stateful functions - one responsible for managing the users' shopping carts (`UserShoppingCartFn`), and the other responsible for managing the stock (`StockFn`). It is intended to showcase a somewhat more complex business logic where consistent state guarantees span multiple interacting stateful functions. You can think about them as two microservices that 'magically' always stay in consistent state with respect to each other and the output, without having to synchronize them or reconciliate their state in case of failures. This example uses an egress in exactly-once mode. This means that the receipt is produced to the output only if the internal fault-tolerate state of the functions got consistently updated according to the checkout request (requires `read_committed` consumer isolation level).  

If you are new to stateful functions, we recommend you to first look at a more simple example, the [Greeter Example](../greeter).

## Directory structure

- `src/`, `pom.xml` and `Dockerfile`: These files and directories are the contents of a Java Maven project which builds
  our functions service, hosting the `UserShoppingCartFn` and `StockFn` behind a HTTP endpoint. Check out the source code under
  `src/main/java`. The `Dockerfile` is used to build a Docker image for our functions service.
- `module.yaml`: The [Module Specification]() file to be mounted to the StateFun runtime process containers. This
  configures a few things for a StateFun application, such as the service endpoints of the application's functions, as
  well as definitions of [Ingresses and Egresses]() which the application will use.
- `docker-compose.yml`: Docker Compose file to spin up everything.
- `playthrough`: utilities for automatically playing through the interactions scenarios.

## Prerequisites

- Docker
- Docker Compose

## Running the example

This example works with Docker Compose, and runs a few services that build up an end-to-end StateFun application:
- Functions service that runs your functions and expose them through an HTTP endpoint.
- StateFun runtime processes (a manager plus workers) that will handle ingress, egress, and inter-function messages as
  well as function state storage in a consistent and fault-tolerant manner.
- Apache Kafka broker for the application ingress and egress. StateFun currently natively supports AWS Kinesis as well,
  and you can also extend to connect with other systems.

To build the example, execute:

```
cd java/shopping-cart
docker-compose build
```

This pulls all the necessary Docker images (StateFun and Kafka), and also builds the functions service image. This can
take a few minutes as it also needs to build the function's Java project.

Afterward the build completes, start running all the services:

```
docker-compose up
```

## Play around!

The `playground` folder contains scenario(s) and utilities which allow you to easily execute a set of steps that emulate interactions with the stateful functions.

In order to run a scenario, execute:
```
cd java/shopping-cart/playthrough
./scenario_1.sh
```

It will send a series of messages, results of which you can observe in the logs of the `shopping-cart-functions` component:
```
docker-compose logs -f shopping-cart-functions
```
Note: `Caller: Optional.empty` in the logs corresponds to the messages that came via an ingress rather than from another stateful function. 

To see the results produced to the egress:
```
docker-compose exec kafka bash -c '/usr/bin/kafka-console-consumer --topic receipts --bootstrap-server kafka:9092'
```

If you want to modify the code, you can do a hot redeploy of your functions service:
```
docker-compose up -d --build shopping-cart-functions
```
This rebuilds the functions service image with the updated code, and restarts the service with the new image.



 91  
java/shopping-cart/docker-compose.yml
@@ -0,0 +1,91 @@
################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################
version: "2.1"

services:

  ###############################################################
  #    Functions service
  ###############################################################

  shopping-cart-functions:
    build:
      dockerfile: Dockerfile
      context: .
    expose:
      - "1108"

  ###############################################################
  #    StateFun runtime
  ###############################################################

  statefun-manager:
    image: flink-statefun:3.0-SNAPSHOT
    expose:
      - "6123"
    ports:
      - "8081:8081"
    environment:
      ROLE: master
      MASTER_HOST: statefun-manager
    volumes:
      - ./module.yaml:/opt/statefun/modules/shopping-cart/module.yaml

  statefun-worker:
    image: flink-statefun:3.0-SNAPSHOT
    expose:
      - "6121"
      - "6122"
    depends_on:
      - statefun-manager
      - kafka
      - shopping-cart-functions
    links:
      - "statefun-manager:statefun-manager"
      - "kafka:kafka"
      - "shopping-cart-functions:shopping-cart-functions"
    environment:
      ROLE: worker
      MASTER_HOST: statefun-manager
    volumes:
      - ./module.yaml:/opt/statefun/modules/shopping-cart/module.yaml

  ###############################################################
  #    Kafka for ingress and egress
  ###############################################################

  zookeeper:
    image: confluentinc/cp-zookeeper:5.4.3
    environment:
      ZOOKEEPER_CLIENT_PORT: "2181"
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:5.4.3
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    links:
      - "zookeeper:zookeeper"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
 87  
java/shopping-cart/module.yaml
@@ -0,0 +1,87 @@
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
version: "3.0"
module:
  meta:
    type: remote
  spec:
    endpoints:
      - endpoint:
          meta:
            kind: http
          spec:
            functions: com.example/*
            urlPathTemplate: http://shopping-cart-functions:1108/
    ingresses:
      # user-shopping-cart:
      # TODO: add RemoveFromCart
      - ingress:
          meta:
            type: io.statefun.kafka/ingress
            id: example.com/add-to-cart
          spec:
            address: kafka:9092
            consumerGroupId: my-group-id
            topics:
              - topic: add-to-cart
                valueType: com.example/AddToCart
                targets:
                  - com.example/user-shopping-cart
      - ingress:
          meta:
            type: io.statefun.kafka/ingress
            id: example.com/clear-cart
          spec:
            address: kafka:9092
            consumerGroupId: my-group-id
            topics:
              - topic: clear-cart
                valueType: com.example/ClearCart
                targets:
                  - com.example/user-shopping-cart
      - ingress:
          meta:
            type: io.statefun.kafka/ingress
            id: example.com/checkout
          spec:
            address: kafka:9092
            consumerGroupId: my-group-id
            topics:
              - topic: checkout
                valueType: com.example/Checkout
                targets:
                  - com.example/user-shopping-cart
      - ingress:
          meta:
            type: io.statefun.kafka/ingress
            id: com.example/restock-items
          spec:
            address: kafka:9092
            consumerGroupId: my-group-id
            topics:
              - topic: restock-items
                valueType: com.example/RestockItem
                targets:
                  - com.example/stock
    egresses:
      - egress:
          meta:
            type: io.statefun.kafka/egress
            id: com.example/receipts
          spec:
            address: kafka:9092
            deliverySemantic:
              type: exactly-once
              transactionTimeoutMillis: 100000 
 37  
java/shopping-cart/playthrough/scenario_1.sh
@@ -0,0 +1,37 @@
#!/bin/bash

source $(dirname "$0")/utils.sh

######## Scenario 1:
#  1) add socks to stock (via StockFn)
#  2) put socks for userId "1" into the shopping cart (via UserShoppingCartFn)
#  3) checkout (via UserShoppingCartFn)
#--------------------------------
# 1)
key="socks" # itemId
json=$(cat <<JSON
  {"itemId":"socks","quantity":50}
JSON
)
ingress_topic="restock-items" # StockFn
send_to_kafka $key $json $ingress_topic
sleep 1
#--------------------------------
# 2)
key="1" # userId
json=$(cat <<JSON
  {"userId":"1","quantity":3,"itemId":"socks"}
JSON
)
ingress_topic="add-to-cart" # UserShoppingCartFn
send_to_kafka $key $json $ingress_topic
sleep 1
#--------------------------------
# 3)
key="1" # userId
json=$(cat <<JSON
  {"userId":"1"}
JSON
)
ingress_topic="checkout" # UserShoppingCartFn
send_to_kafka $key $json $ingress_topic 
 14  
java/shopping-cart/playthrough/utils.sh
@@ -0,0 +1,14 @@
#!/bin/bash

# Sends messages to Kafka within docker-compose setup.
# Parameters:
#  - param1: message key
#  - param2: message payload
#  - param3: Kafka topic
send_to_kafka () {
    local key=$1
    local payload=$2
    local topic=$3
    echo "Sending \"$payload\" with key \"$key\" to \"$topic\" topic"
    docker-compose exec kafka bash -c "echo '$key: $payload' | /usr/bin/kafka-console-producer --topic $topic --broker-list kafka:9092 --property 'parse.key=true' --property 'key.separator=:'"
} 
 132  
java/shopping-cart/pom.xml
@@ -0,0 +1,132 @@
<?xml version="1.0" encoding="UTF-8"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <artifactId>shopping-cart</artifactId>
    <groupId>org.apache.flink</groupId>
    <version>3.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <statefun.version>3.0-SNAPSHOT</statefun.version>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <untertow.version>1.4.18.Final</untertow.version>
        <jackson-databind.version>2.12.2</jackson-databind.version>
        <junit.version>4.12</junit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>statefun-sdk-java</artifactId>
            <version>${statefun.version}</version>
        </dependency>

        <dependency>
            <groupId>io.undertow</groupId>
            <artifactId>undertow-core</artifactId>
            <version>${untertow.version}</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson-databind.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-slf4j-impl</artifactId>
            <version>2.14.1</version>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.25</version>
        </dependency>

        <!-- test -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Build a fat executable jar -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <mainClass>org.apache.flink.statefun.playground.java.shoppingcart.Expose</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Java code style -->
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
                <version>1.20.0</version>
                <configuration>
                    <java>
                        <googleJavaFormat>
                            <version>1.7</version>
                            <style>GOOGLE</style>
                        </googleJavaFormat>
                        <removeUnusedImports/>
                    </java>
                </configuration>
                <executions>
                    <execution>
                        <id>spotless-check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
 105  
...ing-cart/src/main/java/org/apache/flink/statefun/playground/java/shoppingcart/Expose.java
@@ -0,0 +1,105 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.statefun.playground.java.shoppingcart;

import static io.undertow.UndertowOptions.ENABLE_HTTP2;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.statefun.sdk.java.StatefulFunctionSpec;
import org.apache.flink.statefun.sdk.java.StatefulFunctions;
import org.apache.flink.statefun.sdk.java.handler.RequestReplyHandler;
import org.apache.flink.statefun.sdk.java.slice.Slice;
import org.apache.flink.statefun.sdk.java.slice.Slices;

public class Expose {

  public static void main(String... args) {
    StatefulFunctionSpec stockFn =
        StatefulFunctionSpec.builder(StockFn.TYPE)
            .withValueSpec(StockFn.STOCK)
            .withSupplier(StockFn::new)
            .build();

    StatefulFunctionSpec userShoppingCartFn =
        StatefulFunctionSpec.builder(UserShoppingCartFn.TYPE)
            .withValueSpec(UserShoppingCartFn.BASKET)
            .withSupplier(UserShoppingCartFn::new)
            .build();

    StatefulFunctions functions = new StatefulFunctions();
    functions.withStatefulFunction(stockFn).withStatefulFunction(userShoppingCartFn);
    RequestReplyHandler handler = functions.requestReplyHandler();

    /* This example uses the Undertow http server, but any HTTP server/framework will work as-well */
    Undertow server =
        Undertow.builder()
            .addHttpListener(1108, "0.0.0.0")
            .setHandler(new UndertowStateFunHandler(handler))
            .setServerOption(ENABLE_HTTP2, true)
            .build();

    server.start();
  }

  private static final class UndertowStateFunHandler implements HttpHandler {
    private final RequestReplyHandler handler;

    UndertowStateFunHandler(RequestReplyHandler handler) {
      this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
      exchange.getRequestReceiver().receiveFullBytes(this::onRequestBody);
    }

    private void onRequestBody(HttpServerExchange exchange, byte[] requestBytes) {
      try {
        CompletableFuture<Slice> future = handler.handle(Slices.wrap(requestBytes));
        exchange.dispatch();
        future.whenComplete(
            (responseBytes, ex) -> {
              if (ex != null) {
                onException(exchange, ex);
              } else {
                onSuccess(exchange, responseBytes);
              }
            });
      } catch (Throwable t) {
        onException(exchange, t);
      }
    }

    private void onException(HttpServerExchange exchange, Throwable t) {
      t.printStackTrace(System.out);
      exchange.getResponseHeaders().put(Headers.STATUS, 500);
      exchange.endExchange();
    }

    private void onSuccess(HttpServerExchange exchange, Slice result) {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
      exchange.getResponseSender().send(result.asReadOnlyByteBuffer());
    }
  }
}
