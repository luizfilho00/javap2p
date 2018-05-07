import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Mensagem implements Serializable{
	
	private String operacao;
	private Map<String, Object> params;
	
	public Mensagem(String operacao){
		this.operacao = operacao;
		params = new HashMap<>();
	}
	
	public String getOperacao() {
		return operacao;
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
		m += "\nParametros:";
		
		for(String chave : params.keySet()) {
			m += "\n" + chave + ": " + params.get(chave);
		}
		
		return m;
	}
}
