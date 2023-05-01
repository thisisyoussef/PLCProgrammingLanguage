package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.ast.Dimension;

import java.util.*;

public class CodeGenerator implements ASTVisitor {
String str;

SymbolTable thisProgram = new SymbolTable();

public int scope = 0;

Set<String> nameSet = new HashSet<>();

CodeGenerator() {
}

CodeGenerator(String inpStr) {
   str = inpStr;
}

public HashMap<String, Type> table = new HashMap<>();

public static class SymbolTable {

   //HashMap called "symTabMap" to store symbols and their scope
   public HashMap<String, Stack<Integer>> symTabMap = new HashMap<>();

   // Method to enter a new scope for a symbol in the symbol table
   void enterScope(String name, int scope) {
      Stack s;
      // Check if the symbol already exists in the symbol table
      s = symTabMap.containsKey(name) ? symTabMap.get(name) : new Stack<>();
      s.push(scope);
      // Update the symbol table with the new symbol and its corresponding scope
      symTabMap.put(name, s);
   }

   void closeScope(int scope) {
      // Iterate through each symbol in the symbol table
      Iterator<Map.Entry<String, Stack<Integer>>> iterator = symTabMap.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<String, Stack<Integer>> entry = iterator.next();
         Stack<Integer> getStack = entry.getValue();
         // If the symbol's current scope matches the given scope, remove it from the symbol table
         if (getStack.peek() == scope) {
            getStack.pop();
         }
      }
   }
}


