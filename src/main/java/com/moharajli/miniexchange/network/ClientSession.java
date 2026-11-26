package com.moharajli.miniexchange.network;

import com.moharajli.miniexchange.common.Validation;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class ClientSession {

    private final PrintWriter out;
    private final ReentrantLock lock = new ReentrantLock();

    public ClientSession(PrintWriter out) {
        Validation.requireNotNull(out, "Output");
        this.out = out;
    }

    public void send(List<String> lines) {
        lock.lock();
        try {
            for (String line : lines) {
                out.print(line + "\n");
            }
            out.flush();
        } finally {
            lock.unlock();
        }
    }
}
