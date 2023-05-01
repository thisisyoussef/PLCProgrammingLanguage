//package assignmentone;
package edu.ufl.cise.plcsp23;
public class Token implements IToken{
	final int row;
	final int col; 
	final Kind kind;
	String content;
	SourceLocation loc;
	public Token(String text, int r, int c, Kind k){
		this.content = text;
		this.row = r;
		this.col = c;
		this.kind = k;
		this.loc = new SourceLocation(row,col);
	}
	public Kind getKind(){
		return kind;
	}
	public String getTokenString(){
		return content;
	}
	public SourceLocation getSourceLocation(){
		return loc;
	}
	//prints token, used during development
@Override  public String toString() {
	return getTokenString();
}
}