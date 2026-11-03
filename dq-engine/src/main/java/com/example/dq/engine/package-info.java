/**
 * Public API of the rule engine. Entry point: {@link com.example.dq.engine.ValidationEngine}.
 *
 * <p>Everything is non-null unless annotated {@code @Nullable}. All public types are immutable and
 * thread-safe, except {@link com.example.dq.engine.ResultSink} implementations, which the host provides.
 */
@NullMarked
package com.example.dq.engine;

import org.jspecify.annotations.NullMarked;
