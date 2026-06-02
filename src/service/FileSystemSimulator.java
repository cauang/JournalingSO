package service;

import model.FSNode;
import model.Directory;
import model.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.*;

public class FileSystemSimulator {
    private Directory raiz;
    private Directory dirAtual;
    private Journal journal;
    private String arquivoCheckpoint;

    public FileSystemSimulator(String arquivoCheckpoint, Journal journal) {
        this.arquivoCheckpoint = arquivoCheckpoint;
        this.journal = journal;
        carregarCheckpoint();
    }

    public Directory getDirAtual() { return dirAtual; }

    public String getCaminhoAtual() { return dirAtual.getCaminho(); }

    public void mkdir(String caminho, boolean log) throws Exception {
        if (log) {
            String abs = paraAbsoluto(caminho);
            long id = journal.proximoId();
            journal.registrarInicio(id, "mkdir", abs);
            criarDiretorio(caminho);
            journal.registrarCommit(id, "mkdir");
        } else {
            criarDiretorio(caminho);
        }
    }

    public void rm(String caminho, boolean log) throws Exception {
        if (log) {
            String abs = paraAbsoluto(caminho);
            long id = journal.proximoId();
            journal.registrarInicio(id, "rm", abs);
            remover(caminho);
            journal.registrarCommit(id, "rm");
        } else {
            remover(caminho);
        }
    }

    public void mv(String origem, String destino, boolean log) throws Exception {
        if (log) {
            long id = journal.proximoId();
            journal.registrarInicio(id, "mv", paraAbsoluto(origem), paraAbsoluto(destino));
            mover(origem, destino);
            journal.registrarCommit(id, "mv");
        } else {
            mover(origem, destino);
        }
    }

    public void cp(String origem, String destino, boolean log) throws Exception {
        if (log) {
            long id = journal.proximoId();
            journal.registrarInicio(id, "cp", paraAbsoluto(origem), paraAbsoluto(destino));
            copiar(origem, destino);
            journal.registrarCommit(id, "cp");
        } else {
            copiar(origem, destino);
        }
    }

    public void write(String caminho, String conteudo, boolean log) throws Exception {
        if (log) {
            String abs = paraAbsoluto(caminho);
            long id = journal.proximoId();
            journal.registrarInicio(id, "write", abs, conteudo);
            escreverArquivo(caminho, conteudo);
            journal.registrarCommit(id, "write");
        } else {
            escreverArquivo(caminho, conteudo);
        }
    }

    public void cd(String caminho) throws Exception {
        FSNode no = resolver(caminho);
        if (no == null) throw new Exception("Diretorio nao encontrado: " + caminho);
        if (!no.ehDiretorio()) throw new Exception("Nao e um diretorio: " + caminho);
        dirAtual = (Directory) no;
    }

    public Collection<FSNode> ls(String caminho) throws Exception {
        FSNode no = (caminho == null || caminho.isEmpty()) ? dirAtual : resolver(caminho);
        if (no == null) throw new Exception("Caminho nao encontrado: " + caminho);
        if (no.ehDiretorio()) return ((Directory) no).getFilhos();
        return Collections.singletonList(no);
    }

    public String cat(String caminho) throws Exception {
        FSNode no = resolver(caminho);
        if (no == null) throw new Exception("Arquivo nao encontrado: " + caminho);
        if (no.ehDiretorio()) throw new Exception("Nao e um arquivo: " + caminho);
        return ((model.File) no).getConteudo();
    }

    public void checkpoint() {
        salvarCheckpoint();
    }

