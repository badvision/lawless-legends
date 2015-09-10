/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */
 
package org.badvision.outlaweditor.test;

import java.util.concurrent.CountDownLatch;
 
import javax.swing.SwingUtilities;
 
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
 
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
 
/**
 * A JUnit {@link Rule} for running tests on the JavaFX thread and performing
 * JavaFX initialization.  To include in your test case, add the following code:
 * 
 * <pre>
 * {@literal @}Rule
 * public JavaFXThreadingRule jfxRule = new JavaFXThreadingRule();
 * </pre>
 * 
 * @author Andy Till
 * 
 */
public class JavaFXThreadingRule implements TestRule {
    
    /**
     * Flag for setting up the JavaFX, we only need to do this once for all tests.
     */
    private static boolean jfxIsSetup;
 
    @Override
    public Statement apply(Statement statement, Description description) {
        
        return new OnJFXThreadStatement(statement);
    }
 
    private static class OnJFXThreadStatement extends Statement {
        
        private final Statement statement;
 
        public OnJFXThreadStatement(Statement aStatement) {
            statement = aStatement;
        }
 
        private Throwable rethrownException = null;
        
        @Override
        public void evaluate() throws Throwable {
            
            if(!jfxIsSetup) {
                setupJavaFX();
                
                jfxIsSetup = true;
            }
            
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            
            Platform.runLater(() -> {
                try {
                    statement.evaluate();
                } catch (Throwable e) {
                    rethrownException = e;
                }
                countDownLatch.countDown();
            });
            
            countDownLatch.await();
            
            // if an exception was thrown by the statement during evaluation,
            // then re-throw it to fail the test
            if(rethrownException != null) {
                throw rethrownException;
            }
        }
 
        protected void setupJavaFX() throws InterruptedException {
            
            long timeMillis = System.currentTimeMillis();
            
            final CountDownLatch latch = new CountDownLatch(1);
            
            SwingUtilities.invokeLater(() -> {
                // initializes JavaFX environment
                new JFXPanel();
                
                latch.countDown();
            });
            
            System.out.println("javafx initialising...");
            latch.await();
            System.out.println("javafx is initialised in " + (System.currentTimeMillis() - timeMillis) + "ms");
        }
        
    }
}