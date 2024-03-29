---
title: "Apache Kafka Connector"
nav-title: Kafka
nav-parent_id: connectors
nav-pos: 1
---
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

* This will be replaced by the TOC
{:toc}

This connector provides access to event streams served by [Apache Kafka](https://kafka.apache.org/).

Flink provides special Kafka Connectors for reading and writing data from/to Kafka topics.
The Flink Kafka Consumer integrates with Flink's checkpointing mechanism to provide
exactly-once processing semantics. To achieve that, Flink does not purely rely on Kafka's consumer group
offset tracking, but tracks and checkpoints these offsets internally as well.

Please pick a package (maven artifact id) and class name for your use-case and environment.
For most users, the `FlinkKafkaConsumer08` (part of `flink-connector-kafka`) is appropriate.

<table class="table table-bordered">
  <thead>
    <tr>
      <th class="text-left">Maven Dependency</th>
      <th class="text-left">Supported since</th>
      <th class="text-left">Consumer and <br>
      Producer Class name</th>
      <th class="text-left">Kafka version</th>
      <th class="text-left">Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
        <td>flink-connector-kafka-0.8{{ site.scala_version_suffix }}</td>
        <td>1.0.0</td>
        <td>FlinkKafkaConsumer08<br>
        FlinkKafkaProducer08</td>
        <td>0.8.x</td>
        <td>Uses the <a href="https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+SimpleConsumer+Example">SimpleConsumer</a> API of Kafka internally. Offsets are committed to ZK by Flink.</td>
    </tr>
    <tr>
        <td>flink-connector-kafka-0.9{{ site.scala_version_suffix }}</td>
        <td>1.0.0</td>
        <td>FlinkKafkaConsumer09<br>
        FlinkKafkaProducer09</td>
        <td>0.9.x</td>
        <td>Uses the new <a href="http://kafka.apache.org/documentation.html#newconsumerapi">Consumer API</a> Kafka.</td>
    </tr>
    <tr>
        <td>flink-connector-kafka-0.10{{ site.scala_version_suffix }}</td>
        <td>1.2.0</td>
        <td>FlinkKafkaConsumer010<br>
        FlinkKafkaProducer010</td>
        <td>0.10.x</td>
        <td>This connector supports <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-32+-+Add+timestamps+to+Kafka+message">Kafka messages with timestamps</a> both for producing and consuming.</td>
    </tr>
    <tr>
        <td>flink-connector-kafka-0.11{{ site.scala_version_suffix }}</td>
        <td>1.4.0</td>
        <td>FlinkKafkaConsumer011<br>
        FlinkKafkaProducer011</td>
        <td>0.11.x</td>
        <td>Since 0.11.x Kafka does not support scala 2.10. This connector supports <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-98+-+Exactly+Once+Delivery+and+Transactional+Messaging">Kafka transactional messaging</a> to provide exactly once semantic for the producer.</td>
    </tr>
    <tr>
        <td>flink-connector-kafka{{ site.scala_version_suffix }}</td>
        <td>1.7.0</td>
        <td>FlinkKafkaConsumer<br>
        FlinkKafkaProducer</td>
        <td>>= 1.0.0</td>
        <td>
        This universal Kafka connector attempts to track the latest version of the Kafka client.
        The version of the client it uses may change between Flink releases. Starting with Flink 1.9 release, it uses the Kafka 2.2.0 client.
        Modern Kafka clients are backwards compatible with broker versions 0.10.0 or later.
        However for Kafka 0.11.x and 0.10.x versions, we recommend using dedicated
        flink-connector-kafka-0.11{{ site.scala_version_suffix }} and flink-connector-kafka-0.10{{ site.scala_version_suffix }} respectively.
        </td>
    </tr>
  </tbody>
</table>

Then, import the connector in your maven project:

{% highlight xml %}
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-connector-kafka{{ site.scala_version_suffix }}</artifactId>
  <version>{{ site.version }}</version>
</dependency>
{% endhighlight %}

Note that the streaming connectors are currently not part of the binary distribution.
See how to link with them for cluster execution [here]({{ site.baseurl}}/dev/projectsetup/dependencies.html).

## Installing Apache Kafka

* Follow the instructions from [Kafka's quickstart](https://kafka.apache.org/documentation.html#quickstart) to download the code and launch a server (launching a Zookeeper and a Kafka server is required every time before starting the application).
* If the Kafka and Zookeeper servers are running on a remote machine, then the `advertised.host.name` setting in the `config/server.properties` file must be set to the machine's IP address.

## Kafka 1.0.0+ Connector

Starting with Flink 1.7, there is a new universal Kafka connector that does not track a specific Kafka major version.
Rather, it tracks the latest version of Kafka at the time of the Flink release.

If your Kafka broker version is 1.0.0 or newer, you should use this Kafka connector.
If you use an older version of Kafka (0.11, 0.10, 0.9, or 0.8), you should use the connector corresponding to the broker version.

### Compatibility

The universal Kafka connector is compatible with older and newer Kafka brokers through the compatibility guarantees of the Kafka client API and broker.
It is compatible with broker versions 0.11.0 or newer, depending on the features used.
For details on Kafka compatibility, please refer to the [Kafka documentation](https://kafka.apache.org/protocol.html#protocol_compatibility).

### Migrating Kafka Connector from 0.11 to universal

In order to perform the migration, see the [upgrading jobs and Flink versions guide]({{ site.baseurl }}/ops/upgrading.html)
and:
* Use Flink 1.9 or newer for the whole process.
* Do not upgrade the Flink and operators at the same time.
* Make sure that Kafka Consumer and/or Kafka Producer used in your job have assigned unique identifiers (`uid`):
* Use stop with savepoint feature to take the savepoint (for example by using `stop --withSavepoint`)[CLI command]({{ site.baseurl }}/ops/cli.html).

### Usage

To use the universal Kafka connector add a dependency to it:

{% highlight xml %}
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-connector-kafka{{ site.scala_version_suffix }}</artifactId>
  <version>{{ site.version }}</version>
</dependency>
{% endhighlight %}

Then instantiate the new source (`FlinkKafkaConsumer`) and sink (`FlinkKafkaProducer`).
The API is backward compatible with the Kafka 0.11 connector,
except of dropping specific Kafka version from the module and class names.

## Kafka Consumer

Flink's Kafka consumer is called `FlinkKafkaConsumer08` (or 09 for Kafka 0.9.0.x versions, etc.
or just `FlinkKafkaConsumer` for Kafka >= 1.0.0 versions). It provides access to one or more Kafka topics.

The constructor accepts the following arguments:

1. The topic name / list of topic names
2. A DeserializationSchema / KafkaDeserializationSchema for deserializing the data from Kafka
3. Properties for the Kafka consumer.
  The following properties are required:
  - "bootstrap.servers" (comma separated list of Kafka brokers)
  - "zookeeper.connect" (comma separated list of Zookeeper servers) (**only required for Kafka 0.8**)
  - "group.id" the id of the consumer group

Example:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
Properties properties = new Properties();
properties.setProperty("bootstrap.servers", "localhost:9092");
// only required for Kafka 0.8
properties.setProperty("zookeeper.connect", "localhost:2181");
properties.setProperty("group.id", "test");
DataStream<String> stream = env
	.addSource(new FlinkKafkaConsumer08<>("topic", new SimpleStringSchema(), properties));
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val properties = new Properties()
properties.setProperty("bootstrap.servers", "localhost:9092")
// only required for Kafka 0.8
properties.setProperty("zookeeper.connect", "localhost:2181")
properties.setProperty("group.id", "test")
stream = env
    .addSource(new FlinkKafkaConsumer08[String]("topic", new SimpleStringSchema(), properties))
    .print()
{% endhighlight %}
</div>
</div>

### The `DeserializationSchema`

The Flink Kafka Consumer needs to know how to turn the binary data in Kafka into Java/Scala objects. The
`DeserializationSchema` allows users to specify such a schema. The `T deserialize(byte[] message)`
method gets called for each Kafka message, passing the value from Kafka.

It is usually helpful to start from the `AbstractDeserializationSchema`, which takes care of describing the
produced Java/Scala type to Flink's type system. Users that implement a vanilla `DeserializationSchema` need
to implement the `getProducedType(...)` method themselves.

For accessing the key, value and metadata of the Kafka message, the `KafkaDeserializationSchema` has
the following deserialize method `T deserialize(ConsumerRecord<byte[], byte[]> record)`.

For convenience, Flink provides the following schemas:

1. `TypeInformationSerializationSchema` (and `TypeInformationKeyValueSerializationSchema`) which creates
    a schema based on a Flink's `TypeInformation`. This is useful if the data is both written and read by Flink.
    This schema is a performant Flink-specific alternative to other generic serialization approaches.

2. `JsonDeserializationSchema` (and `JSONKeyValueDeserializationSchema`) which turns the serialized JSON
    into an ObjectNode object, from which fields can be accessed using `objectNode.get("field").as(Int/String/...)()`.
    The KeyValue objectNode contains a "key" and "value" field which contain all fields, as well as
    an optional "metadata" field that exposes the offset/partition/topic for this message.
    
3. `AvroDeserializationSchema` which reads data serialized with Avro format using a statically provided schema. It can
    infer the schema from Avro generated classes (`AvroDeserializationSchema.forSpecific(...)`) or it can work with `GenericRecords`
    with a manually provided schema (with `AvroDeserializationSchema.forGeneric(...)`). This deserialization schema expects that
    the serialized records DO NOT contain embedded schema.

    - There is also a version of this schema available that can lookup the writer's schema (schema which was used to write the record) in
      [Confluent Schema Registry](https://docs.confluent.io/current/schema-registry/docs/index.html). Using these deserialization schema
      record will be read with the schema that was retrieved from Schema Registry and transformed to a statically provided( either through 
      `ConfluentRegistryAvroDeserializationSchema.forGeneric(...)` or `ConfluentRegistryAvroDeserializationSchema.forSpecific(...)`).

    <br>To use this deserialization schema one has to add the following additional dependency:
    
<div class="codetabs" markdown="1">
<div data-lang="AvroDeserializationSchema" markdown="1">
{% highlight xml %}
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-avro</artifactId>
  <version>{{site.version }}</version>
</dependency>
{% endhighlight %}
</div>
<div data-lang="ConfluentRegistryAvroDeserializationSchema" markdown="1">
{% highlight xml %}
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-avro-confluent-registry</artifactId>
  <version>{{site.version }}</version>
</dependency>
{% endhighlight %}
</div>
</div>

When encountering a corrupted message that cannot be deserialized for any reason, there
are two options - either throwing an exception from the `deserialize(...)` method
which will cause the job to fail and be restarted, or returning `null` to allow
the Flink Kafka consumer to silently skip the corrupted message. Note that
due to the consumer's fault tolerance (see below sections for more details),
failing the job on the corrupted message will let the consumer attempt
to deserialize the message again. Therefore, if deserialization still fails, the
consumer will fall into a non-stop restart and fail loop on that corrupted
message.

### Kafka Consumers Start Position Configuration

The Flink Kafka Consumer allows configuring how the start position for Kafka
partitions are determined.

Example:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

FlinkKafkaConsumer08<String> myConsumer = new FlinkKafkaConsumer08<>(...);
myConsumer.setStartFromEarliest();     // start from the earliest record possible
myConsumer.setStartFromLatest();       // start from the latest record
myConsumer.setStartFromTimestamp(...); // start from specified epoch timestamp (milliseconds)
myConsumer.setStartFromGroupOffsets(); // the default behaviour

DataStream<String> stream = env.addSource(myConsumer);
...
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val env = StreamExecutionEnvironment.getExecutionEnvironment()

val myConsumer = new FlinkKafkaConsumer08[String](...)
myConsumer.setStartFromEarliest()      // start from the earliest record possible
myConsumer.setStartFromLatest()        // start from the latest record
myConsumer.setStartFromTimestamp(...)  // start from specified epoch timestamp (milliseconds)
myConsumer.setStartFromGroupOffsets()  // the default behaviour

val stream = env.addSource(myConsumer)
...
{% endhighlight %}
</div>
</div>

All versions of the Flink Kafka Consumer have the above explicit configuration methods for start position.

 * `setStartFromGroupOffsets` (default behaviour): Start reading partitions from
 the consumer group's (`group.id` setting in the consumer properties) committed
 offsets in Kafka brokers (or Zookeeper for Kafka 0.8). If offsets could not be
 found for a partition, the `auto.offset.reset` setting in the properties will be used.
 * `setStartFromEarliest()` / `setStartFromLatest()`: Start from the earliest / latest
 record. Under these modes, committed offsets in Kafka will be ignored and
 not used as starting positions.
 * `setStartFromTimestamp(long)`: Start from the specified timestamp. For each partition, the record
 whose timestamp is larger than or equal to the specified timestamp will be used as the start position.
 If a partition's latest record is earlier than the timestamp, the partition will simply be read
 from the latest record. Under this mode, committed offsets in Kafka will be ignored and not used as
 starting positions.
 
You can also specify the exact offsets the consumer should start from for each partition:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
Map<KafkaTopicPartition, Long> specificStartOffsets = new HashMap<>();
specificStartOffsets.put(new KafkaTopicPartition("myTopic", 0), 23L);
specificStartOffsets.put(new KafkaTopicPartition("myTopic", 1), 31L);
specificStartOffsets.put(new KafkaTopicPartition("myTopic", 2), 43L);

myConsumer.setStartFromSpecificOffsets(specificStartOffsets);
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val specificStartOffsets = new java.util.HashMap[KafkaTopicPartition, java.lang.Long]()
specificStartOffsets.put(new KafkaTopicPartition("myTopic", 0), 23L)
specificStartOffsets.put(new KafkaTopicPartition("myTopic", 1), 31L)
specificStartOffsets.put(new KafkaTopicPartition("myTopic", 2), 43L)

myConsumer.setStartFromSpecificOffsets(specificStartOffsets)
{% endhighlight %}
</div>
</div>

The above example configures the consumer to start from the specified offsets for
partitions 0, 1, and 2 of topic `myTopic`. The offset values should be the
next record that the consumer should read for each partition. Note that
if the consumer needs to read a partition which does not have a specified
offset within the provided offsets map, it will fallback to the default
group offsets behaviour (i.e. `setStartFromGroupOffsets()`) for that
particular partition.

Note that these start position configuration methods do not affect the start position when the job is
automatically restored from a failure or manually restored using a savepoint.
On restore, the start position of each Kafka partition is determined by the
offsets stored in the savepoint or checkpoint
(please see the next section for information about checkpointing to enable
fault tolerance for the consumer).

### Kafka Consumers and Fault Tolerance

With Flink's checkpointing enabled, the Flink Kafka Consumer will consume records from a topic and periodically checkpoint all
its Kafka offsets, together with the state of other operations, in a consistent manner. In case of a job failure, Flink will restore
the streaming program to the state of the latest checkpoint and re-consume the records from Kafka, starting from the offsets that were
stored in the checkpoint.

The interval of drawing checkpoints therefore defines how much the program may have to go back at most, in case of a failure.

To use fault tolerant Kafka Consumers, checkpointing of the topology needs to be enabled at the execution environment:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.enableCheckpointing(5000); // checkpoint every 5000 msecs
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val env = StreamExecutionEnvironment.getExecutionEnvironment()
env.enableCheckpointing(5000) // checkpoint every 5000 msecs
{% endhighlight %}
</div>
</div>

Also note that Flink can only restart the topology if enough processing slots are available to restart the topology.
So if the topology fails due to loss of a TaskManager, there must still be enough slots available afterwards.
Flink on YARN supports automatic restart of lost YARN containers.

If checkpointing is not enabled, the Kafka consumer will periodically commit the offsets to Zookeeper.

### Kafka Consumers Topic and Partition Discovery

#### Partition discovery

The Flink Kafka Consumer supports discovering dynamically created Kafka partitions, and consumes them with
exactly-once guarantees. All partitions discovered after the initial retrieval of partition metadata (i.e., when the
job starts running) will be consumed from the earliest possible offset.

By default, partition discovery is disabled. To enable it, set a non-negative value
for `flink.partition-discovery.interval-millis` in the provided properties config,
representing the discovery interval in milliseconds. 

<span class="label label-danger">Limitation</span> When the consumer is restored from a savepoint from Flink versions
prior to Flink 1.3.x, partition discovery cannot be enabled on the restore run. If enabled, the restore would fail
with an exception. In this case, in order to use partition discovery, please first take a savepoint in Flink 1.3.x and
then restore again from that.

#### Topic discovery

At a higher-level, the Flink Kafka Consumer is also capable of discovering topics, based on pattern matching on the
topic names using regular expressions. See the below for an example:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

Properties properties = new Properties();
properties.setProperty("bootstrap.servers", "localhost:9092");
properties.setProperty("group.id", "test");

FlinkKafkaConsumer011<String> myConsumer = new FlinkKafkaConsumer011<>(
    java.util.regex.Pattern.compile("test-topic-[0-9]"),
    new SimpleStringSchema(),
    properties);

DataStream<String> stream = env.addSource(myConsumer);
...
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val env = StreamExecutionEnvironment.getExecutionEnvironment()

val properties = new Properties()
properties.setProperty("bootstrap.servers", "localhost:9092")
properties.setProperty("group.id", "test")

val myConsumer = new FlinkKafkaConsumer08[String](
  java.util.regex.Pattern.compile("test-topic-[0-9]"),
  new SimpleStringSchema,
  properties)

val stream = env.addSource(myConsumer)
...
{% endhighlight %}
</div>
</div>

In the above example, all topics with names that match the specified regular expression
(starting with `test-topic-` and ending with a single digit) will be subscribed by the consumer
when the job starts running.

To allow the consumer to discover dynamically created topics after the job started running,
set a non-negative value for `flink.partition-discovery.interval-millis`. This allows
the consumer to discover partitions of new topics with names that also match the specified
pattern.

### Kafka Consumers Offset Committing Behaviour Configuration

The Flink Kafka Consumer allows configuring the behaviour of how offsets
are committed back to Kafka brokers (or Zookeeper in 0.8). Note that the
Flink Kafka Consumer does not rely on the committed offsets for fault
tolerance guarantees. The committed offsets are only a means to expose
the consumer's progress for monitoring purposes.

The way to configure offset commit behaviour is different, depending on
whether or not checkpointing is enabled for the job.

 - *Checkpointing disabled:* if checkpointing is disabled, the Flink Kafka
 Consumer relies on the automatic periodic offset committing capability
 of the internally used Kafka clients. Therefore, to disable or enable offset
 committing, simply set the `enable.auto.commit` (or `auto.commit.enable`
 for Kafka 0.8) / `auto.commit.interval.ms` keys to appropriate values
 in the provided `Properties` configuration.
 
 - *Checkpointing enabled:* if checkpointing is enabled, the Flink Kafka
 Consumer will commit the offsets stored in the checkpointed states when
 the checkpoints are completed. This ensures that the committed offsets
 in Kafka brokers is consistent with the offsets in the checkpointed states.
 Users can choose to disable or enable offset committing by calling the
 `setCommitOffsetsOnCheckpoints(boolean)` method on the consumer (by default,
 the behaviour is `true`).
 Note that in this scenario, the automatic periodic offset committing
 settings in `Properties` is completely ignored.

### Kafka Consumers and Timestamp Extraction/Watermark Emission

In many scenarios, the timestamp of a record is embedded (explicitly or implicitly) in the record itself.
In addition, the user may want to emit watermarks either periodically, or in an irregular fashion, e.g. based on
special records in the Kafka stream that contain the current event-time watermark. For these cases, the Flink Kafka
Consumer allows the specification of an `AssignerWithPeriodicWatermarks` or an `AssignerWithPunctuatedWatermarks`.

You can specify your custom timestamp extractor/watermark emitter as described
[here]({{ site.baseurl }}/dev/event_timestamps_watermarks.html), or use one from the
[predefined ones]({{ site.baseurl }}/dev/event_timestamp_extractors.html). After doing so, you
can pass it to your consumer in the following way:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
Properties properties = new Properties();
properties.setProperty("bootstrap.servers", "localhost:9092");
// only required for Kafka 0.8
properties.setProperty("zookeeper.connect", "localhost:2181");
properties.setProperty("group.id", "test");

FlinkKafkaConsumer08<String> myConsumer =
    new FlinkKafkaConsumer08<>("topic", new SimpleStringSchema(), properties);
myConsumer.assignTimestampsAndWatermarks(new CustomWatermarkEmitter());

DataStream<String> stream = env
	.addSource(myConsumer)
	.print();
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val properties = new Properties()
properties.setProperty("bootstrap.servers", "localhost:9092")
// only required for Kafka 0.8
properties.setProperty("zookeeper.connect", "localhost:2181")
properties.setProperty("group.id", "test")

val myConsumer = new FlinkKafkaConsumer08[String]("topic", new SimpleStringSchema(), properties)
myConsumer.assignTimestampsAndWatermarks(new CustomWatermarkEmitter())
stream = env
    .addSource(myConsumer)
    .print()
{% endhighlight %}
</div>
</div>

Internally, an instance of the assigner is executed per Kafka partition.
When such an assigner is specified, for each record read from Kafka, the
`extractTimestamp(T element, long previousElementTimestamp)` is called to assign a timestamp to the record and
the `Watermark getCurrentWatermark()` (for periodic) or the
`Watermark checkAndGetNextWatermark(T lastElement, long extractedTimestamp)` (for punctuated) is called to determine
if a new watermark should be emitted and with which timestamp.

**Note**: If a watermark assigner depends on records read from Kafka to advance its watermarks
(which is commonly the case), all topics and partitions need to have a continuous stream of records.
Otherwise, the watermarks of the whole application cannot advance and all time-based operations,
such as time windows or functions with timers, cannot make progress. A single idle Kafka partition causes this behavior.
A Flink improvement is planned to prevent this from happening 
(see [FLINK-5479: Per-partition watermarks in FlinkKafkaConsumer should consider idle partitions](
https://issues.apache.org/jira/browse/FLINK-5479)).
In the meanwhile, a possible workaround is to send *heartbeat messages* to all consumed partitions that advance the watermarks of idle partitions.

## Kafka Producer

Flink’s Kafka Producer is called `FlinkKafkaProducer011` (or `010` for Kafka 0.10.0.x versions, etc. or just `FlinkKafkaProducer` for Kafka >= 1.0.0 versions).
It allows writing a stream of records to one or more Kafka topics.

Example:

<div class="codetabs" markdown="1">
<div data-lang="java" markdown="1">
{% highlight java %}
DataStream<String> stream = ...;

FlinkKafkaProducer011<String> myProducer = new FlinkKafkaProducer011<String>(
        "localhost:9092",            // broker list
        "my-topic",                  // target topic
        new SimpleStringSchema());   // serialization schema

// versions 0.10+ allow attaching the records' event timestamp when writing them to Kafka;
// this method is not available for earlier Kafka versions
myProducer.setWriteTimestampToKafka(true);

stream.addSink(myProducer);
{% endhighlight %}
</div>
<div data-lang="scala" markdown="1">
{% highlight scala %}
val stream: DataStream[String] = ...

val myProducer = new FlinkKafkaProducer011[String](
        "localhost:9092",         // broker list
        "my-topic",               // target topic
        new SimpleStringSchema)   // serialization schema

// versions 0.10+ allow attaching the records' event timestamp when writing them to Kafka;
// this method is not available for earlier Kafka versions
myProducer.setWriteTimestampToKafka(true)

stream.addSink(myProducer)
{% endhighlight %}
</div>
</div>

The above examples demonstrate the basic usage of creating a Flink Kafka Producer
to write streams to a single Kafka target topic. For more advanced usages, there
are other constructor variants that allow providing the following:

 * *Providing custom properties*:
 The producer allows providing a custom properties configuration for the internal `KafkaProducer`.
 Please refer to the [Apache Kafka documentation](https://kafka.apache.org/documentation.html) for
 details on how to configure Kafka Producers.
 * *Custom partitioner*: To assign records to specific
 partitions, you can provide an implementation of a `FlinkKafkaPartitioner` to the
 constructor. This partitioner will be called for each record in the stream
 to determine which exact partition of the target topic the record should be sent to.
 Please see [Kafka Producer Partitioning Scheme](#kafka-producer-partitioning-scheme) for more details.
 * *Advanced serialization schema*: Similar to the consumer,
 the producer also allows using an advanced serialization schema called `KeyedSerializationSchema`,
 which allows serializing the key and value separately. It also allows to override the target topic,
 so that one producer instance can send data to multiple topics.
 
### Kafka Producer Partitioning Scheme
 
By default, if a custom partitioner is not specified for the Flink Kafka Producer, the producer will use
a `FlinkFixedPartitioner` that maps each Flink Kafka Producer parallel subtask to a single Kafka partition
(i.e., all records received by a sink subtask will end up in the same Kafka partition).

A custom partitioner can be implemented by extending the `FlinkKafkaPartitioner` class. All
Kafka versions' constructors allow providing a custom partitioner when instantiating the producer.
Note that the partitioner implementation must be serializable, as they will be transferred across Flink nodes.
Also, keep in mind that any state in the partitioner will be lost on job failures since the partitioner
is not part of the producer's checkpointed state.

It is also possible to completely avoid using and kind of partitioner, and simply let Kafka partition
the written records by their attached key (as determined for each record using the provided serialization schema).
To do this, provide a `null` custom partitioner when instantiating the producer. It is important
to provide `null` as the custom partitioner; as explained above, if a custom partitioner is not specified
the `FlinkFixedPartitioner` is used instead.

### Kafka Producers and Fault Tolerance

#### Kafka 0.8

Before 0.9 Kafka did not provide any mechanisms to guarantee at-least-once or exactly-once semantics.

#### Kafka 0.9 and 0.10

With Flink's checkpointing enabled, the `FlinkKafkaProducer09` and `FlinkKafkaProducer010`
can provide at-least-once delivery guarantees.

Besides enabling Flink's checkpointing, you should also configure the setter
methods `setLogFailuresOnly(boolean)` and `setFlushOnCheckpoint(boolean)` appropriately.

 * `setLogFailuresOnly(boolean)`: by default, this is set to `false`.
 Enabling this will let the producer only log failures
 instead of catching and rethrowing them. This essentially accounts the record
 to have succeeded, even if it was never written to the target Kafka topic. This
 must be disabled for at-least-once.
 * `setFlushOnCheckpoint(boolean)`: by default, this is set to `true`.
 With this enabled, Flink's checkpoints will wait for any
 on-the-fly records at the time of the checkpoint to be acknowledged by Kafka before
 succeeding the checkpoint. This ensures that all records before the checkpoint have
 been written to Kafka. This must be enabled for at-least-once.
 
In conclusion, the Kafka producer by default has at-least-once guarantees for versions
0.9 and 0.10, with `setLogFailureOnly` set to `false` and `setFlushOnCheckpoint` set
to `true`.

**Note**: By default, the number of retries is set to "0". This means that when `setLogFailuresOnly` is set to `false`,
the producer fails immediately on errors, including leader changes. The value is set to "0" by default to avoid
duplicate messages in the target topic that are caused by retries. For most production environments with frequent broker changes,
we recommend setting the number of retries to a higher value.

**Note**: There is currently no transactional producer for Kafka, so Flink can not guarantee exactly-once delivery
into a Kafka topic.

#### Kafka 0.11 and newer

With Flink's checkpointing enabled, the `FlinkKafkaProducer011` (`FlinkKafkaProducer` for Kafka >= 1.0.0 versions) can provide
exactly-once delivery guarantees.

Besides enabling Flink's checkpointing, you can also choose three different modes of operating
chosen by passing appropriate `semantic` parameter to the `FlinkKafkaProducer011` (`FlinkKafkaProducer` for Kafka >= 1.0.0 versions):

 * `Semantic.NONE`: Flink will not guarantee anything. Produced records can be lost or they can
 be duplicated.
 * `Semantic.AT_LEAST_ONCE` (default setting): similar to `setFlushOnCheckpoint(true)` in
 `FlinkKafkaProducer010`. This guarantees that no records will be lost (although they can be duplicated).
 * `Semantic.EXACTLY_ONCE`: uses Kafka transactions to provide exactly-once semantic. Whenever you write
 to Kafka using transactions, do not forget about setting desired `isolation.level` (`read_committed`
 or `read_uncommitted` - the latter one is the default value) for any application consuming records
 from Kafka.

##### Caveats

`Semantic.EXACTLY_ONCE` mode relies on the ability to commit transactions
that were started before taking a checkpoint, after recovering from the said checkpoint. If the time
between Flink application crash and completed restart is larger than Kafka's transaction timeout
there will be data loss (Kafka will automatically abort transactions that exceeded timeout time).
Having this in mind, please configure your transaction timeout appropriately to your expected down
times.

Kafka brokers by default have `transaction.max.timeout.ms` set to 15 minutes. This property will
not allow to set transaction timeouts for the producers larger than it's value.
`FlinkKafkaProducer011` by default sets the `transaction.timeout.ms` property in producer config to
1 hour, thus `transaction.max.timeout.ms` should be increased before using the
`Semantic.EXACTLY_ONCE` mode.

In `read_committed` mode of `KafkaConsumer`, any transactions that were not finished
(neither aborted nor completed) will block all reads from the given Kafka topic past any
un-finished transaction. In other words after following sequence of events:

1. User started `transaction1` and written some records using it
2. User started `transaction2` and written some further records using it
3. User committed `transaction2`

Even if records from `transaction2` are already committed, they will not be visible to
the consumers until `transaction1` is committed or aborted. This has two implications:

 * First of all, during normal working of Flink applications, user can expect a delay in visibility
 of the records produced into Kafka topics, equal to average time between completed checkpoints.
 * Secondly in case of Flink application failure, topics into which this application was writing,
 will be blocked for the readers until the application restarts or the configured transaction 
 timeout time will pass. This remark only applies for the cases when there are multiple
 agents/applications writing to the same Kafka topic.

**Note**:  `Semantic.EXACTLY_ONCE` mode uses a fixed size pool of KafkaProducers
per each `FlinkKafkaProducer011` instance. One of each of those producers is used per one
checkpoint. If the number of concurrent checkpoints exceeds the pool size, `FlinkKafkaProducer011`
will throw an exception and will fail the whole application. Please configure max pool size and max
number of concurrent checkpoints accordingly.

**Note**: `Semantic.EXACTLY_ONCE` takes all possible measures to not leave any lingering transactions
that would block the consumers from reading from Kafka topic more then it is necessary. However in the
event of failure of Flink application before first checkpoint, after restarting such application there
is no information in the system about previous pool sizes. Thus it is unsafe to scale down Flink
application before first checkpoint completes, by factor larger than `FlinkKafkaProducer011.SAFE_SCALE_DOWN_FACTOR`.

## Using Kafka timestamps and Flink event time in Kafka 0.10

Since Apache Kafka 0.10+, Kafka's messages can carry
[timestamps](https://cwiki.apache.org/confluence/display/KAFKA/KIP-32+-+Add+timestamps+to+Kafka+message), indicating
the time the event has occurred (see ["event time" in Apache Flink](../event_time.html)) or the time when the message
has been written to the Kafka broker.

The `FlinkKafkaConsumer010` will emit records with the timestamp attached, if the time characteristic in Flink is 
set to `TimeCharacteristic.EventTime` (`StreamExecutionEnvironment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)`).

The Kafka consumer does not emit watermarks. To emit watermarks, the same mechanisms as described above in 
"Kafka Consumers and Timestamp Extraction/Watermark Emission"  using the `assignTimestampsAndWatermarks` method are applicable.

There is no need to define a timestamp extractor when using the timestamps from Kafka. The `previousElementTimestamp` argument of 
the `extractTimestamp()` method contains the timestamp carried by the Kafka message.

A timestamp extractor for a Kafka consumer would look like this:
{% highlight java %}
public long extractTimestamp(Long element, long previousElementTimestamp) {
    return previousElementTimestamp;
}
{% endhighlight %}



The `FlinkKafkaProducer010` only emits the record timestamp, if `setWriteTimestampToKafka(true)` is set.

{% highlight java %}
FlinkKafkaProducer010.FlinkKafkaProducer010Configuration config = FlinkKafkaProducer010.writeToKafkaWithTimestamps(streamWithTimestamps, topic, new SimpleStringSchema(), standardProps);
config.setWriteTimestampToKafka(true);
{% endhighlight %}



## Kafka Connector metrics

Flink's Kafka connectors provide some metrics through Flink's [metrics system]({{ site.baseurl }}/monitoring/metrics.html) to analyze
the behavior of the connector.
The producers export Kafka's internal metrics through Flink's metric system for all supported versions. The consumers export 
all metrics starting from Kafka version 0.9. The Kafka documentation lists all exported metrics 
in its [documentation](http://kafka.apache.org/documentation/#selector_monitoring).

In addition to these metrics, all consumers expose the `current-offsets` and `committed-offsets` for each topic partition.
The `current-offsets` refers to the current offset in the partition. This refers to the offset of the last element that
we retrieved and emitted successfully. The `committed-offsets` is the last committed offset.

The Kafka Consumers in Flink commit the offsets back to Zookeeper (Kafka 0.8) or the Kafka brokers (Kafka 0.9+). If checkpointing
is disabled, offsets are committed periodically.
With checkpointing, the commit happens once all operators in the streaming topology have confirmed that they've created a checkpoint of their state. 
This provides users with at-least-once semantics for the offsets committed to Zookeeper or the broker. For offsets checkpointed to Flink, the system 
provides exactly once guarantees.

The offsets committed to ZK or the broker can also be used to track the read progress of the Kafka consumer. The difference between
the committed offset and the most recent offset in each partition is called the *consumer lag*. If the Flink topology is consuming
the data slower from the topic than new data is added, the lag will increase and the consumer will fall behind.
For large production deployments we recommend monitoring that metric to avoid increasing latency.

## Enabling Kerberos Authentication (for versions 0.9+ and above only)

Flink provides first-class support through the Kafka connector to authenticate to a Kafka installation
configured for Kerberos. Simply configure Flink in `flink-conf.yaml` to enable Kerberos authentication for Kafka like so:

1. Configure Kerberos credentials by setting the following -
 - `security.kerberos.login.use-ticket-cache`: By default, this is `true` and Flink will attempt to use Kerberos credentials in ticket caches managed by `kinit`.
 Note that when using the Kafka connector in Flink jobs deployed on YARN, Kerberos authorization using ticket caches will not work.
 This is also the case when deploying using Mesos, as authorization using ticket cache is not supported for Mesos deployments.
 - `security.kerberos.login.keytab` and `security.kerberos.login.principal`: To use Kerberos keytabs instead, set values for both of these properties.
 
2. Append `KafkaClient` to `security.kerberos.login.contexts`: This tells Flink to provide the configured Kerberos credentials to the Kafka login context to be used for Kafka authentication.

Once Kerberos-based Flink security is enabled, you can authenticate to Kafka with either the Flink Kafka Consumer or Producer
by simply including the following two settings in the provided properties configuration that is passed to the internal Kafka client:

- Set `security.protocol` to `SASL_PLAINTEXT` (default `NONE`): The protocol used to communicate to Kafka brokers.
When using standalone Flink deployment, you can also use `SASL_SSL`; please see how to configure the Kafka client for SSL [here](https://kafka.apache.org/documentation/#security_configclients). 
- Set `sasl.kerberos.service.name` to `kafka` (default `kafka`): The value for this should match the `sasl.kerberos.service.name` used for Kafka broker configurations.
A mismatch in service name between client and server configuration will cause the authentication to fail.

For more information on Flink configuration for Kerberos security, please see [here]({{ site.baseurl}}/ops/config.html).
You can also find [here]({{ site.baseurl}}/ops/security-kerberos.html) further details on how Flink internally setups Kerberos-based security.

## Troubleshooting

<div class="alert alert-warning">
If you have a problem with Kafka when using Flink, keep in mind that Flink only wraps
<a href="https://kafka.apache.org/documentation/#consumerapi">KafkaConsumer</a> or
<a href="https://kafka.apache.org/documentation/#producerapi">KafkaProducer</a>
and your problem might be independent of Flink and sometimes can be solved by upgrading Kafka brokers,
reconfiguring Kafka brokers or reconfiguring <tt>KafkaConsumer</tt> or <tt>KafkaProducer</tt> in Flink.
Some examples of common problems are listed below.
</div>

### Data loss

Depending on your Kafka configuration, even after Kafka acknowledges
writes you can still experience data loss. In particular keep in mind about the following properties
in Kafka config:

- `acks`
- `log.flush.interval.messages`
- `log.flush.interval.ms`
- `log.flush.*`

Default values for the above options can easily lead to data loss.
Please refer to the Kafka documentation for more explanation.

### UnknownTopicOrPartitionException

One possible cause of this error is when a new leader election is taking place,
for example after or during restarting a Kafka broker.
This is a retriable exception, so Flink job should be able to restart and resume normal operation.
It also can be circumvented by changing `retries` property in the producer settings.
However this might cause reordering of messages,
which in turn if undesired can be circumvented by setting `max.in.flight.requests.per.connection` to 1.

{% top %}
