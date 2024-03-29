---
title: "Checkpoints"
nav-parent_id: ops_state
nav-pos: 7
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


* toc
{:toc}

## 概述
Checkpoint 使 Flink 的状态具有良好的容错性，通过 checkpoint 机制，Flink 可以对作业的状态和计算位置进行恢复。

参考 [Checkpointing]({{ site.baseurl }}/zh/dev/stream/state/checkpointing.html) 查看如何在 Flink 程序中开启和配置 checkpoint。

## 保留 Checkpoint

Checkpoint 在默认的情况下仅用于恢复失败的作业，并不保留，当程序取消时 checkpoint 就会被删除。当然，你可以通过配置来保留 checkpoint，这些被保留的 checkpoint 在作业失败或取消时不会被清除。这样，你就可以使用该 checkpoint 来恢复失败的作业。

{% highlight java %}
CheckpointConfig config = env.getCheckpointConfig();
config.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
{% endhighlight %}

`ExternalizedCheckpointCleanup` 配置项定义了当作业取消时，对作业 checkpoint 的操作：
- **`ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION`**：当作业取消时，保留作业的 checkpoint。注意，这种情况下，需要手动清除该作业保留的 checkpoint。
- **`ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION`**：当作业取消时，删除作业的 checkpoint。仅当作业失败时，作业的 checkpoint 才会被保留。

### 目录结构

与 [savepoints](savepoints.html) 相似，checkpoint 由元数据文件、数据文件（与 state backend 相关）组成。可通过配置文件中 "state.checkpoints.dir" 配置项来指定元数据文件和数据文件的存储路径，另外也可以在代码中针对单个作业特别指定该配置项。

#### 通过配置文件全局配置

{% highlight yaml %}
state.checkpoints.dir: hdfs:///checkpoints/
{% endhighlight %}

#### 创建 state backend 对单个作业进行配置

{% highlight java %}
env.setStateBackend(new RocksDBStateBackend("hdfs:///checkpoints-data/"));
{% endhighlight %}

### Checkpoint 与 Savepoint 的区别

Checkpoint 与 [savepoints](savepoints.html) 有一些区别，体现在 checkpoint ：
- 使用 state backend 特定的数据格式，可能以增量方式存储。
- 不支持 Flink 的特定功能，比如扩缩容。

### 从保留的 checkpoint 中恢复状态

与 savepoint 一样，作业可以从 checkpoint 的元数据文件恢复运行（[savepoint恢复指南](../cli.html#restore-a-savepoint)）。注意，如果元数据文件中信息不充分，那么 jobmanager 就需要使用相关的数据文件来恢复作业(参考[目录结构](#directory-structure))。

{% highlight shell %}
$ bin/flink run -s :checkpointMetaDataPath [:runArgs]
{% endhighlight %}

{% top %}
