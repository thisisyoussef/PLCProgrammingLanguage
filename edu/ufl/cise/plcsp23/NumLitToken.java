//package assignmentoneforCOP4020;
package edu.ufl.cise.plcsp23;
import  edu.ufl.cise.plcsp23.IToken.Kind;
import  edu.ufl.cise.plcsp23.IToken.SourceLocation;
public class NumLitToken extends Token implements INumLitToken  {

	private int value;
	private Kind kind;
	public NumLitToken(String text, int r, int c, Kind k){
		super(text,r,c,k);
		value = Integer.parseInt(text);
		this.kind = Kind.NUM_LIT;
	}


	public int getValue() {
		return value;
	}
	public SourceLocation getSourceLocation(){
		SourceLocation loc = new SourceLocation(0,0);
		return loc;
	}
	public String getTokenString(){
		return content;
	}
	public Kind getKind(){
		return this.kind;
	}

}