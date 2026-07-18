package br.com.matricula.web.exception;

public class ExclusaoBloqueadaVinculoAtivoException extends RuntimeException {

	private final String codigo;

	public ExclusaoBloqueadaVinculoAtivoException(String mensagem) {
		super(mensagem);
		this.codigo = "EXCLUSAO_BLOQUEADA_VINCULO_ATIVO";
	}

	public String getCodigo() {
		return codigo;
	}
}
