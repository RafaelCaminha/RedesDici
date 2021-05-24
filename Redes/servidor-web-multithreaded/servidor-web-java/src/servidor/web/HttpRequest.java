/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidor.web;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author lanne
 */
public final class HttpRequest implements Runnable {
    private final static String CRLF = "\r\n";
    private Socket socket;

    // Construtor.
    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    // Implementa o método run () da Interface Runnable.
    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    // Retorno do MIME para o Content-Type do cabeçalho.
    
    private static String contentType(String filename) {
        
        // Para arquivos do tipo HTML.
        if (filename.endsWith(".htm") || filename.endsWith(".html")) {
            return "text/html";
        }

        // Para arquivos do tipo GIF.
        if (filename.endsWith(".gif")) {
            return "image/gif";
        }

        // Para arquivos do tipo PNG.
        if (filename.endsWith(".png")) {
            return "image/png";
        }

        // Para arquivos do tipo JPEG, que corresponde às extensões jpg e jpeg.
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        // default: application/octet-stream.
        return "application/octet-stream";

    }
        // Envio do arquivo solicitado.
    private static void sendBytes (FileInputStream fis, DataOutputStream os) throws Exception {
        
        // Constroe um Buffer de 1K para comportar os bytes no caminho para o socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia o arquivo solicitado dentro do fluxo de saída do socket.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }
    // Processamento da requisição
    private void processRequest() throws Exception {

        // Obtem uma referência aos fluxos de entrada e saída do socket.
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Configura os filtros do fluxo de entrada.
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Obtem a linha de solicitação da mensagem de requisição HTTP.
        String requestLine = br.readLine();

        // Exibe linha de requisição.
        System.out.println();
        System.out.println(requestLine);

        // Obtem e exibe as linhas do cabeçalho.
        String headerLine;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Extrai o nome do arquivo da linha de requisição.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        
        // Ignora o método, que deve ser "GET".
        tokens.nextToken(); 
        
        // Obtem o nome do arquivo.
        String fileName = tokens.nextToken(); 

        // Certifica-se de exibir a página de índice correta.
        if (fileName.equals("/")) {
            fileName = "/hello.html";
        }

        // Anexa um "." de modo que a requisição de arquivo esteja dentro do diretório atual.
        fileName = "." + fileName;

        // Abre o arquivo de solicitação.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false; // Se executar essa exceção, o arquivo não existe.
        }

        // Construção da mensagem de resposta.
        String statusLine = null; // Cabeçalho HTTP com código.
        String contentTypeLine = null; // Tipo do arquivo como HTML, JPG...
        String entityBody = null; // O conteúdo em si, seja os bytes ou o código HTML.
        
        // Se o arquivo existir, envia o código do cabeçalho 200 e exibe o arquivo.
        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF;
            contentTypeLine = "Tipo de arquivo: " + contentType(fileName) + CRLF;
        }
        
        // Caso contrário, informa um erro no HTML e exibe-o.
        else {
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;
            contentTypeLine = "Tipo de arquivo: text/html" + CRLF;
            entityBody = "<html>" +
                    "<head>" +
                    "<title>Not Found</title>" +
                    "</head>" +
                    "<body>Não encontrado</body>" +
                    "</html>";
        }

        // Envia a linha de status.
        os.writeBytes(statusLine);

        // Envia a linha do tipo de conteúdo.
        os.writeBytes(contentTypeLine);

        // Envia uma linha em branco para indicar o fim das linhas do cabeçalho.
        os.writeBytes(CRLF);

        // Envia o corpo da entidade.
        if (fileExists) {
            try {
                // Tenta enviar os dados, apenas se o arquivo existir.
                sendBytes(fis, os);
            } catch (Exception e) {
                statusLine = "HTTP/1.0 500 Internal Error" + CRLF;
                entityBody = "<html>" +
                        "<head>" +
                        "<title>Internal Error</title>" +
                        "</head>" +
                        "<body>" +
                        "<h1>Erro interno</h1>" +
                        "<p>" + e.toString() + "</p>" +
                        "</body>" +
                        "</html>";
                os.writeBytes(entityBody);
            } finally {
                // Fecha o fluxo de entrada do arquivo quando terminado.
                fis.close();
            }
        }
        else {
            // Escreve o código HTML "Não encontrado".
            os.writeBytes(entityBody);
        }

        // Fecha streams e socket.
        os.close();
        br.close();
        socket.close();
    }
}