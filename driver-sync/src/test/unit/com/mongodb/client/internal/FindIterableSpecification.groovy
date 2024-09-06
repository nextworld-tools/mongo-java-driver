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

package com.mongodb.client.internal

import com.mongodb.CursorType
import com.mongodb.Function
import com.mongodb.MongoException
import com.mongodb.MongoNamespace
import com.mongodb.ReadConcern
import com.mongodb.client.ClientSession
import com.mongodb.client.model.Collation
import com.mongodb.internal.operation.BatchCursor
import com.mongodb.internal.operation.FindOperation
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.OldDocument
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.DocumentCodec
import org.bson.codecs.DocumentCodecProvider
import org.bson.codecs.ValueCodecProvider
import org.bson.conversions.Bson
import spock.lang.Specification

import java.util.function.Consumer

import static com.mongodb.ClusterFixture.TIMEOUT_SETTINGS
import static com.mongodb.CustomMatchers.isTheSameAs
import static com.mongodb.ReadPreference.secondary
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.bson.codecs.configuration.CodecRegistries.fromProviders
import static spock.util.matcher.HamcrestSupport.expect

class FindIterableSpecification extends Specification {

    def codecRegistry = fromProviders([new ValueCodecProvider(), new DocumentCodecProvider(),
                                       new BsonValueCodecProvider()])
    def readPreference = secondary()
    def readConcern = ReadConcern.MAJORITY
    def namespace = new MongoNamespace('db', 'coll')
    def collation = Collation.builder().locale('en').build()

    def 'should build the expected findOperation'() {
        given:
        def executor = new TestOperationExecutor([null, null, null])
        def findIterable = new FindIterableImpl(null, namespace, OldDocument, OldDocument, codecRegistry, readPreference, readConcern,
                executor, new OldDocument('filter', 1), true, TIMEOUT_SETTINGS)
                .sort(new OldDocument('sort', 1))
                .projection(new OldDocument('projection', 1))
                .batchSize(100)
                .limit(100)
                .skip(10)
                .cursorType(CursorType.NonTailable)
                .noCursorTimeout(false)
                .partial(false)
                .collation(null)
                .comment(new BsonString('my comment'))
                .hintString('a_1')
                .min(new OldDocument('min', 1))
                .max(new OldDocument('max', 1))
                .returnKey(false)
                .showRecordId(false)
                .allowDiskUse(false)

        when: 'default input should be as expected'
        findIterable.iterator()

        def operation = executor.getReadOperation() as FindOperation<OldDocument>
        def readPreference = executor.getReadPreference()

        then:
        expect operation, isTheSameAs(new FindOperation<OldDocument>(namespace, new DocumentCodec())
                .filter(new BsonDocument('filter', new BsonInt32(1)))
                .sort(new BsonDocument('sort', new BsonInt32(1)))
                .projection(new BsonDocument('projection', new BsonInt32(1)))
                .batchSize(100)
                .limit(100)
                .skip(10)
                .cursorType(CursorType.NonTailable)
                .comment(new BsonString('my comment'))
                .hint(new BsonString('a_1'))
                .min(new BsonDocument('min', new BsonInt32(1)))
                .max(new BsonDocument('max', new BsonInt32(1)))
                .returnKey(false)
                .showRecordId(false)
                .allowDiskUse(false)
                .retryReads(true)
        )
        readPreference == secondary()

        when: 'overriding initial options'
        findIterable.filter(new OldDocument('filter', 2))
                .sort(new OldDocument('sort', 2))
                .projection(new OldDocument('projection', 2))
                .maxTime(101, MILLISECONDS)
                .maxAwaitTime(1001, MILLISECONDS)
                .batchSize(99)
                .limit(99)
                .skip(9)
                .cursorType(CursorType.Tailable)
                .noCursorTimeout(true)
                .partial(true)
                .collation(collation)
                .comment('alt comment')
                .hint(new OldDocument('hint', 2))
                .min(new OldDocument('min', 2))
                .max(new OldDocument('max', 2))
                .returnKey(true)
                .showRecordId(true)
                .allowDiskUse(true)
                .iterator()

        operation = executor.getReadOperation() as FindOperation<OldDocument>

        then: 'should use the overrides'
        expect operation, isTheSameAs(
                new FindOperation<OldDocument>(namespace, new DocumentCodec())
                        .filter(new BsonDocument('filter', new BsonInt32(2)))
                        .sort(new BsonDocument('sort', new BsonInt32(2)))
                        .projection(new BsonDocument('projection', new BsonInt32(2)))
                        .batchSize(99)
                        .limit(99)
                        .skip(9)
                        .cursorType(CursorType.Tailable)
                        .noCursorTimeout(true)
                        .partial(true)
                        .collation(collation)
                        .comment(new BsonString('alt comment'))
                        .hint(new BsonDocument('hint', new BsonInt32(2)))
                        .min(new BsonDocument('min', new BsonInt32(2)))
                        .max(new BsonDocument('max', new BsonInt32(2)))
                        .returnKey(true)
                        .showRecordId(true)
                        .allowDiskUse(true)
                        .retryReads(true)
        )

        when: 'passing nulls to nullable methods'
        new FindIterableImpl(null, namespace, OldDocument, OldDocument, codecRegistry, readPreference, readConcern,
                executor, new OldDocument('filter', 1), true, TIMEOUT_SETTINGS)
                .filter(null as Bson)
                .collation(null)
                .projection(null)
                .sort(null as Bson)
                .comment(null as BsonString)
                .hint(null)
                .max(null as Bson)
                .min(null as Bson)
                .iterator()

        operation = executor.getReadOperation() as FindOperation<OldDocument>

        then: 'should set an empty doc for the filter'
        expect operation, isTheSameAs(new FindOperation<OldDocument>(namespace, new DocumentCodec())
                .filter(new BsonDocument()).retryReads(true))
    }

