import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;

/**
 * Thread recebe um pacote do tipo DatagramPacket e um parametro que indica qual método deve ser invocado
 */
public class TratamentoRequisicao implements Runnable{

    private DatagramPacket pacote;
    private String param;

    public TratamentoRequisicao(DatagramPacket pacote, String param){
        this.pacote = pacote;
        this.param = param;
    }

    /**
     * Imprime ip do cliente que executa a thread
     */
    private void listarIpsConectados(){
        if (pacote == null){
            System.out.println("Ocorreu um erro.");
            return;
        }
        System.out.println(pacote.getAddress().getHostAddress());
    }

    /**
     * Lista todos arquivos da rede compartilhada
     */
    private void listarTodosArquivos() throws IOException, ClassNotFoundException {
        ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(pacote.getData()));
        String[] listaDeArquivos = (String[]) entrada.readObject();
        for(String arquivo : listaDeArquivos){
            String cliente = pacote.getAddress().getHostAddress();
            System.out.println(cliente + " " + arquivo);
        }
    }

    @Override
    public void run() {
        switch(param){
            case "listarUsuarios":
                listarIpsConectados();
                break;
            case "listarArquivos":
                try {
                    listarTodosArquivos();
                }
                catch (IOException | ClassNotFoundException e) {}
                break;
        }
    }
}
