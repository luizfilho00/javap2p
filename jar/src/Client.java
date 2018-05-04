
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable{

    private HashSet<String> ipsConectados;
    private HashMap<String, String[]> listaArquivos;
    private DatagramSocket datagramSocket;
    private Socket socketTCP;

    public Client() throws SocketException {
        ipsConectados = new HashSet<>();
        listaArquivos = new HashMap<>();
    }

    /**
     * Cria um pacote do tipo DatagramPacket na porta 5555 com um buffer de tamanho 4096bytes
     * @return pacote com endereço de broadcast na porta 5555
     * @throws UnknownHostException
     */
    private DatagramPacket criaPacote() throws UnknownHostException {
        byte[] buffer = new byte[4096];
        return new DatagramPacket(buffer, buffer.length);
    }

    /**
     * Envia um pacote via broadcast para todos os servidores conectados na rede
     * @param msg = Mensagem que será encaminhada para o servidor através do pacote
     * @throws IOException
     */
    private void enviaPacoteBroadcast(Mensagem msg) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput saidaObjeto = new ObjectOutputStream(bStream);
        saidaObjeto.writeObject(msg);
        saidaObjeto.close();

        byte[] serializedMsg = bStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(serializedMsg, serializedMsg.length,
                InetAddress.getByName("255.255.255.255"), 5555);
        datagramSocket = new DatagramSocket();
        datagramSocket.setBroadcast(true);
        datagramSocket.send(packet);
    }

    /**
     * Envia requisição via broadcast solicitando ip de todos usuários conectados
     * @throws UnknownHostException
     * @throws IOException
     */
    public void listarUsuarios() throws UnknownHostException, IOException {
        Mensagem msg = new Mensagem("listarUsuarios");
        enviaPacoteBroadcast(msg);
        DatagramPacket pacoteResposta = criaPacote();
        System.out.println("### CLIENTE ###");
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

    //Cliente espera chegar algo na entrada = InputStream
    public Mensagem buscarArquivo(String nomeArquivo) throws IOException, ClassNotFoundException {
        if (arquivoJaExiste(nomeArquivo)){
            System.out.println("Arquivo já existe!");
            return null;
        }

        Mensagem msg = new Mensagem("buscarArquivo");
        msg.setParam("nomeArquivo", nomeArquivo);
        enviaPacoteBroadcast(msg);

        //Recebe resposta
        byte[] buf = new byte[4096];
        DatagramPacket pacote = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(pacote);
        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
        Mensagem resposta = (Mensagem)entrada.readObject();
        entrada.close();
        if ((boolean)resposta.getParam("arquivoEncontrado")){
            System.out.println("## CLIENTE ##");
            String cliente = (String) resposta.getParam("cliente");
            long tamanho = (long) resposta.getParam("tamanhoArquivo");
            System.out.println("Encontrado: " + cliente + ", tamanho: " + tamanho);
            return resposta;
        }
        else{
            System.out.println("Arquivo não encontrado na RCA!");
            return null;
        }
    }

    public boolean transferirArquivo(String nomeArquivo) throws IOException, ClassNotFoundException {
        Mensagem localizacao = buscarArquivo(nomeArquivo);
        if (localizacao == null){
            return false;
        }
        else{
            String ipCliente = (String) localizacao.getParam("cliente");
            String nomeComExtensao = (String) localizacao.getParam("nomeComExtensao");
            socketTCP = new Socket(ipCliente, 12002);
            ObjectOutputStream saida = new ObjectOutputStream(socketTCP.getOutputStream());
            Mensagem msg = new Mensagem("transferirArquivo");
            msg.setParam("nomeComExtensao", nomeComExtensao);
            saida.writeObject(msg);
            saida.flush();
//            File diretorio = new File("rca2");
//            if (!diretorio.exists()) diretorio.mkdir();

            String rcaPath = System.getProperty("user.dir") + "/rca/";
            byte[] contents = new byte[4096];

            //Initialize the FileOutputStream to the output file's full path.
            File file = new File(rcaPath, nomeComExtensao);
            FileOutputStream fileOutput = new FileOutputStream(file);
            DataInputStream inputStream = new DataInputStream(socketTCP.getInputStream());

            //No of bytes read in one read() call
            int bytesRead = 0;

            while((bytesRead = inputStream.read(contents)) != -1)
                fileOutput.write(contents, 0, bytesRead);
            fileOutput.close();
            inputStream.close();
            socketTCP.close();
            return true;
        }
    }

    private boolean arquivoJaExiste(String arquivoBuscado){
        File file = new File("rca");
        if (!file.exists()) return false;
        String arquivoNaPasta[] = file.list();
        for (String arq : arquivoNaPasta)
            if (arq.contains(arquivoBuscado))
                return true;
        return false;
    }

    public void listarArquivos() throws IOException, ClassNotFoundException {
//        Mensagem msg = new Mensagem("listarArquivos");
//        enviaPacoteBroadcast(msg);
//        //Recebe resposta
//        //datagramSocket.receive(pacote);
//        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
//        String[] listaDeArquivos = (String[]) entrada.readObject();
//        entrada.close();
//        System.out.println("## CLIENTE ##");
//        if (listaDeArquivos.length < 1) System.out.println("Lista de arquivos vazia");
//        populaListaArquivos(pacote.getAddress().getHostAddress(), listaDeArquivos);
//        entrada.close();
    }

    private synchronized void populaListaArquivos(String ip, String[] lista){
        listaArquivos.put(ip, lista);
        for(String cliente : listaArquivos.keySet()){
            String[] listaDoCliente = listaArquivos.get(cliente);
            for(String arquivo : listaDoCliente)
                System.out.println(cliente + " " + arquivo);
        }
    }

    private synchronized void populaIpsConectados(String ip){
        if (!ipsConectados.contains(ip)){
            ipsConectados.add(ip);
        }
    }

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
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
