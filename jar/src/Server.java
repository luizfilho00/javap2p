import java.io.*;
import java.net.*;
import java.util.List;

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

    public void trataConexaoUDP() throws IOException {

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
            String received = new String(packet.getData(), 0, packet.getLength());
            received = received.trim();
            System.out.println("Recebido do ip: "+address.getHostAddress()+ ", mensagem: "+received);

            if (received.equals("listarUsuarios")) {
                buf = new byte[4096];
                listarUsuarios(packet, serverSocketUDP);
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

            if (msg.getOperacao().equals("buscarArquivo")){
                boolean resultadoBusca = buscarArquivo((String)msg.getParam("nomeArquivo"));
                saida.writeBoolean(resultadoBusca);
                saida.close();
                cliente.close();
            }
            else if (msg.getOperacao().equals("listarArquivos")){
                String[] listaDeArquivos = listarArquivos();
                saida.writeObject(listaDeArquivos);
                saida.close();
                cliente.close();
            }
        }
    }

    public synchronized void listarUsuarios(DatagramPacket packet, DatagramSocket datagramSocket) throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        int port = packet.getPort();
        System.out.println("Ip servidor: " +address.getHostAddress());
        packet = new DatagramPacket(buf, buf.length, address, port);
        datagramSocket.send(packet);
    }

    public synchronized String[] listarArquivos(){
        File file = new File("rca");
        if (!file.exists())
            file.mkdir();
        String[] arquivos = file.list();
        System.out.println("Arquivos encontrados:");
        for(String arq : arquivos)
            System.out.println(arq);
        return arquivos;
    }

    public boolean buscarArquivo(String nomeArquivoProcurado){
        File file = new File("rca");
        if (!file.exists()) return false;
        String[] arquivos = file.list();
        for(String arquivo : arquivos) {
            if (arquivo.contains(nomeArquivoProcurado))
                return true;
        }
        return false;
    }

    public boolean transferirArquivo(String arquivo){
        //TODO transferir arquivo via tcp
        return false;
    }


}
