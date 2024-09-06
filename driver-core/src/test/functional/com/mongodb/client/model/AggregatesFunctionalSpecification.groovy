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

package com.mongodb.client.model

import com.mongodb.MongoCommandException
import com.mongodb.MongoNamespace
import com.mongodb.OperationFunctionalSpecification
import com.mongodb.client.model.fill.FillOutputField
import org.bson.BsonDecimal128
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.OldDocument
import org.bson.conversions.Bson
import org.bson.types.Decimal128
import spock.lang.IgnoreIf

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

import static com.mongodb.ClusterFixture.serverVersionLessThan
import static com.mongodb.client.model.Accumulators.accumulator
import static com.mongodb.client.model.Accumulators.addToSet
import static com.mongodb.client.model.Accumulators.avg
import static com.mongodb.client.model.Accumulators.bottom
import static com.mongodb.client.model.Accumulators.bottomN
import static com.mongodb.client.model.Accumulators.first
import static com.mongodb.client.model.Accumulators.firstN
import static com.mongodb.client.model.Accumulators.last
import static com.mongodb.client.model.Accumulators.lastN
import static com.mongodb.client.model.Accumulators.max
import static com.mongodb.client.model.Accumulators.maxN
import static com.mongodb.client.model.Accumulators.mergeObjects
import static com.mongodb.client.model.Accumulators.min
import static com.mongodb.client.model.Accumulators.minN
import static com.mongodb.client.model.Accumulators.push
import static com.mongodb.client.model.Accumulators.stdDevPop
import static com.mongodb.client.model.Accumulators.stdDevSamp
import static com.mongodb.client.model.Accumulators.sum
import static com.mongodb.client.model.Accumulators.top
import static com.mongodb.client.model.Accumulators.topN
import static com.mongodb.client.model.Aggregates.addFields
import static com.mongodb.client.model.Aggregates.bucket
import static com.mongodb.client.model.Aggregates.bucketAuto
import static com.mongodb.client.model.Aggregates.count
import static com.mongodb.client.model.Aggregates.densify
import static com.mongodb.client.model.Aggregates.facet
import static com.mongodb.client.model.Aggregates.fill
import static com.mongodb.client.model.Aggregates.graphLookup
import static com.mongodb.client.model.Aggregates.group
import static com.mongodb.client.model.Aggregates.limit
import static com.mongodb.client.model.Aggregates.lookup
import static com.mongodb.client.model.Aggregates.match
import static com.mongodb.client.model.Aggregates.merge
import static com.mongodb.client.model.Aggregates.out
import static com.mongodb.client.model.Aggregates.project
import static com.mongodb.client.model.Aggregates.replaceRoot
import static com.mongodb.client.model.Aggregates.replaceWith
import static com.mongodb.client.model.Aggregates.sample
import static com.mongodb.client.model.Aggregates.set
import static com.mongodb.client.model.Aggregates.setWindowFields
import static com.mongodb.client.model.Aggregates.skip
import static com.mongodb.client.model.Aggregates.sort
import static com.mongodb.client.model.Aggregates.sortByCount
import static com.mongodb.client.model.Aggregates.unionWith
import static com.mongodb.client.model.Aggregates.unwind
import static com.mongodb.client.model.Filters.eq
import static com.mongodb.client.model.Filters.expr
import static com.mongodb.client.model.Projections.computed
import static com.mongodb.client.model.Projections.exclude
import static com.mongodb.client.model.Projections.excludeId
import static com.mongodb.client.model.Projections.fields
import static com.mongodb.client.model.Projections.include
import static com.mongodb.client.model.Sorts.ascending
import static com.mongodb.client.model.Sorts.descending
import static com.mongodb.client.model.Windows.Bound.CURRENT
import static com.mongodb.client.model.Windows.Bound.UNBOUNDED
import static com.mongodb.client.model.Windows.documents
import static com.mongodb.client.model.Windows.range
import static com.mongodb.client.model.Windows.timeRange
import static com.mongodb.client.model.densify.DensifyOptions.densifyOptions
import static com.mongodb.client.model.densify.DensifyRange.fullRangeWithStep
import static com.mongodb.client.model.densify.DensifyRange.partitionRangeWithStep
import static com.mongodb.client.model.densify.DensifyRange.rangeWithStep
import static com.mongodb.client.model.fill.FillOptions.fillOptions
import static java.util.Arrays.asList
import static java.util.stream.Collectors.toList
import static org.spockframework.util.CollectionUtil.containsAny

class AggregatesFunctionalSpecification extends OperationFunctionalSpecification {

    def a = new OldDocument('_id', 1).append('x', 1)
                                  .append('y', 'a')
                                  .append('z', false)
                                  .append('a', [1, 2, 3])
                                  .append('a1', [new OldDocument('c', 1).append('d', 2), new OldDocument('c', 2).append('d', 3)])
                                  .append('o', new OldDocument('a', 1))

    def b = new OldDocument('_id', 2).append('x', 2)
                                  .append('y', 'b')
                                  .append('z', true)
                                  .append('a', [3, 4, 5, 6])
                                  .append('a1', [new OldDocument('c', 2).append('d', 3), new OldDocument('c', 3).append('d', 4)])
                                  .append('o', new OldDocument('b', 2))

    def c = new OldDocument('_id', 3).append('x', 3)
                                  .append('y', 'c')
                                  .append('z', true)
                                  .append('o', new OldDocument('c', 3))

    def setup() {
        getCollectionHelper().insertDocuments(a, b, c)
    }


    def aggregate(List<Bson> pipeline) {
        getCollectionHelper().aggregate(pipeline)
    }

    def '$match'() {
        expect:
        aggregate([match(Filters.exists('a1'))]) == [a, b]
    }

    def '$project'() {
        expect:
        aggregate([project(fields(include('x'), computed('c', '$y')))]) == [new OldDocument('_id', 1).append('x', 1).append('c', 'a'),
                                                                            new OldDocument('_id', 2).append('x', 2).append('c', 'b'),
                                                                            new OldDocument('_id', 3).append('x', 3).append('c', 'c')]
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$project an exclusion'() {
        expect:
        aggregate([project(exclude('a', 'a1', 'z', 'o'))]) == [new OldDocument('_id', 1).append('x', 1).append('y', 'a'),
                                                               new OldDocument('_id', 2).append('x', 2).append('y', 'b'),
                                                               new OldDocument('_id', 3).append('x', 3).append('y', 'c')]
    }

    def '$sort'() {
        expect:
        aggregate([sort(descending('x'))]) == [c, b, a]
    }

    def '$skip'() {
        expect:
        aggregate([skip(1)]) == [b, c]
    }

    def '$limit'() {
        expect:
        aggregate([limit(2)]) == [a, b]
    }

