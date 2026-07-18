package br.com.matricula.web.exception;

public class EntidadeNaoEncontradaException extends RuntimeException {

	private final String codigo;

	public EntidadeNaoEncontradaException(String mensagem) {
		super(mensagem);
		this.codigo = "ENTIDADE_NAO_ENCONTRADA";
	}

	public EntidadeNaoEncontradaException(String codigo, String mensagem) {
		super(mensagem);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}
}
