import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class Server{
	
	private DatagramSocket serverSocket;
	private byte[] buf = new byte[256];
	private boolean running;
	
	public void criarServerSocket(int porta) throws IOException {
		serverSocket = new DatagramSocket(5555);
	}
	
	public DatagramSocket criaConexao() throws IOException {
		return this.serverSocket;
	}
	
	public void trataConexao(DatagramSocket socket) throws IOException {
		running = true;
		 
        while (running) {
        	System.out.println("Esperando conexao...");
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
				socket.receive(packet);
				System.out.println("Cliente conectado!");
			} catch (IOException e) {
				e.printStackTrace();
			}
             
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String received = new String(packet.getData(), 0, packet.getLength());
             
            if (received.equals("end")) {
                running = false;
                continue;
            }
            System.out.println("Recebido do ip: "+address+ ", mensagem: "+received);
            try {
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
            buf = new byte[256];
        }
        socket.close();
	}
}
