package jace.assembly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
public class AssemblyHandler implements LanguageHandler<ByteBuffer> {
    @Override
    public String getNewDocumentContent() {
        return "\t\t*= $300;\n\t\t!cpu 65c02;\n;--- Insert your code here ---\n";
    }    

    @Override
    public CompileResult<ByteBuffer> compile(Program proxy) {
        AcmeCompiler compiler = new AcmeCompiler();
        compiler.compile(proxy);
        return compiler;
    }

    public void compileToRam(String code) {
        HeadlessProgram prg = new HeadlessProgram(Program.DocumentType.assembly);
        prg.setValue(code);
        
        CompileResult<ByteBuffer> lastResult = compile(prg);
        if (lastResult.isSuccessful()) {
            Emulator.withComputer(c -> {
                RAM memory = c.getMemory();
                ByteBuffer input = lastResult.getCompiledAsset();
                input.rewind();
                int startLSB = input.get();
                int startMSB = input.get();
                int start = startLSB + startMSB << 8;
                System.out.printf("Storing assembled code to $%s%n", Integer.toHexString(start));
                c.getCpu().whileSuspended(() -> {
                    int pos = start;
                    while (input.hasRemaining()) {
                        memory.write(pos++, input.get(), false, true);
                    }
                });
            });
        }
    }
    
    @Override
    public void execute(CompileResult<ByteBuffer> lastResult) {
        if (lastResult.isSuccessful()) {
            Emulator.withComputer(c -> {
                RAM memory = c.getMemory();
                ByteBuffer input = lastResult.getCompiledAsset();
                input.rewind();
                int startLSB = input.get();
                int startMSB = input.get();
                int start = startLSB + startMSB << 8;
                System.out.printf("Issuing JSR to $%s%n", Integer.toHexString(start));
                c.getCpu().whileSuspended(() -> {
                    int pos = start;
                    while (input.hasRemaining()) {
                        memory.write(pos++, input.get(), false, true);
                    }
                    c.getCpu().JSR(start);
                });
            });
        }
        clean(lastResult);
    }

    @Override
    public void clean(CompileResult<ByteBuffer> lastResult) {
        // Nothing to do here
    }
}
