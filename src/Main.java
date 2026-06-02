import service.FileSystemSimulator;
import service.Journal;
import shell.Shell;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        new File("data").mkdirs();

        Journal journal = new Journal("data/journal.log");
        FileSystemSimulator fs = new FileSystemSimulator("data/filesystem.db", journal);

        fs.replayJournal();

        Shell shell = new Shell(fs);
        shell.iniciar();
    }
}
