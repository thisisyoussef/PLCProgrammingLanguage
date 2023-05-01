//package assignmentoneforCOP4020;
package edu.ufl.cise.plcsp23;
public class StringLitToken extends Token implements IStringLitToken {
	String value;

	public StringLitToken(String text, int r, int c, IToken.Kind k) throws LexicalException{
		super(text,r,c,k);
		this.value = "";
		for(int i =1; i< text.length()-1;i++){
			if(text.charAt(i)=='\n' || text.charAt(i) == '\r'){
				LexicalException e = new LexicalException("input is LF or CR");
				throw e;
			}
			if(text.charAt(i)=='\\' && i<text.length()-2) {
					if(text.charAt(i+1)=='b'){
						value += '\b';
					}
					else if(text.charAt(i+1)=='t'){
						value+='\t';
					}
					else if(text.charAt(i+1)=='n'){
						value+='\n';
					}
					else if(text.charAt(i+1)=='r'){
						value+='\r';
					}
					else if(text.charAt(i+1)=='"'){
						value+='\"';
					}
					else if(text.charAt(i+1)=='\\'){
						value+='\\';
					}
					else{
						LexicalException e = new LexicalException("not a proper escape sequence");
						throw e;
					}
				i++;
			}
			else{
				value += text.charAt(i);
			}
		}
	}

	public String getValue() {

		return value;
	}
}