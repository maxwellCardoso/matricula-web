package br.com.matricula.web.exception;

public class RecursoDuplicadoException extends RuntimeException {

	private final String codigo;

	public RecursoDuplicadoException(String codigo, String mensagem) {
		super(mensagem);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}
}
