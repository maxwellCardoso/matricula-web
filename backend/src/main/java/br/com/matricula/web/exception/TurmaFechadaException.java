package br.com.matricula.web.exception;

public class TurmaFechadaException extends RuntimeException {

	private final String codigo;

	public TurmaFechadaException(String mensagem) {
		super(mensagem);
		this.codigo = "TURMA_FECHADA";
	}

	public String getCodigo() {
		return codigo;
	}
}
