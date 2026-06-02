package shell;

import service.FileSystemSimulator;
import model.FSNode;
import java.util.Collection;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Shell {
    private FileSystemSimulator fs;
    private Scanner sc;

    public Shell(FileSystemSimulator fs) {
        this.fs = fs;
        this.sc = new Scanner(System.in);
    }

    public void iniciar() {
        System.out.println("==============================================");
        System.out.println("   SIMULADOR DE SISTEMA DE ARQUIVOS          ");
        System.out.println("==============================================");
        System.out.println("Digite 'help' para ver os comandos.");
        System.out.println("Digite 'crash' para simular uma falha.");
        System.out.println("Digite 'exit' para sair salvando o estado.");
        System.out.println("==============================================\n");

        while (true) {
            System.out.print("so-fs:" + fs.getCaminhoAtual() + "$ ");
            if (!sc.hasNextLine()) break;

            String linha = sc.nextLine().trim();
            if (linha.isEmpty()) continue;

            String[] tokens = linha.split("\\s+");
            String cmd = tokens[0].toLowerCase();
            String[] args = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, args, 0, args.length);

            try {
                switch (cmd) {
                    case "help":
                        System.out.println("Comandos:");
                        System.out.println("  ls [caminho]           - lista arquivos");
                        System.out.println("  cd <caminho>           - entra no diretorio");
                        System.out.println("  mkdir <nome>           - cria diretorio");
                        System.out.println("  touch <nome>           - cria arquivo vazio");
                        System.out.println("  write <arquivo> <text> - escreve no arquivo");
                        System.out.println("  cat <arquivo>          - mostra conteudo");
                        System.out.println("  rm <caminho>           - remove arquivo/dir");
                        System.out.println("  mv <origem> <destino>  - move ou renomeia");
                        System.out.println("  cp <origem> <destino>  - copia");
                        System.out.println("  checkpoint             - salva estado em disco");
                        System.out.println("  crash                  - simula falha abrupta");
                        System.out.println("  exit                   - salva e sai");
                        break;

                    case "ls":
                        String caminho = args.length > 0 ? args[0] : "";
                        Collection<FSNode> nos = fs.ls(caminho);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        System.out.printf("%-8s %-10s %-18s %s\n", "TIPO", "TAMANHO", "MODIFICADO", "NOME");
                        System.out.println("--------------------------------------------------");
                        for (FSNode no : nos) {
                            String tipo = no.ehDiretorio() ? "DIR" : "FILE";
                            String data = sdf.format(new Date(no.getDataModificacao()));
                            System.out.printf("%-8s %-10s %-18s %s\n", tipo, no.getTamanho() + "B", data, no.getNome());
                        }
                        break;

                    case "cd":
                        if (args.length < 1) { System.out.println("Uso: cd <caminho>"); break; }
                        fs.cd(args[0]);
                        break;

                    case "mkdir":
                        if (args.length < 1) { System.out.println("Uso: mkdir <nome>"); break; }
                        fs.mkdir(args[0], true);
                        System.out.println("Diretorio criado.");
                        break;

                    case "touch":
                        if (args.length < 1) { System.out.println("Uso: touch <nome>"); break; }
                        fs.write(args[0], "", true);
                        System.out.println("Arquivo criado.");
                        break;

                    case "write":
                        if (args.length < 1) { System.out.println("Uso: write <arquivo> <conteudo>"); break; }
                        String arq = args[0];
                        int idx = linha.indexOf(arq, 5) + arq.length();
                        String conteudo = idx < linha.length() ? linha.substring(idx).trim() : "";
                        fs.write(arq, conteudo, true);
                        System.out.println("Arquivo salvo.");
                        break;

                    case "cat":
                        if (args.length < 1) { System.out.println("Uso: cat <arquivo>"); break; }
                        System.out.println(fs.cat(args[0]));
                        break;

                    case "rm":
                        if (args.length < 1) { System.out.println("Uso: rm <caminho>"); break; }
                        fs.rm(args[0], true);
                        System.out.println("Removido.");
                        break;

                    case "mv":
                        if (args.length < 2) { System.out.println("Uso: mv <origem> <destino>"); break; }
                        fs.mv(args[0], args[1], true);
                        System.out.println("Movido/Renomeado.");
                        break;

                    case "cp":
                        if (args.length < 2) { System.out.println("Uso: cp <origem> <destino>"); break; }
                        fs.cp(args[0], args[1], true);
                        System.out.println("Copiado.");
                        break;

                    case "checkpoint":
                        fs.checkpoint();
                        System.out.println("Checkpoint salvo.");
                        break;

                    case "crash":
                        System.out.println("Simulando crash - encerrando sem salvar...");
                        System.exit(99);
                        break;

                    case "exit":
                        fs.checkpoint();
                        System.out.println("Estado salvo. Saindo...");
                        return;

                    default:
                        System.out.println("Comando desconhecido: " + cmd);
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }
}
