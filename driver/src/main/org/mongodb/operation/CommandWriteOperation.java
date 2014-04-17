package org.mongodb.operation;

import org.mongodb.CommandResult;
import org.mongodb.Decoder;
import org.mongodb.Document;
import org.mongodb.Encoder;
import org.mongodb.MongoFuture;
import org.mongodb.binding.WriteBinding;
import org.mongodb.session.Session;

import static org.mongodb.operation.OperationHelper.executeWrappedCommandProtocol;
import static org.mongodb.operation.OperationHelper.executeWrappedCommandProtocolAsync;

/**
 * An operation that executes an arbitrary command that writes to the server.
 *
 * @since 3.0
 */
public class CommandWriteOperation implements AsyncOperation<CommandResult>, WriteOperation<CommandResult> {
    private final Encoder<Document> commandEncoder;
    private final Decoder<Document> commandDecoder;
    private final String database;
    private final Document commandDocument;

    public CommandWriteOperation(final String database, final Document command, final Decoder<Document> commandDecoder,
                                 final Encoder<Document> commandEncoder) {
        this.database = database;
        this.commandEncoder = commandEncoder;
        this.commandDecoder = commandDecoder;
        this.commandDocument = command;
    }

    @Override
    public CommandResult execute(final WriteBinding binding) {
        return executeWrappedCommandProtocol(database, commandDocument, commandEncoder, commandDecoder, binding);
    }

    @Override
    public MongoFuture<CommandResult> executeAsync(final Session session) {
        return executeWrappedCommandProtocolAsync(database, commandDocument, commandEncoder, commandDecoder, session);
    }
}
