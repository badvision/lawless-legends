package jace.assembly;

import jace.Emulator;
import jace.core.RAM;
import jace.ide.CompileResult;
import jace.ide.LanguageHandler;
import jace.ide.Program;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author blurry
 */
public class AssemblyHandler implements LanguageHandler<File> {
    @Override
    public String getNewDocumentContent() {
        return "\t\t*= $300;\n\t\t!cpu 65c02;\n;--- Insert your code here ---\n";
    }    

    @Override
    public CompileResult<File> compile(Program proxy) {
        AcmeCompiler compiler = new AcmeCompiler();
        compiler.compile(proxy);
        return compiler;
    }

    @Override
    public void execute(CompileResult<File> lastResult) {
        if (lastResult.isSuccessful()) {
                RAM memory = Emulator.getComputer().getMemory();
                try {
                    FileInputStream input = new FileInputStream(lastResult.getCompiledAsset());
                    int startLSB = input.read();
                    int startMSB = input.read();
                    int start = startLSB + startMSB << 8;
                    System.out.printf("Issuing JSR to $%s%n", Integer.toHexString(start));
                    Emulator.getComputer().getCpu().whileSuspended(() -> {
                        try {
                            int pos = start;
                            int next;
                            while ((next=input.read()) != -1) {
                                memory.write(pos++, (byte) next, false, true);
                            }
                            Emulator.getComputer().getCpu().JSR(start);
                        } catch (IOException ex) {
                            Logger.getLogger(AssemblyHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(AssemblyHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        clean(lastResult);
    }

    @Override
    public void clean(CompileResult<File> lastResult) {
        if (lastResult.getCompiledAsset() != null) {
            lastResult.getCompiledAsset().delete();
        }
    }
}
