package edu.ufl.cise.plcsp23.ast;
import java.util.HashMap;
import java.util.Stack;

public class SymbolTable {
    private Stack<HashMap<String, NameDef>> symbolTableStack;

    public SymbolTable() {
        symbolTableStack = new Stack<HashMap<String, NameDef>>();
        symbolTableStack.push(new HashMap<String, NameDef>());
    }

    public void addSymbol(String name, NameDef def) {
        symbolTableStack.peek().put(name, def);
    }

    public NameDef getSymbol(String name) {
        for (int i = symbolTableStack.size() - 1; i >= 0; i--) {
            HashMap<String, NameDef> symbolTable = symbolTableStack.get(i);
            if (symbolTable.containsKey(name)) {
                return symbolTable.get(name);
            }
        }
        return null;
    }

    public void enterScope() {
        symbolTableStack.push(new HashMap<String, NameDef>());
    }

    public void exitScope() {
        symbolTableStack.pop();
    }

    public boolean insert(String name, Declaration declaration) {
    if (symbolTableStack.peek().containsKey(name)) {
            return false;
        }
        symbolTableStack.peek().put(name, declaration.getNameDef());
        return true;
    }
    public boolean insert(String name, NameDef namedef) {
        if (symbolTableStack.peek().containsKey(name)) {
            return false;
        }
        symbolTableStack.peek().put(name, namedef);
        return true;
    }
}