    def '$unwind'() {
        expect:
        aggregate([project(fields(include('a'), excludeId())), unwind('$a')]) == [new OldDocument('a', 1),
                                                                                  new OldDocument('a', 2),
                                                                                  new OldDocument('a', 3),
                                                                                  new OldDocument('a', 3),
                                                                                  new OldDocument('a', 4),
                                                                                  new OldDocument('a', 5),
                                                                                  new OldDocument('a', 6)]
    }

    @IgnoreIf({ serverVersionLessThan(3, 2) })
    def '$unwind with UnwindOptions'() {
        given:
        getCollectionHelper().drop()
        getCollectionHelper().insertDocuments(new OldDocument('a', [1]), new OldDocument('a', null), new OldDocument('a', []))

        when:
        def results = aggregate([project(fields(include('a'), excludeId())), unwind('$a', options)])

        then:
        results == expectedResults

        where:
        options                                                  | expectedResults
        new UnwindOptions()                                      | [OldDocument.parse('{a: 1}')]
        new UnwindOptions().preserveNullAndEmptyArrays(true)     | [OldDocument.parse('{a: 1}'), OldDocument.parse('{a: null}'),
                                                                    OldDocument.parse('{}')]
        new UnwindOptions()
                .preserveNullAndEmptyArrays(true)
                .includeArrayIndex('b')                          | [OldDocument.parse('{a: 1, b: 0}'), OldDocument.parse('{a: null, b: null}'),
                                                                    OldDocument.parse('{b: null}')]
    }

    def '$group'() {
        expect:
        aggregate([group(null)]) == [new OldDocument('_id', null)]

        aggregate([group('$z')]).containsAll([new OldDocument('_id', true), new OldDocument('_id', false)])

        aggregate([group(null, sum('acc', '$x'))]) == [new OldDocument('_id', null).append('acc', 6)]

        aggregate([group(null, avg('acc', '$x'))]) == [new OldDocument('_id', null).append('acc', 2)]

        aggregate([group(null, first('acc', '$x'))]) == [new OldDocument('_id', null).append('acc', 1)]

        aggregate([group(null, last('acc', '$x'))]) == [new OldDocument('_id', null).append('acc', 3)]

        aggregate([group(null, max('acc', '$x'))]) == [new OldDocument('_id', null).append('acc', 3)]

        aggregate([group(null, min('acc', '$x'))]) == [new OldDocument('_id', null).append('acc', 1)]

        aggregate([group('$z', push('acc', '$z'))]).containsAll([new OldDocument('_id', true).append('acc', [true, true]),
                                                                 new OldDocument('_id', false).append('acc', [false])])

        aggregate([group('$z', addToSet('acc', '$z'))]).containsAll([new OldDocument('_id', true).append('acc', [true]),
                                                                     new OldDocument('_id', false).append('acc', [false])])
    }

    @IgnoreIf({ serverVersionLessThan(3, 6) })
    def '$group with $mergeObjects'() {
        aggregate([group(null, mergeObjects('acc', '$o'))]).containsAll(
                [new OldDocument('_id', null).append('acc', new OldDocument('a', 1).append('b', 2).append('c', 3))])
    }

    @IgnoreIf({ serverVersionLessThan(5, 2) })
    def '$group with top or bottom n'() {
        when:
        List<OldDocument> results = aggregate([group(new OldDocument('gid', '$z'),
                minN('res', '$y',
                        new OldDocument('$cond', new OldDocument('if', new OldDocument('$eq', asList('$gid', true)))
                                .append('then', 2).append('else', 1))))])
                .collect()
        then:
        ((OldDocument) results.stream().find { it.get('_id') == new OldDocument('gid', true) })
                .get('res', List).toSet() == ['b', 'c'].toSet()

        when:
        results = aggregate([group(null,
                maxN('res', '$y', 1))])
        then:
        results.first().get('res', List).toSet() == ['c'].toSet()

        when:
        results = aggregate([
                sort(ascending('x')),
                group(null,
                        firstN('res', '$y', 2))])
        then:
        results.first().get('res', List) == ['a', 'b']

        when:
        results = aggregate([
                sort(ascending('x')),
                group(null,
                        lastN('res', '$y', 1))])
        then:
        results.first().get('res', List) == ['c']

        when:
        results = aggregate([group(new OldDocument('gid', '$z'),
                bottom('res', descending('y'), ['$x', '$y']))])
                .collect()
        then:
        ((OldDocument) results.stream().find { it.get('_id') == new OldDocument('gid', true) })
                .get('res', List) == [2, 'b']

        when:
        results = aggregate([group(new OldDocument('gid', '$z'),
                bottomN('res', descending('y'), ['$x', '$y'],
                        new OldDocument('$cond', new OldDocument('if', new OldDocument('$eq', asList('$gid', true)))
                                .append('then', 2).append('else', 1))))])
                .collect()
        then:
        ((OldDocument) results.stream().find { it.get('_id') == new OldDocument('gid', true) })
                .get('res', List) == [[3, 'c'], [2, 'b']]

        when:
        results = aggregate([group(null,
                top('res', ascending('x'), '$y'))])
        then:
        results.first().get('res') == 'a'

        when:
        results = aggregate([group(null,
                topN('res', descending('x'), '$y', 1))])
        then:
        results.first().get('res', List) == ['c']
    }

