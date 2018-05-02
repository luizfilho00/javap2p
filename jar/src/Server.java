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
                break;
            }
            buf = new byte[4096];
        }
	}

	public void trataConexaoTCP() throws IOException, ClassNotFoundException {
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
        }else{
            System.out.println("Que porra recebeu nessa merda?");
        }
    }

    public synchronized void listarUsuarios(DatagramPacket packet, DatagramSocket datagramSocket) throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        int port = packet.getPort();
        System.out.println("Ip servidor: " +address.getHostAddress());
        packet = new DatagramPacket(buf, buf.length, address, port);
        datagramSocket.send(packet);
    }

    public boolean buscarArquivo(String arquivo){
        System.out.println("Chamado metodo para buscar aquivo: " + arquivo);
        return true;
    }

    public boolean transferirArquivo(String arquivo){
	    //TODO transferir arquivo via tcp
	    return false;
    }


}
