package service;

import java.io.*;
import java.util.*;
import java.util.Base64;

public class Journal {
    private File arquivo;
    private long proximoId = 1;

    public Journal(String caminho) {
        arquivo = new File(caminho);
        try {
            if (!arquivo.exists()) {
                arquivo.getParentFile().mkdirs();
                arquivo.createNewFile();
            } else {
                List<Transacao> lista = lerLog();
                if (!lista.isEmpty()) {
                    proximoId = lista.get(lista.size() - 1).id + 1;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir journal: " + e.getMessage());
        }
    }

    public long proximoId() {
        return proximoId++;
    }

    private String encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    private String decode(String s) {
        return new String(Base64.getDecoder().decode(s));
    }

    public void registrarInicio(long id, String operacao, String... args) {
        StringBuilder linha = new StringBuilder(id + "|START|" + operacao);
        for (String a : args) {
            linha.append("|").append(encode(a));
        }
        escreverLinha(linha.toString());
    }

    public void registrarCommit(long id, String operacao) {
        escreverLinha(id + "|COMMIT|" + operacao);
    }

    private void escreverLinha(String linha) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(arquivo, true))) {
            pw.println(linha);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no journal: " + e.getMessage());
        }
    }

    public void limpar() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(arquivo, false))) {
            pw.print("");
            proximoId = 1;
        } catch (IOException e) {
            System.err.println("Erro ao limpar journal: " + e.getMessage());
        }
    }

    public List<Transacao> lerLog() {
        List<Transacao> lista = new ArrayList<>();
        Map<Long, Transacao> mapa = new LinkedHashMap<>();

        if (!arquivo.exists()) return lista;

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split("\\|");
                if (partes.length < 3) continue;

                long id = Long.parseLong(partes[0]);
                String tipo = partes[1];
                String operacao = partes[2];

                if (tipo.equals("START")) {
                    String[] args = new String[partes.length - 3];
                    for (int i = 3; i < partes.length; i++) {
                        args[i - 3] = decode(partes[i]);
                    }
                    mapa.put(id, new Transacao(id, operacao, args));
                } else if (tipo.equals("COMMIT")) {
                    if (mapa.containsKey(id)) {
                        mapa.get(id).commitado = true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler journal: " + e.getMessage());
        }

        lista.addAll(mapa.values());
        return lista;
    }

    public List<Transacao> getTransacoesPendentes() {
        List<Transacao> pendentes = new ArrayList<>();
        for (Transacao t : lerLog()) {
            if (t.commitado) pendentes.add(t);
        }
        return pendentes;
    }

    public static class Transacao {
        public long id;
        public String operacao;
        public String[] args;
        public boolean commitado;

        public Transacao(long id, String operacao, String[] args) {
            this.id = id;
            this.operacao = operacao;
            this.args = args;
            this.commitado = false;
        }

        public String toString() {
            return "Tx#" + id + " " + operacao + " " + Arrays.toString(args);
        }
    }
}