    def 'should use ClientSession'() {
        given:
        def batchCursor = Stub(BatchCursor) {
            _ * hasNext() >> { false }
        }
        def executor = new TestOperationExecutor([batchCursor, batchCursor])
        def findIterable = new FindIterableImpl(clientSession, namespace, OldDocument, OldDocument, codecRegistry, readPreference, readConcern,
                executor, new OldDocument('filter', 1), true, TIMEOUT_SETTINGS)

        when:
        findIterable.first()

        then:
        executor.getClientSession() == clientSession

        when:
        findIterable.iterator()

        then:
        executor.getClientSession() == clientSession

        where:
        clientSession << [null, Stub(ClientSession)]
    }

    def 'should handle mixed types'() {
        given:
        def executor = new TestOperationExecutor([null, null])
        def findIterable = new FindIterableImpl(null, namespace, OldDocument, OldDocument, codecRegistry, readPreference, readConcern,
                executor, new OldDocument('filter', 1), true, TIMEOUT_SETTINGS)

        when:
        findIterable.filter(new OldDocument('filter', 1))
                  .sort(new BsonDocument('sort', new BsonInt32(1)))
                  .iterator()

        def operation = executor.getReadOperation() as FindOperation<OldDocument>

        then:
        expect operation, isTheSameAs(new FindOperation<OldDocument>(namespace, new DocumentCodec())
                .filter(new BsonDocument('filter', new BsonInt32(1)))
                .sort(new BsonDocument('sort', new BsonInt32(1)))
                .cursorType(CursorType.NonTailable)
                .retryReads(true)
        )
    }

    def 'should follow the MongoIterable interface as expected'() {
        given:
        def cannedResults = [new OldDocument('_id', 1), new OldDocument('_id', 2), new OldDocument('_id', 3)]
        def cursor = {
            Stub(BatchCursor) {
                def count = 0
                def results
                def getResult = {
                    count++
                    results = count == 1 ? cannedResults : null
                    results
                }
                next() >> {
                    getResult()
                }
                hasNext() >> {
                    count == 0
                }
            }
        }
        def executor = new TestOperationExecutor([cursor(), cursor(), cursor(), cursor()])
        def mongoIterable = new FindIterableImpl(null, namespace, OldDocument, OldDocument, codecRegistry, readPreference, readConcern,
                executor, new OldDocument(), true, TIMEOUT_SETTINGS)

        when:
        def results = mongoIterable.first()

        then:
        results == cannedResults[0]

        when:
        def count = 0
        mongoIterable.forEach(new Consumer<OldDocument>() {
            @Override
            void accept(OldDocument document) {
                count++
            }
        })

        then:
        count == 3

        when:
        def target = []
        mongoIterable.into(target)

        then:
        target == cannedResults

        when:
        target = []
        mongoIterable.map(new Function<OldDocument, Integer>() {
            @Override
            Integer apply(OldDocument document) {
                document.getInteger('_id')
            }
        }).into(target)

        then:
        target == [1, 2, 3]
    }

    def 'should get and set batchSize as expected'() {
        when:
        def batchSize = 5
        def mongoIterable = new FindIterableImpl(null, namespace, OldDocument, OldDocument, codecRegistry, readPreference,
                readConcern, Stub(OperationExecutor), new OldDocument(), true, TIMEOUT_SETTINGS)

        then:
        mongoIterable.getBatchSize() == null

        when:
        mongoIterable.batchSize(batchSize)

        then:
        mongoIterable.getBatchSize() == batchSize
    }

    // Really testing MongoIterableImpl#forEach, but doing it once here since that class is abstract
    def 'forEach should close cursor when there is an exception during iteration'() {
        given:
        def cursor = Mock(BatchCursor) {
            hasNext() >> {
                throw new MongoException('')
            }
        }
        def executor = new TestOperationExecutor([cursor])
        def mongoIterable = new FindIterableImpl(null, namespace, OldDocument, OldDocument, codecRegistry, readPreference, readConcern,
                executor, new OldDocument(), true, TIMEOUT_SETTINGS)

        when:
        mongoIterable.forEach(new Consumer<OldDocument>() {
            @Override
            void accept(OldDocument document) {
            }
        })

        then:
        thrown(MongoException)
        1 * cursor.close()
    }
}
