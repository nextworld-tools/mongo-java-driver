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

package com.mongodb.reactivestreams.client;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.OldDocument;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.mongodb.ClusterFixture.isDiscoverableReplicaSet;
import static com.mongodb.reactivestreams.client.MongoFixture.DEFAULT_TIMEOUT_MILLIS;
import static com.mongodb.reactivestreams.client.MongoFixture.PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS;
import static com.mongodb.reactivestreams.client.MongoFixture.run;

public class ChangeStreamPublisherVerification extends PublisherVerification<ChangeStreamDocument<OldDocument>> {

    public static final AtomicInteger COUNTER = new AtomicInteger();

    public ChangeStreamPublisherVerification() {
        super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);
    }

    @Override
    public Publisher<ChangeStreamDocument<OldDocument>> createPublisher(final long elements) {
        assert (elements <= maxElementsFromPublisher());
        if (!isDiscoverableReplicaSet()) {
            notVerified();
        }

        MongoCollection<OldDocument> collection = MongoFixture.getDefaultDatabase()
                .getCollection("ChangeStreamTest" + COUNTER.getAndIncrement());

        if (elements > 0) {
            MongoFixture.ObservableSubscriber<ChangeStreamDocument<OldDocument>> observer =
                    new MongoFixture.ObservableSubscriber<>(() -> run(collection.insertOne(OldDocument.parse("{a: 1}"))));
            collection.watch().first().subscribe(observer);

            ChangeStreamDocument<OldDocument> changeDocument =  observer.get().get(0);

            // Limit the number of elements returned - this is essentially an infinite stream but to high will cause a OOM.
            long maxElements = elements > 10000 ? 10000 : elements;
            List<OldDocument> documentList = LongStream.rangeClosed(1, maxElements).boxed()
                    .map(i -> new OldDocument("a", i)).collect(Collectors.toList());

            run(collection.insertMany(documentList));

            return collection.watch().startAfter(changeDocument.getResumeToken());
        }

        return collection.watch();
    }

    @Override
    public Publisher<ChangeStreamDocument<OldDocument>> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return publisherUnableToSignalOnComplete();
    }
}
