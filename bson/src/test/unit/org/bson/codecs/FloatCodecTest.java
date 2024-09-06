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
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class FloatCodecTest extends CodecTestCase {

    @Test
    public void shouldRoundTripFloatValues() {
        roundTrip(new OldDocument("a", Float.MAX_VALUE));
        roundTrip(new OldDocument("a", -Float.MAX_VALUE));
    }

    @Test
    public void shouldRoundTripNegativeFloatValues() {
        roundTrip(new OldDocument("a", -1f));
    }

    @Test
    public void shouldHandleAlternativeNumberValues() {
        OldDocument expected = new OldDocument("a", 10f);
        roundTrip(new OldDocument("a", 10), expected);
        roundTrip(new OldDocument("a", 10L), expected);
        roundTrip(new OldDocument("a", 9.9999999999999992), expected);
        roundTrip(new OldDocument("a", Decimal128.parse("10")), expected);
    }

    @Test
    public void shouldErrorDecodingOutsideMinRange() {
        assertThrows(BsonInvalidOperationException.class, () -> roundTrip(new OldDocument("a", -Double.MAX_VALUE)));
    }

    @Test
    public void shouldErrorDecodingOutsideMaxRange() {
        assertThrows(BsonInvalidOperationException.class, () -> roundTrip(new OldDocument("a", Double.MAX_VALUE)));
    }

    @Test
    public void shouldThrowWhenHandlingLossyDecimal128Values() {
        assertThrows(BsonInvalidOperationException.class, () -> roundTrip(new OldDocument("a", Decimal128.parse("10.0"))));
    }

    @Override
    DocumentCodecProvider getDocumentCodecProvider() {
        return getSpecificNumberDocumentCodecProvider(Float.class);
    }
}
