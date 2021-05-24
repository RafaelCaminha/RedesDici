/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidor.web;

import java.net.*;

/**
 *
 * @author lanne
 */
public class WebServer {
    
    public static void main(String argv[]) throws Exception
    {
        // Define o número da porta.
        int port = 6789;

        System.out.println("Iniciando servidor web, ouvindo na porta " + port);

        // Estabelece o socket de escuta.
        ServerSocket listenSocket = new ServerSocket(port);

        // Processa requisições de serviço HTTP.
        while (true) 
       {
            // Escuta a requisição de conexão TCP.
            Socket connectionSocket = listenSocket.accept();

            // Constroe um objeto para processar a mensagem de requisição HTTP.
            HttpRequest request = new HttpRequest(connectionSocket);

            // Cria um novo thread para processar a requisição.
            Thread thread = new Thread(request);

            // Inicia o thread.
            thread.start();
        }
    }
}
