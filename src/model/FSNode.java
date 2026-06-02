package model;

import java.io.Serializable;

public abstract class FSNode implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String nome;
    protected transient Directory pai;
    protected long dataCriacao;
    protected long dataModificacao;

    public FSNode(String nome, Directory pai) {
        this.nome = nome;
        this.pai = pai;
        this.dataCriacao = System.currentTimeMillis();
        this.dataModificacao = this.dataCriacao;
    }

    public String getNome() { return nome; }

    public void setNome(String nome) {
        this.nome = nome;
        this.dataModificacao = System.currentTimeMillis();
    }

    public Directory getPai() { return pai; }

    public void setPai(Directory pai) {
        this.pai = pai;
    }

    public long getDataModificacao() { return dataModificacao; }

    public void atualizarData() {
        this.dataModificacao = System.currentTimeMillis();
    }

    public abstract long getTamanho();
    public abstract boolean ehDiretorio();

    public String getCaminho() {
        if (pai == null) return "/";
        String caminhoPai = pai.getCaminho();
        if (caminhoPai.equals("/")) return "/" + nome;
        return caminhoPai + "/" + nome;
    }
}
