---
title: "State Schema Evolution"
nav-parent_id: streaming_state
nav-pos: 6
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

* ToC
{:toc}

Apache Flink streaming applications are typically designed to run indefinitely or for long periods of time.
As with all long-running services, the applications need to be updated to adapt to changing requirements.
This goes the same for data schemas that the applications work against; they evolve along with the application.

This page provides an overview of how you can evolve your state type's data schema. 
The current restrictions varies across different types and state structures (`ValueState`, `ListState`, etc.).

Note that the information on this page is relevant only if you are using state serializers that are
generated by Flink's own [type serialization framework]({{ site.baseurl }}/dev/types_serialization.html).
That is, when declaring your state, the provided state descriptor is not configured to use a specific `TypeSerializer`
or `TypeInformation`, in which case Flink infers information about the state type:

<div data-lang="java" markdown="1">
{% highlight java %}
ListStateDescriptor<MyPojoType> descriptor =
    new ListStateDescriptor<>(
        "state-name",
        MyPojoType.class);

checkpointedState = getRuntimeContext().getListState(descriptor);
{% endhighlight %}
</div>

Under the hood, whether or not the schema of state can be evolved depends on the serializer used to read / write
persisted state bytes. Simply put, a registered state's schema can only be evolved if its serializer properly
supports it. This is handled transparently by serializers generated by Flink's type serialization framework
(current scope of support is listed [below]({{ site.baseurl }}/dev/stream/state/schema_evolution.html#supported-data-types-for-schema-evolution)).

If you intend to implement a custom `TypeSerializer` for your state type and would like to learn how to implement
the serializer to support state schema evolution, please refer to
[Custom State Serialization]({{ site.baseurl }}/dev/stream/state/custom_serialization.html).
The documentation there also covers necessary internal details about the interplay between state serializers and Flink's
state backends to support state schema evolution.

## Evolving state schema

To evolve the schema of a given state type, you would take the following steps:

 1. Take a savepoint of your Flink streaming job.
 2. Update state types in your application (e.g., modifying your Avro type schema).
 3. Restore the job from the savepoint. When accessing state for the first time, Flink will assess whether or not
 the schema had been changed for the state, and migrate state schema if necessary.

The process of migrating state to adapt to changed schemas happens automatically, and independently for each state.
This process is performed internally by Flink by first checking if the new serializer for the state has different
serialization schema than the previous serializer; if so, the previous serializer is used to read the state to objects,
and written back to bytes again with the new serializer.

Further details about the migration process is out of the scope of this documentation; please refer to
[here]({{ site.baseurl }}/dev/stream/state/custom_serialization.html).

## Supported data types for schema evolution

Currently, schema evolution is supported only for POJO and Avro types. Therefore, if you care about schema evolution for
state, it is currently recommended to always use either Pojo or Avro for state data types.

There are plans to extend the support for more composite types; for more details,
please refer to [FLINK-10896](https://issues.apache.org/jira/browse/FLINK-10896).

### POJO types

Flink supports evolving schema of [POJO types]({{ site.baseurl }}/dev/types_serialization.html#rules-for-pojo-types), 
based on the following set of rules:

 1. Fields can be removed. Once removed, the previous value for the removed field will be dropped in future checkpoints and savepoints.
 2. New fields can be added. The new field will be initialized to the default value for its type, as
    [defined by Java](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html).
 3. Declared fields types cannot change.
 4. Class name of the POJO type cannot change, including the namespace of the class.

Note that the schema of POJO type state can only be evolved when restoring from a previous savepoint with Flink versions
newer than 1.8.0. When restoring with Flink versions older than 1.8.0, the schema cannot be changed.

### Avro types

Flink fully supports evolving schema of Avro type state, as long as the schema change is considered compatible by
[Avro's rules for schema resolution](http://avro.apache.org/docs/current/spec.html#Schema+Resolution).

One limitation is that Avro generated classes used as the state type cannot be relocated or have different
namespaces when the job is restored.

{% warn Attention %} Schema evolution of keys is not supported.

Example: RocksDB state backend relies on binary objects identity, rather than `hashCode` method implementation. Any changes to the keys object structure could lead to non deterministic behaviour.  

{% warn Attention %} **Kryo** cannot be used for schema evolution.  

When Kryo is used, there is no possibility for the framework to verify if any incompatible changes have been made.

{% top %}
