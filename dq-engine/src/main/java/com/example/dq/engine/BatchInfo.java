package com.example.dq.engine;

/**
 * A batch that has been fully evaluated and emitted.
 *
 * @param index         zero-based batch number
 * @param firstPosition input position of the batch's first record
 * @param size          number of records in the batch
 */
public record BatchInfo(long index, long firstPosition, int size) {
}
