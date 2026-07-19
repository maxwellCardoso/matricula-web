package br.com.matricula.web.exception;

public class VagaIndisponivelException extends RuntimeException {

	private final String codigo;

	public VagaIndisponivelException(String mensagem) {
		super(mensagem);
		this.codigo = "VAGA_INDISPONIVEL";
	}

	public String getCodigo() {
		return codigo;
	}
}
