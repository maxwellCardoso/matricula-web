package br.com.matricula.web.exception;

public class MatriculaDuplicadaException extends RuntimeException {

	private final String codigo;

	public MatriculaDuplicadaException(String mensagem) {
		super(mensagem);
		this.codigo = "MATRICULA_DUPLICADA";
	}

	public String getCodigo() {
		return codigo;
	}
}
