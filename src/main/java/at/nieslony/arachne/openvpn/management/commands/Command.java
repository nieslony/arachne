/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management.commands;

import at.nieslony.arachne.openvpn.management.ManagementException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author claas
 */
abstract public class Command<T> {

    private final String command;
    private final BlockingQueue<Command> queue;
    protected final CompletableFuture<T> value;
    protected final CompletableFuture<Void> lock;

    protected Command(BlockingQueue<Command> queue, String command) {
        this.command = command;
        this.queue = queue;
        this.value = new CompletableFuture<>();
        this.lock = new CompletableFuture<>();
    }

    public int writeCommand(SocketChannel channel) throws IOException {
        String cmdLine = command + "\n";
        ByteBuffer buf = ByteBuffer.wrap(cmdLine.getBytes());
        return channel.write(buf);
    }

    public void cancel(Throwable ex) {
        value.completeExceptionally(ex);
    }

    public void waitForUnlock() throws ExecutionException, InterruptedException {
        lock.get();
    }

    public abstract boolean processResultLine(String line) throws ManagementException;

    public abstract void processResult() throws ManagementException;

    public T waitForResult() throws ManagementException {
        try {
            this.queue.put(this);
            lock.complete(null);
            return value.get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new ManagementException(
                    "Error executing command %s: %s"
                            .formatted(command, ex.getMessage()),
                    ex
            );
        }
    }

    @Override
    public String toString() {
        return "Command <%s>".formatted(command);
    }
}
