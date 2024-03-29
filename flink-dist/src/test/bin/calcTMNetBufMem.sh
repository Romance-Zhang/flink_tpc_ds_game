#!/usr/bin/env bash
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

# Wrapper script to compare the TM heap size calculation of config.sh with Java
USAGE="Usage: calcTMNetBufMem.sh <memTotal> <netBufFrac> <netBufMin> <netBufMax>"

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

FLINK_TM_HEAP=$1
FLINK_TM_NET_BUF_FRACTION=$2
FLINK_TM_NET_BUF_MIN=$3
FLINK_TM_NET_BUF_MAX=$4

if [[ -z "${FLINK_TM_NET_BUF_MAX}" ]]; then
  echo "$USAGE"
  exit 1
fi

FLINK_CONF_DIR=${bin}/../../main/resources
. ${bin}/../../main/flink-bin/bin/config.sh > /dev/null

FLINK_TM_HEAP_MB=$(getMebiBytes $(parseBytes ${FLINK_TM_HEAP}))

calculateNetworkBufferMemory
