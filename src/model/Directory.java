package model;

import java.util.HashMap;
import java.util.Collection;

public class Directory extends FSNode {
    private static final long serialVersionUID = 1L;

    private HashMap<String, FSNode> filhos;

    public Directory(String nome, Directory pai) {
        super(nome, pai);
        this.filhos = new HashMap<>();
    }

    public Collection<FSNode> getFilhos() {
        return filhos.values();
    }

    public FSNode getFilho(String nome) {
        return filhos.get(nome);
    }

    public void adicionarFilho(FSNode no) {
        filhos.put(no.getNome(), no);
        no.setPai(this);
        atualizarData();
    }

    public void removerFilho(String nome) {
        FSNode no = filhos.remove(nome);
        if (no != null) {
            no.setPai(null);
            atualizarData();
        }
    }

    @Override
    public long getTamanho() {
        long total = 0;
        for (FSNode f : filhos.values()) {
            total += f.getTamanho();
        }
        return total;
    }

    @Override
    public boolean ehDiretorio() {
        return true;
    }

    public void resolverPais(Directory pai) {
        this.pai = pai;
        for (FSNode filho : filhos.values()) {
            if (filho instanceof Directory) {
                ((Directory) filho).resolverPais(this);
            } else {
                filho.setPai(this);
            }
        }
    }
}
