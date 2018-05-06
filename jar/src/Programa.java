import java.io.IOException;
import java.net.DatagramSocket;

public class Programa {

	public static void main(String[] args) throws InterruptedException, IOException {
	    int portaUDP = 5555;
	    int portaTCP = 12002;
        Client cliente = new Client(portaUDP, portaTCP);
        Thread threadCliente = new Thread(cliente);

        Server server = new Server(portaUDP, portaTCP);

        Runnable servidorUDP = () -> {
            try {
                server.criaConexaoUDP();
                server.trataConexaoUDP();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };
        Thread threadUDP = new Thread(servidorUDP);

        Runnable servidorTCP = () -> {
            try {
                server.criaConexaoTCP();
                server.trataConexaoTCP();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread threadTCP = new Thread(servidorTCP);

        System.out.println("## CLIENTE INICIADO ##");
        System.out.println("## SERVIDOR INICIADO ##");
        threadCliente.start();
        threadUDP.start();
        threadTCP.start();

		threadCliente.join();
		if (!threadCliente.isAlive()){
		    server.fechaLog();
		    threadTCP.interrupt();
		    threadUDP.interrupt();
		    System.exit(0);
        }
	}

}
