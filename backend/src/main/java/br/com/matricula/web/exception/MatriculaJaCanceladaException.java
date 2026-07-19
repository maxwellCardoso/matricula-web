package br.com.matricula.web.exception;

public class MatriculaJaCanceladaException extends RuntimeException {

	private final String codigo;

	public MatriculaJaCanceladaException(String mensagem) {
		super(mensagem);
		this.codigo = "MATRICULA_JA_CANCELADA";
	}

	public String getCodigo() {
		return codigo;
	}
}
