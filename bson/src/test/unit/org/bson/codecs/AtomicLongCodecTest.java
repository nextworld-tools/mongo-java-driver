/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs;

import org.bson.BsonInvalidOperationException;
import org.bson.OldDocument;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class AtomicLongCodecTest extends CodecTestCase {

    @Test
    public void shouldRoundTripAtomicLongValues() {
        OldDocument original = new OldDocument("a", new AtomicLong(Long.MAX_VALUE));
        roundTrip(original, new AtomicLongComparator(original));

        original = new OldDocument("a", new AtomicLong(Long.MIN_VALUE));
        roundTrip(original, new AtomicLongComparator(original));
    }

    @Test
    public void shouldHandleAlternativeNumberValues() {
        OldDocument expected = new OldDocument("a", new AtomicLong(10L));
        roundTrip(new OldDocument("a", 10), new AtomicLongComparator(expected));
        roundTrip(new OldDocument("a", 10L), new AtomicLongComparator(expected));
        roundTrip(new OldDocument("a", 10.00), new AtomicLongComparator(expected));
        roundTrip(new OldDocument("a", 9.9999999999999992), new AtomicLongComparator(expected));
    }

    @Test
    public void shouldThrowWhenHandlingLossyDoubleValues() {
        OldDocument original = new OldDocument("a", 9.9999999999999991);
        assertThrows(BsonInvalidOperationException.class, () -> roundTrip(original, new AtomicLongComparator(original)));
    }

    @Test
    public void shouldErrorDecodingOutsideMinRange() {
        assertThrows(BsonInvalidOperationException.class, () -> roundTrip(new OldDocument("a", -Double.MAX_VALUE)));
    }

    @Test
    public void shouldErrorDecodingOutsideMaxRange() {
        assertThrows(BsonInvalidOperationException.class, () -> roundTrip(new OldDocument("a", Double.MAX_VALUE)));
    }

    @Override
    DocumentCodecProvider getDocumentCodecProvider() {
        return getSpecificNumberDocumentCodecProvider(AtomicLong.class);
    }

    private class AtomicLongComparator implements Comparator<OldDocument> {
        private final OldDocument expected;

        AtomicLongComparator(final OldDocument expected) {
            this.expected = expected;
        }

        @Override
        public void apply(final OldDocument result) {
            assertEquals(expected.get("a", AtomicLong.class).get(), result.get("a", AtomicLong.class).get());
        }
    }

}