/**
 * It starts with a switch statement that processes the left side of the assignment statement. 
 * If the left side is a PIXEL object, it appends the result of visiting the expression on the right side to a StringBuilder, localSB, and returns it. 
 * If the left side is an IMAGE object, the method checks if there is a pixel selector and a color channel or only a pixel selector or neither. 
 * If neither is present, it appends code to localSB to create a new image based on the right-hand side expression and copy it into the left-hand side image
 */

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        StringBuilder localSB = new StringBuilder();
        switch(table.get(statementAssign.getLv().getIdent().getName())) {
            case PIXEL:
                localSB.append(statementAssign.getE().visit(this, arg).toString());
                return localSB;
            case IMAGE:
                PixelSelector pS = statementAssign.getLv().getPixelSelector();
                ColorChannel cC = statementAssign.getLv().getColor();
                String returnString = "";
                if (pS == null && cC == null) {
                    switch(statementAssign.getE().getType()) {
                        case STRING:
                            localSB.append("ImageOps.copyInto(FileURLIO.readImage(").append(statementAssign.getE().visit(this, arg).toString()).append("), ").append(statementAssign.getLv().visit(this, arg)).append(")");
                            break;
                        case IMAGE:
                            localSB.append("ImageOps.copyInto(").append(statementAssign.getE().visit(this, arg).toString()).append(", ").append(statementAssign.getLv().visit(this, arg)).append(")");
                            break;
                        case PIXEL:
                            localSB.append("ImageOps.setAllPixels(").append(statementAssign.getLv().visit(this, arg).toString()).append(", ").append(statementAssign.getE().visit(this, arg)).append(")");
                            break;
                    }
                }
                else if (pS != null && cC != null) {
                    String Color = cC.toString();
                    Color = Color.substring(0,1).toUpperCase() + Color.substring(1).toLowerCase();
                    returnString = "for (int y = 0; y != " + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek() + ".getHeight(); y ++) { \n";
                    returnString += "for (int x = 0; x != " + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek() + ".getWidth(); x++) { \n";
                    returnString += "ImageOps.setRGB(" + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek() + ", x, y, PixelOps.set" + Color + "(ImageOps.getRGB(" + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek();
                    returnString += ", x, y), " + statementAssign.getE().visit(this, arg) + "));\n}\n}";
                    return returnString;
                }
                else if (pS != null && cC == null) {
                    returnString = "for (int y = 0; y != " + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek() + ".getHeight(); y ++) { \n";
                returnString += "for (int x = 0; x != " + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek() + ".getWidth(); x++) { \n";
                returnString += "ImageOps.setRGB(" + statementAssign.getLv().getIdent().getName() + thisProgram.symTabMap.get(statementAssign.getLv().getIdent().getName()).peek() + ", x, y, " + statementAssign.getE().visit(this, arg) + ");\n}\n}";
                return returnString;
                }
                return localSB;
        }
        localSB.append(statementAssign.getLv().visit(this, arg));
        localSB.append(" = ");

        switch (statementAssign.getE().getClass().getSimpleName()) {
            case "BinaryExpr":
                IToken.Kind operator = ((BinaryExpr) statementAssign.getE()).getOp();
                switch (operator) {
                    case OR:
                    case AND:
                    case LT:
                    case GT:
                    case GE:
                    case LE:
                    case EQ:
                        localSB.append(statementAssign.getE().visit(this, arg));
                        localSB.append(" == false ? 0 : 1");
                        break;
                    default:
                        localSB.append(statementAssign.getE().visit(this, arg));
                        break;
                }
                break;
            default:
                localSB.append(statementAssign.getE().visit(this, arg));
                break;
        }
        return localSB;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        String leftString = binaryExpr.getLeft().visit(this, arg).toString();
        Type rT = binaryExpr.getRight().getType();
        Type lT = binaryExpr.getLeft().getType();
        StringBuilder bStringBuilder = new StringBuilder();
        bStringBuilder.append("(");
        BinaryExpr oL = null;
        switch (binaryExpr.getOp()) {
            case AND:
            case OR:
                if (binaryExpr.getLeft().getClass() == BinaryExpr.class) {
                    oL = (BinaryExpr) binaryExpr.getLeft();
                    switch (oL.getOp()) {
                        case OR:
                        case AND:
                        case LT:
                        case GT:
                        case GE:
                        case LE:
                        case EQ:
                            bStringBuilder.append(leftString);
                            break;
                        default:
                            bStringBuilder.append("(" + leftString + " == 0 ? false : true)");
                            break;
                    }
                } else {
                    bStringBuilder.append("(" + leftString + " == 0 ? false : true)");
                }
                break;
            default:
                if (binaryExpr.getLeft().getClass() == BinaryExpr.class) {
                    oL = (BinaryExpr) binaryExpr.getLeft();
                    switch (oL.getOp()) {
                        case OR:
                        case AND:
                        case LT:
                        case GT:
                        case GE:
                        case LE:
                        case EQ:
                            bStringBuilder.append(leftString);
                            break;
                        default:
                            bStringBuilder.append("(" + leftString + "? 1 : 0)");
                            break;
                    }
                } else {
                    bStringBuilder.append(leftString);
                }
                break;
        }
        String beOPStr = "";
        IToken.Kind bEOP = binaryExpr.getOp();
        if (bEOP == IToken.Kind.EQ) {
            beOPStr = "==";
        }
        else if (bEOP == IToken.Kind.GT) {
            beOPStr = ">";
        }
        else if (bEOP == IToken.Kind.MOD) {
            beOPStr = "%";
        }
        else if (bEOP == IToken.Kind.PLUS) {
            beOPStr = "+";
        }
        else if (bEOP == IToken.Kind.LT) {
            beOPStr = "<";
        }
        else if (bEOP == IToken.Kind.AND) {
            beOPStr = "&&";
        }
        else if (bEOP == IToken.Kind.OR) {
            beOPStr = "||";
        }
        else if (bEOP == IToken.Kind.MINUS) {
            beOPStr = "-";
        }
        else if (bEOP == IToken.Kind.TIMES) {
            beOPStr = "*";
        }
        else if (bEOP == IToken.Kind.BITAND) {
            beOPStr = "&";
        }
        else if (bEOP == IToken.Kind.BITOR) {
            beOPStr = "|";
        }
        else if (bEOP == IToken.Kind.EXP) {
            beOPStr = "**";
        }
        else if (bEOP == IToken.Kind.GE) {
            beOPStr = ">=";
        }
        else if (bEOP == IToken.Kind.LE) {
            beOPStr = "<=";
        }
        else if (bEOP == IToken.Kind.DIV) {
            beOPStr = "/";
        }
        bStringBuilder.append(beOPStr);
        String rStr = binaryExpr.getRight().visit(this, arg).toString();
        switch (binaryExpr.getOp()) {
            case AND:
            case OR:
                if (binaryExpr.getRight() instanceof BinaryExpr opRight
                        && !(Set.of(IToken.Kind.AND, IToken.Kind.OR, IToken.Kind.LT, IToken.Kind.GT, IToken.Kind.GE, IToken.Kind.LE, IToken.Kind.EQ).contains(opRight.getOp()))) {
                    bStringBuilder.append("(").append(rStr).append(" == 0 ? false : true)");
                } else {
                    bStringBuilder.append(rStr);
                }
                break;
            default:
                bStringBuilder.append(rStr);
                break;
        }

        bStringBuilder.append(")");
        if (beOPStr == "**") {
            String change = bStringBuilder.toString();
            if (change.charAt(0) == '(' && change.charAt(change.length() - 1) == ')') {
                change = change.substring(1, change.length() - 1);
            }
            String[] operands = change.split("\\*\\*");
            if (operands.length == 2) {
                int base = Integer.parseInt(operands[0].trim());
                int exponent = Integer.parseInt(operands[1].trim());
                int result = (int) Math.pow(base, exponent);
                return Integer.toString(result);
            } else {
                return change;
            }
        }
        //check if lT is null
        if (lT == null) {
            return bStringBuilder.toString();
        }
        switch (lT) {
            case IMAGE:
                switch (rT) {
                    case IMAGE:
                        switch (beOPStr) {
                            case "+":
                            case "-":
                            case "*":
                            case "/":
                            case "%":
                                String imageOPS = "ImageOps.binaryImageImageOp(ImageOps.OP." + bEOP + ", " + leftString + ", " + rStr + ")";
                                return imageOPS;
                            default:
                                break;
                        }
                        break;
                    case INT:
                        switch (beOPStr) {
                            case "+":
                            case "-":
                            case "*":
                            case "/":
                            case "%":
                                String imageOPS = "ImageOps.binaryImageScalarOp(ImageOps.OP." + bEOP + ", " + leftString + ", " + rStr + ")";
                                return imageOPS;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case PIXEL:
                switch (beOPStr) {
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                    case "%":
                        String imageOPS = "ImageOps.binaryPackedPixelPixelOp(ImageOps.OP." + bEOP + ", " + leftString + ", " + rStr + ")";
                        return imageOPS;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return bStringBuilder;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        StringBuilder bStrBuilder = new StringBuilder();
        for (Declaration dec : block.getDecList()) {
            String name = dec.getNameDef().getIdent().getName().toString();
            bStrBuilder.append(visitDeclaration(dec, arg)).append("\n");
            if (!nameSet.contains(name)) {
                nameSet.add(name + "" + scope);
            }
            else {
                nameSet.add(name + "" + scope);
            }
        }
        for (Statement statement : block.getStatementList()) {
            bStrBuilder.append(statement.visit(this, arg));
            bStrBuilder.append(";\n");
        }

        return bStrBuilder;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Expr guard = conditionalExpr.getGuard();
        StringBuilder conStrBuilder = new StringBuilder();

        String add = "(";
        conStrBuilder.append(add);
        switch(guard.getClass().getSimpleName()) {
            case "BinaryExpr":
                IToken.Kind operator = ((BinaryExpr) guard).getOp();
                switch(operator) {
                    case OR:
                    case AND:
                    case LT:
                    case GT:
                    case GE:
                    case LE:
                    case EQ:
                        conStrBuilder.append(conditionalExpr.getGuard().visit(this, arg));
                        conStrBuilder.append(") ? ");
                        conStrBuilder.append(conditionalExpr.getTrueCase().visit(this, arg));
                        conStrBuilder.append(" : ");
                        conStrBuilder.append(conditionalExpr.getFalseCase().visit(this, arg));
                        break;
                    default:
                        conStrBuilder.append(conditionalExpr.getGuard().visit(this, arg));
                        conStrBuilder.append(" == 0 ? false : true)");
                        conStrBuilder.append(" ? ");
                        conStrBuilder.append(conditionalExpr.getTrueCase().visit(this, arg));
                        conStrBuilder.append(" : ");
                        conStrBuilder.append(conditionalExpr.getFalseCase().visit(this, arg));
                        break;
                }
                break;
            default:
                conStrBuilder.append(conditionalExpr.getGuard().visit(this, arg));
                conStrBuilder.append(" == 0 ? false : true)");
                conStrBuilder.append(" ? ");
                conStrBuilder.append(conditionalExpr.getTrueCase().visit(this, arg));
                conStrBuilder.append(" : ");
                conStrBuilder.append(conditionalExpr.getFalseCase().visit(this, arg));
                break;
        }
        return conStrBuilder;

    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        String addThis = (String) declaration.getNameDef().visit(this, arg);
        boolean riwp = false;
        StringBuilder decStrBuilder = new StringBuilder();
        boolean closeStatement = false;
        boolean cr = false;
        decStrBuilder.append(addThis);
        String initStr = "";

        if (declaration.getInitializer() != null) {
            if (declaration.getInitializer().visit(this, arg) != null) {
                initStr = declaration.getInitializer().visit(this, arg).toString();
            }
            decStrBuilder.append(" = (");
            if (declaration.getNameDef().getType() == Type.IMAGE) {
                if (declaration.getNameDef().getDimension() == null) {
                    switch (declaration.getInitializer().getType()) {
                        case STRING:
                            decStrBuilder.append("FileURLIO.readImage(");
                            closeStatement = true;
                            break;
                        case INT:
                            decStrBuilder.append("ImageOps.cloneImage(");
                            closeStatement = true;
                            break;
                        default:
                            break;
                    }
                }
                else if (declaration.getNameDef().getDimension() != null) {
                    switch(declaration.getInitializer().getType()) {
                        case STRING:
                            decStrBuilder.append("FileURLIO.readImage((");
                            closeStatement = true;
                            riwp = true;
                            break;
                        case IMAGE:
                            decStrBuilder.append("ImageOps.copyAndResize((");
                            closeStatement = true;
                            cr = true;
                            break;
                        case PIXEL:
                            decStrBuilder.append("ImageOps.makeImage(");
                            decStrBuilder.append(declaration.getNameDef().getDimension().getWidth().visit(this, arg)).append(", ").append(declaration.getNameDef().getDimension().getHeight().visit(this, arg)).append("));\n");
                            decStrBuilder.append("ImageOps.setAllPixels(").append(declaration.getNameDef().getIdent().getName()).append(thisProgram.symTabMap.get(declaration.getNameDef().getIdent().getName()).peek()).append(", ").append(declaration.getInitializer().visit(this, arg)).append(");");
                            return decStrBuilder;
                        case INT:
                            String returnStr = "for (int y = 0; y != " + declaration.getNameDef().getIdent().getName() + thisProgram.symTabMap.get(declaration.getNameDef().getIdent().getName()).peek() + ".getHeight(); y ++) { \n";
                            returnStr += "for (int x = 0; x != " + declaration.getNameDef().getIdent().getName() + thisProgram.symTabMap.get(declaration.getNameDef().getIdent().getName()).peek() + ".getWidth(); x++) { \n";
                            returnStr += "ImageOps.setRGB(" + declaration.getNameDef().getIdent().getName() + thisProgram.symTabMap.get(declaration.getNameDef().getIdent().getName()).peek() + ", x, y, PixelOps.setGrn(ImageOps.getRGB(" + declaration.getNameDef().getIdent().getName() + thisProgram.symTabMap.get(declaration.getNameDef().getIdent().getName()).peek();
                            returnStr += ", x, y), " + declaration.getInitializer().visit(this, arg) + "));";
                            return returnStr;
                        default:
                            break;
                    }

                }
            }
            if (declaration.getNameDef().getType() == Type.STRING && declaration.getInitializer().getType() == Type.INT) {
                decStrBuilder.append("String.valueOf(");
            }
            if (declaration.getInitializer().getClass() == BinaryExpr.class) {
                IToken.Kind operator = ((BinaryExpr) declaration.getInitializer()).getOp();
                boolean condition = operator == IToken.Kind.OR || operator == IToken.Kind.AND || operator == IToken.Kind.LT || operator == IToken.Kind.GT || operator == IToken.Kind.GE || operator == IToken.Kind.LE || operator == IToken.Kind.EQ;
                if (condition) {
                    decStrBuilder.append(initStr);
                    if (declaration.getNameDef().getType() == Type.STRING && declaration.getInitializer().getType() == Type.INT) {
                        decStrBuilder.append(")");
                    }
                    decStrBuilder.append(")");

                    if (condition) {
                        decStrBuilder.append(" == false ? 0 : 1");
                    }
                }
                else {
                    decStrBuilder.append(initStr);
                    if (declaration.getNameDef().getType() == Type.STRING && declaration.getInitializer().getType() == Type.INT) {
                        decStrBuilder.append(")");
                    }
                    decStrBuilder.append(")");
                }
            }
            else {
                decStrBuilder.append(initStr);
                if (declaration.getNameDef().getType() == Type.STRING && declaration.getInitializer().getType() == Type.INT) {
                    decStrBuilder.append(")");
                }
                decStrBuilder.append(")");
            }
        }
        else {
            if (declaration.getNameDef().getDimension() != null) {
                decStrBuilder.append(" = ImageOps.makeImage(" + declaration.getNameDef().getDimension().getWidth().visit(this, arg) + ", "  + declaration.getNameDef().getDimension().getHeight().visit(this, arg) + ")") ;
            }
        }
        if (cr || riwp) {
            decStrBuilder.append(", ").append(declaration.getNameDef().getDimension().getWidth().visit(this, arg)).append(", ").append(declaration.getNameDef().getDimension().getHeight().visit(this, arg)).append(")");
        }
        if (closeStatement) {decStrBuilder.append(")");}
        decStrBuilder.append(";");
        return decStrBuilder.toString();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        return String.format("PixelOps.pack(%s, %s, %s)",
                expandedPixelExpr.getRedExpr().visit(this, arg),
                expandedPixelExpr.getGrnExpr().visit(this, arg),
                expandedPixelExpr.getBluExpr().visit(this, arg));
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        String identity = ident.getName();
        return identity + thisProgram.symTabMap.get(identity).peek();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        return identExpr.getName() + thisProgram.symTabMap.get(identExpr.getName()).peek();
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        StringBuilder lStringBuilder = new StringBuilder()
                .append(lValue.getIdent().visit(this, arg))
                .append(" ");

        if (lValue.getPixelSelector() != null) {
            lStringBuilder.append(lValue.getPixelSelector().visit(this, arg))
                    .append(" ");
        }

        if (lValue.getColor() != null) {
            lStringBuilder.append(lValue.getColor())
                    .append(" ");
        }

        return lStringBuilder;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        table.put(nameDef.getIdent().getName(), nameDef.getType());
        thisProgram.enterScope(nameDef.getIdent().getName(), scope);

        String typeString;
        switch (nameDef.getType()) {
            case STRING:
                typeString = "String";
                break;
            case PIXEL:
                typeString = "int";
                break;
            case IMAGE:
                typeString = "BufferedImage";
                break;
            default:
                typeString = nameDef.getType().toString().toLowerCase();
                break;
        }

        String name = nameDef.getIdent().getName() + thisProgram.symTabMap.get(nameDef.getIdent().getName()).peek();
        return new StringBuilder(typeString)
                .append(' ')
                .append(name)
                .toString();

    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        return String.valueOf(numLitExpr.getValue());
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        StringBuilder pixelString = new StringBuilder();
        Object xExpr = pixelSelector.getX().visit(this, arg);
        if (xExpr != null) {
            pixelString.append(xExpr.toString());
            pixelString.append(", ");
            pixelString.append(pixelSelector.getY().visit(this, arg).toString());
        }
        return pixelString.toString();
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        String pName = program.getIdent().getName().toString();
        String pType = program.getType().toString().toLowerCase();
        if (pType.equals("string")) {
            pType = "String";
        } else if (pType.equals("pixel")) {
            pType = "int";
        } else if (pType.equals("image")) {
            pType = "BufferedImage";
        }

        StringBuilder paramsStringBuilder = new StringBuilder();
        for (int i = 0; i < program.getParamList().size(); ++i) {
            String paramString = program.getParamList().get(i).visit(this, arg).toString();
            if (i != program.getParamList().size() - 1) {
                paramsStringBuilder.append(paramString).append(", ");
            } else {
                paramsStringBuilder.append(paramString);
            }
        }

        String blockStrBuilder = program.getBlock().visit(this, pType).toString();

        StringBuilder importStatements = new StringBuilder();
        importStatements.append("import edu.ufl.cise.plcsp23.runtime.*;\n")
                .append("import java.lang.Math;\n")
                .append("import java.awt.image.BufferedImage;\n")
                .append("public class ").append(pName).append(" {\n")
                .append("\tpublic static ").append(pType).append(" apply (").append(paramsStringBuilder).append(") {\n")
                .append("\t\t").append(blockStrBuilder).append("\n")
                .append("\t}\n}");

        return importStatements.toString();
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        int randomExprInt = (int) Math.floor(Math.random() * 256);
        return Integer.toString(randomExprInt);
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        //TODO: Continue here
        String type = arg.toString().toLowerCase();
        String returnStatementString = "return (";
        switch (type) {
            case "string":
                returnStatementString += "String.valueOf(";
                break;
            default:
                break;
        }

        if (returnStatement.getE().getClass() == BinaryExpr.class) {
            IToken.Kind operator = ((BinaryExpr) returnStatement.getE()).getOp();
            switch (operator) {
                case OR:
                case AND:
                case LT:
                case GT:
                case GE:
                case LE:
                case EQ:
                    returnStatementString += returnStatement.getE().visit(this, arg);
                    returnStatementString += " == false ? 0 : 1";
                    break;
                default:
                    returnStatementString += returnStatement.getE().visit(this, arg);
                    break;
            }
        }
        else {
            returnStatementString += returnStatement.getE().visit(this, arg);
        }

        switch (type) {
            case "string":
                returnStatementString += ")";
                break;
            default:
                break;
        }

        returnStatementString += ")";
        return returnStatementString;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        return "\"" + stringLitExpr.getValue() + "\"";
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        return unaryExpr.getE().visit(this, arg).toString();
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        StringBuilder uStrBuilder = new StringBuilder();
        if (unaryExprPostfix.getPrimary().getType() == Type.IMAGE) {
            if (unaryExprPostfix.getPixel() == null && unaryExprPostfix.getColor() != null) {
                if (unaryExprPostfix.getColor().toString().toLowerCase().equals("blu")) {
                    uStrBuilder.append("ImageOps.extractBlu(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(")");
                }
                else if (unaryExprPostfix.getColor().toString().toLowerCase().equals("red")) {
                    uStrBuilder.append("ImageOps.extractRed(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(")");
                }
                else if (unaryExprPostfix.getColor().toString().toLowerCase().equals("grn")) {
                    uStrBuilder.append("ImageOps.extractGrn(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(")");
                }
            }
            else if (unaryExprPostfix.getPixel() != null && unaryExprPostfix.getColor() != null) {
                if (unaryExprPostfix.getColor().toString().toLowerCase().equals("blu")) {
                    uStrBuilder.append("PixelOps.blu(ImageOps.getRGB(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getX().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getY().visit(this, arg).toString()).append("))");
                }
                else if (unaryExprPostfix.getColor().toString().toLowerCase().equals("red")) {
                    uStrBuilder.append("PixelOps.red(ImageOps.getRGB(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getX().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getY().visit(this, arg).toString()).append("))");
                }
                else if (unaryExprPostfix.getColor().toString().toLowerCase().equals("grn")) {
                    uStrBuilder.append("PixelOps.grn(ImageOps.getRGB(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getX().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getY().visit(this, arg).toString()).append("))");
                }
            }
            else if (unaryExprPostfix.getPixel() != null && unaryExprPostfix.getColor() == null) {
                uStrBuilder.append("ImageOps.getRGB(").append(unaryExprPostfix.getPrimary().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getX().visit(this, arg).toString()).append(",").append(unaryExprPostfix.getPixel().getY().visit(this, arg).toString()).append(")");
            }
        }
        else if (unaryExprPostfix.getPrimary().getType() == Type.PIXEL) {
            String colorStrLower = unaryExprPostfix.getColor().toString().toLowerCase();
            switch (colorStrLower) {
                case "blu":
                    uStrBuilder.append("PixelOps.blu(").append(unaryExprPostfix.getPrimary().visit(this, arg)).append(")");
                    break;
                case "red":
                    uStrBuilder.append("PixelOps.red(").append(unaryExprPostfix.getPrimary().visit(this, arg)).append(")");
                    break;
                case "grn":
                    uStrBuilder.append("PixelOps.grn(").append(unaryExprPostfix.getPrimary().visit(this, arg)).append(")");
                    break;
            }

        }
        return uStrBuilder;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        StringBuilder whileStringBuilder = new StringBuilder();

        // Add while loop header
        whileStringBuilder.append("while (");
        String guardString = whileStatement.getGuard().visit(this, arg).toString();
        whileStringBuilder.append(guardString);

        // Handle complex guard expressions
        Expr guardExpression = whileStatement.getGuard();
        if (guardExpression.getClass() == BinaryExpr.class) {
            BinaryExpr binaryGuard = (BinaryExpr) guardExpression;
            IToken.Kind operator = binaryGuard.getOp();
            if (operator == IToken.Kind.OR || operator == IToken.Kind.AND || operator == IToken.Kind.LT || operator == IToken.Kind.GT || operator == IToken.Kind.GE || operator == IToken.Kind.LE || operator == IToken.Kind.EQ) {
                // Simple comparison operators, no need to modify the guard
            }
            else {
                whileStringBuilder.append(" == 0 ? false : true");
            }
        }
        else if (guardExpression.getClass() == IdentExpr.class) {
            whileStringBuilder.append(" == 0 ? false : true");
        }

        // Close while loop header and add loop body
        whileStringBuilder.append(") {\n");
        scope++;
        whileStringBuilder.append("\t");
        whileStringBuilder.append(whileStatement.getBlock().visit(this, arg).toString());
        whileStringBuilder.append("\n}");

        // Close while loop scope
        thisProgram.closeScope(scope);
        scope--;

        return whileStringBuilder.toString();

    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        StringBuilder writer = new StringBuilder();
        writer.append("ConsoleIO.write(").append(statementWrite.getE().visit(this, arg).toString()).append(");");
        return writer.toString();
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        int num = 255;
        String strNum = String.valueOf(num);
        return strNum;
    }
}