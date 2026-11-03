package com.example.dq.engine;

/** Anything the engine emits per rule and record. Sealed, so a consumer can switch over it exhaustively. */
public sealed interface Outcome permits Result, RuleError, RuleSkipped {

    RecordRef record();
}
