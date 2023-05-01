package edu.ufl.cise.plcsp23;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.List;

public class TypeChecker implements ASTVisitor{
    //Create a class that implements the ASTVisitor interface and performs type checking. You will also need to
    //implement a symbol table class. Typing rules for the language are described in detail in the Typing rules
    //document. You will need to implement a symbol table that maps names to their declaring NameDef object.
    //This language has nested scopes introduced in WhileStatements. Since this is a fairly small
    //part of the language, I suggest starting out with a simple implementation for a symbol table that
    //does not handle scopes and implement type checking for the rest of the language.

    SymbolTable symbolTable = new SymbolTable();

    Program programForThis;

    private void check(boolean condition, AST node, String message)
            throws TypeCheckException {

        if (! condition) { throw new TypeCheckException(message);}
    }
    private void insert(String name, NameDef def) throws TypeCheckException {
        symbolTable.addSymbol(name, def);
    }
    private boolean assignmentCompatible(Type targetType, Type rhsType) {
        boolean compatible = true;
        if((targetType == Type.IMAGE && rhsType == Type.INT) ||
                (targetType == Type.PIXEL && (rhsType == Type.IMAGE || rhsType == Type.STRING))
                || (targetType == Type.INT && (rhsType ==Type.STRING || rhsType == Type.IMAGE))
        ){
        compatible = false;
        }
        return compatible;
    }
    //Code:

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        //Start of code
        LValue lValue = statementAssign.getLv();
        Expr expr = statementAssign.getE();
        Type lValueType = (Type) lValue.visit(this, arg);
        Type exprType = (Type) expr.visit(this, arg);
        statementAssign.getE().setType(exprType);
        //Check if Lvalue is properly typed
        if (lValueType == null) {
            throw new TypeCheckException(("LValue is not properly typed"));
        }
        //Check if Expr is properly typed
        if (exprType == null) {
            throw new TypeCheckException("Expr is not properly typed");
        }
        if ((lValueType == Type.IMAGE && exprType == Type.INT) ||
                (lValueType == Type.PIXEL && (exprType == Type.IMAGE || exprType == Type.STRING))
                || (lValueType == Type.INT && (exprType ==Type.STRING || exprType == Type.IMAGE))
        ){
            //TODO: Implement proper assignments
            throw new TypeCheckException("Type mismatch in assignment to " + lValue);
        }
        //Visit LValue and Expr
        lValue.visit(this, arg);
        expr.visit(this, arg);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        //Start of code
        IToken.Kind op = binaryExpr.getOp();
        Expr e0 = binaryExpr.getLeft();
        Expr e1 = binaryExpr.getRight();
        Type e0Type = (Type) e0.visit(this, arg);
        Type e1Type = (Type) e1.visit(this, arg);
        binaryExpr.getLeft().setType(e0Type);
        binaryExpr.getRight().setType(e1Type);
        binaryExpr.setType(e0Type);
        switch (op){
            case BITOR, BITAND -> {
                if (e0Type != Type.PIXEL || e1Type != Type.PIXEL) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                binaryExpr.setType(Type.PIXEL);
                return Type.PIXEL;
            }
            case OR, AND -> {
                if (e0Type != Type.INT || e1Type != Type.INT) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                binaryExpr.setType(Type.INT);
                return Type.INT;
            }
            case LT, GT, LE, GE -> {
                if (e0Type != Type.INT || e1Type != Type.INT) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                binaryExpr.setType(Type.INT);
                return Type.INT;
            }
            case EQ -> {
                //e0 and e1 can be int, pixel, image, or string but must be the same type
                if (e0Type != e1Type) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                binaryExpr.setType(Type.INT);
                return Type.INT;
            }
            case EXP -> {
                //e0 can be int and e1 can be int
                //e0 can be pixel and e1 can be int
                if (e0Type == Type.INT && e1Type == Type.INT) {
                    binaryExpr.setType(Type.INT);
                    return Type.INT;
                }
                if (e0Type == Type.PIXEL && e1Type == Type.INT) {
                    binaryExpr.setType(Type.PIXEL);
                    return Type.PIXEL;
                }
                throw new TypeCheckException("Type mismatch in binary expression");
            }
            case PLUS -> {
                //e0 and e1 can be int, pixel, image, or string but must be the same type
                if (e0Type != e1Type) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                binaryExpr.setType(e0Type);
                return e0Type;
            }
            case MINUS -> {
                //e0 and e1 can be int, pixel, image, but must be the same type
                if (e0Type != e1Type) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                //Check if either are strings
                if (e0Type == Type.STRING || e1Type == Type.STRING) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                return e0Type;
            }
            case TIMES, DIV, MOD -> {
                //e0 and e1 can be int, pixel, image, and both must be the same type or e0 can be image and e1 can be int e0 can also be pixel and e1 can be int
                //check if e0 is image and e1 is int
                if (e0Type == Type.IMAGE && e1Type == Type.INT) {
                    return Type.IMAGE;
                }
                //check if e0 is pixel and e1 is int
                if (e0Type == Type.PIXEL && e1Type == Type.INT) {
                    return Type.PIXEL;
                }
                //check if e0 and e1 are the same type
                if (e0Type != e1Type) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                //Check if either are strings
                if (e0Type == Type.STRING || e1Type == Type.STRING) {
                    throw new TypeCheckException("Type mismatch in binary expression");
                }
                return e0Type;
            }
        }
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        Object returned = null;
        List<Declaration> decList = block.getDecList();
        List<Statement> statementList = block.getStatementList();
        for (Declaration d : decList) {
            d.visit(this, arg);
        }
        for (Statement s : statementList) {
            returned = s.visit(this, arg);
        }
        return returned;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Expr e0 = conditionalExpr.getGuard();
        Expr e1 = conditionalExpr.getTrueCase();
        Expr e2 = conditionalExpr.getFalseCase();
        Type e0Type = (Type) e0.visit(this, arg);
        Type e1Type = (Type) e1.visit(this, arg);
        Type e2Type = (Type) e2.visit(this, arg);
        //Check if Expr0.type == int
        if (e0Type != Type.INT) {
            throw new TypeCheckException("Type mismatch in conditional expression at " + "getSourceLoc()");
        }
        //Check if Expr1.type == Expr2.type
        if (e1Type != e2Type) {
            throw new TypeCheckException("Type mismatch in conditional expression at " + "getSourceLoc()");
        }
        //Return Expr1.type
        return e1Type;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        String name = declaration.getNameDef().getIdent().getName();
        Expr initializer = declaration.getInitializer();
        Type ExprType = null;
        if (initializer != null) {
            ExprType = (Type) initializer.visit(this, arg);
            initializer.setType(ExprType);

            check(declaration.getNameDef().firstToken.getTokenString() != initializer.firstToken.getTokenString(),
                    declaration, "name of namedef and initializer the same");
        }
        Type nameDefType = (Type)declaration.getNameDef().visit(this,arg);
        if(initializer!=null){
            check(assignmentCompatible(nameDefType, ExprType), declaration, "incompatible types in declaration");
        }
        if(nameDefType == Type.IMAGE){
            check((initializer!=null || declaration.getNameDef().getDimension()!=null), declaration, "name def is image" +
                    "but doesnt have initializer or dimension");
        }
        return null;
    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        // Check that the expression in the dimension is valid.
        // If the expression is not valid, throw a PLCException with the message "Invalid expression at " + getSourceLoc().
        // If the expression is valid, return null.
        //NameDef ::= Type Ident (Dimension | ε )
        //Constraints:
        //• If (Dimension != ε) Type == image
        //• If (Dimension != ε) Dimension is
        //properly typed
        //• Ident.name has not been previously
        //declared in this scope.
        //• Type != void
        //Insert (name, NameDef) into symbol table.
        //Start of code
        Expr expr = dimension.getWidth();
        expr.visit(this, arg);
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        Expr expr0 = expandedPixelExpr.getRedExpr();
        Expr expr1 = expandedPixelExpr.getGrnExpr();
        Expr expr2 = expandedPixelExpr.getBluExpr();

