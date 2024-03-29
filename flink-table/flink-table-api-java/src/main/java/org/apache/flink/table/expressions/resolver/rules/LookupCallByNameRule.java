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

package org.apache.flink.table.expressions.resolver.rules;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.expressions.resolver.LookupCallResolver;
import org.apache.flink.table.functions.FunctionDefinition;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves {@link org.apache.flink.table.expressions.LookupCallExpression} to
 * a corresponding {@link FunctionDefinition}.
 */
@Internal
final class LookupCallByNameRule implements ResolverRule {
	@Override
	public List<Expression> apply(List<Expression> expression, ResolutionContext context) {
		LookupCallResolver lookupCallResolver = new LookupCallResolver(context.functionLookup());
		return expression.stream().map(expr -> expr.accept(lookupCallResolver)).collect(Collectors.toList());
	}
}
