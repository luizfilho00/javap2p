import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable{

    private HashSet<String> ipsConectados;
    private HashMap<String, String[]> listaArquivos;
    private DatagramSocket datagramSocket;

    public Client() throws SocketException {
        ipsConectados = new HashSet<>();
        listaArquivos = new HashMap<>();
        datagramSocket = new DatagramSocket();
        datagramSocket.setBroadcast(true);
    }

    private DatagramPacket trataConexao(Mensagem msg) throws IOException {
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
        DatagramPacket packet = trataConexao(msg);
        //Recebe resposta
        datagramSocket.receive(packet);
        System.out.println("Recebeu resposta");
        String ip = packet.getAddress().getHostAddress();
        populaIpsConectados(ip);
        //datagramSocket.close();
    }

    //Cliente espera chegar algo na entrada = InputStream
    public void buscarArquivo(String nomeArquivo) throws IOException, ClassNotFoundException {
        //Socket socket = new Socket("localhost", 12002);
        Mensagem msg = new Mensagem("buscarArquivo");
        msg.setParam("nomeArquivo", nomeArquivo); //TODO Adaptar para buscar nome independente de extensão
        DatagramPacket packet = trataConexao(msg);
        //Recebe resposta
        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
        boolean arquivoEncontrado = (boolean)entrada.readObject();
        if (arquivoEncontrado){
            //TODO fazer alguma coisa aqui
        }
    }

    public void listarArquivos() throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 12002);
        Mensagem msg = new Mensagem("listarArquivos");
        ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
        saida.writeObject(msg);
        saida.flush();
        ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
        String[] listaDeArquivos = (String[]) entrada.readObject();
        if (listaDeArquivos.length < 1) System.out.println("Lista de arquivos vazia");
        populaListaArquivos(socket.getInetAddress().getHostName(), listaDeArquivos);
        entrada.close();
    }

    private synchronized void populaListaArquivos(String ip, String[] lista){
        listaArquivos.put(ip, lista);
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
                System.out.println("0 - Sair");
                BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in));
                comando = leitor.readLine();
                switch(comando){
                    case "1":
                        listarUsuarios();
                        break;
                    case "2":
                        String nomeArquivo = leitor.readLine();
                        buscarArquivo(nomeArquivo);
                        break;
                    case "3":
                        listarArquivos();
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