        check(expandedPixelExpr.getType()==Type.PIXEL, expandedPixelExpr, "expanded pixel expression should be type Pixel");
        Type expr0Type = (Type) expr0.visit(this, arg);
        Type expr1Type = (Type) expr1.visit(this, arg);
        Type expr2Type = (Type) expr2.visit(this, arg);
        check(expr0Type==Type.INT, expr0, "expanded pixels pixel should be int");
        check(expr1Type==Type.INT, expr1, "expanded pixels pixel should be int");
        check(expr2Type==Type.INT, expr2, "expanded pixels pixel should be int");
        return Type.PIXEL;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {

        String name = ident.getName();
        //insert into symbol table
        if (symbolTable.getSymbol(name) == null) {
            throw new TypeCheckException("Identifier " + name + " is not declared");
        }
        Type a = symbolTable.getSymbol(name).getType();
        return a;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        if(symbolTable.getSymbol(identExpr.getName())!=null){
            identExpr.setType(symbolTable.getSymbol(identExpr.getName()).getType());
            return symbolTable.getSymbol(identExpr.getName()).getType();
        }
        throw new TypeCheckException("ident expression has not been definted");
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        //Ident.type PixelSelector ChannelSelector LValue.type
        //image no no image
        //yes no pixel
        //no yes image
        //yes yes int ***
        //pixel no no pixel
        //no yes int ***
        //string no no string
        //int no no int

        //Get Ident.type
        Ident ident = lValue.getIdent();
        check(symbolTable.getSymbol(ident.getName()) != null, ident, "ident has not been declared" +
                "for l value");
        lValue.setType(symbolTable.getSymbol(ident.getName()).getType());
        //Make pixelSelector and channelSelector
        PixelSelector pixelSelector = lValue.getPixelSelector();
        Object channelSelector = lValue.getColor();
        //Switch on Ident.type
        //Start of code
        Type identType = (Type) ident.visit(this, arg);
        switch (identType) {
            case IMAGE:
                //If both pixelSelector and channelSelector are null, return image
                if (pixelSelector == null && channelSelector == null) {
                    return Type.IMAGE;
                }
                //If pixelSelector is not null and channelSelector is null, return pixel
                if (pixelSelector != null && channelSelector == null) {
                    return Type.PIXEL;
                }
                //If pixelSelector is null and channelSelector is not null, return image
                if (pixelSelector == null && channelSelector != null) {
                    return Type.IMAGE;
                }
                //If pixelSelector is not null and channelSelector is not null, return int
                if (pixelSelector != null && channelSelector != null) {
                    return Type.INT;
                }
                break;
            case PIXEL:
                //If both pixelSelector and channelSelector are null, return pixel
                if (pixelSelector == null && channelSelector == null) {
                    return Type.PIXEL;
                }
                //If pixelSelector is null and channelSelector is not null, return int
                if (pixelSelector == null && channelSelector != null) {
                    return Type.INT;
                }
                break;
            case STRING:
                //If both pixelSelector and channelSelector are null, return string
                if (pixelSelector == null && channelSelector == null) {
                    return Type.STRING;
                }
                break;
            case INT:
                //If both pixelSelector and channelSelector are null, return int
                if (pixelSelector == null && channelSelector == null) {
                    return Type.INT;
                }
                break;
            default:
                throw new TypeCheckException("Invalid type " + identType);

        }
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName();
        boolean inserted = symbolTable.insert(name, nameDef);
        check(inserted, nameDef, "variable " + name + "already declared");
        boolean correctType = true;
        Type nameType = nameDef.getType();
        if(nameType==null || nameType== Type.VOID){
            correctType=false;
        }
        check(correctType,nameDef, "parameter has null or void type");
        if(nameDef.getDimension()!=null){
            check(nameDef.getType()==Type.IMAGE, nameDef, "nameDef with dimensions must be images");
           nameDef.getDimension().visit(this,arg);
        }
        return nameType;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        return Type.INT;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        Type pixel =(Type)pixelFuncExpr.getSelector().visit(this,arg);
        check(pixel== Type.PIXEL, pixelFuncExpr, "type must be int");
        return Type.INT;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        Type xType = (Type)pixelSelector.getX().visit(this,arg);
        Type yType = (Type)pixelSelector.getY().visit(this,arg);
        pixelSelector.getX().setType(xType);
        pixelSelector.getY().setType(yType);
        check(xType == Type.INT && yType==Type.INT, pixelSelector, "pixel selector has non ints");
        return Type.PIXEL;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        // Check that the expression in the predeclared variable expression is valid.
        // If the expression is not valid, throw a PLCException with the message "Invalid expression at " + getSourceLoc().
        // If the expression is valid, return null.
        //PredeclaredVarExpr ::= x | y | a | r
        //PredeclaredVarExpr.type ← int
        //Start of code
        return Type.INT;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        boolean correctProgType= true;
        if(program.getType()== null){
            correctProgType= false;
        }
        programForThis = program;
        symbolTable.enterScope();
        check(correctProgType, program, "wrong program type");
        List<NameDef> params = program.getParamList();
        for (NameDef p : params) {
            p.visit(this, arg);
        }
        Type returned = (Type)program.getBlock().visit(this, arg);
        symbolTable.exitScope();
        return null;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        randomExpr.setType(Type.INT);
        return Type.INT;
    }
    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        Type returnType = (Type)returnStatement.getE().visit(this,arg);
        Type programType = programForThis.getType();
        check(programType!=Type.VOID, returnStatement, "void program returning something");
        check(assignmentCompatible(programType,returnType),returnStatement, "return statement" +
                "is not compatible with return type");
        returnStatement.getE().setType(returnType);
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        return stringLitExpr.getType();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        //UnaryExpr ::= (! | - | sin | cos | atan) Expr Constraints:
        //• Expr properly typed
        //• See table for allowed operator type
        //combinations and result type
        //UnaryExpr.type ← result type
        //PrimaryExpr.type PixelSelector ChannelSelector UnaryExprPostfix.type
        //Pixel No Yes Int ****
        //Image No Yes image
        //Image Yes No pixel
        //Image Yes Yes int *****
        //Note that at least one of PixelSelector or ChannelSelector should be present in order to create a
        //UnayrExprPostfix object

