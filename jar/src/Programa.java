import java.io.IOException;
import java.net.DatagramSocket;

public class Programa {

	public static void main(String[] args) throws IOException{
		Server servidor = new Server();
		Runnable tServidorUDP = () -> {
			try {
				servidor.criaConexaoUDP(5555);
				servidor.trataConexaoUDP();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Conexao UDP finalizada");
		};
		new Thread(tServidorUDP).start();

		Runnable tServidorTCP = () -> {
			try {
				servidor.criaConexaoTCP(12002);
				servidor.trataConexaoTCP();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("Conexao TCP finalizada");
		};
		new Thread(tServidorTCP).start();

		Client clienteA = new Client();
		Thread tClientA = new Thread(clienteA);
		tClientA.start();
	}

}
