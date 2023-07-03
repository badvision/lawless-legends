package jace.assembly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jace.Emulator;
import jace.core.RAM;
import jace.ide.CompileResult;
import jace.ide.HeadlessProgram;
import jace.ide.LanguageHandler;
import jace.ide.Program;

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

    public void compileToRam(String code) {
        HeadlessProgram prg = new HeadlessProgram(Program.DocumentType.assembly);
        prg.setValue(code);
        
        CompileResult<File> lastResult = compile(prg);
        if (lastResult.isSuccessful()) {
            Emulator.withComputer(c -> {
                RAM memory = c.getMemory();
                try {
                    FileInputStream input = new FileInputStream(lastResult.getCompiledAsset());
                    int startLSB = input.read();
                    int startMSB = input.read();
                    int start = startLSB + startMSB << 8;
                    System.out.printf("Storing assembled code to $%s%n", Integer.toHexString(start));
                    c.getCpu().whileSuspended(() -> {
                        try {
                            int pos = start;
                            int next;
                            while ((next=input.read()) != -1) {
                                memory.write(pos++, (byte) next, false, true);
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(AssemblyHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(AssemblyHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }
    
    @Override
    public void execute(CompileResult<File> lastResult) {
        if (lastResult.isSuccessful()) {
            Emulator.withComputer(c -> {
                RAM memory = c.getMemory();
                try {
                    FileInputStream input = new FileInputStream(lastResult.getCompiledAsset());
                    int startLSB = input.read();
                    int startMSB = input.read();
                    int start = startLSB + startMSB << 8;
                    System.out.printf("Issuing JSR to $%s%n", Integer.toHexString(start));
                    c.getCpu().whileSuspended(() -> {
                        try {
                            int pos = start;
                            int next;
                            while ((next=input.read()) != -1) {
                                memory.write(pos++, (byte) next, false, true);
                            }
                            c.getCpu().JSR(start);
                        } catch (IOException ex) {
                            Logger.getLogger(AssemblyHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(AssemblyHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
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