    def '$out'() {
        given:
        def outCollectionName = getCollectionName() + '.out'

        when:
        aggregate([out(outCollectionName)])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]
    }

    @IgnoreIf({ serverVersionLessThan(4, 4) })
    def '$out to specified database'() {
        given:
        def outDatabaseName = getDatabaseName() + '_out'
        def outCollectionName = getCollectionName() + '.out'
        getCollectionHelper(new MongoNamespace(outDatabaseName, outCollectionName)).create()

        when:
        aggregate([out(outDatabaseName, outCollectionName)])

        then:
        getCollectionHelper(new MongoNamespace(outDatabaseName, outCollectionName)).find() == [a, b, c]
    }

    @IgnoreIf({ serverVersionLessThan(4, 2) })
    def '$merge'() {
        given:
        def outCollectionName = getCollectionName() + '.out'
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName))
                .createUniqueIndex(new OldDocument('x', 1))
        getCollectionHelper(new MongoNamespace('db1', outCollectionName)).create()

        when:
        aggregate([merge(outCollectionName)])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(new MongoNamespace('db1', outCollectionName))])

        then:
        getCollectionHelper(new MongoNamespace('db1', outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.REPLACE)
                .whenNotMatched(MergeOptions.WhenNotMatched.FAIL))])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.KEEP_EXISTING))])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.MERGE))])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.FAIL))])

        then:
        thrown(MongoCommandException)

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.REPLACE)
                .whenNotMatched(MergeOptions.WhenNotMatched.DISCARD))])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.REPLACE)
                .whenNotMatched(MergeOptions.WhenNotMatched.INSERT))])

        then:
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]

        when:
        aggregate([merge(outCollectionName, new MergeOptions()
                .uniqueIdentifier('x')
                .whenMatched(MergeOptions.WhenMatched.PIPELINE)
                .variables([new Variable<Integer>('b', 1)])
                .whenMatchedPipeline([addFields([new Field<String>('b', '$$b')])])
                .whenNotMatched(MergeOptions.WhenNotMatched.FAIL))])

        then:
        a.append('b', 1)
        b.append('b', 1)
        c.append('b', 1)
        getCollectionHelper(new MongoNamespace(getDatabaseName(), outCollectionName)).find() == [a, b, c]
    }

    @IgnoreIf({ serverVersionLessThan(3, 2) })
    def '$stdDev'() {
        when:
        def results = aggregate([group(null, stdDevPop('stdDevPop', '$x'), stdDevSamp('stdDevSamp', '$x'))]).first()

        then:
        results.keySet().containsAll(['_id', 'stdDevPop', 'stdDevSamp'])
        results.get('_id') == null
        results.getDouble('stdDevPop').round(10) == new Double(Math.sqrt(2 / 3)).round(10)
        results.get('stdDevSamp') == 1.0
    }

    @IgnoreIf({ serverVersionLessThan(3, 2) })
    def '$sample'() {
        expect:
        containsAny([a, b, c], aggregate([sample(1)]).first())
    }


    @IgnoreIf({ serverVersionLessThan(3, 2) })
    def '$lookup'() {
        given:
        def fromCollectionName = 'lookupCollection'
        def fromHelper = getCollectionHelper(new MongoNamespace(getDatabaseName(), fromCollectionName))

        getCollectionHelper().drop()
        fromHelper.drop()

        getCollectionHelper().insertDocuments(new OldDocument('_id', 0).append('a', 1),
                new OldDocument('_id', 1).append('a', null), new OldDocument('_id', 2))
        fromHelper.insertDocuments(new OldDocument('_id', 0).append('b', 1), new OldDocument('_id', 1).append('b', null), new OldDocument('_id', 2))
        def lookupDoc = lookup(fromCollectionName, 'a', 'b', 'same')

        when:
        def results = aggregate([lookupDoc])

        then:
        results == [
            OldDocument.parse('{_id: 0, a: 1, "same": [{_id: 0, b: 1}]}'),
            OldDocument.parse('{_id: 1, a: null, "same": [{_id: 1, b: null}, {_id: 2}]}'),
            OldDocument.parse('{_id: 2, "same": [{_id: 1, b: null}, {_id: 2}]}')
        ]

        cleanup:
        fromHelper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 6) })
    def '$lookup with pipeline'() {
        given:
        def fromCollectionName = 'warehouses'
        def fromHelper = getCollectionHelper(new MongoNamespace(getDatabaseName(), fromCollectionName))
        def collection = getCollectionHelper()

        collection.drop()
        fromHelper.drop()

        fromHelper.insertDocuments(
                OldDocument.parse('{ "_id" : 1, "stock_item" : "abc", warehouse: "A", "instock" : 120 }'),
                OldDocument.parse('{ "_id" : 2, "stock_item" : "abc", warehouse: "B", "instock" : 60 }'),
                OldDocument.parse('{ "_id" : 3, "stock_item" : "xyz", warehouse: "B", "instock" : 40 }'),
                OldDocument.parse('{ "_id" : 4, "stock_item" : "xyz", warehouse: "A", "instock" : 80 }'))

        collection.insertDocuments(
                OldDocument.parse('{ "_id" : 1, "item" : "abc", "price" : 12, "ordered" : 2 }'),
                OldDocument.parse('{ "_id" : 2, "item" : "xyz", "price" : 10, "ordered" : 60 }')
        )

        def let = asList(new Variable('order_item', '$item'), new Variable('order_qty', '$ordered'))

        def  pipeline = asList(
                match(expr(new OldDocument('$and',
                        asList( new OldDocument('$eq', asList('$stock_item', '$$order_item')),
                                new OldDocument('$gte', asList('$instock', '$$order_qty')))))),
                project(fields(exclude('stock_item'), excludeId())))

        def lookupDoc = lookup(fromCollectionName, let, pipeline, 'stockdata')

        when:
        def results = aggregate([lookupDoc])

        then:
        results == [
            OldDocument.parse('{ "_id" : 1.0, "item" : "abc", "price" : 12.0, "ordered" : 2.0, ' +
                        '"stockdata" : [ { "warehouse" : "A", "instock" : 120.0 }, { "warehouse" : "B", "instock" : 60.0 } ] }'),
            OldDocument.parse('{ "_id" : 2.0, "item" : "xyz", "price" : 10.0, "ordered" : 60.0, ' +
                        '"stockdata" : [ { "warehouse" : "A", "instock" : 80.0 } ] }') ]

        cleanup:
        fromHelper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 6) })
    def '$lookup with pipeline without variables'() {
        given:
        def fromCollectionName = 'holidays'
        def fromCollection = getCollectionHelper(new MongoNamespace(getDatabaseName(), fromCollectionName))
        def collection = getCollectionHelper()

        collection.drop()
        fromCollection.drop()

        fromCollection.insertDocuments(
                OldDocument.parse('{ "_id" : 1, year: 2018, name: "New Years", date: { $date : "2018-01-01T00:00:00Z"} }'),
                OldDocument.parse('{ "_id" : 2, year: 2018, name: "Pi Day", date: { $date : "2018-03-14T00:00:00Z" } }'),
                OldDocument.parse('{ "_id" : 3, year: 2018, name: "Ice Cream Day", date: { $date : "2018-07-15T00:00:00Z"} }'),
                OldDocument.parse('{ "_id" : 4, year: 2017, name: "New Years", date: { $date : "2017-01-01T00:00:00Z" } }'),
                OldDocument.parse('{ "_id" : 5, year: 2017, name: "Ice Cream Day", date: { $date : "2017-07-16T00:00:00Z" } }')
        )

        collection.insertDocuments(
                OldDocument.parse('''{ "_id" : 1, "student" : "Ann Aardvark",
                            sickdays: [ { $date : "2018-05-01T00:00:00Z" }, { $date : "2018-08-23T00:00:00Z" } ] }'''),
                OldDocument.parse('''{ "_id" : 2, "student" : "Zoe Zebra",
                            sickdays: [ { $date : "2018-02-01T00:00:00Z" }, { $date : "2018-05-23T00:00:00Z" } ] }''')
        )

        def  pipeline = asList(
                match(eq('year', 2018)),
                project(fields(excludeId(), computed('date', fields(computed('name', '$name'), computed('date', '$date'))))),
                replaceRoot('$date')
        )

        def lookupDoc = lookup(fromCollectionName, pipeline, 'holidays')

        when:
        def results = aggregate([lookupDoc])

        then:
        results == [
            OldDocument.parse(
                        '''{ '_id' : 1, 'student' : "Ann Aardvark",
                        'sickdays' : [ ISODate("2018-05-01T00:00:00Z"), ISODate("2018-08-23T00:00:00Z") ],
                        'holidays' : [  { 'name' : "New Years", 'date' : ISODate ("2018-01-01T00:00:00Z") },
                                        { 'name' : "Pi Day", 'date' : ISODate("2018-03-14T00:00:00Z") },
                                        { 'name' : "Ice Cream Day", 'date' : ISODate("2018-07-15T00:00:00Z") } ] }'''),
            OldDocument.parse(
                        '''{ '_id' : 2, 'student' : "Zoe Zebra",
                        'sickdays' : [ ISODate("2018-02-01T00:00:00Z"), ISODate("2018-05-23T00:00:00Z") ],
                        'holidays' : [  { 'name' : "New Years", 'date' : ISODate("2018-01-01T00:00:00Z") },
                                        { 'name' : "Pi Day", 'date' : ISODate("2018-03-14T00:00:00Z") },
                                        { 'name' : "Ice Cream Day", 'date' : ISODate("2018-07-15T00:00:00Z") } ] }''') ]

        cleanup:
        fromCollection?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$facet'() {
        given:
        def helper = getCollectionHelper()

        helper.drop()

        (0..50).each {
            def size = (35 + it)
            def manufacturer = ['Sony', 'Samsung', 'Vizio'][it % 3]
            helper.insertDocuments(OldDocument.parse("""    {
                  title: "${manufacturer} ${size} inch HDTV",
                  attributes: {
                    "type": "HD",
                    "screen_size": ${size},
                    "manufacturer": "${manufacturer}",
                  }
                }"""))
        }
        def stage = facet(
                new Facet('Manufacturer',
                        sortByCount('$attributes.manufacturer'),
                        limit(5)),
                new Facet('Screen Sizes',
                          unwind('$attributes'),
                          bucketAuto('$attributes.screen_size', 5, new BucketAutoOptions()
                                  .output(sum('count', 1)))))

        when:
        def results = aggregate([stage,
                                 unwind('$Manufacturer'),
                                 sort(ascending('Manufacturer')),
                                 group('_id', push('Manufacturer', '$Manufacturer'),
                                         first('Screen Sizes', '$Screen Sizes')),
                                 project(excludeId())])

        then:
        results == [
            OldDocument.parse(
                    '''{ 'Manufacturer': [
                        {'_id': "Samsung", 'count': 17},
                        {'_id': "Sony", 'count': 17},
                        {'_id': "Vizio", 'count': 17}
                    ], 'Screen Sizes': [
                        {'_id': {'min': 35, 'max': 45}, 'count': 10},
                        {'_id': {'min': 45, 'max': 55}, 'count': 10},
                        {'_id': {'min': 55, 'max': 65}, 'count': 10},
                        {'_id': {'min': 65, 'max': 75}, 'count': 10},
                        {'_id': {'min': 75, 'max': 85}, 'count': 11}
                    ]}
                    '''),
        ]

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$graphLookup'() {
        given:
        def fromCollectionName = 'contacts'
        def fromHelper = getCollectionHelper(new MongoNamespace(getDatabaseName(), fromCollectionName))

        fromHelper.drop()

        fromHelper.insertDocuments(OldDocument.parse('{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 1, name: "Anna Jones", friends: ["Bob Smith", "Chris Green", "Joe Lee"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 2, name: "Chris Green", friends: ["Anna Jones", "Bob Smith"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 3, name: "Joe Lee", friends: ["Anna Jones", "Fred Brown"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 4, name: "Fred Brown", friends: ["Joe Lee"] }'))

        def lookupDoc = graphLookup('contacts', new BsonString('$friends'), 'friends', 'name', 'socialNetwork')

        when:
        def results = fromHelper.aggregate([lookupDoc,
                                            unwind('$socialNetwork'),
                                            sort(new OldDocument('_id', 1).append('socialNetwork._id', 1))])

        then:
        results.subList(0, 5) == [
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"] } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 1, name: "Anna Jones", friends: ["Bob Smith", "Chris Green", "Joe Lee"] } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 2, name: "Chris Green", friends: ["Anna Jones", "Bob Smith"] } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 3, name: "Joe Lee", friends: ["Anna Jones", "Fred Brown" ] } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 4, name: "Fred Brown", friends: ["Joe Lee"] } }''')
        ]

        cleanup:
        fromHelper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$graphLookup with depth options'() {
        given:
        def fromCollectionName = 'contacts'
        def fromHelper = getCollectionHelper(new MongoNamespace(getDatabaseName(), fromCollectionName))

        fromHelper.drop()

        fromHelper.insertDocuments(OldDocument.parse('{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 1, name: "Anna Jones", friends: ["Bob Smith", "Chris Green", "Joe Lee"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 2, name: "Chris Green", friends: ["Anna Jones", "Bob Smith"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 3, name: "Joe Lee", friends: ["Anna Jones", "Fred Brown"] }'))
        fromHelper.insertDocuments(OldDocument.parse('{ _id: 4, name: "Fred Brown", friends: ["Joe Lee"] }'))

        def lookupDoc = graphLookup('contacts', new BsonString('$friends'), 'friends', 'name', 'socialNetwork',
                                    new GraphLookupOptions()
                                        .maxDepth(1)
                                        .depthField('depth'))

        when:
        def results = fromHelper.aggregate([lookupDoc,
                                            unwind('$socialNetwork'),
                                            sort(new OldDocument('_id', 1)
                                                         .append('socialNetwork._id', 1))])

        then:
        results.subList(0, 4) == [
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], depth:1 } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 1, name: "Anna Jones", friends: ["Bob Smith", "Chris Green", "Joe Lee"], depth:0 } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 2, name: "Chris Green", friends: ["Anna Jones", "Bob Smith"], depth:0 } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"], socialNetwork: {
                    _id: 3, name: "Joe Lee", friends: ["Anna Jones", "Fred Brown" ], depth:1 } }''')
        ]

        cleanup:
        fromHelper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$graphLookup with query filter option'() {
        given:
        def fromCollectionName = 'contacts'
        def fromHelper = getCollectionHelper(new MongoNamespace(getDatabaseName(), fromCollectionName))

        fromHelper.drop()

        fromHelper.insertDocuments(
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"],
                hobbies : ["tennis", "unicycling", "golf"] }'''),
            OldDocument.parse('''{ _id: 1, name: "Anna Jones", friends: ["Bob Smith", "Chris Green", "Joe Lee"],
                 hobbies : ["archery", "golf", "woodworking"] }'''),
            OldDocument.parse('''{ _id: 2, name: "Chris Green", friends: ["Anna Jones", "Bob Smith"],
                hobbies : ["knitting", "frisbee"] }'''),
            OldDocument.parse('''{ _id: 3, name: "Joe Lee", friends: ["Anna Jones", "Fred Brown"],
                hobbies : [ "tennis", "golf", "topiary" ] }'''),
            OldDocument.parse('''{ _id: 4, name: "Fred Brown", friends: ["Joe Lee"],
                hobbies : [ "travel", "ceramics", "golf" ] }'''))


        def lookupDoc = graphLookup('contacts', new BsonString('$friends'), 'friends', 'name', 'golfers',
                new GraphLookupOptions()
                        .restrictSearchWithMatch(eq('hobbies', 'golf')))

        when:
        def results = fromHelper.aggregate([lookupDoc,
                                            unwind('$golfers'),
                                            sort(new OldDocument('_id', 1)
                                                    .append('golfers._id', 1))])

        then:
        results.subList(0, 4) == [
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"],
                        hobbies : ["tennis", "unicycling", "golf"], golfers: {_id: 0, name: "Bob Smith",
                        friends: ["Anna Jones", "Chris Green"], hobbies : ["tennis", "unicycling", "golf"] } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"],
                       hobbies: ["tennis", "unicycling", "golf"], golfers:{ _id: 1, name: "Anna Jones",
                       friends: ["Bob Smith", "Chris Green", "Joe Lee"], hobbies : ["archery", "golf", "woodworking"] } } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"],
                       hobbies: ["tennis", "unicycling", "golf"], golfers: { _id: 3, name: "Joe Lee",
                       friends: ["Anna Jones", "Fred Brown"], hobbies : [ "tennis", "golf", "topiary" ] } }'''),
            OldDocument.parse('''{ _id: 0, name: "Bob Smith", friends: ["Anna Jones", "Chris Green"],
                       hobbies: ["tennis", "unicycling", "golf"], golfers:{ _id: 4, name: "Fred Brown", friends: ["Joe Lee"],
                       hobbies : [ "travel", "ceramics", "golf" ] } }''')
        ]

        cleanup:
        fromHelper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$bucket'() {
        given:
        def helper = getCollectionHelper()

        helper.drop()

        helper.insertDocuments(OldDocument.parse('{screenSize: 30}'))
        helper.insertDocuments(OldDocument.parse('{screenSize: 24}'))
        helper.insertDocuments(OldDocument.parse('{screenSize: 42}'))
        helper.insertDocuments(OldDocument.parse('{screenSize: 22}'))
        helper.insertDocuments(OldDocument.parse('{screenSize: 55}'))
        helper.insertDocuments(OldDocument.parse('{screenSize: 155}'))
        helper.insertDocuments(OldDocument.parse('{screenSize: 75}'))

        def bucket = bucket('$screenSize', [0, 24, 32, 50, 70], new BucketOptions()
                .defaultBucket('monster')
                .output(sum('count', 1), push('matches', '$screenSize')))

        when:
        def results = helper.aggregate([sort(new OldDocument('screenSize', 1)), bucket])

        then:
        results == [
            OldDocument.parse('{_id: 0, count: 1, matches: [22]}'),
            OldDocument.parse('{_id: 24, count: 2, matches: [24, 30]}'),
            OldDocument.parse('{_id: 32, count: 1, matches: [42]}'),
            OldDocument.parse('{_id: 50, count: 1, matches: [55]}'),
            OldDocument.parse('{_id: "monster", count: 2, matches: [75, 155]}')
        ]
        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$bucketAuto'() {
        given:
        def helper = getCollectionHelper()

        helper.drop()

        (1..100).each {
            helper.insertDocuments(OldDocument.parse("{price: ${it * 2}}"))
        }

        when:
        def results = helper.aggregate([bucketAuto('$price', 10)])

        then:
        results[0]._id.min == 2
        results[0].count == 10

        results[-1]._id.max == 200

        when:
        results = helper.aggregate([bucketAuto('$price', 7)])

        then:
        results[0]._id.min == 2
        results[0].count == 14

        results[-1]._id.max == 200
        results[-1].count == 16

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$bucketAuto with options'() {
        given:
        def helper = getCollectionHelper()

        helper.drop()

        (0..2000).each {
            def document = new OldDocument('price', it * 5.01D)
            helper.insertDocuments(document)
        }

        when:
        def results = helper.aggregate([bucketAuto('$price', 10, new BucketAutoOptions()
            .granularity(BucketGranularity.POWERSOF2)
            .output(sum('count', 1), avg('avgPrice', '$price')))])

        then:
        results.size() == 5
        results[0].count != null
        results[0].avgPrice != null

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$count'() {
        given:
        def helper = getCollectionHelper()

        helper.drop()

        def total = 3
        def documents = []
        (1..total).each {
            documents.add(new BsonDocument())
        }
        helper.insertDocuments(documents)

        when:
        def results = helper.aggregate([count()])

        then:
        results[0].count == total

        when:
        results = helper.aggregate([count('count')])

        then:
        results[0].count == total

        when:
        results = helper.aggregate([count('total')])

        then:
        results[0].total == total

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$sortByCount'() {
        given:
        def helper = getCollectionHelper()

        when:
        helper.drop()

        helper.insertDocuments(OldDocument.parse('{_id: 0, x: 1}'))
        helper.insertDocuments(OldDocument.parse('{_id: 2, x: 1}'))
        helper.insertDocuments(OldDocument.parse('{_id: 3, x: 0}'))

        def results = helper.aggregate([sortByCount('$x')])

        then:
        results == [OldDocument.parse('{_id: 1, count: 2}'),
                    OldDocument.parse('{_id: 0, count: 1}')]

        when:
        helper.drop()

        helper.insertDocuments(OldDocument.parse('{_id: 0, x: 1.4}'))
        helper.insertDocuments(OldDocument.parse('{_id: 2, x: 1.1}'))
        helper.insertDocuments(OldDocument.parse('{_id: 3, x: 0.5}'))

        results = helper.aggregate([sortByCount(new OldDocument('$floor', '$x'))])

        then:
        results == [OldDocument.parse('{_id: 1, count: 2}'),
                    OldDocument.parse('{_id: 0, count: 1}')]

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(4, 4) })
    def '$accumulator'() {
        given:
        def helper = getCollectionHelper()

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 1, x: "string"}'))
        def init = 'function() { return { x: "test string" } }'
        def accumulate = 'function(state) { return state }'
        def merge = 'function(state1, state2) { return state1 }'
        def accumulatorExpr = accumulator('testString', init, accumulate, merge)
        def results1 = helper.aggregate([group('$x', asList(accumulatorExpr))])

        then:
        results1.size() == 1
        results1.contains(OldDocument.parse('{ _id: "string", testString: { x: "test string" } }'))

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 8751, title: "The Banquet", author: "Dante", copies: 2}'),
                OldDocument.parse('{_id: 8752, title: "Divine Comedy", author: "Dante", copies: 1}'),
                OldDocument.parse('{_id: 8645, title: "Eclogues", author: "Dante", copies: 2}'),
                OldDocument.parse('{_id: 7000, title: "The Odyssey", author: "Homer", copies: 10}'),
                OldDocument.parse('{_id: 7020, title: "Iliad", author: "Homer", copies: 10}'))
        def initFunction = 'function(initCount, initSum) { return { count: parseInt(initCount), sum: parseInt(initSum) } }'
        def accumulateFunction = 'function(state, numCopies) { return { count : state.count + 1, sum : state.sum + numCopies } }'
        def mergeFunction = 'function(state1, state2) { return { count : state1.count + state2.count, sum : state1.sum + state2.sum } }'
        def finalizeFunction = 'function(state) { return (state.sum / state.count) }'
        def accumulatorExpression = accumulator('avgCopies', initFunction, [ '0', '0' ], accumulateFunction,
                [ '$copies' ], mergeFunction, finalizeFunction)
        def results2 = helper.aggregate([group('$author', asList(
                new BsonField('minCopies', new OldDocument('$min', '$copies')), accumulatorExpression,
                new BsonField('maxCopies', new OldDocument('$max', '$copies'))))])

        then:
        results2.size() == 2
        results2.contains(OldDocument.parse('{_id: "Dante", minCopies: 1, avgCopies: 1.6666666666666667, maxCopies : 2}'))
        results2.contains(OldDocument.parse('{_id: "Homer", minCopies: 10, avgCopies: 10.0, maxCopies : 10}'))

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$addFields'() {
        given:
        def helper = getCollectionHelper()

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        def results = helper.aggregate([addFields(new Field('newField', null))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, newField: null}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('newField', 'hello'))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, newField: "hello"}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('b', '$a'))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, b: 1}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('this', '$$CURRENT'))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, this: {_id: 0, a: 1}}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('myNewField',
                                                        new OldDocument('c', 3).append('d', 4)))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, myNewField: {c: 3, d: 4}}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('alt3', new OldDocument('$lt', asList('$a', 3))))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, alt3: true}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('b', 3), new Field('c', 5))])

        then:
        results == [OldDocument.parse('{_id: 0, a: 1, b: 3, c: 5}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a: 1}'))
        results = helper.aggregate([addFields(new Field('a', [1, 2, 3]))])

        then:
        results == [OldDocument.parse('{_id: 0, a: [1, 2, 3]}')]

        cleanup:
        helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(4, 2) })
    def '$set'() {
        expect:
        aggregate([set(new Field('c', '$y'))]) == [new OldDocument(a).append('c', 'a'),
                                                   new OldDocument(b).append('c', 'b'),
                                                   new OldDocument(c).append('c', 'c')]
    }

    @IgnoreIf({ serverVersionLessThan(3, 4) })
    def '$replaceRoot'() {
        given:
        def helper = getCollectionHelper()
        def results = []

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a1: {b: 1}, a2: 2}'))
        results = helper.aggregate([replaceRoot('$a1')])

        then:
        results == [OldDocument.parse('{b: 1}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a1: {b: {c1: 4, c2: 5}}, a2: 2}'))
        results = helper.aggregate([replaceRoot('$a1.b')])

        then:
        results == [OldDocument.parse('{c1: 4, c2: 5}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a1: {b: 1, _id: 7}, a2: 2}'))
        results = helper.aggregate([replaceRoot('$a1')])

        then:
        results == [OldDocument.parse('{b: 1, _id: 7}')]
    }

    @IgnoreIf({ serverVersionLessThan(4, 2) })
    def '$replaceWith'() {
        given:
        def helper = getCollectionHelper()
        def results = []

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a1: {b: 1}, a2: 2}'))
        results = helper.aggregate([replaceWith('$a1')])

        then:
        results == [OldDocument.parse('{b: 1}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a1: {b: {c1: 4, c2: 5}}, a2: 2}'))
        results = helper.aggregate([replaceWith('$a1.b')])

        then:
        results == [OldDocument.parse('{c1: 4, c2: 5}')]

        when:
        helper.drop()
        helper.insertDocuments(OldDocument.parse('{_id: 0, a1: {b: 1, _id: 7}, a2: 2}'))
        results = helper.aggregate([replaceWith('$a1')])

        then:
        results == [OldDocument.parse('{b: 1, _id: 7}')]
    }

    @IgnoreIf({ serverVersionLessThan(4, 4) })
    def '$unionWith'() {
        given:
        def coll1Helper = getCollectionHelper(new MongoNamespace(getDatabaseName(), 'coll1'))
        def coll2Helper = getCollectionHelper(new MongoNamespace(getDatabaseName(), 'coll2'))

        coll1Helper.drop()
        coll2Helper.drop()

        coll1Helper.insertDocuments(
                OldDocument.parse('{ "name1" : "almonds" }'),
                OldDocument.parse('{ "name1" : "cookies" }'))

        coll2Helper.insertDocuments(
                OldDocument.parse('{ "name2" : "cookies" }'),
                OldDocument.parse('{ "name2" : "cookies" }'),
                OldDocument.parse('{ "name2" : "pecans" }'))

        def pipeline = asList(match(eq('name2', 'cookies')), project(fields(excludeId(), computed('name', '$name2'))),
                sort(ascending('name')))

        when:
        def results = coll1Helper.aggregate([project(fields(excludeId(), computed('name', '$name1'))),
                                             unionWith('coll2', pipeline)])

        then:
        results == [
            OldDocument.parse('{ name: "almonds" }'),
            OldDocument.parse('{ name: "cookies" }'),
            OldDocument.parse('{ name: "cookies" }'),
            OldDocument.parse('{ name: "cookies" }') ]

        cleanup:
        coll1Helper?.drop()
        coll2Helper?.drop()
    }

    @IgnoreIf({ serverVersionLessThan(5, 2) })
    def '$setWindowFields'(Bson preSortBy, Object partitionBy, Bson sortBy, WindowOutputField output, List<Object> expectedFieldValues) {
        given:
        ZoneId utc = ZoneId.of(ZoneOffset.UTC.getId())
        getCollectionHelper().drop()
        OldDocument[] original = [
                new OldDocument('partitionId', 1)
                        .append('num1', 1)
                        .append('num2', -1)
                        .append('numMissing', 1)
                        .append('date', LocalDateTime.ofInstant(Instant.ofEpochSecond(1), utc)),
                new OldDocument('partitionId', 1)
                        .append('num1', 2)
                        .append('num2', -2)
                        .append('date', LocalDateTime.ofInstant(Instant.ofEpochSecond(2), utc)),
                new OldDocument('partitionId', 2)
                        .append('num1', 3)
                        .append('num2', -3)
                        .append('numMissing', 3)
                        .append('date', LocalDateTime.ofInstant(Instant.ofEpochSecond(3), utc))]
        getCollectionHelper().insertDocuments(original)
        List<Bson> stages = [
                setWindowFields(partitionBy, sortBy, output),
                sort(ascending('num1'))
        ]
        if (preSortBy != null) {
            stages.add(0, sort(preSortBy))
        }
        List<OldDocument> actual = aggregate(stages)
        List<Object> actualFieldValues = actual.stream()
                .map { doc -> doc.get('result') }
                .collect(toList())

        expect:
        actualFieldValues.size() == expectedFieldValues.size()
        for (int i = 0; i < actualFieldValues.size(); i++) {
            Object actualV = actualFieldValues.get(i)
            Object expectedV = expectedFieldValues.get(i)
            if (actualV instanceof Collection && expectedV instanceof Set) {
                assert ((Collection) actualV).toSet() == expectedV
            } else {
                assert actualV == expectedV
            }
        }

        where:
        preSortBy | partitionBy | sortBy | output | expectedFieldValues
        null | null | null | WindowOutputFields
                .sum('result', '$num1', null) | [6, 6, 6]
        null | null | null | WindowOutputFields
                .sum('result', '$num1', documents(UNBOUNDED, UNBOUNDED)) | [6, 6, 6]
        null | '$partitionId' | ascending('num1') | WindowOutputFields
                .sum('result', '$num1', range(0, UNBOUNDED)) | [3, 2, 3]
        null | null | ascending('num1') | WindowOutputFields
                .sum('result', '$num1', range(CURRENT, Integer.MAX_VALUE)) | [6, 5, 3]
        null | null                                   | ascending('num1')  | WindowOutputFields
                .of(new BsonField('result', new OldDocument('$sum', '$num1')
                        .append('window', Windows.of(
                                new OldDocument('range', asList('current', Integer.MAX_VALUE))).toBsonDocument()))) | [6, 5, 3]
        null | null | ascending('date') | WindowOutputFields
                .avg('result', '$num1', timeRange(-1, 0, MongoTimeUnit.QUARTER)) | [1, 1.5, 2]
        null | null | null | WindowOutputFields
                .stdDevSamp('result', '$num1', documents(UNBOUNDED, UNBOUNDED)) | [1.0, 1.0, 1.0]
        null | null | ascending('num1') | WindowOutputFields
                .stdDevPop('result', '$num1', documents(CURRENT, CURRENT)) | [0, 0, 0]
        null | null | ascending('num1') | WindowOutputFields
                .min('result', '$num1', documents(-1, 0)) | [1, 1, 2]
        null | new OldDocument('gid', '$partitionId') | ascending('num1')  | WindowOutputFields
                .minN('result', '$num1',
                        new OldDocument('$cond', new OldDocument('if', new OldDocument('$eq', asList('$gid', 1)))
                                .append('then', 2).append('else', 2)),
                        documents(-1, 0))                                                                           | [ [1].toSet(), [1, 2].toSet(), [3].toSet() ]
        null | null | null | WindowOutputFields
                .max('result', '$num1', null) | [3, 3, 3]
        null | null | ascending('num1') | WindowOutputFields
                .maxN('result', '$num1', 2, documents(-1, 0)) | [ [1].toSet(), [1, 2].toSet(), [2, 3].toSet() ]
        null | '$partitionId' | null | WindowOutputFields
                .count('result', null) | [2, 2, 1]
        null | null | ascending('num1') | WindowOutputFields
                .derivative('result', '$num2', documents(UNBOUNDED, UNBOUNDED)) | [-1, -1, -1]
        null | null | ascending('date') | WindowOutputFields
                .timeDerivative('result', '$num2', documents(UNBOUNDED, UNBOUNDED), MongoTimeUnit.MILLISECOND) | [-0.001, -0.001, -0.001]
        null | null | ascending('num1') | WindowOutputFields
                .integral('result', '$num2', documents(UNBOUNDED, UNBOUNDED)) | [-4, -4, -4]
        null | null | ascending('date') | WindowOutputFields
                .timeIntegral('result', '$num2', documents(UNBOUNDED, UNBOUNDED), MongoTimeUnit.SECOND) | [-4, -4, -4]
        null | null | null | WindowOutputFields
                .covarianceSamp('result', '$num1', '$num2', documents(UNBOUNDED, UNBOUNDED)) | [-1.0, -1.0, -1.0]
        null | null | ascending('num1') | WindowOutputFields
                .covariancePop('result', '$num1', '$num2', documents(CURRENT, CURRENT)) | [0, 0, 0]
        null | null | ascending('num1') | WindowOutputFields
                .expMovingAvg('result', '$num1', 1) | [1, 2, 3]
        null | null | ascending('num1') | WindowOutputFields
                .expMovingAvg('result', '$num1', 0.5) | [1.0, 1.5, 2.25]
        null | null | descending('num1') | WindowOutputFields
                .push('result', '$num1', documents(UNBOUNDED, CURRENT)) | [ [3, 2, 1], [3, 2], [3] ]
        null | null | ascending('num1') | WindowOutputFields
                .addToSet('result', '$partitionId', documents(UNBOUNDED, -1)) | [ [], [1], [1] ]
        null | null | ascending('num1') | WindowOutputFields
                .first('result', '$num1', documents(UNBOUNDED, UNBOUNDED)) | [ 1, 1, 1 ]
        null | null | descending('num1') | WindowOutputFields
                .firstN('result', '$num1', 2, documents(UNBOUNDED, UNBOUNDED)) | [ [3, 2], [3, 2], [3, 2] ]
        null | null | ascending('num1') | WindowOutputFields
                .last('result', '$num1', documents(UNBOUNDED, UNBOUNDED)) | [ 3, 3, 3 ]
        null | null | ascending('num1') | WindowOutputFields
                .lastN('result', '$num1', 2, documents(UNBOUNDED, UNBOUNDED)) | [ [2, 3], [2, 3], [2, 3] ]
        null | null | ascending('num1') | WindowOutputFields
                .shift('result', '$num1', -3, 1) | [ 2, 3, -3 ]
        null | null | ascending('num1') | WindowOutputFields
                .documentNumber('result') | [ 1, 2, 3 ]
        null | null | ascending('partitionId') | WindowOutputFields
                .rank('result') | [ 1, 1, 3 ]
        null | null | ascending('partitionId') | WindowOutputFields
                .denseRank('result') | [ 1, 1, 2 ]
        null | null | null | WindowOutputFields
                .bottom('result', ascending('num1'), '$num1', null) | [ 3, 3, 3 ]
        null | new OldDocument('gid', '$partitionId') | descending('num1') | WindowOutputFields
                .bottomN('result', ascending('num1'), '$num1',
                        new OldDocument('$cond', new OldDocument('if', new OldDocument('$eq', asList('$gid', 1)))
                                .append('then', 2).append('else', 2)),
                        null)                                                                                       | [ [1, 2], [1, 2], [3] ]
        null | null | descending('num1') | WindowOutputFields
                .topN('result', ascending('num1'), '$num1', 2, null) | [ [1, 2], [1, 2], [1, 2] ]
        null | null | null | WindowOutputFields
                .top('result', ascending('num1'), '$num1', null) | [ 1, 1, 1 ]
        ascending('num1') | null | null | WindowOutputFields
                .locf('result', '$numMissing') | [ 1, 1, 3 ]
        null | null | ascending('num1') | WindowOutputFields
                .linearFill('result', '$numMissing') | [ 1, 2, 3 ]
    }

    @IgnoreIf({ serverVersionLessThan(5, 0) })
    def '$setWindowFields with multiple output'() {
        given:
        getCollectionHelper().drop()
        OldDocument[] original = [new OldDocument('num', 1)]
        getCollectionHelper().insertDocuments(original)
        List<OldDocument> actual = aggregate([
                setWindowFields(null, null, [
                        WindowOutputFields.count('count', null),
                        WindowOutputFields.max('max', '$num', null)]),
                project(fields(excludeId()))])

        expect:
        actual.size() == 1
        actual.get(0) == original[0].append('count', 1).append('max', 1)
    }

    @IgnoreIf({ serverVersionLessThan(5, 0) })
    def '$setWindowFields with empty output'() {
        given:
        getCollectionHelper().drop()
        OldDocument[] original = [new OldDocument('num', 1)]
        getCollectionHelper().insertDocuments(original)
        List<OldDocument> actual = aggregate([
                setWindowFields(null, null, []),
                project(fields(excludeId()))])

        expect:
        actual.size() == 1
        actual.get(0) == original[0]
    }

    @IgnoreIf({ serverVersionLessThan(5, 1) })
    def '$densify'(String field, String partitionByField, Bson densifyStage, List<Object> expectedFieldValues) {
        given:
        getCollectionHelper().drop()
        OldDocument[] docs = [
                new OldDocument('partitionId', 1)
                        .append('num', 1)
                        .append('date', Instant.ofEpochMilli(BigInteger.TWO.pow(32).longValueExact())),
                new OldDocument('partitionId', 1)
                        .append('num', 3)
                        .append('date', Instant.ofEpochMilli(BigInteger.TWO.pow(33).longValueExact())),
                new OldDocument('partitionId', 2)
                        .append('num', new BsonDecimal128(new Decimal128(new BigDecimal('4.1'))))
                        .append('date', Instant.ofEpochMilli(BigInteger.TWO.pow(34).longValueExact()))]
        getCollectionHelper().insertDocuments(docs)
        Bson sortSpec = partitionByField == null ? ascending(field) : ascending(partitionByField, field)
        List<Object> actualFieldValues = aggregate([
                densifyStage,
                sort(sortSpec),
                project(fields(include(field), exclude('_id')))])
                .stream()
                .map { doc -> doc.get(field) }
                .map { e -> e instanceof Date ? ((Date) e).toInstant() : e }
                .collect(toList())

        expect:
        actualFieldValues == expectedFieldValues

        where:
        field | partitionByField | densifyStage | expectedFieldValues
        'num' | null | densify(field, fullRangeWithStep(new Decimal128(BigDecimal.ONE))) |
                [1, 2, 3, 4, 4.1]
        'num' | null | densify(field, rangeWithStep(-1.0, 5.0, 1.0)) |
                [-1, 0, 1, 2, 3, 4, 4.1]
        'num' | null | densify(field, rangeWithStep(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)) |
                [1, 3, 4.1]
        'num' | 'partitionId' | densify(field, fullRangeWithStep(1), densifyOptions().partitionByFields(partitionByField)) |
                [1, 2, 3, 4, 1, 2, 3, 4, 4.1]
        'num' | 'partitionId' | densify(field, partitionRangeWithStep(1), densifyOptions().partitionByFields([partitionByField])) |
                [1, 2, 3, 4.1]
        'date' | null | densify(field, rangeWithStep(Instant.EPOCH, Instant.ofEpochMilli(BigInteger.TWO.pow(32).longValueExact()) + 1,
                // there is a server bug that prevents using `step` larger than 2^31 - 1 in versions before 6.1
                BigInteger.TWO.pow(31).longValueExact() - 1, MongoTimeUnit.MILLISECOND)) |
                [Instant.EPOCH,
                 Instant.ofEpochMilli(BigInteger.TWO.pow(31).longValueExact() - 1),
                 Instant.ofEpochMilli(BigInteger.TWO.pow(32).longValueExact() - 2),
                 Instant.ofEpochMilli(BigInteger.TWO.pow(32).longValueExact()),
                 Instant.ofEpochMilli(BigInteger.TWO.pow(33).longValueExact()),
                 Instant.ofEpochMilli(BigInteger.TWO.pow(34).longValueExact())]
    }

    @IgnoreIf({ serverVersionLessThan(5, 3) })
    def '$fill'(Bson preSortBy, String field, Bson fillStage, List<Object> expectedFieldValues) {
        given:
        getCollectionHelper().drop()
        OldDocument[] docs = [
            new OldDocument('partition', new OldDocument('id', 10))
                        .append('_id', 1)
                        .append('field1', 1)
                        .append('doc', new OldDocument('field2', 1)),
            new OldDocument('partition', new OldDocument('id', 10))
                        .append('_id', 2)
                        .append('doc', null),
            new OldDocument('partition', new OldDocument('id', 20))
                        .append('_id', 3)
                        .append('field1', 3)
                        .append('doc', new OldDocument('field2', 3))]
        getCollectionHelper().insertDocuments(docs)
        String resultField = 'result'
        List<Bson> stages = [
                fillStage,
                project(fields(computed(resultField, '$' + field))),
                sort(ascending('_id'))
        ]
        if (preSortBy != null) {
            stages.add(0, sort(preSortBy))
        }
        List<Object> actualFieldValues = aggregate(stages)
                .stream()
                .map { doc -> doc.get(resultField) }
                .collect(toList())

        expect:
        actualFieldValues == expectedFieldValues

        where:
        preSortBy| field | fillStage | expectedFieldValues
        null | 'doc.field2' | fill(
                fillOptions().partitionByFields('p1', 'p2'),
                FillOutputField.value(field, '$partition.id'))                   |
                [1, 10, 3]
        null | 'doc.field2' | fill(
                fillOptions().sortBy(ascending('_id')),
                FillOutputField.linear(field), FillOutputField.locf('newField')) |
                [1, 2, 3]
        null | 'field1' | fill(
                // https://jira.mongodb.org/browse/SERVER-67284 prevents specifying partitionByField('partition.id')
                fillOptions().partitionBy(new OldDocument('p', '$partition.id')).sortBy(descending('_id')),
                FillOutputField.locf('field1'))                                  |
                [1, null, 3]
        descending('_id') | 'field1' | fill(
                fillOptions(),
                FillOutputField.locf('field1'))                                  |
                [1, 3, 3]
    }
}
