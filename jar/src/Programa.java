import java.io.IOException;
import java.net.DatagramSocket;

public class Programa {

	public static void main(String[] args) throws InterruptedException {
        Client cliente = new Client(5555, 12002);
        Thread threadCliente = new Thread(cliente);

        Server server = new Server(5555, 12002);
        Thread threadServidor = new Thread(server);

        System.out.println("## CLIENTE INICIADO ##");
        System.out.println("## SERVIDOR INICIADO ##");
        threadCliente.start();
        threadServidor.start();

		threadCliente.join();
		if (!threadCliente.isAlive()){
		    threadServidor.interrupt();
		    System.exit(0);
        }
	}

}
