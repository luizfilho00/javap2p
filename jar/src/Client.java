import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Client implements Runnable{

	private HashSet<String> ipsConectados = new HashSet<>();

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
        ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream());
        saida.writeObject(msg);
        saida.flush();
        //ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
        //boolean encontrouArquivo = (boolean)entrada.readObject();
        boolean encontrouArquivo = entrada.readBoolean();
        //socket.close();
        if (encontrouArquivo) System.out.println("Arquivo encontrado!");
        else System.out.println("Arquivo não encontrado :(");
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
			listarUsuarios();
			buscarArquivo("teste.pdf");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
