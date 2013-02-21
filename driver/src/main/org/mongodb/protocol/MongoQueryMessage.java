/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
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

package org.mongodb.protocol;

import org.bson.io.OutputBuffer;
import org.bson.types.Document;
import org.mongodb.operation.MongoCommand;
import org.mongodb.operation.MongoFind;
import org.mongodb.operation.MongoQuery;
import org.mongodb.serialization.Serializer;

public class MongoQueryMessage extends MongoRequestMessage {

    public MongoQueryMessage(final String collectionName, final MongoFind find, final OutputBuffer buffer,
                             final Serializer<Document> serializer) {
        super(collectionName, find.getOptions(), find.getReadPreference(), buffer);

        init(find);
        addDocument(getQueryDocument(find), serializer);
        if (find.getFields() != null) {
            addDocument(find.getFields().toDocument(), serializer);
        }
        backpatchMessageLength();
    }

    public MongoQueryMessage(final String collectionName, final MongoCommand commandOperation,
                             final OutputBuffer buffer, final Serializer<Document> serializer) {
        super(collectionName, 0, commandOperation.getReadPreference(), buffer);

        init(commandOperation);
        addDocument(commandOperation.toDocument(), serializer);
        backpatchMessageLength();
    }

    private void init(final MongoQuery query) {
        int allOptions = 0;
        if (query.getReadPreference().isSlaveOk()) {
            allOptions |= QueryOptions.SLAVEOK;
        }

        writeQueryPrologue(allOptions, query);
    }

    private void writeQueryPrologue(final int queryOptions, final MongoQuery query) {
        getBuffer().writeInt(queryOptions);
        getBuffer().writeCString(getCollectionName());

        getBuffer().writeInt(query.getSkip());
        getBuffer().writeInt(chooseBatchSize(query.getBatchSize(), query.getLimit(), 0));
    }

    // TODO: test this, extensively
    private int chooseBatchSize(final int batchSize, final int limit, final int fetched) {
        final int bs = Math.abs(batchSize);
        final int remaining = limit > 0 ? limit - fetched : 0;
        int res;
        if (bs == 0 && remaining > 0) {
            res = remaining;
        }
        else if (bs > 0 && remaining == 0) {
            res = bs;
        }
        else {
            res = Math.min(bs, remaining);
        }

        if (batchSize < 0) {
            // force close
            res = -res;
        }

        return res;
    }

    private Document getQueryDocument(final MongoFind find) {
        final Document document = new Document();
        document.put("query", find.getFilter());
        if (find.getOrder() != null && !find.getOrder().isEmpty()) {
            document.put("orderby", find.getOrder());
        }
        if (find.isSnapshotMode()) {
            document.put("$snapshot", true);
        }
        // TODO: only to mongos according to spec
        if (find.getReadPreference() != null) {
            document.put("$readPreference", find.getReadPreference().toDocument());
        }
        // TODO: explain and hint
        return document;
    }
}
