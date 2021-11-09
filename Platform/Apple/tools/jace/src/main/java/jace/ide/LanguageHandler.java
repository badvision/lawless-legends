package jace.ide;

public interface LanguageHandler<T> {

    String getNewDocumentContent();
    
    CompileResult<T> compile(Program program);

    void execute(CompileResult<T> lastResult);
    
    void clean(CompileResult<T> lastResult);
}