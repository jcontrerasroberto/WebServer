import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class Handler extends Thread{

    private Socket socket;
    private PrintWriter printWriter;
    private DataInputStream dis;
    private DataOutputStream dos;

    public Handler(Socket newCLient){
        this.socket = newCLient;
    }

    @Override
    public void run() {
        try{
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            printWriter = new PrintWriter(new OutputStreamWriter(dos));

            String request = dis.readLine();

            if(request==null)
            {
                printWriter.print("<html><head><title>Servidor WEB");
                printWriter.print("</title><body bgcolor=\"#AACCFF\"<br>Linea Vacia</br>");
                printWriter.print("</body></html>");
                socket.close();
                return;
            }

            System.out.println("");
            System.out.println("\nNew client from " + socket.getInetAddress() + " : " + socket.getPort() );

            if(request.startsWith("GET")) handleGet(request);
            else if(request.startsWith("HEAD")) handleHead(request);
            else if(request.startsWith("POST")) handlePost(request);
            else if(request.startsWith("PUT")) handlePut(request);
            else sendHeader("", null, 501);

            dos.flush();
            socket.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handlePut(String request) throws IOException {
        System.out.println("PUT request received");
        System.out.println(request);
        String fileName = getFileName(request);
        int statusCode = createFile(fileName);
        sendFile("successput.html", statusCode);
    }

    private int createFile(String fileName) throws IOException {
        File toCreate = new File(fileName);
        if(toCreate.exists()){
            return 204;
        }else{
            if(toCreate.createNewFile()) return 201;
            else return 500;
        }
    }

    private void handlePost(String request) throws IOException {
        System.out.println("POST request received");
        System.out.println(request);
        String fileName = getFileName(request);
        byte[] tmp= new byte[1500];
        int n= dis.read(tmp);
        String data = new String(tmp,0,n);
        System.out.println(data);
        System.out.println( "(POST) Getting the params...");
        int startOfParams = data.indexOf("\n\r");
        String params = data.substring(startOfParams);
        System.out.println("(POST) Params okey: " + params);
        sendFile("success.html", 200);
    }

    private void handleHead(String request) throws IOException {
        System.out.println("HEAD request received");
        System.out.println(request);
        String fileName = getFileName(request);
        System.out.println("(HEAD) Client waiting for info of: " + fileName);
        sendHead(fileName);
    }

    private void sendHead(String fileName) throws IOException {
        DataInputStream fileStream = new DataInputStream(new FileInputStream(fileName));
        int blockSize = 0;
        if (fileStream.available()>=1024){
            blockSize = 1024;
        }else{
            fileStream.available();
        }

        sendHeader(fileName, fileStream, 200);
    }

    public String getFileName(String request){
        int startPos;
        int endPos;
        startPos = request.indexOf("/");
        endPos = request.indexOf(" ", startPos);
        return request.substring(startPos+1, endPos);
    }

    public void handleParams(String request){
        StringTokenizer tokens=new StringTokenizer(request,"?");
        String req_a=tokens.nextToken();
        String req=tokens.nextToken();
        System.out.println("Sending params response...");
        printWriter.println("HTTP/1.1 200 Okay");
        printWriter.flush();
        printWriter.println();
        printWriter.flush();
        printWriter.print("<html><head><title>SERVIDOR WEB");
        printWriter.flush();
        printWriter.print("</title></head><body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos...</br></h1>");
        printWriter.flush();
        printWriter.print("<h3><b>"+req+"</b></h3>");
        printWriter.flush();
        printWriter.print("</center></body></html>");
        printWriter.flush();
    }

    public void handleGet(String request){
        System.out.println("GET request received");
        System.out.println(request);
        if(request.contains("?")){
            handleParams(request);
        }else{
            String fileName = getFileName(request);
            if(fileName.equals("")) fileName = "index.html";
            System.out.println("(GET) Client waiting for: " + fileName);
            sendFile(fileName, 200);
        }
    }

    public void sendHeader(String fileName, DataInputStream fileStream, int statusCode) throws IOException {
        int fileSize = 0;
        if(fileStream != null)
            fileSize = fileStream.available();

        String header = "";

        switch (statusCode){
            case 200: header = header +"HTTP/1.1 200 Ok\n"; break;
            case 202: header = header +"HTTP/1.1 202 Accepted\n"; break;
            case 201: header = header +"HTTP/1.1 201 Created\n"; break;
            case 404: header = header +"HTTP/1.1 404 Not found\n"; break;
            case 500: header = header +"HTTP/1.1 500 Error interno del servidor\n"; break;
            case 204: header = header +"HTTP/1.1 204 No content\n"; break;
            case 501: header = header +"HTTP/1.1 501 Not implemented \n"; break;
        }


        header = header +"Server: Contreras Server/1.1 \n";
        header = header +"Date: " + new Date()+" \n";
        header = header +"Content-Type: ";

        String ext = fileName.substring(fileName.lastIndexOf('.')+1);   //obtenemos la extensión del recurso solicitado
        switch (ext){
            case "html" : header = header +"text/html"; break;
            case "jpg" : header +="image/jpeg"; break;
            case "png" : header +="image/png"; break;
            case "doc" : header +="application/msword\n"; break;
            case "pdf" : header +="application/pdf"; break;
            case "xls" : header +="application/vnd.ms-excel"; break;
            case "ppt" : header += "application/vnd.ms-powerpoint"; break;
            case "txt" : header += "text/plain"; break;
        }

        header = header + "\n";
        header = header +"Content-Length: "+ fileSize + " \n";
        header = header +"\n";

        System.out.println("Sending header...");
        System.out.println("--------------");
        System.out.println(header);

        dos.write(header.getBytes());
        dos.flush();

    }

    public void sendFile(String fileName, int statusCode) {
        try{

            File toSend = new File(fileName);

            if (!toSend.exists()){
                fileName = "404.html";
                statusCode = 404;
            }

            DataInputStream fileStream = new DataInputStream(new FileInputStream(fileName));
            int blockSize = 0;
            if (fileStream.available()>=1024){
                blockSize = 1024;
            }else{
                fileStream.available();
            }

            sendHeader(fileName, fileStream, statusCode);

            byte[] buffer = new byte[1024];
            int byteLeido;
            while ((byteLeido = fileStream.read(buffer, 0, buffer.length))!=-1){
                dos.write(buffer, 0, byteLeido);
                dos.flush();
            }

            fileStream.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