        //Start of code
        Token.Kind op = unaryExpr.getOp();
        Type expr = (Type) unaryExpr.getE().visit(this, arg);
        unaryExpr.setType(expr);
        Type result = null;
        switch(op){
            case BANG ->{
                if(expr == Type.INT){
                    result = Type.INT;
                }
                else if(expr == Type.PIXEL){
                    result = Type.PIXEL;
                }
                else check(false, unaryExpr, "incompatible types for operator");
            }
            case MINUS, RES_cos, RES_sin, RES_atan ->{
                if(expr == Type.INT){
                    result = Type.INT;
                }
                else check(false, unaryExpr, "incompatible types for operator");
            }
        }
        unaryExpr.getE().setType(result);
        return result;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        //Find PrimaryExpr.type
        //UnaryExprPostfix.type ← PrimaryExpr.type PixelSelector ChannelSelector
        //Start of code
        //Get the type of the primary expression
        Type type =  (Type)unaryExprPostfix.getPrimary().visit(this,arg);
        Type pixelSelector = (Type) unaryExprPostfix.getPixel().visit(this, arg);
        Object channelSelector = unaryExprPostfix.getColor();
        //check if Type is Pixel in switch statement
        switch (type) {
            case PIXEL:
                //check if PixelSelector is present and ChannelSelector is not present, if not then throw exception
                //Start of code
                if (pixelSelector == null && channelSelector != null) {
                    return Type.INT;
                }
                else{
                    throw new TypeCheckException("Invalid expression");
                }
            case IMAGE:
                //check if PixelSelector is present and ChannelSelector is not present, if not then throw exception
                //Start of code
                if (pixelSelector != null && channelSelector == null) {
                    return Type.PIXEL;
                }
                //check if PixelSelector is present and ChannelSelector is present, if not then throw exception
                else if (pixelSelector != null && channelSelector != null) {
                    return Type.INT;
                }
                //check if PixelSelector is not present and ChannelSelector is not present, if not then throw exception
                else if (pixelSelector == null && channelSelector != null) {
                    return Type.IMAGE;
                }
                else{
                    throw new TypeCheckException("Invalid expression");
                }
            default:
                //PixelSelector and ChannelSelector are not present
                //UnaryExprPostfix.type ← PrimaryExpr.type PixelSelector ChannelSelector
                break;
        }
        return type;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        Type ExprType = (Type)whileStatement.getGuard().visit(this,arg);
        check(ExprType==Type.INT, whileStatement, "while statement does not have int guard");
        symbolTable.enterScope();
        whileStatement.getBlock().visit(this,arg);
        symbolTable.exitScope();
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        return statementWrite.getE().visit(this,arg);
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        zExpr.setType(Type.INT);
        return Type.INT;
    }
}