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

// @import: start
import com.mongodb.client.result.UpdateResult;
import org.bson.OldDocument;
import org.junit.Test;
import reactivestreams.helpers.SubscriberHelpers.ObservableSubscriber;
import reactivestreams.helpers.SubscriberHelpers.PrintSubscriber;
// @import: end

public class UpdatePrimer extends PrimerTestCase {

    @Test
    public void updateTopLevelFields() {
        // @begin: update-top-level-fields
        ObservableSubscriber<UpdateResult> updateSubscriber = new PrintSubscriber<>("Update complete: %s");
        db.getCollection("restaurants").updateOne(new OldDocument("name", "Juni"),
                new OldDocument("$set", new OldDocument("cuisine", "American (New)"))
                        .append("$currentDate", new OldDocument("lastModified", true)))
                .subscribe(updateSubscriber);
        updateSubscriber.await();

        /*
        // @post: start
            The updateOne operation returns a ``UpdateResult`` which contains information about the operation.
            The ``getModifiedCount`` method returns the number of documents modified
        // @post: end
        */
        // @end: update-top-level-fields
    }

    @Test
    public void updateEmbeddedField() {
        // @begin: update-top-level-fields
        ObservableSubscriber<UpdateResult> updateSubscriber = new PrintSubscriber<>("Update complete: %s");
        db.getCollection("restaurants").updateOne(new OldDocument("restaurant_id", "41156888"),
                new OldDocument("$set", new OldDocument("address.street", "East 31st Street")))
                .subscribe(updateSubscriber);
        updateSubscriber.await();

        /*
        // @post: start
            The updateOne operation returns a ``UpdateResult`` which contains information about the operation.
            The ``getModifiedCount`` method returns the number of documents modified
        // @post: end
        */
        // @end: update-top-level-fields
    }


    @Test
    public void updateMultipleDocuments() {

        // @begin: update-multiple-documents
        ObservableSubscriber<UpdateResult> updateSubscriber = new PrintSubscriber<>("Update complete: %s");
        db.getCollection("restaurants").updateMany(new OldDocument("address.zipcode", "10016").append("cuisine", "Other"),
                new OldDocument("$set", new OldDocument("cuisine", "Category To Be Determined"))
                        .append("$currentDate", new OldDocument("lastModified", true)))
                .subscribe(updateSubscriber);
        updateSubscriber.await();

        /*
        // @post: start
            The updateMany operation returns a ``UpdateResult`` which contains information about the operation.
            The ``getModifiedCount`` method returns the number of documents modified
        // @post: end
        */
        // @end: update-multiple-documents
    }
}
