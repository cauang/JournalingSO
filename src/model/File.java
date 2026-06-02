package model;

public class File extends FSNode {
    private static final long serialVersionUID = 1L;

    private String conteudo;

    public File(String nome, Directory pai) {
        super(nome, pai);
        this.conteudo = "";
    }

    public File(String nome, Directory pai, String conteudo) {
        super(nome, pai);
        this.conteudo = conteudo == null ? "" : conteudo;
    }

    public String getConteudo() { return conteudo; }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo == null ? "" : conteudo;
        atualizarData();
    }

    @Override
    public long getTamanho() {
        return conteudo.getBytes().length;
    }

    @Override
    public boolean ehDiretorio() {
        return false;
    }
}
