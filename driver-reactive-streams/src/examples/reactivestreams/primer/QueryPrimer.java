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
 *
 */

package reactivestreams.primer;

// @imports: start
import com.mongodb.reactivestreams.client.FindPublisher;
import org.bson.OldDocument;
import org.junit.Test;
import reactivestreams.helpers.SubscriberHelpers.ObservableSubscriber;
import reactivestreams.helpers.SubscriberHelpers.PrintDocumentSubscriber;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Sorts.ascending;
import static java.util.Arrays.asList;
// @imports: end


public class QueryPrimer extends PrimerTestCase {

    @Test
    public void queryAll() {
        // @begin: query-all
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find();
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();
        // @code: end
        // @end: query-all
    }


    @Test
    public void logicalAnd() {

        // @begin: logical-and
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("cuisine", "Italian").append("address.zipcode", "10075"));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();
        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(and(eq("cuisine", "Italian"), eq("address.zipcode", "10075")));
        // @code: end

        // @end: logical-and
    }

    @Test
    public void logicalOr() {

        // @begin: logical-or
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("$or", asList(new OldDocument("cuisine", "Italian"),
                        new OldDocument("address.zipcode", "10075"))));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();
        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(or(eq("cuisine", "Italian"), eq("address.zipcode", "10075")));
        // @code: end

        // @end: logical-or
    }

    @Test
    public void queryTopLevelField() {
        // @begin: query-top-level-field
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("borough", "Manhattan"));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();

        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(eq("borough", "Manhattan"));
        // @code: end
        // @end: query-top-level-field
    }

    @Test
    public void queryEmbeddedDocument() {
        // @begin: query-embedded-document
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("address.zipcode", "10075"));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();

        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(eq("address.zipcode", "10075"));
        // @code: end
        // @end: query-embedded-document
    }

    @Test
    public void queryFieldInArray() {
        // @begin: query-field-in-array
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("grades.grade", "B"));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();

        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(eq("grades.grade", "B"));
        // @code: end
        // @end: query-field-in-array
    }

    @Test
    public void greaterThan() {
        // @begin: greater-than
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("grades.score", new OldDocument("$gt", 30)));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();

        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(gt("grades.score", 30));
        // @code: end
        // @end: greater-than
    }

    @Test
    public void lessThan() {
        // @begin: less-than
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find(
                new OldDocument("grades.score", new OldDocument("$lt", 10)));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();

        // @code: end

        // @pre: To simplify building queries the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find(lt("grades.score", 10));
        // @code: end
        // @end: less-than
    }


    @Test
    public void sort() {
        // @begin: sort
        // @code: start
        FindPublisher<OldDocument> publisher = db.getCollection("restaurants").find()
                .sort(new OldDocument("borough", 1).append("address.zipcode", 1));
        // @code: end

        // @pre: Iterate the results and apply a block to each resulting document
        // @code: start
        ObservableSubscriber<OldDocument> documentSubscriber = new PrintDocumentSubscriber();
        publisher.subscribe(documentSubscriber);
        documentSubscriber.await();

        // @code: end

        // @pre: To simplify sorting fields the Java driver provides static helpers
        // @code: start
        db.getCollection("restaurants").find().sort(ascending("borough", "address.zipcode"));
        // @code: end
        // @end: sort
    }
}