    public void replayJournal() {
        List<Journal.Transacao> pendentes = journal.getTransacoesPendentes();
        if (pendentes.isEmpty()) return;

        System.out.println("\n[JOURNAL] " + pendentes.size() + " transacoes pendentes. Recuperando...");
        for (Journal.Transacao t : pendentes) {
            try {
                System.out.println("  Reexecutando: " + t);
                switch (t.operacao) {
                    case "mkdir": mkdir(t.args[0], false); break;
                    case "rm":    rm(t.args[0], false);    break;
                    case "mv":    mv(t.args[0], t.args[1], false); break;
                    case "cp":    cp(t.args[0], t.args[1], false); break;
                    case "write": write(t.args[0], t.args[1], false); break;
                }
            } catch (Exception e) {
                System.err.println("  Erro ao reexecutar tx" + t.id + ": " + e.getMessage());
            }
        }
        System.out.println("[JOURNAL] Recuperacao concluida. Salvando checkpoint...\n");
        salvarCheckpoint();
    }

    private FSNode resolver(String caminho) {
        if (caminho == null || caminho.isEmpty()) return dirAtual;
        FSNode atual = caminho.startsWith("/") ? raiz : dirAtual;
        for (String parte : caminho.split("/")) {
            if (parte.isEmpty() || parte.equals(".")) continue;
            if (parte.equals("..")) {
                if (atual.getPai() != null) atual = atual.getPai();
                continue;
            }
            if (!atual.ehDiretorio()) return null;
            FSNode filho = ((Directory) atual).getFilho(parte);
            if (filho == null) return null;
            atual = filho;
        }
        return atual;
    }

    private String[] separarCaminho(String caminho) {
        if (caminho.endsWith("/") && caminho.length() > 1)
            caminho = caminho.substring(0, caminho.length() - 1);
        int i = caminho.lastIndexOf('/');
        if (i == -1) return new String[]{".", caminho};
        if (i == 0) return new String[]{"/", caminho.substring(1)};
        return new String[]{caminho.substring(0, i), caminho.substring(i + 1)};
    }

    public String paraAbsoluto(String caminho) {
        if (caminho.startsWith("/")) {
            FSNode no = resolver(caminho);
            if (no != null) return no.getCaminho();
            String[] p = separarCaminho(caminho);
            FSNode pai = resolver(p[0]);
            if (pai != null) {
                String c = pai.getCaminho();
                return c.equals("/") ? "/" + p[1] : c + "/" + p[1];
            }
            return caminho;
        }
        String base = dirAtual.getCaminho();
        String combinado = base.equals("/") ? "/" + caminho : base + "/" + caminho;
        FSNode no = resolver(combinado);
        if (no != null) return no.getCaminho();
        String[] p = separarCaminho(combinado);
        FSNode pai = resolver(p[0]);
        if (pai != null) {
            String c = pai.getCaminho();
            return c.equals("/") ? "/" + p[1] : c + "/" + p[1];
        }
        return combinado;
    }

    private void criarDiretorio(String caminho) throws Exception {
        String[] p = separarCaminho(caminho);
        FSNode pai = resolver(p[0]);
        if (pai == null || !pai.ehDiretorio()) throw new Exception("Diretorio pai nao encontrado: " + p[0]);
        Directory dir = (Directory) pai;
        if (dir.getFilho(p[1]) != null) throw new Exception("Ja existe: " + p[1]);
        dir.adicionarFilho(new Directory(p[1], dir));
    }

    private void remover(String caminho) throws Exception {
        FSNode no = resolver(caminho);
        if (no == null) throw new Exception("Nao encontrado: " + caminho);
        if (no == raiz) throw new Exception("Nao pode remover a raiz.");
        String caminhoNo = no.getCaminho();
        String caminhoAtual = dirAtual.getCaminho();
        if (caminhoAtual.equals(caminhoNo) || caminhoAtual.startsWith(caminhoNo + "/"))
            throw new Exception("Nao pode remover o diretorio atual.");
        no.getPai().removerFilho(no.getNome());
    }

