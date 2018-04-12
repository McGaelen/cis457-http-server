package proj4;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class Server {

    public static void main(String args[]) {
        int portNum = Integer.parseInt(args[0]);

//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("running shutdown hook");
//            Iterator it = Server.userSockets.entrySet().iterator();
//            while (it.hasNext()) {
//                Map.Entry pair = (Map.Entry) it.next();
//                ClientConnection cc = (ClientConnection)pair.getValue();
//                try {
//                    Server.writeSocket("!shutdown", cc, new SecureRandom(), crypto);
//                    cc.sc.close();
//                } catch (IOException e) {
//                    System.out.println(e.getMessage());
//                }
//            }
//        }));

        SocketChannel sc;
        try {
            ServerSocketChannel c = ServerSocketChannel.open();
            c.bind(new InetSocketAddress(8080));
            while (true) {
                sc = c.accept();
                System.out.println("client connected");
                TcpServerThread t = new TcpServerThread(sc);
                t.start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class TcpServerThread extends Thread {
    private SocketChannel sc;

    TcpServerThread(SocketChannel sc) {
        this.sc = sc;
    }

    public void run() {
        String command;

        try {
            while (true) {
                ByteBuffer cmd = ByteBuffer.allocate(4096);
                sc.read(cmd);
                command = new String(cmd.array());
                if (command.equals("")) {
                    continue;
                }

                System.out.println("Got from client: " + command);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
