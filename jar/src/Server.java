import java.io.*;
import java.net.*;

public class Server{

    private DatagramSocket serverSocketUDP;
    private ServerSocket serverSocketTCP;
    private byte[] buf = new byte[4096];

    public void criaConexaoUDP(int porta) throws IOException {
        serverSocketUDP = new DatagramSocket(porta);
    }


    public void criaConexaoTCP(int porta) throws IOException {
        serverSocketTCP = new ServerSocket(porta);
    }

    public void trataConexaoUDP() throws IOException, ClassNotFoundException {

        while (true) {
            System.out.println("Esperando conexao UDP...");
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                serverSocketUDP.receive(packet);
                System.out.println("Cliente UDP conectado!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
            Mensagem msg = (Mensagem) entrada.readObject();
            entrada.close();
            System.out.println("Recebido do ip: "+address.getHostAddress()+ ", operacao: "+ msg.getOperacao());

            if (msg.getOperacao().equals("listarUsuarios")) {
                buf = new byte[4096];
                listarUsuarios(packet);
            }
            if (msg.getOperacao().equals("listarArquivos")) {
                String[] listaArquivos = listarArquivos();
                //Cria um novo buffer com tamanho do Objeto a ser enviado
                buf = new byte[listaArquivos.length];
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
                packet = new DatagramPacket(objetoSerializado, objetoSerializado.length,
                        address, port);
                serverSocketUDP.send(packet);
            }
            if (msg.getOperacao().equals("buscarArquivo")) {
                buf = new byte[4096];
                String nomeArquivo = (String) msg.getParam("nomeArquivo");

                //Cria msg que encapsula resposta
                Mensagem resposta = buscarArquivo(nomeArquivo);

                String cliente = InetAddress.getByName("localhost").getHostAddress();
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
                packet = new DatagramPacket(objetoSerializado, objetoSerializado.length,
                        address, port);
                serverSocketUDP.send(packet);
            }
            buf = new byte[4096];
        }
    }

    public void trataConexaoTCP() throws IOException, ClassNotFoundException {
        while(true){
            System.out.println("Esperando conexao TCP...");
            Socket cliente = serverSocketTCP.accept();
            System.out.println("Cliente TCP conectado: " + cliente.getInetAddress().getHostAddress());
            ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
            ObjectOutputStream saida = new ObjectOutputStream(cliente.getOutputStream());

            Mensagem msg = (Mensagem) entrada.readObject();
            String nomeArquivo = (String) msg.getParam("nomeComExtensao");
            File arquivo = getArquivo(nomeArquivo);
            System.out.println("Nome arquivo: " +arquivo.getName() + " tamanho: " +arquivo.length());
            //saida.writeObject(arquivo);
            //saida.close();
            transfereArquivo(arquivo, cliente);
        }
    }

    private void transfereArquivo(File file, Socket socket) throws IOException {
        FileInputStream fileInput = new FileInputStream(file);
        BufferedInputStream buffInputStream = new BufferedInputStream(fileInput);
        DataOutputStream saida = new DataOutputStream(socket.getOutputStream());

        //Le o arquivo no buffer
        byte[] buffer;
        long tamanhoDoArquivo = file.length();
        long tamAux = 0;

        long start = System.nanoTime();
        while(tamAux != tamanhoDoArquivo){
            int size = 4096;
            if(tamanhoDoArquivo - tamAux >= size)
                tamAux += size;
            else{
                size = (int)(tamanhoDoArquivo - tamAux);
                tamAux = tamanhoDoArquivo;
            }
            buffer = new byte[size];
            buffInputStream.read(buffer, 0, size);
            saida.write(buffer);
            System.out.println("Enviando arquivo... "+(tamAux*100)/tamanhoDoArquivo+"% completo!");
        }
        long finish = System.nanoTime();
        saida.flush();

        socket.close();
        //serverSocketTCP.close();
        System.out.println("Arquivo enviado com sucesso!");
        System.out.println("Tempo gasto: " + (finish-start)/1000000 + "ms");
    }

    private synchronized File getArquivo(String nomeArquivo){
        File file = new File("rca");
        String[] arquivos = file.list();
        for(String arquivo : arquivos) {
            if (arquivo.contains(nomeArquivo)) {
                String workingDir = System.getProperty("user.dir");
                return new File (workingDir+"/rca/"+arquivo);
            }
        }
        return null;
    }

    public synchronized void listarUsuarios(DatagramPacket packet) throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        int port = packet.getPort();
        //System.out.println("Ip servidor: " +address.getHostAddress());
        packet = new DatagramPacket(buf, buf.length, address, port);
        serverSocketUDP.send(packet);
    }

    public synchronized String[] listarArquivos(){
        File file = new File("rca");
        if (!file.exists())
            file.mkdir();
        String[] arquivos = file.list();
        System.out.println("## SERVIDOR ##\nArquivos encontrados:");
        for(String arq : arquivos)
            System.out.println(arq);
        return arquivos;
    }

    public Mensagem buscarArquivo(String nomeArquivoProcurado){
        Mensagem mapArquivos = new Mensagem("detalharArquivo");
        mapArquivos.setParam("arquivoEncontrado", false);
        File file = new File("rca");
        File arqEncontrado;
        if (!file.exists()) return mapArquivos;
        String[] arquivos = file.list();
        int arquivosEncontrados = 0;
        for(String arquivo : arquivos) {
            if (arquivo.contains(nomeArquivoProcurado)) {
                String workingDir = System.getProperty("user.dir");
                arqEncontrado = new File (workingDir+"/rca/"+arquivo);
                mapArquivos.setParam("arquivoEncontrado", true);
                mapArquivos.setParam("nomeComExtensao", arquivo);
                mapArquivos.setParam("tamanhoArquivo", arqEncontrado.length());
            }
        }
        return mapArquivos;
    }

}
