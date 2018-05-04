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

    private void listaIpsConectados(){
        if (pacote == null){
            System.out.println("Ocorreu um erro.");
            return;
        }
        System.out.println(pacote.getAddress().getHostAddress());
    }

    @Override
    public void run() {
        switch(param){
            case "listarUsuarios":
                listaIpsConectados();
                break;
        }
    }
}
