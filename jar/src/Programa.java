import java.io.IOException;
import java.net.DatagramSocket;

public class Programa {
	
	public static void main(String[] args) throws IOException {
		Server servidor = new Server();		
		servidor.criarServerSocket(5555);
		Runnable tServidor = () -> {
			while(true) {
				System.out.println("Servidor A - Aguardando conexão...");
				DatagramSocket socket;
				try {
					socket = servidor.criaConexao();
					servidor.trataConexao(socket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Cliente finalizado!");
			}
		};
		new Thread(tServidor).start();
		
		Client clienteA = new Client();
		Thread tClientA = new Thread(clienteA);
		tClientA.start();
	}

}
