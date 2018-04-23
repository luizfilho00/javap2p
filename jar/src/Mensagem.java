import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Mensagem implements Serializable{
	
	private String operacao;
	private Status status;
	private Map<String, Object> params;
	
	public Mensagem(String operacao){
		this.operacao = operacao;
		params = new HashMap<>();
	}
	
	public String getOperacao() {
		return operacao;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status s) {
		this.status = s;
	}
	
	public Object getParam(String chave) {
		return params.get(chave);
	}
	
	public void setParam(String chave, Object valor) {
		params.put(chave, valor);
	}
	
	@Override
	public String toString() {
		String m = "Operacao " + operacao;
		m += "\nStatus: " + status;
		m += "\nParametros:";
		
		for(String chave : params.keySet()) {
			m += "\n" + chave + ": " + params.get(chave);
		}
		
		return m;
	}
}
