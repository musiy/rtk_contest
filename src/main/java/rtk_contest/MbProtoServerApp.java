package rtk_contest;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import rtk_contest.server.MbProtoServiceImpl;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MbProtoServerApp {

    private Server server;
    private final int port;

    public MbProtoServerApp(int port) {
        this.port = port;
    }

    private void start() throws IOException {
        for (int i = 0; i < 50_000_000; i++) {
            new Object();
        }
        Runtime.getRuntime().gc();
        /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
                .addService(new MbProtoServiceImpl())
                .executor(Executors.newFixedThreadPool(1))
                .build()
                .start();
        System.out.println("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws Exception {
        int port = 80;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        final MbProtoServerApp server = new MbProtoServerApp(port);
        server.start();
        server.blockUntilShutdown();
    }

}
