package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser {
    Scanner theScanner;

    Parser(Scanner s) {
        theScanner = s;
    }



    public Expr primary(Token a) throws PLCException {
        Token current = a;
        if (current.getKind() == Kind.NUM_LIT) {
            a = theScanner.next();
            NumLitExpr aNumber = new NumLitExpr(current);
            aNumber.setType(Type.INT);
            return aNumber;
        } else if (current.getKind() == Kind.STRING_LIT) {
            a = theScanner.next();
            StringLitExpr aString = new StringLitExpr(current);
            aString.setType(Type.STRING);
            return aString;
        } else if (current.getKind() == Kind.IDENT) {
            a = theScanner.next();
            IdentExpr identifier = new IdentExpr(current);
            //identifier.setType(Type.IMAGE);
            return identifier;
        } else if (current.getKind() == Kind.RES_rand) {
            a = theScanner.next();
            RandomExpr aRandom = new RandomExpr(current);
            return aRandom;
        } else if (current.getKind() == Kind.RES_Z) {
            a = theScanner.next();
            ZExpr aZ = new ZExpr(current);
            return aZ;
        } else if (current.getKind() == Kind.LPAREN) {
            a = theScanner.next();
            Expr theone = expression(a);
            if(theScanner.getCurrent().getKind()==Kind.RPAREN) {
                theScanner.next();
                return theone;
            }else{
                throw new SyntaxException("no right parantheses to match left");
            }
        } else if(current.getKind() == Kind.RES_x || current.getKind() == Kind.RES_y || current.getKind() == Kind.RES_a
                || current.getKind() == Kind.RES_r){
            a = theScanner.next();
            Expr aVariable = new PredeclaredVarExpr(current);
            aVariable.setType(Type.INT);
            return aVariable;
        } else if (current.getKind() == Kind.LSQUARE){
            Expr red = expression(theScanner.next());
            red.setType(Type.INT);
            Expr blue = expression(theScanner.next());
            blue.setType(Type.INT);
            Expr green = expression(theScanner.next());
            green.setType(Type.INT);
           ExpandedPixelExpr thePix = new ExpandedPixelExpr(current,red,blue,green);
           thePix.setType(Type.PIXEL);
           if(theScanner.getCurrent().getKind()!=Kind.RSQUARE){
               throw new SyntaxException("missing right bracket");
           }
           theScanner.next();
           return thePix;
        }else if(current.getKind() == Kind.RES_y_cart || current.getKind() == Kind.RES_x_cart ||
                current.getKind() == Kind.RES_a_polar ||current.getKind() == Kind.RES_r_polar){
                 return (Expr)pixelfunction(current);
        }
        SyntaxException nothing = new SyntaxException("theres nothing");
        throw nothing;
    }
    public AST pixelfunction(Token a) throws PLCException{
       Token current = a;
       Kind theKind = a.getKind();
       PixelSelector selector = pixelselector(a);
       PixelFuncExpr thePixelExpr = new PixelFuncExpr(current,theKind,selector);
       thePixelExpr.setType(Type.PIXEL);
       return thePixelExpr;
    }
    public PixelSelector pixelselector(Token a) throws PLCException{
        theScanner.next();
        Expr x = expression(theScanner.next());
        Expr y = expression(theScanner.next());
        theScanner.next();
        return new PixelSelector(a,x,y);
    }
    //the unary function will return unarys : idents, etc,
    public Expr unary(Token a) throws PLCException {
        UnaryExpr aUnary = null;
        if (a.getKind() == Kind.BANG || a.getKind() == Kind.RES_sin ||
                a.getKind() == Kind.RES_cos || a.getKind() == Kind.RES_atan || a.getKind() == Kind.MINUS) {
            aUnary = new UnaryExpr(a, a.getKind(), unary(theScanner.next()));
            return aUnary;
        }
        a = theScanner.getCurrent();
        Expr primary = primary(a);
        PixelSelector thePixels = null;
        boolean postFix = false;
        //checking for unary post fix
        if (theScanner.getCurrent().getKind() == Kind.LSQUARE) {
            postFix = true;
            Token firstP = theScanner.getCurrent();
            Expr firstE = expression(theScanner.next());
            Expr secondE = expression(theScanner.next());
            thePixels = new PixelSelector(firstP, firstE, secondE);
            if (theScanner.getCurrent().getKind() != Kind.RSQUARE) {
                throw new SyntaxException("missing right bracket");
            }
            theScanner.next();
        }
        if (theScanner.getCurrent().getKind() == Kind.COLON) {
            try {
                Token color = theScanner.next();
                theScanner.next();
                UnaryExprPostfix aUnaryPost = new UnaryExprPostfix(a, primary, thePixels, ColorChannel.getColor(color));
                aUnaryPost.setType(Type.INT);
               return aUnaryPost;
            } catch (Exception e) {
                throw new SyntaxException("not the right color");
            }
        }
        if(postFix){
            UnaryExprPostfix aUnaryPost = new UnaryExprPostfix(a, primary, thePixels, null);
            return aUnaryPost;
        }
        return primary;

    }

    public Expr multiplicative(Token a) throws PLCException {
        Expr left = unary(a);
        Type theType = left.getType();
        Token first = a;
       a = theScanner.getCurrent();
        while (a.getKind() == Kind.TIMES || a.getKind() == Kind.MOD || a.getKind() == Kind.DIV) {
            Kind theKind = a.getKind();
            a = theScanner.next();
            Expr right = (Expr) unary(a);
            left = new BinaryExpr(first, left, theKind, right);
            left.setType(theType);
           a = theScanner.getCurrent();
        }
        return left;
    }

    public Expr additive(Token a) throws PLCException {
        Expr left = multiplicative(a);
        Type theType = left.getType();
        Token first = a;
        a= theScanner.getCurrent();
        while(a.getKind() == Kind.PLUS || a.getKind() == Kind.MINUS) {
            Kind theKind = a.getKind();
            a = theScanner.next();
            Expr right = (Expr) multiplicative(a);
            left = new BinaryExpr(first, left, theKind, right);
            left.setType(theType);
            a = theScanner.getCurrent();
        }
        return left;
    }

    public Expr power(Token a) throws PLCException {
        Expr left = additive(a);
        Type theType = left.getType();
        Token first = a;
        a= theScanner.getCurrent();
        while(a.getKind() == Kind.EXP) {
            Kind theKind = a.getKind();
            a = theScanner.next();
            Expr right = (Expr) additive(a);
            left = new BinaryExpr(first, left, theKind, right);
            left.setType(theType);
            a= theScanner.getCurrent();
        }
        return left;
    }

    public Expr comparison(Token a) throws PLCException {
        Expr left = power(a);
        Token first = a;
        a= theScanner.getCurrent();
        while (a.getKind() == Kind.LT || a.getKind() == Kind.LE || a.getKind() == Kind.GT || a.getKind() == Kind.GE || a.getKind() == Kind.EQ) {
            Kind theKind = a.getKind();
            a = theScanner.next();
            Expr right = (Expr) power(a);
            left = new BinaryExpr(first, left, theKind, right);
            left.setType(Type.BOOL);
            a= theScanner.getCurrent();
        }
        return left;
    }

    public Expr and(Token a) throws PLCException {
        Expr left = comparison(a);
        Token first = a;
        a= theScanner.getCurrent();
        while (a.getKind() == Kind.BITAND || a.getKind() == Kind.AND) {
            Kind theKind = a.getKind();
            a = theScanner.next();
            Expr right = (Expr) comparison(a);
            left = new BinaryExpr(first, left, theKind, right);
            a= theScanner.getCurrent();
        }
        return left;
    }

    public Expr or(Token a) throws PLCException {
        Expr left = and(a);
        Token first = a;
        a= theScanner.getCurrent();
        while(a.getKind() == Kind.BITOR || a.getKind() == Kind.OR) {
            Kind theKind = a.getKind();
            a = theScanner.next();
            Expr right = (Expr) and(a);
            left = new BinaryExpr(first, left, theKind, right);
            a= theScanner.getCurrent();
        }
        return left;
    }

    public Expr conditional(Token a) throws PLCException {
        Token current = a;
        if (current.getKind() != Kind.RES_if) {
            return null;
        }
        Expr guard = expression(theScanner.next());
        Expr trueExpr = expression(theScanner.next());
        System.out.println(trueExpr.getType());
        Expr falseExpr = expression(theScanner.next());
        ConditionalExpr theConditional = new ConditionalExpr(current, guard, trueExpr, falseExpr);
        theConditional.setType(trueExpr.getType());
        return theConditional;
    }




    public Expr expression(Token a)throws PLCException{
        if (a.getKind() == Kind.RES_if) {
            return conditional(a);
        }
        return or(a);
    }

    public Declaration declaration(Token a) throws PLCException{
        Token current = a;
        NameDef theNamedef = namedef(a);
        Expr initializer= null;
        a = theScanner.next();
        if(a.getKind() == Kind.ASSIGN){
          Token theToken = theScanner.next();
       initializer = expression(theToken);
       //set the type of the initializer
        }
        return new Declaration(current,theNamedef, initializer);
    }
    public List<Declaration>declist(Token a )throws PLCException{
        List <Declaration> declist = new ArrayList<>();
        Type theType;
        try{
           theType= Type.getType(a);
           declist.add(declaration(a));
        }catch(Exception e){
           // throw new SyntaxException("this one doesnt have declarations");
        }
        while(theScanner.getCurrent().getKind()==Kind.DOT){
            if(theScanner.next().getKind() == Kind.RCURLY){
                break;
            }
            try{
                Type.getType(theScanner.getCurrent());
                declist.add(declaration(theScanner.getCurrent()));
                if(theScanner.getCurrent().getKind()!=Kind.DOT){
                    throw new SyntaxException("didn't have dot at the end of a declaration");
                }
            }
            catch(Exception e){
                break;
            }
        }
        return declist;
    }
    public Statement statement() throws PLCException {
        Token first = theScanner.getCurrent();
        LValue theL = null;
        if (theScanner.getCurrent().getKind() == Kind.RES_while) {
            Expr theExpr = expression(theScanner.next());
            Block theBlock = block();
            return new WhileStatement(first, theExpr, theBlock);
        } else if (theScanner.getCurrent().getKind() == Kind.RES_write) {
            Expr theExpr = expression(theScanner.next());
            return new WriteStatement(first, theExpr);
        }else if (theScanner.getCurrent().getKind() == Kind.COLON) {
            System.out.println("got to the colon primary syntax");
            Expr theExpr = expression(theScanner.next());
            return new ReturnStatement(first, theExpr);
        }

            PixelSelector thePixels = null;
            Ident theIdent = new Ident(theScanner.getCurrent());
            if (theScanner.next().getKind() == Kind.LSQUARE) {
                Token firstP = theScanner.getCurrent();
                Expr firstE = expression(theScanner.next());
                Expr secondE = expression(theScanner.next());
                thePixels = new PixelSelector(firstP, firstE, secondE);
                if (theScanner.getCurrent().getKind() != Kind.RSQUARE) {
                    throw new SyntaxException("missing right bracket");
                }
                theScanner.next();
            }
            if (theScanner.getCurrent().getKind() == Kind.COLON) {
                try {
                    theL = new LValue(first, theIdent, thePixels, ColorChannel.getColor(theScanner.next()));
                } catch (Exception e) {
                    throw new SyntaxException("not the right color");
                }
                theScanner.next();
            }
            else{
                theL = new LValue(first, theIdent, thePixels, null);
            }
        if(theScanner.getCurrent().getKind()!= Kind.ASSIGN){
            throw new SyntaxException("missing assign symbol");
        }
        Expr assignto = expression(theScanner.next());
        if(theScanner.getCurrent().getKind()!=Kind.DOT){
            throw new SyntaxException("no dot after statement");
        }

        return new AssignmentStatement(first,theL, assignto);
    }
   public List<Statement> statementList(Token a)throws PLCException{
        List <Statement> statementList = new ArrayList<>();
        while(theScanner.getCurrent().getKind()== Kind.RES_while ||theScanner.getCurrent().getKind()== Kind.RES_write
        || theScanner.getCurrent().getKind()== Kind.IDENT || theScanner.getCurrent().getKind()==Kind.COLON){
            statementList.add(statement());
            theScanner.next();
        }
        return statementList;
    }
    public Block block()throws PLCException{
        // Token current = theScanner.getCurrent();
        if(theScanner.getCurrent().getKind()!= Kind.LCURLY){
          throw new SyntaxException("not lcurly for block");
        }
        List<Declaration>declarations = declist(theScanner.next());
        List<Statement>statements = statementList(theScanner.getCurrent());
        if (theScanner.getCurrent().getKind()!= Kind.RCURLY){
            throw new SyntaxException("doesnt end with rcurly");
        }
        theScanner.next();
        return new Block (theScanner.getCurrent(), declarations, statements);
    }

    public Program program(Token a) throws PLCException{
        Token first = theScanner.getCurrent();
        Token progName = theScanner.next();
        if(progName.getKind() != Kind.IDENT){
            throw new SyntaxException("program name is not an ident");
        }
        Ident theIdent = new Ident(progName);
        theScanner.next();
        if(theScanner.getCurrent().getKind()==Kind.LPAREN){
            theScanner.next();
        }
        else{
            throw new SyntaxException("no parameters left parantheses part");
        }
        List <NameDef> parameters = new ArrayList<>();
        while (theScanner.getCurrent().getKind() != Kind.RPAREN) {
            parameters.add(namedef(theScanner.getCurrent()));
           theScanner.next();
           if(theScanner.getCurrent().getKind() == Kind.COMMA){
               theScanner.next();
           }
        }

        if(theScanner.getCurrent().getKind()==Kind.RPAREN) {
           theScanner.next();
        }
        Block theBlock = block();

        if(theScanner.getCurrent().getKind() != Kind.EOF){
            throw new SyntaxException("there's more after the program is done");
        }
        return new Program(first, Type.getType(first), theIdent,parameters, theBlock);
    }
    public NameDef namedef(Token a)throws PLCException{
        Expr width;
        Expr height;
        try {
            if (theScanner.next().getKind() == Kind.LSQUARE) {
                width = expression(theScanner.next());
                height = expression(theScanner.next());
                Ident theIdent = new Ident(theScanner.next());
                Dimension theDimension = new Dimension(a, width, height);
                NameDef newOne = new NameDef(a, Type.getType(a), theDimension, theIdent);;
                theIdent.setDef(newOne);
                return newOne;
            }
           if(theScanner.getCurrent().getKind()!=Kind.IDENT){
               throw new SyntaxException("parameter variable name is not an ident");
           }
            Ident theIdent = new Ident(theScanner.getCurrent());
            Dimension theDimension = null;
            NameDef newOne = new NameDef(a, Type.getType(a), theDimension, theIdent);;
            theIdent.setDef(newOne);
            return newOne;
        }catch(Exception e){
            throw new SyntaxException("namedef is wrong");
        }
    }
    @Override
    public AST parse() throws PLCException {
        Token a = theScanner.next();
        //seems like primary expression is just a token
        //or parentheses around a token
        return program(a);
    }
}