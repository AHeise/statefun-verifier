package com.github.igalshilman.statefun.verifier.e2e;

import com.github.igalshilman.statefun.verifier.CommandFlinkSource;
import com.github.igalshilman.statefun.verifier.generated.VerificationResult;
import org.apache.flink.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class SimpleTcpServer {
  private static final Logger LOG = LoggerFactory.getLogger(CommandFlinkSource.class);

  private final LinkedBlockingDeque<VerificationResult> results = new LinkedBlockingDeque<>();
  private final ExecutorService executor;
  private final AtomicBoolean started = new AtomicBoolean(false);

  public SimpleTcpServer() {
    this.executor = MoreExecutors.newCachedDaemonThreadPool();
  }

  StartedServer start() {
    if (!started.compareAndSet(false, true)) {
      throw new IllegalArgumentException("Already started.");
    }
    try {
      ServerSocket serverSocket = new ServerSocket(0);
      serverSocket.setReuseAddress(true);
      LOG.info("Starting server at " + serverSocket.getLocalPort());
      executor.submit(() -> acceptClients(serverSocket));
      return new StartedServer(serverSocket.getLocalPort(), results());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to bind the TCP server.", e);
    }
  }

  private Supplier<VerificationResult> results() {
    return () -> {
      try {
        return results.take();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @SuppressWarnings("InfiniteLoopStatement")
  private void acceptClients(ServerSocket serverSocket) {
    while (true) {
      try {
        Socket client = serverSocket.accept();
        InputStream input = client.getInputStream();
        executor.submit(() -> pumpVerificationResults(client, input));
      } catch (IOException e) {
        LOG.info("Exception while trying to acceept a connection.", e);
      }
    }
  }

  private void pumpVerificationResults(Socket client, InputStream input) {
    while (true) {
      try {
        VerificationResult result = VerificationResult.parseDelimitedFrom(input);
        if (result != null) {
          results.add(result);
        }
      } catch (IOException e) {
        LOG.info(
            "Exception reading a verification result from "
                + client.getRemoteSocketAddress()
                + ", bye...",
            e);
        IOUtils.closeQuietly(client);
        return;
      }
    }
  }

  public static final class StartedServer {
    private final int port;
    private final Supplier<VerificationResult> results;

    public StartedServer(int port, Supplier<VerificationResult> results) {
      this.port = port;
      this.results = results;
    }

    public int port() {
      return port;
    }

    public Supplier<VerificationResult> results() {
      return results;
    }
  }
}
