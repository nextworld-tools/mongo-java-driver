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

package primer;

import org.bson.OldDocument;
import org.junit.jupiter.api.Test;

// @import: start

import static java.util.Arrays.asList;
// @import: end


public class UpdatePrimer extends PrimerTestCase {

    @Test
    public void updateTopLevelFields() {
        // @begin: update-top-level-fields
        // @code: start
        db.getCollection("restaurants").updateOne(new OldDocument("name", "Juni"),
                new OldDocument("$set", new OldDocument("cuisine", "American (New)"))
                    .append("$currentDate", new OldDocument("lastModified", true)));
        // @code: end

        /*
        // @post: start
            The updateOne operation returns a ``UpdateResult`` which contains information about the operation.
            The ``getModifiedCount`` method returns the number of documents modified.
        // @post: end
        */
        // @end: update-top-level-fields
    }

    @Test
    public void updateEmbeddedField() {
        // @begin: update-embedded-field
        // @code: start
        db.getCollection("restaurants").updateOne(new OldDocument("restaurant_id", "41156888"),
                new OldDocument("$set", new OldDocument("address.street", "East 31st Street")));

        // @code: end
        /*
        // @post: start
            The updateOne operation returns a ``UpdateResult`` which contains information about the operation.
            The ``getModifiedCount`` method returns the number of documents modified.
        // @post: end
        */
        // @end: update-embedded-field
    }


    @Test
    public void updateMultipleDocuments() {
        // @begin: update-multiple-documents
        // @code: start
        db.getCollection("restaurants").updateMany(new OldDocument("address.zipcode", "10016").append("cuisine", "Other"),
                new OldDocument("$set", new OldDocument("cuisine", "Category To Be Determined"))
                        .append("$currentDate", new OldDocument("lastModified", true)));
        // @code: end

        /*
        // @post: start
            The updateMany operation returns a ``UpdateResult`` which contains information about the operation.
            The ``getModifiedCount`` method returns the number of documents modified.
        // @post: end
        */
        // @end: update-multiple-documents
    }

    @Test
    public void replaceDocument() {
        // @begin: replace-document
        // @code: start
        db.getCollection("restaurants").replaceOne(new OldDocument("restaurant_id", "41704620"),
                new OldDocument("address",
                        new OldDocument()
                                .append("street", "2 Avenue")
                                .append("zipcode", "10075")
                                .append("building", "1480")
                                .append("coord", asList(-73.9557413, 40.7720266)))
                        .append("name", "Vella 2"));
       // @code: end
       /*
       // @post: start
           The replaceOne operation returns a ``UpdateResult`` which contains information about the operation.
           The ``getModifiedCount`` method returns the number of documents modified.
       // @post: end
       */

       // @end: replace-document
    }
}
