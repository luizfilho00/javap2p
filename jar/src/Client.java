import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Client implements Runnable{

	public void conectar() throws UnknownHostException, IOException {
		DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);
		String msg = "Opa, sou o cliente.";
		byte[] buffer = msg.getBytes();

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), 5555);
		socket.send(packet);
		socket.close();
	}

	@Override
	public void run() {
		try {
			conectar();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
