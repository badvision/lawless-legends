package jace.cheat;

import javax.script.ScriptException;

import jace.core.RAMEvent;
import jace.core.RAMListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;

/**
 *
 * @author blurry
 */
public class DynamicCheat extends RAMListener {
    int id;
    IntegerProperty addr;
    StringProperty expression;
    BooleanProperty active;
    StringProperty name;
    String cheatName;
    Callback<RAMEvent, Integer> expressionCallback;

    public DynamicCheat(String cheatName, int address, String expr) {
        super(cheatName, RAMEvent.TYPE.ANY, RAMEvent.SCOPE.ADDRESS, RAMEvent.VALUE.ANY);
        id = (int) (Math.random() * 10000000);
        addr = new SimpleIntegerProperty(address);
        expression = new SimpleStringProperty(expr);
        active = new SimpleBooleanProperty(false);
        name = new SimpleStringProperty("Untitled");
        expression.addListener((param, oldValue, newValue) -> {
            expressionCallback = parseExpression(newValue);
        });
        expressionCallback = parseExpression(expr);
        doConfig();
    }

    @Override
    protected void doConfig() {
        if (addr != null) {
            setScopeStart(addr.getValue());
        }
    }

    @Override
    protected void doEvent(RAMEvent e) {
        if (active.get() && expressionCallback != null) {
            Integer newVal = expressionCallback.call(e);
            if (newVal != null) {
                e.setNewValue(newVal);
            } else {
                active.set(false);
                expressionCallback = null;
            }
        }
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public IntegerProperty addressProperty() {
        return addr;
    }

    public StringProperty expressionProperty() {
        return expression;
    }

    private Callback<RAMEvent, Integer> parseExpression(String expr) {
        String functionName = "processCheat" + id;
        String functionBody = "function " + functionName + "(old,val){" + (expr.contains("return") ? expr : "return " + expr) + "}";
        try {
            MetaCheat.NASHORN_ENGINE.eval(functionBody);
            return (RAMEvent e) -> {
                try {
                    Object result = MetaCheat.NASHORN_INVOCABLE.invokeFunction(functionName, e.getOldValue(), e.getNewValue());
                    if (result instanceof Number) {
                        return ((Number) result).intValue();
                    } else {
                        System.err.println("Not able to handle non-numeric return value: " + result.getClass());
                        return null;
                    }
                } catch (ScriptException | NoSuchMethodException ex) {
                    return null;
                }
            };
        } catch (ScriptException ex) {
            return null;
        }
    }

    public static String escape(String in) {
        return in.replaceAll(";", "~~").replaceAll("\n","\\n");
    }
    
    public static String unescape(String in) {
        return in.replaceAll("~~", ";").replaceAll("\\n", "\n");
    }
    
    public static final String DELIMITER = ";";
    public String serialize() {
        return escape(cheatName) + DELIMITER + escape(name.get()) + DELIMITER 
                + escape("$"+Integer.toHexString(addr.get())) + DELIMITER
                + escape(expression.get());
    }

    static public DynamicCheat deserialize(String in) {
        String[] parts = in.split(DELIMITER);
        String cheatName = unescape(parts[0]);
        String name = unescape(parts[1]);
        Integer addr = Integer.parseInt(parts[2].substring(1), 16);
        String expr = unescape(parts[3]);
        
        DynamicCheat out = new DynamicCheat(cheatName, addr, expr);
        out.name.set(name);
        return out;
    }
}
