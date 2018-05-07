import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class Programa extends Application implements EventHandler<ActionEvent> {

    private final Object mutexTransf = new Object();
    private Button btnListarUsuarios;
    private Button btnListarArquivos;
    private Button btnBuscarArquivo;
    private Button btnTransferirArquivo;
    private Button btnVoltar;
    private TextInputDialog entradaTexto;
    private Button btnSair;
    private Stage janela;
    private Scene telaInicial, telaListaUsuarios, telaListaArq, telaBusca, telaTransf;
    private Server server;
    private Client cliente;
    private Thread serverThreadUDP, serverThreadTCP, threadCliente;
    private VBox layoutListaUsers, layoutListaArq, layoutBusca;
    private ListView listViewUsuarios, listViewArq, listViewBusca;
    private Alert alert, alertDownload;
    private String itemSelecionado;

	public static void main(String[] args) throws InterruptedException, IOException {
        // Inicia interface
        launch(args);
	}

	private void inicalizaThreads() throws IOException, InterruptedException {
        int portaUDP = 5555;
        int portaTCP = 12002;
        cliente = new Client(portaUDP, portaTCP, mutexTransf);
        threadCliente = new Thread(cliente);

        server = new Server(portaUDP, portaTCP);

        Runnable servidorUDP = () -> {
            try {
                server.criaConexaoUDP();
                server.trataConexaoUDP();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };
        serverThreadUDP = new Thread(servidorUDP);

        Runnable servidorTCP = () -> {
            try {
                server.criaConexaoTCP();
                server.trataConexaoTCP();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        serverThreadTCP = new Thread(servidorTCP);

        System.out.println("## CLIENTE INICIADO ##");
        System.out.println("## SERVIDOR INICIADO ##");
        threadCliente.start();
        serverThreadTCP.start();
        serverThreadUDP.start();
    }

    private void finalizaThreads() throws InterruptedException, IOException {
	    cliente.finalizaCliente();
        threadCliente.join();
        if (!threadCliente.isAlive()){
            System.out.println("Finalizando programa...");
            server.fechaLog();
            //server.fechaSockets();
            serverThreadUDP.interrupt();
            serverThreadTCP.interrupt();
            System.exit(0);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

	    inicalizaThreads();

	    janela = primaryStage;

        primaryStage.setTitle("Java P2P");
        btnListarUsuarios = new Button("Listar usuarios conectados");
        btnListarArquivos = new Button("Listar arquivos na rede");
        btnBuscarArquivo = new Button("Buscar um arquivo");
        btnTransferirArquivo = new Button("Transferir arquivo");
        btnSair = new Button("Sair");
        btnVoltar = new Button("Voltar");

        alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText("Arquivo não encontrado ou já existe em seu diretório!");

        alertDownload = new Alert(Alert.AlertType.INFORMATION);
        alertDownload.setTitle("Transferência de arquivo");
        alertDownload.setHeaderText(null);
        alertDownload.setContentText("Arquivo já existe em seu diretório!");

        entradaTexto = new TextInputDialog("");
        entradaTexto.setTitle("Buscar arquivo por nome");
        entradaTexto.setHeaderText(null);
        entradaTexto.setContentText("Nome do arquivo: ");


        btnListarUsuarios.setOnAction(this);
        btnListarArquivos.setOnAction(this);
        btnBuscarArquivo.setOnAction(this);
        btnTransferirArquivo.setOnAction(this);
        btnSair.setOnAction(this);
        btnVoltar.setOnAction(this);

        listViewUsuarios = new ListView();
        listViewArq = new ListView();
        listViewBusca = new ListView();

        listViewArq.setOnMouseClicked(event -> {
            itemSelecionado = (String) listViewArq.getSelectionModel().getSelectedItem();
        });
        listViewBusca.setOnMouseClicked(event -> {
            itemSelecionado = (String) listViewBusca.getSelectionModel().getSelectedItem();
        });

        Label lblUsuariosConectados = new Label("Usuários conectados:");
        Label lblListaArquivos = new Label("Lista de arquivos:");
        Label lblBuscaArquivo = new Label("Buscar arquivo:");

        layoutListaUsers = new VBox(20);
        layoutListaUsers.setAlignment(Pos.CENTER);
        layoutListaArq = new VBox(20);
        layoutListaArq.setAlignment(Pos.CENTER);
        layoutBusca = new VBox(20);
        layoutBusca.setAlignment(Pos.CENTER);

        layoutListaUsers.getChildren().add(lblUsuariosConectados);
        layoutListaArq.getChildren().add(lblListaArquivos);
        layoutBusca.getChildren().add(lblBuscaArquivo);


        VBox layoutTelaInicial = new VBox(20);
        layoutTelaInicial.setAlignment(Pos.CENTER);
        layoutTelaInicial.getChildren().addAll(btnListarUsuarios, btnListarArquivos, btnBuscarArquivo, btnSair);

        telaInicial = new Scene(layoutTelaInicial, 640, 480);
        telaListaUsuarios = new Scene(layoutListaUsers, 640, 480);
        telaListaArq = new Scene(layoutListaArq, 640, 480);
        telaBusca = new Scene(layoutBusca, 640, 480);
        primaryStage.setScene(telaInicial);
        primaryStage.show();
    }

    @Override
    public void handle(ActionEvent event) {
        synchronized (cliente) {
            cliente.notify();
        }
        if (event.getSource().equals(btnListarUsuarios)) {
            try {
                List<String> listaDeUsuarios = new ArrayList<>();
                cliente.listarUsuarios(listaDeUsuarios);
                listViewUsuarios.getItems().clear();
                for(String usuarioConectado : listaDeUsuarios){
                    listViewUsuarios.getItems().add(usuarioConectado);
                }
                if (!layoutListaUsers.getChildren().contains(listViewUsuarios))
                    layoutListaUsers.getChildren().add(listViewUsuarios);
                if (!layoutListaUsers.getChildren().contains(btnVoltar))
                    layoutListaUsers.getChildren().add(btnVoltar);
                janela.setScene(telaListaUsuarios);
                janela.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (event.getSource().equals(btnListarArquivos)){
            try {
                List<Mensagem> listaDeArquivos = new ArrayList<>();
                cliente.listarArquivos(listaDeArquivos);
                listViewArq.getItems().clear();
                for(Mensagem msg : listaDeArquivos){
                    String cliente = (String) msg.getParam("ip");
                    String arquivo = (String) msg.getParam("nomeArquivo");
                    listViewArq.getItems().add(cliente + "\t" + arquivo);
                }
                if (!layoutListaArq.getChildren().contains(listViewArq))
                    layoutListaArq.getChildren().add(listViewArq);
                if (!layoutListaArq.getChildren().contains(btnVoltar))
                    layoutListaArq.getChildren().add(btnVoltar);
                if (!layoutListaArq.getChildren().contains(btnTransferirArquivo))
                    layoutListaArq.getChildren().add(btnTransferirArquivo);
                janela.setScene(telaListaArq);
                janela.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (event.getSource().equals(btnBuscarArquivo)){
            try {
                List<String> listaDeArquivos = new ArrayList<>();
                Optional<String> result = entradaTexto.showAndWait();
                if (result.isPresent()){
                    cliente.buscarArquivo(result.get(), listaDeArquivos);
                }
                else return;
                if (listaDeArquivos.size() < 1)
                    alert.showAndWait();
                else{
                    listViewBusca.getItems().clear();
                    for(String arquivoEncontrado : listaDeArquivos){
                        listViewBusca.getItems().add(arquivoEncontrado);
                    }
                    if (!layoutBusca.getChildren().contains(listViewBusca))
                        layoutBusca.getChildren().add(listViewBusca);
                    if (!layoutBusca.getChildren().contains(btnVoltar))
                        layoutBusca.getChildren().add(btnVoltar);
                    if (!layoutBusca.getChildren().contains(btnTransferirArquivo))
                        layoutBusca.getChildren().add(btnTransferirArquivo);
                    janela.setScene(telaBusca);
                    janela.show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (event.getSource().equals(btnTransferirArquivo)){
            try {
               if (itemSelecionado != null){
                   String[] selec = itemSelecionado.split("\t");
                   String ipCliente, nomeArquivo;
                   boolean fimTransf = false;
                   if (selec.length > 0){
                       ipCliente = selec[0];
                       nomeArquivo = selec[1];
                       cliente.transferirArquivo(nomeArquivo);
                       List<Mensagem> listaTransf = cliente.getListaTransferencias();
                       Mensagem msgAux = null;
                       boolean encontrou = false;
                       for (Mensagem msg : listaTransf){
                           if (ipCliente.equals((String) msg.getParam("ip"))
                                   && nomeArquivo.equals((String) msg.getParam("nomeArquivo"))){
                               msgAux = msg;
                               synchronized (mutexTransf){
                                   alertDownload.setContentText("Transferência concluida com sucesso!");
                                   while (!(boolean) msg.getParam("concluido")){
                                       this.wait(300000); // Aguarda 5min no maximo
                                       if (!(boolean) msg.getParam("concluido")) {
                                           alert.setContentText("Ocorreu um erro na transferência");
                                       }
                                   }
                               }
                           }
                           break;
                       }
                       alertDownload.show();
                       if (msgAux != null)
                           cliente.removeTransferenciaDaLista(msgAux);
                   }
               }
               else{
                   Alert alertSelecao = new Alert(Alert.AlertType.WARNING);
                   alertSelecao.setTitle("Atenção!");
                   alertSelecao.setHeaderText(null);
                   alertSelecao.setContentText("Você precisa selecionar um arquivo para transferir!");
                   alertSelecao.show();
               }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if (event.getSource().equals(btnVoltar)){
            janela.setScene(telaInicial);
            janela.show();
        }
        else if (event.getSource().equals(btnSair)){
            try {
                finalizaThreads();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
