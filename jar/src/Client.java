import java.io.*;
import java.net.*;
import java.util.*;

public class Client implements Runnable{

    private HashSet<String> ipsConectados = new HashSet<>();
    private HashMap<String, String[]> listaArquivos = new HashMap<String, String[]>();

    public void listarUsuarios() throws UnknownHostException, IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        String msg = "listarUsuarios";
        byte[] buffer = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName("255.255.255.255"), 5555);
        socket.send(packet);

        //Recebe resposta
        socket.receive(packet);
        System.out.println("Recebeu resposta");
        String ip = packet.getAddress().getHostAddress();
        populaIpsConectados(ip);
        socket.close();
    }

    //Cliente espera chegar algo na entrada = InputStream
    public void buscarArquivo(String nomeArquivo) throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 12002);
        Mensagem msg = new Mensagem("buscarArquivo");
        msg.setParam("nomeArquivo", nomeArquivo); //TODO Adaptar para buscar nome independente de extensão
        ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
        saida.writeObject(msg);
        saida.flush();
        ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
        boolean encontrouArquivo = entrada.readBoolean();
        if (encontrouArquivo) System.out.println("Arquivo encontrado!");
        else System.out.println("Arquivo não encontrado :(");
        entrada.close();
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
            System.out.println("Entre com a opcao desejada:");
            System.out.println("1 - Listar usuarios conectados na rede");
            System.out.println("2 - Buscar arquivo pelo nome");
            System.out.println("3 - Listar todos os arquivos da RCA");
            BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in));
            String comando = leitor.readLine();
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
                    System.out.println("Deu ruim");
                    break;

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
