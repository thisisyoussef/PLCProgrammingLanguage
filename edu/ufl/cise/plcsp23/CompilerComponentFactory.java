package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.ASTVisitor;

public class CompilerComponentFactory {
	public static IScanner makeScanner(String input) {
		//Add statement to return an instance of your scanner
		return new Scanner(input);
	}
	public static IParser makeAssignment2Parser(String input)
			throws LexicalException {
		//add code to create a scanner and parser and return the parser.
		Scanner newS = new Scanner(input);
		Parser newp = new Parser(newS);
		return newp;
	}
	public static IParser makeParser(String input)
			throws LexicalException {
		Scanner s = new Scanner(input);
		Parser newp = new Parser(s);
		return newp;
	}
	public static ASTVisitor makeTypeChecker() {
		//Add statement to return an instance of your type checker
		return new TypeChecker();
	}
	public static ASTVisitor makeCodeGenerator(String PackageName) {
		//code to instantiate a return an ASTVisitor for code generation
		return new CodeGenerator(PackageName);
	}
}