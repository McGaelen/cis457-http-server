package proj4;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

class Server {

    public static void main(String args[]) {
        int portNum = Integer.parseInt(args[0]);
		String docroot = args[1];
		String logfile = args[2];


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

				HTTPRequest request = parseRequest(command);

				if (!request.opcode.equals("GET")) {
					continue;
				}

				FileInputStream file;
				File f;
				try {
				 	file = new FileInputStream("." + request.path);
					f = new File("." + request.path);
				} catch (IOException e) {
					// send 404 message
					System.out.println("404");
					continue;
				}

				HTTPResponse response = new HTTPResponse();
				response.statusCode = "200";
				response.statusMessage = "OK";
				response.date = gmtDate(new Date());
				response.lastModified = gmtDate(new Date(f.lastModified()));
				response.contentType = contentType(request.path);

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

	public String contentType(String filename) {
		String[] split = filename.split("\\.");
		String extension = split[split.length - 1];

		if (extension.equals("html")) {
			return "text/html; charset=utf-8";
		} else if (extension.equals("txt")) {
			return "text/plain; charset=utf-8";
		} else if (extension.equals("jpg")) {
			return "image/jpeg";
		} else if (extension.equals("pdf")) {
			return "application/pdf";
		} else {
			return "";
		}
	}

	public String gmtDate(Date d) {
		int day = d.getDay();
		String dayName;
		if (day == 0) {
			dayName = "Sun, ";
		} else if (day == 1) {
			dayName = "Mon, ";
		} else if (day == 2) {
			dayName = "Tue, ";
		} else if (day == 3) {
			dayName = "Wed, ";
		} else if (day == 4) {
			dayName = "Thu, ";
		} else if (day == 5) {
			dayName = "Fri, ";
		} else {
			dayName = "Sat, ";
		}

		return dayName + d.toGMTString();
	}

	public HTTPRequest parseRequest(String request) {
		HTTPRequest requestObj = new HTTPRequest();

		String[] lines = request.split("\n");

		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				String[] line1 = lines[0].split(" ");
				requestObj.opcode = line1[0];
				requestObj.path = line1[1];
			} else {
				String[] line = lines[i].split(" ");

				if (line[0].equals("if-modified-since:")) {
					requestObj.ifModifiedSince = line[1];
				} else if (line[0].equals("Connection:")) {
					requestObj.connection = line[1];
				}
			}
		}
		return requestObj;
	}
}

class HTTPRequest {
	public String opcode;
	public String path;

	public String ifModifiedSince;
	public String connection;

	public String toString() {
		return "request: " + opcode + "  " + path + "  " + ifModifiedSince + "  " + connection;
	}
}

class HTTPResponse {
	public final String version = "http/1.1";

	public String statusCode;
	public String statusMessage;

	public String date;
	public String lastModified;
	public String contentType;
	public String contentLength;

	public String body;
}
