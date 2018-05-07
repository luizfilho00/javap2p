
import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable{

    private DatagramSocket datagramSocket;
    private Socket socketTCP;
    private int portaUDP, portaTCP;
    private boolean fim = false;
    private Mensagem transferenciaArquivo;
    private List<Mensagem> listaTransferencias;
    private Object mutexTransf;

    Client(int portaUDP, int portaTCP, Object mutexTransf)  {
        this.mutexTransf = mutexTransf;
        this.listaTransferencias = new ArrayList<>();
        this.portaUDP = portaUDP;
        this.portaTCP = portaTCP;
    }

    public List<Mensagem> getListaTransferencias(){
        return listaTransferencias;
    }

    public void removeTransferenciaDaLista(Mensagem msg){
        listaTransferencias.remove(msg);
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
    public void listarUsuarios(List<String> listaDeClientes) throws UnknownHostException, IOException {
        Mensagem msg = new Mensagem("listarUsuarios");
        enviaPacoteBroadcast(msg);
        DatagramPacket pacoteResposta = criaPacote();
        System.out.println("Clientes conectados:");
        datagramSocket.setSoTimeout(1000); //1 segundo
        while(true) {
            try {
                datagramSocket.receive(pacoteResposta);
                ((Runnable) () -> {
                    synchronized (this) {
                        listaDeClientes.add(pacoteResposta.getAddress().getHostAddress());
                    }
                }).run();
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
                if (arq.equals(arquivoBuscado))
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
    public Mensagem buscarArquivo(String nomeArquivo, List<String> listaArquivosEncontrados) throws IOException {
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
        Mensagem arquivoDetalhado = new Mensagem("detalharArquivo");
        arquivoDetalhado.setParam("arquivoEncontrado", false);
        while(true) {
            try {
                datagramSocket.receive(pacoteResposta);
                Runnable threadBuscaArquivo = () -> {
                    try {
                        ObjectInputStream entrada = new
                                ObjectInputStream(new ByteArrayInputStream(pacoteResposta.getData()));
                        Mensagem msgResposta = (Mensagem) entrada.readObject();
                        if ((boolean)msgResposta.getParam("arquivoEncontrado")) {
                            String cliente = pacoteResposta.getAddress().getHostAddress();
                            long tamanho = (long) msgResposta.getParam("tamanhoArquivo");
                            synchronized (mutex){
                                arquivoDetalhado.setParam("arquivoEncontrado", true);
                                arquivoDetalhado.setParam("tamanhoArquivo", tamanho);
                                arquivoDetalhado.setParam("ipCliente", cliente);
                                arquivoDetalhado.setParam("nomeComExtensao",
                                        msgResposta.getParam("nomeComExtensao"));
                            }
                            if (listaArquivosEncontrados != null) {
                                listaArquivosEncontrados.add(cliente + "\t"
                                        + (String) msgResposta.getParam("nomeComExtensao") + "\t"
                                        + "tamanho: " + tamanho + " bytes");
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
        return arquivoDetalhado;
    }

    /**
     * Lista todos os arquivos encontrados na rede compartilhada
     * @throws IOException;
     */
    public void listarArquivos(List<Mensagem> listaArquivos) throws IOException {
        Mensagem msg = new Mensagem("getListaArquivos");
        enviaPacoteBroadcast(msg);

        DatagramPacket pacoteResposta = criaPacote();
        datagramSocket.setSoTimeout(1000);
        System.out.println("Arquivos encontrados na rede:");
        while(true) {
            try {
                datagramSocket.receive(pacoteResposta);
                ((Runnable) () -> {
                    try {
                        listarTodosArquivos(listaArquivos, pacoteResposta);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }).run();
            }catch (IOException se){
                break;
            }
        }
        datagramSocket.close();
    }

    private void listarTodosArquivos(List<Mensagem> list, DatagramPacket pacote)
            throws IOException, ClassNotFoundException {
        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
        String[] listaDeArquivos = (String[]) entrada.readObject();
        for (String arquivo : listaDeArquivos) {
            String cliente = pacote.getAddress().getHostAddress();
            System.out.println(cliente + " " + arquivo);
            Mensagem msg = new Mensagem("");
            msg.setParam("ip", cliente);
            msg.setParam("nomeArquivo", arquivo);
            list.add(msg);
        }
    }

        /**
         * Faz download de um arquivo da rede de compartilhamento
         * @param nomeArquivo = nome do arquivo a ser transferido
         * ou houve falha na transferência
         * @throws IOException;
         */
        public void transferirArquivo(String nomeArquivo) throws IOException {
            Mensagem arquivoDetalhado = buscarArquivo(nomeArquivo, null);

            File diretorio = new File("rca");
            if (!diretorio.exists())
                diretorio.mkdir();
            String rcaPath = System.getProperty("user.dir") + "/rca/";

            if (arquivoDetalhado == null) return;
            if (!(boolean)arquivoDetalhado.getParam("arquivoEncontrado"))
                return;

            String ipCliente = (String) arquivoDetalhado.getParam("ipCliente");
            final String nomeArquivoComExtensao = (String) arquivoDetalhado.getParam("nomeComExtensao");
            final long tamanhoDoArquivo = (long) arquivoDetalhado.getParam("tamanhoArquivo");
            socketTCP = new Socket(ipCliente, portaTCP);
            transferenciaArquivo = new Mensagem("");
            transferenciaArquivo.setParam("ip", ipCliente);
            transferenciaArquivo.setParam("nomeArquivo", nomeArquivoComExtensao);
            transferenciaArquivo.setParam("concluido", false);
            listaTransferencias.add(transferenciaArquivo);
            Runnable threadDownload = () -> {
                try {
                    transferirArquivo(nomeArquivoComExtensao, tamanhoDoArquivo, rcaPath, transferenciaArquivo);
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
        private void transferirArquivo(String arquivoComExtensao, long tamanhoDoArquivo, String rcaPath,
                Mensagem transferenciaArquivo) throws IOException {
            ObjectOutputStream saidaObjeto = new ObjectOutputStream(socketTCP.getOutputStream());
            Mensagem msg = new Mensagem("transferirArquivo");
            msg.setParam("nomeComExtensao", arquivoComExtensao);
            msg.setParam("tamanhoArquivo", tamanhoDoArquivo);
            saidaObjeto.writeObject(msg);
            saidaObjeto.flush();

            FileOutputStream writeFile = new FileOutputStream(rcaPath + arquivoComExtensao);
            BufferedOutputStream writeBuffer = new BufferedOutputStream(writeFile);
            InputStream entradaBytes = socketTCP.getInputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = entradaBytes.read(buffer)) > 0){
                writeBuffer.write(buffer, 0, count);
            }
            writeBuffer.flush();
            writeBuffer.close();
            writeFile.close();
            saidaObjeto.close();
            synchronized (mutexTransf){
                transferenciaArquivo.setParam("concluido", true);
                mutexTransf.notify();
            }
        }

        public void finalizaCliente() throws IOException {
            if (socketTCP != null)
                socketTCP.close();
            if (datagramSocket != null)
                datagramSocket.close();
            fim = true;
        }

        @Override
        public void run() {
            try {
                synchronized (this) {
                    while(true) {
                        this.wait();
                        break;
                    }
                }
                if (fim) return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
