package wordchain.server;

import wordchain.common.Protocol;

/** Standalone dedicated server: {@code java -cp wordchain.jar wordchain.server.ServerMain [port]}. */
public final class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        GameServer server = new GameServer(Dictionary.load());
        server.start(port);
        System.out.println("WordChain server listening on port " + port);
        for (String ip : GameServer.localAddresses()) System.out.println("  LAN address: " + ip + ":" + port);
        Thread.currentThread().join();
    }
}