    private void mover(String origem, String destino) throws Exception {
        FSNode src = resolver(origem);
        if (src == null) throw new Exception("Origem nao encontrada: " + origem);
        if (src == raiz) throw new Exception("Nao pode mover a raiz.");

        FSNode dst = resolver(destino);
        Directory paiDestino;
        String nomeDestino;

        if (dst != null && dst.ehDiretorio()) {
            paiDestino = (Directory) dst;
            nomeDestino = src.getNome();
        } else {
            String[] p = separarCaminho(destino);
            FSNode pai = resolver(p[0]);
            if (pai == null || !pai.ehDiretorio()) throw new Exception("Destino invalido: " + p[0]);
            paiDestino = (Directory) pai;
            nomeDestino = p[1];
        }

        if (paiDestino.getFilho(nomeDestino) != null)
            throw new Exception("Ja existe no destino: " + nomeDestino);

        if (src.ehDiretorio()) {
            String sp = src.getCaminho();
            String dp = paiDestino.getCaminho();
            if (dp.equals(sp) || dp.startsWith(sp + "/"))
                throw new Exception("Nao pode mover para dentro de si mesmo.");
        }

        src.getPai().removerFilho(src.getNome());
        src.setNome(nomeDestino);
        paiDestino.adicionarFilho(src);
    }

    private void copiar(String origem, String destino) throws Exception {
        FSNode src = resolver(origem);
        if (src == null) throw new Exception("Origem nao encontrada: " + origem);

        FSNode dst = resolver(destino);
        Directory paiDestino;
        String nomeDestino;

        if (dst != null && dst.ehDiretorio()) {
            paiDestino = (Directory) dst;
            nomeDestino = src.getNome();
        } else {
            String[] p = separarCaminho(destino);
            FSNode pai = resolver(p[0]);
            if (pai == null || !pai.ehDiretorio()) throw new Exception("Destino invalido: " + p[0]);
            paiDestino = (Directory) pai;
            nomeDestino = p[1];
        }

        if (paiDestino.getFilho(nomeDestino) != null)
            throw new Exception("Ja existe no destino: " + nomeDestino);

        paiDestino.adicionarFilho(clonar(src, paiDestino, nomeDestino));
    }

    private FSNode clonar(FSNode no, Directory novoPai, String novoNome) {
        if (no instanceof model.File) {
            return new model.File(novoNome, novoPai, ((model.File) no).getConteudo());
        }
        Directory copia = new Directory(novoNome, novoPai);
        for (FSNode filho : ((Directory) no).getFilhos()) {
            copia.adicionarFilho(clonar(filho, copia, filho.getNome()));
        }
        return copia;
    }

    private void escreverArquivo(String caminho, String conteudo) throws Exception {
        String[] p = separarCaminho(caminho);
        FSNode pai = resolver(p[0]);
        if (pai == null || !pai.ehDiretorio()) throw new Exception("Diretorio pai nao encontrado.");
        Directory dir = (Directory) pai;
        FSNode existente = dir.getFilho(p[1]);
        if (existente != null) {
            if (existente.ehDiretorio()) throw new Exception(p[1] + " e um diretorio.");
            ((model.File) existente).setConteudo(conteudo);
        } else {
            dir.adicionarFilho(new model.File(p[1], dir, conteudo));
        }
    }

    private void carregarCheckpoint() {
        java.io.File f = new java.io.File(arquivoCheckpoint);
        if (!f.exists()) {
            raiz = new Directory("root", null);
            dirAtual = raiz;
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            raiz = (Directory) in.readObject();
            raiz.resolverPais(null);
            dirAtual = raiz;
        } catch (Exception e) {
            System.err.println("Falha ao carregar checkpoint: " + e.getMessage());
            raiz = new Directory("root", null);
            dirAtual = raiz;
        }
    }

    private void salvarCheckpoint() {
        java.io.File f = new java.io.File(arquivoCheckpoint);
        f.getParentFile().mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(raiz);
            journal.limpar();
        } catch (IOException e) {
            System.err.println("Erro ao salvar checkpoint: " + e.getMessage());
        }
    }
}
