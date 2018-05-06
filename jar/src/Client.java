
import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable{

    private DatagramSocket datagramSocket;
    private Socket socketTCP;
    private int portaUDP, portaTCP;

    Client(int portaUDP, int portaTCP)  {
        this.portaUDP = portaUDP;
        this.portaTCP = portaTCP;
    }

    /**
     * Cria um pacote do tipo DatagramPacket na porta 5555 com um buffer de tamanho 4096bytes
     * @return pacote com endereço de broadcast na porta 5555
     */
    private DatagramPacket criaPacote() {
        byte[] buffer = new byte[4096];
        return new DatagramPacket(buffer, buffer.length);
    }

    /**
     * Envia um pacote via broadcast para todos os servidores conectados na rede
     * @param msg = Mensagem que será encaminhada para o servidor através do pacote
     * @throws IOException;
     */
    private void enviaPacoteBroadcast(Mensagem msg) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput saidaObjeto = new ObjectOutputStream(bStream);
        saidaObjeto.writeObject(msg);
        saidaObjeto.close();

        byte[] serializedMsg = bStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(serializedMsg, serializedMsg.length,
                InetAddress.getByName("255.255.255.255"), portaUDP);
        datagramSocket = new DatagramSocket();
        datagramSocket.setBroadcast(true);
        datagramSocket.send(packet);
    }

    /**
     * Envia requisição via broadcast solicitando ip de todos usuários conectados
     * @throws UnknownHostException;
     * @throws IOException;
     */
    private void listarUsuarios() throws UnknownHostException, IOException {
        Mensagem msg = new Mensagem("listarUsuarios");
        enviaPacoteBroadcast(msg);
        DatagramPacket pacoteResposta = criaPacote();
        System.out.println("Clientes conectados:");
        datagramSocket.setSoTimeout(1000); //Timeout == 1 segundo == 1000millisegundos
        while(true) {
            try {
                datagramSocket.receive(pacoteResposta);
                new TratamentoRequisicao(pacoteResposta, "listarUsuarios").run();
            }catch (IOException se){
                break;
            }
        }
        datagramSocket.close();
    }

    /**
     * Verifica se dado arquivo já existe no diretório compartilhado da rca
     * @param arquivoBuscado = nome do arquivo
     * @return true se arquivo já existe, false caso arquivo não exista
     */
    private boolean arquivoJaExiste(String arquivoBuscado){
        File file = new File("rca");
        if (!file.exists()) return false;
        String arquivoNaPasta[] = file.list();
        if (arquivoNaPasta != null){
            for (String arq : arquivoNaPasta)
                if (arq.contains(arquivoBuscado))
                    return true;
        }
        return false;
    }

    /**
     * Busca um arquivo na rede compartilhada e informa qual cliente possui o arquivo e seu tamanho em bytes
     * @param nomeArquivo = nome do arquivo à ser procurado
     * @return Mensagem contendo informações do arquivo e se este foi encontrado ou não
     * @throws IOException = Exceção caso operação IO ocorra falha
     */
    private HashMap<String, String> buscarArquivo(String nomeArquivo) throws IOException {
        HashMap<String, String> clientesPossuemArquivo = new HashMap<>();
        final Object mutex = new Object();
        if (arquivoJaExiste(nomeArquivo)){
            System.out.println("Arquivo \"" + nomeArquivo + "\" ja existe em seu diretorio");
            return null;
        }

        Mensagem msg = new Mensagem("buscarArquivo");
        msg.setParam("nomeArquivo", nomeArquivo);
        enviaPacoteBroadcast(msg);

        DatagramPacket pacoteResposta = criaPacote();
        datagramSocket.setSoTimeout(1000);
        System.out.println("Buscando arquivo \"" + nomeArquivo + "\" na rede...");
        while(true) {
            try {
                datagramSocket.receive(pacoteResposta);
                Runnable threadBuscaArquivo = () -> {
                    try {
                        ObjectInputStream entrada = new
                                ObjectInputStream(new ByteArrayInputStream(pacoteResposta.getData()));
                        Mensagem msgResposta = (Mensagem) entrada.readObject();
                        if ((boolean)msgResposta.getParam("arquivoEncontrado")) {
                            String cliente = (String) msgResposta.getParam("cliente");
                            long tamanho = (long) msgResposta.getParam("tamanhoArquivo");
                            synchronized (mutex){
                                clientesPossuemArquivo.put(cliente, (String)msgResposta.getParam("nomeComExtensao"));
                            }
                            System.out.println(cliente + ", tamanho: " + tamanho);
                        }
                    }
                    catch (IOException | ClassNotFoundException ignored) {}
                };
                threadBuscaArquivo.run();
            }catch (IOException io){
                break;
            }
        }
        datagramSocket.close();
        if (clientesPossuemArquivo.size() < 1) System.out.println("Arquivo \"" + nomeArquivo + "\" não existe na rede!");
        return clientesPossuemArquivo;
    }

    /**
     * Lista todos os arquivos encontrados na rede compartilhada
     * @throws IOException;
     */
    private void listarArquivos() throws IOException {
        Mensagem msg = new Mensagem("getListaArquivos");
        enviaPacoteBroadcast(msg);

        DatagramPacket pacoteResposta = criaPacote();
        datagramSocket.setSoTimeout(2000);
        System.out.println("Arquivos encontrados na rede:");
        while(true) {
            try {
                datagramSocket.receive(pacoteResposta);
                new TratamentoRequisicao(pacoteResposta, "getListaArquivos").run();
            }catch (IOException se){
                break;
            }
        }
        datagramSocket.close();
    }


    /**
     * Faz download de um arquivo da rede de compartilhamento
     * @param nomeArquivo = nome do arquivo a ser transferido
     * ou houve falha na transferência
     * @throws IOException;
     */
    private void transferirArquivo(String nomeArquivo) throws IOException {
        HashMap<String, String> clientesPossuemArquivo = buscarArquivo(nomeArquivo);

        File diretorio = new File("rca");
        if (!diretorio.exists())
            diretorio.mkdir();
        String rcaPath = System.getProperty("user.dir") + "/rca/";

        String ipCliente = null, arquivoComExtensao = null;
        if (clientesPossuemArquivo != null){
            for(String cliente : clientesPossuemArquivo.keySet()){
                String arquivo = clientesPossuemArquivo.get(cliente);
                ipCliente = cliente;
                arquivoComExtensao = arquivo;
                break;
            }
        }
        else{
            return;
        }

        if (arquivoComExtensao == null) {
            return;
        }

        final String finalArquivoExtensao = arquivoComExtensao;
        socketTCP = new Socket(ipCliente, portaTCP);
        Runnable threadDownload = () -> {
            try {
                transferirArquivo(finalArquivoExtensao, rcaPath);
            } catch (IOException ignored) {}
        };
        threadDownload.run();
    }

    /**
     * Método privado que trata da conexão com a rede para transferir o arquivo solicitado
     * @param arquivoComExtensao = Nome do arquivo com sua extensão
     * @param rcaPath = Caminho da pasta compartilhada entre os programas da rede
     * @throws IOException;
     */
    private void transferirArquivo(String arquivoComExtensao, String rcaPath) throws IOException {
        ObjectOutputStream saidaObjeto = new ObjectOutputStream(socketTCP.getOutputStream());
        Mensagem msg = new Mensagem("transferirArquivo");
        msg.setParam("nomeComExtensao", arquivoComExtensao);
        saidaObjeto.writeObject(msg);
        saidaObjeto.flush();

        FileOutputStream writeFile = new FileOutputStream(rcaPath + arquivoComExtensao);
        BufferedOutputStream writeBuffer = new BufferedOutputStream(writeFile);
        InputStream entradaBytes = socketTCP.getInputStream();
        byte[] buffer = new byte[6022386];
        int bytesRead = entradaBytes.read(buffer,0, buffer.length);
        int current = bytesRead;
        do {
            bytesRead = entradaBytes.read(buffer, current, (buffer.length-current));
            if(bytesRead >= 0) current += bytesRead;
        } while(bytesRead > -1);
        writeBuffer.write(buffer, 0 , current);

        writeBuffer.flush();
        writeFile.close();
        writeBuffer.close();
    }

    /**
     * Executa thread invocando o método correspondente à requisição desejada
     */
    @Override
    public void run() {
        try {
            String comando = "";
            while(!comando.equals("0")){
                System.out.println("######### Funcoes #########");
                System.out.println("1 - Listar usuarios conectados na rede");
                System.out.println("2 - Buscar arquivo pelo nome");
                System.out.println("3 - Listar todos os arquivos da RCA");
                System.out.println("4 - Transferir arquivo");
                System.out.println("0 - Sair");
                BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in));
                comando = leitor.readLine();
                String nomeArquivo;
                switch(comando){
                    case "1":
                        listarUsuarios();
                        break;
                    case "2":
                        nomeArquivo = leitor.readLine();
                        buscarArquivo(nomeArquivo);
                        break;
                    case "3":
                        listarArquivos();
                        break;
                    case "4":
                        nomeArquivo = leitor.readLine();
                        transferirArquivo(nomeArquivo);
                        break;
                    case "0":
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
