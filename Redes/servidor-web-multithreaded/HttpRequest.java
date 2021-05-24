import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class HttpRequest implements Runnable {
    final static String CRLF = "\r\n";
    final static String SPACE = " ";
    Socket socket;
    boolean auth = true;
    boolean listDirectory = true;

    // Construtor 
    public HttpRequest(final Socket socket) throws Exception {
        this.socket = socket;

    }


    // Interface Runnable
    public void run() {
        try {
            processRequest();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    // Retorno do MIME para o Content-Type do cabeçalho

    String line = "Content-type: ";

    private String contentType(final String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return line + "text/html";
        }

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
            return line +  "image/jpeg";
        }

        if (fileName.endsWith(".gif")) {
            return line +  "image/gif";
        }

        if (fileName.endsWith(".pdf")) {
            return line +  "application/pdf";
        }

        return line +  "application/octet-stream";
    }

    // Envio do arquivo solicitado
    private int sendBytes(final FileInputStream fis, final OutputStream os) throws Exception {
        // Construir um buffer de 1K para comportar os bytes no caminho para o socket.
        final byte[] buffer = new byte[2048];
        int bytes = 0;
        int totalBytes = 0;
        // Copiar o arquivo requisitado dentro da cadeia de saída do socket
        while((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
            totalBytes += bytes;
        }

        return totalBytes;
    }


    void writeLog(final String line) {
        final File log = new File("data.text");
        try {
            final FileWriter fwLog = new FileWriter(log, true);
            fwLog.write(line + CRLF);
            fwLog.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }


/*     private void verifyAuthorization(final String authorization) {
        if (authorization != null) {
            final byte[] decoded = DatatypeConverter.parseBase64Binary(authorization);
            final String decodedString = new String(decoded);
            final String[] userInfo = decodedString.split(":");       

            auth = userInfo[0].equals("redes") && userInfo[1].equals("diploma");
        }
    } */


    void response(final String fileName, final String address, final String httpVersion, final DataOutputStream os)
            throws Exception {
        FileInputStream fis = null;
        Boolean fileExists = true;
        int bytes = 0;

        // Constrói a mensagem de resposta
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;

        final File path = new File(fileName);

        if (!path.isDirectory()) {
            // Tenta abrir o arquivo requisitado
            try {
                fis = new FileInputStream(fileName);
            } catch (final FileNotFoundException e) {
                fileExists = false;
            }
            if (fileExists) {
                statusLine = httpVersion + " 200" + CRLF;
                contentTypeLine = contentType(fileName) + CRLF;
            } else {
                statusLine = httpVersion + " 404" + CRLF;
                contentTypeLine  = contentType(".htm") + CRLF;
                entityBody =   "<html>"
                        + "<head><title>Not Found</title></head>" 
                        + "<body>" + fileName + " não encontrado</body>"
                        + "</html>";
            }

            // Enviar a linha de status.
            os.writeBytes(statusLine);
            // Enviar a linha de tipo de conteúdo.
            os.writeBytes(contentTypeLine);
            // Enviar uma linha em branco para indicar o fim das linhas de cabeçalho.      
            os.writeBytes(CRLF);

            if(fileExists) {
                bytes = sendBytes(fis, os);
                fis.close();
            } else {
                os.writeBytes(entityBody);
            }

        } else {
            if (auth) {
                if (listDirectory) {
                    final File folder = new File(fileName);
                    final File[] files = folder.listFiles();

                    statusLine = httpVersion + " 200" + " OK" + CRLF;
                    contentTypeLine = contentType(".html") + CRLF;
                    entityBody =  "<html>"
                                + "<head><title>Arquivos do diretório " + fileName + " </title></head>" 
                                + "<body><h1>Listagem dos arquivos do diretório " + fileName + "</h1>";

                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile()) {
                            entityBody += "<br><a href=\""+ fileName + "/" + files[i].getName() +" \">" + files[i].getName() + "</a> ";
                        } else if (files[i].isDirectory()) {
                            entityBody += "<br><a href=\""+ fileName + "/" + files[i].getName() +" \">/" + files[i].getName() + "</a> ";
                        }
                    }       
                    
                    entityBody += "</body></html>";
                    os.writeBytes(statusLine);
                    os.writeBytes(contentTypeLine);
                    os.writeBytes(CRLF);
                    os.writeBytes(entityBody);
                } else {
                    statusLine = httpVersion + " 403" + CRLF;
                    contentTypeLine  = contentType(".htm") + CRLF;
                    entityBody  = "<html>";
                    entityBody += "<head><title>Forbidden</title></head>";
                    entityBody += "<body>O conteúdo do diretório não pode ser listado</body>";
                    entityBody += "</html>";
                                
                    
                    os.writeBytes(statusLine);
                    os.writeBytes(contentTypeLine);
                    os.writeBytes(CRLF);
                    os.writeBytes(entityBody);
                }
            }

        }           

        final Calendar cal = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        final String log = address + SPACE +
                sdf.format(cal.getTime()) + SPACE + 
                fileName + SPACE +
                bytes;

        System.out.println(log);
        writeLog(log);
        
    }

    // Processamento da requisição
    private void processRequest() throws Exception {
        final InputStream is = this.socket.getInputStream();
        final DataOutputStream os = new DataOutputStream(this.socket.getOutputStream());
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // Extração do nome do arquivo a linha de requisição.
        StringTokenizer tokens = new StringTokenizer(br.readLine());
        // Ignora o método, que deve ser "GET"
        tokens.nextToken();
        // Obtenção do nome do arquivo
        String fileName = tokens.nextToken(); 
        // Obtenção da versão do HTML
        final String httpVersion = tokens.nextToken();
        // Ajuste para que o arquivo seja buscado no diretório local
        fileName = "." + fileName;

        if (fileName.equals("./"))
            fileName += "";

        tokens = new StringTokenizer(br.readLine());

        // Ignora "Host:"
        tokens.nextToken();

        // Obtem o endereço e a porta de origem
        final String address = tokens.nextToken();
       /*  String authorization = null; */

        String headerLine = null;

        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        /* verifyAuthorization(authorization); */
        response(fileName, address, httpVersion, os);

        // Fechamento das cadeias e socket
        os.close();
        br.close();
        socket.close();     
    }
}