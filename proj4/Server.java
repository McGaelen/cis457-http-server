package proj4;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

class Server {

    public static void main(String args[]) {
        int portNum = 8080;
		String docroot = ".";
		String logfile;

		for (int i = 0; i < args.length; i++) {
		    if (args[i].equals("-p")) {
		        portNum = Integer.parseInt(args[i+1]);
            } else if (args[i].equals("-docroot")) {
		        docroot = args[i+1];
            } else if (args[i].equals("-logfile")) {
		        logfile = args[i+1];
            }
        }

        SocketChannel sc;
        try {
            ServerSocketChannel c = ServerSocketChannel.open();
            c.bind(new InetSocketAddress(portNum));
            while (true) {
                sc = c.accept();
                System.out.println("client connected");
                TcpServerThread t = new TcpServerThread(sc, docroot);
                t.start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class TcpServerThread extends Thread {
    private SocketChannel sc;
    private String docroot;

    TcpServerThread(SocketChannel sc, String docroot) {
        this.sc = sc;
        this.docroot = docroot;
    }

    public void run() {
        String command;

        try {
            while (true) {
                ByteBuffer cmd = ByteBuffer.allocate(4096);
                sc.read(cmd);
                command = new String(cmd.array());
                if (command.equals("")) {
                    sc.close();
                    return;
                }

                System.out.println(command);

				HTTPRequest request = parseRequest(command);

				if (!request.opcode.equals("GET")) {
                    sc.write(ByteBuffer.wrap(HTTPResponse.string501().getBytes()));
                    sc.close();
                    return;
				}

				FileInputStream file;
				File f;
				try {
				 	file = new FileInputStream(docroot + request.path);
					f = new File(docroot + request.path);
				} catch (IOException e) {
					// send 404 message
					sc.write(ByteBuffer.wrap(HTTPResponse.string404().getBytes()));
					sc.close();
					return;
				}

				HTTPResponse response = new HTTPResponse();
				boolean is304Response = false;

                Date lastModified = new Date(f.lastModified());
                response.lastModified = HTTPResponse.gmtDate(lastModified);

                if (request.ifModifiedSince != null) {
                    Date ifModifiedSince = new Date(request.ifModifiedSince.substring(5));

                    if (!ifModifiedSince.before(lastModified)) {
                        is304Response = true;
                    }
                }

                if (is304Response) {
                    response.statusCode = "304";
                    response.statusMessage = "Not Modified";
                } else {
                    response.statusCode = "200";
                    response.statusMessage = "OK";
                }

				response.date = HTTPResponse.gmtDate(new Date());
				response.contentType = HTTPResponse.contentType(request.path);

                String[] split = f.getName().split("\\.");
                String extension = split[split.length - 1];

                if (extension.equals("html") || extension.equals("txt")) {
                    response.contentLength = Long.toString(f.length());
                    if (!is304Response) {
                        Scanner scanner = new Scanner(f);
                        response.body = scanner.useDelimiter("\\Z").next();
                        scanner.close();
                    } else {
                        response.body = "";
                    }
                    sc.write(ByteBuffer.wrap(response.toString().getBytes()));
                } else {
                    response.contentLength = Long.toString(f.length());
                    response.body = "";
                    if (!is304Response) {
                        ByteBuffer b = ByteBuffer.allocate((int) f.length());
                        file.getChannel().read(b);
                        b.flip();
                        ByteBuffer[] bufs = {ByteBuffer.wrap(response.toString().getBytes()), b};
                        sc.write(bufs);
                    } else {
                        sc.write(ByteBuffer.wrap(response.toString().getBytes()));
                    }
                }

                System.out.println(request.connection);
                if (request.connection.equals("keep-alive")) {
                    System.out.println("closing connection");
                    sc.close();
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

	public HTTPRequest parseRequest(String request) {
		HTTPRequest requestObj = new HTTPRequest();

		String[] lines = request.split("\n");

		for (int i = 0; i < lines.length; i++) {
			if (i == 0) {
				String[] line1 = lines[0].split(" ");
				requestObj.opcode = line1[0];
				if (line1.length > 2) {
                    requestObj.path = line1[1];
                }
			} else {
				String[] line = lines[i].split(": ");

				if (line[0].equals("If-Modified-Since")) {
					requestObj.ifModifiedSince = line[1];
				} else if (line[0].equals("Connection")) {
					requestObj.connection = line[1];
				}
			}
		}
		return requestObj;
	}
}

class HTTPRequest {
	String opcode;
	String path;

	String ifModifiedSince;
	String connection;

	public String toString() {
		return opcode + "  " + path + "  " + ifModifiedSince + "  " + connection;
	}
}

class HTTPResponse {
    final static String version = "http/1.1";

    String statusCode;
    String statusMessage;

    String date;
    String lastModified;
    String contentType;
    String contentLength;

	String body;

    static String contentType(String filename) {
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

    static String gmtDate(Date d) {
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

	public String toString() {
	    return version + " " + statusCode + " " + statusMessage + "\n"
                + "Date: " + date + "\n"
                + "Last-Modified: " + lastModified + "\n"
                + "Content-Type: " + contentType + "\n"
                + "Content-Length: " + contentLength + "\n\n"
                + body;
    }

    public static String string404() {
        String message = "<h1>NOT FOUND!!!!!!!!</h1>";
        return HTTPResponse.version + " 404 Not Found\n"
                + "Date: " + gmtDate(new Date()) + "\n"
                + "Content-Type: text/html; charset=utf-8\n"
                + "Content-Length: " + (message.length()) + "\n\n"
                + message;
    }

    public static String string501() {
        String message = "<h1>NOT MY JOB!!!!!!!</h1>";
        return HTTPResponse.version + " 501 Not Implemented\n"
                + "Date: " + gmtDate(new Date()) + "\n"
                + "Content-Type: text/html; charset=utf-8\n"
                + "Content-Length: " + (message.length()) + "\n\n"
                + message;
    }


}
