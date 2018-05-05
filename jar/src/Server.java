import java.io.*;
import java.net.*;

public class Server{

    private DatagramSocket serverSocketUDP;
    private ServerSocket serverSocketTCP;
    private byte[] buf = new byte[4096];
    private int portaUDP, portaTCP;

    Server(int portaUDP, int portaTCP){
        this.portaUDP = portaUDP;
        this.portaTCP = portaTCP;
    }

    public void criaConexaoUDP(int porta) throws IOException {
        serverSocketUDP = new DatagramSocket(porta);
    }


    public void criaConexaoTCP(int porta) throws IOException {
        serverSocketTCP = new ServerSocket(porta);
    }

    public void trataConexaoUDP() throws IOException, ClassNotFoundException {

        while (true) {
            DatagramPacket pacote = new DatagramPacket(buf, buf.length);
            try {
                serverSocketUDP.receive(pacote);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = pacote.getAddress();
            int port = pacote.getPort();
            pacote = new DatagramPacket(buf, buf.length, address, port);
            ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
            Mensagem msg = (Mensagem) entrada.readObject();
            entrada.close();

            switch (msg.getOperacao()){
                case "listarUsuarios":
                    listarUsuarios(pacote);
                    break;
                case "getListaArquivos":
                    String[] listaArquivos = getListaArquivos();
                    listarArquivos(listaArquivos, pacote);
                    break;
                case "buscarArquivo":
                    String nomeArquivo = (String) msg.getParam("nomeArquivo");
                    enviaResultadoBusca(pacote, nomeArquivo);
                    break;
            }
        }
    }

    public void trataConexaoTCP() {
        while(true){
            Runnable threadUpload = () -> {
                try {
                    Socket socketCliente = serverSocketTCP.accept();
                    OutputStream saidaBytes = socketCliente.getOutputStream();

                    ObjectInputStream entradaObjeto = new ObjectInputStream(socketCliente.getInputStream());
                    Mensagem msg = (Mensagem) entradaObjeto.readObject();

                    File diretorio = new File("rca");
                    if (!diretorio.exists()) {
                        System.out.println("Diretorio excluido ou corrompido");
                        return;
                    }
                    String rcaPath = System.getProperty("user.dir") + "/rca/";

                    File arquivoParaEnvio = new File (rcaPath + msg.getParam("nomeComExtensao"));
                    byte[] buffer = new byte [(int)arquivoParaEnvio.length()];
                    FileInputStream entradaArquivo = new FileInputStream(arquivoParaEnvio);
                    BufferedInputStream entradaBytes = new BufferedInputStream(entradaArquivo);
                    entradaBytes.read(buffer, 0, buffer.length);
                    System.out.println("Enviando " + msg.getParam("nomeComExtensao") + "(" + buffer.length + " bytes)");
                    saidaBytes.write(buffer, 0, buffer.length);
                    saidaBytes.flush();
                    System.out.println("Enviado!");
                    entradaBytes.close();
                    saidaBytes.close();
                }
                catch (IOException | ClassNotFoundException ignored){}
            };
            threadUpload.run();
        }
    }

    private void listarUsuarios(DatagramPacket packet) throws IOException {
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        packet = new DatagramPacket(buf, buf.length, address, port);
        serverSocketUDP.send(packet);
    }

    private String[] getListaArquivos(){
        File file = new File("rca");
        if (!file.exists())
            file.mkdir();
        return file.list();
    }

    private void listarArquivos(String[] listaArquivos, DatagramPacket pacote) throws IOException {
        InetAddress address = pacote.getAddress();
        int port = pacote.getPort();

        //Cria um array de bytes output
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //Cria um object output para encapsular o objeto a ser enviado na rede
        ObjectOutput saidaObjeto = new ObjectOutputStream(byteArrayOutputStream);
        //Escreve o objeto na saidaObjeto
        saidaObjeto.writeObject(listaArquivos);
        saidaObjeto.close();

        //Cria um array de bytes com o objeto serializado pelo Array de bytes
        byte[] objetoSerializado = byteArrayOutputStream.toByteArray();

        //Cria um pacote com o objeto serializado e envia para quem pediu a lista de arquivos
        pacote = new DatagramPacket(objetoSerializado, objetoSerializado.length,
                address, port);
        serverSocketUDP.send(pacote);
    }

    private void enviaResultadoBusca(DatagramPacket pacote, String nomeArquivo) throws IOException {
        InetAddress address = pacote.getAddress();
        int port = pacote.getPort();

        //Cria msg que encapsula resposta
        Mensagem resposta = buscarArquivo(nomeArquivo);

        String cliente = pacote.getAddress().getHostAddress();
        resposta.setParam("cliente", cliente);

        //Cria um array de bytes output
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //Cria um object output para encapsular o objeto a ser enviado na rede
        ObjectOutput saidaObjeto = new ObjectOutputStream(byteArrayOutputStream);
        //Escreve o objeto na saidaObjeto
        saidaObjeto.writeObject(resposta);
        saidaObjeto.close();

        //Cria um array de bytes com o objeto serializado pelo Array de bytes
        byte[] objetoSerializado = byteArrayOutputStream.toByteArray();

        //Cria um pacote com o objeto serializado e envia para quem pediu a lista de arquivos
        pacote = new DatagramPacket(objetoSerializado, objetoSerializado.length,
                address, port);
        serverSocketUDP.send(pacote);
    }

    private Mensagem buscarArquivo(String nomeArquivoProcurado){
        Mensagem mapArquivos = new Mensagem("detalharArquivo");
        mapArquivos.setParam("arquivoEncontrado", false);
        File file = new File("rca");
        File arqEncontrado;
        if (!file.exists()) return mapArquivos;
        String[] arquivos = file.list();
        if (arquivos != null){
            for(String arquivo : arquivos) {
                if (arquivo != null){
                    if (arquivo.contains(nomeArquivoProcurado)) {
                        String workingDir = System.getProperty("user.dir");
                        arqEncontrado = new File (workingDir+"/rca/"+arquivo);
                        mapArquivos.setParam("arquivoEncontrado", true);
                        mapArquivos.setParam("nomeComExtensao", arquivo);
                        mapArquivos.setParam("tamanhoArquivo", arqEncontrado.length());
                    }
                }
            }
        }
        return mapArquivos;
    }
}
