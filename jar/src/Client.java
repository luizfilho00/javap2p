import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable{

    private HashSet<String> ipsConectados;
    private HashMap<String, String[]> listaArquivos;
    private DatagramSocket datagramSocket;
    private Socket socketTCP;

    public Client() throws SocketException {
        ipsConectados = new HashSet<>();
        listaArquivos = new HashMap<>();
        datagramSocket = new DatagramSocket();
        datagramSocket.setBroadcast(true);
    }

    private DatagramPacket enviaPacote(Mensagem msg) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput saidaObjeto = new ObjectOutputStream(bStream);
        saidaObjeto.writeObject(msg);
        saidaObjeto.close();

        byte[] serializedMsg = bStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(serializedMsg, serializedMsg.length,
                InetAddress.getByName("255.255.255.255"), 5555);
        datagramSocket.send(packet);

        return packet;
    }

    public void listarUsuarios() throws UnknownHostException, IOException {
        // Serialize to a byte array
        Mensagem msg = new Mensagem("listarUsuarios");
        DatagramPacket pacote = enviaPacote(msg);
        //Recebe resposta
        datagramSocket.receive(pacote);
        System.out.println("## CLIENTE ##\nUsuários conectados:");
        String ip = pacote.getAddress().getHostAddress();
        populaIpsConectados(ip);
        //datagramSocket.close();
    }

    //Cliente espera chegar algo na entrada = InputStream
    public Mensagem buscarArquivo(String nomeArquivo) throws IOException, ClassNotFoundException {
        if (arquivoJaExiste(nomeArquivo)){
            System.out.println("Arquivo já existe!");
            return null;
        }

        Mensagem msg = new Mensagem("buscarArquivo");
        msg.setParam("nomeArquivo", nomeArquivo);
        enviaPacote(msg);

        //Recebe resposta
        byte[] buf = new byte[4096];
        DatagramPacket pacote = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(pacote);
        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
        Mensagem resposta = (Mensagem)entrada.readObject();

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
            File diretorio = new File("rca2");
            if (!diretorio.exists()) diretorio.mkdir();

            String rcaPath = System.getProperty("user.dir") + "/rca2/";
            byte[] contents = new byte[10000];

            //Initialize the FileOutputStream to the output file's full path.
            FileOutputStream fos = new FileOutputStream(rcaPath + nomeComExtensao);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            InputStream is = socketTCP.getInputStream();

            //No of bytes read in one read() call
            int bytesRead = 0;

            while((bytesRead=is.read(contents))!=-1)
                bos.write(contents, 0, bytesRead);

            bos.flush();
            socketTCP.close();
            return true;
        }
    }

    private boolean arquivoJaExiste(String arquivoBuscado){
        File file = new File("rca");
        if (!file.exists()) return false;
        String arquivoNaPasta[] = file.list();
        for (String arq : arquivoNaPasta)
            if (arq.equals(arquivoBuscado))
                return true;
        return false;
    }

    public void listarArquivos() throws IOException, ClassNotFoundException {
        Mensagem msg = new Mensagem("listarArquivos");
        DatagramPacket pacote = enviaPacote(msg);
        //Recebe resposta
        datagramSocket.receive(pacote);
        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
        String[] listaDeArquivos = (String[]) entrada.readObject();
        System.out.println("## CLIENTE ##");
        if (listaDeArquivos.length < 1) System.out.println("Lista de arquivos vazia");
        populaListaArquivos(pacote.getAddress().getHostAddress(), listaDeArquivos);
        entrada.close();
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
        if (ipsConectados.contains(ip) || ip.equals("127.0.0.1")) {
            System.out.println("Ip ja existe na lista ou eh ip local");
        }else{
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
