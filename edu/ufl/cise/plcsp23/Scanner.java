//package assignmentone;
package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.IToken.Kind;

import java.util.Arrays;
import java.util.HashMap;

public class Scanner implements IScanner{

	Token current;
	 public static HashMap<String, Kind> reservedWords;
     static{
		reservedWords = new HashMap<String, Kind>();
		reservedWords.put("image", Kind.RES_image);
		reservedWords.put("int", Kind.RES_int);
		reservedWords.put("pixel", Kind.RES_pixel);
		reservedWords.put("int", Kind.RES_int);
		reservedWords.put("string", Kind.RES_string);
		reservedWords.put("void", Kind.RES_void);
		reservedWords.put("nil", Kind.RES_nil);
		reservedWords.put("load", Kind.RES_load);
		reservedWords.put("display", Kind.RES_display);
		reservedWords.put("write", Kind.RES_write);
		reservedWords.put("x", Kind.RES_x);
		reservedWords.put("y", Kind.RES_y);
		reservedWords.put("a", Kind.RES_a);
		reservedWords.put("r", Kind.RES_r);
		reservedWords.put("X", Kind.RES_X);
		reservedWords.put("Y", Kind.RES_Y);
		reservedWords.put("Z", Kind.RES_Z);
		reservedWords.put("x_cart", Kind.RES_x_cart);
		reservedWords.put("y_cart", Kind.RES_y_cart);
		reservedWords.put("a_polar", Kind.RES_a_polar);
		reservedWords.put("r_polar", Kind.RES_r_polar);
		reservedWords.put("rand", Kind.RES_rand);
		reservedWords.put("sin", Kind.RES_sin);
		reservedWords.put("cos", Kind.RES_cos);
		reservedWords.put("atan", Kind.RES_atan);
		reservedWords.put("if", Kind.RES_if);
		reservedWords.put("while", Kind.RES_while);
		reservedWords.put("red", Kind.RES_red);
		 reservedWords.put("grn", Kind.RES_grn);
		 reservedWords.put("blu", Kind.RES_blu);
     }
	public static HashMap<String, Kind> opsAndSeps;
	static {
		opsAndSeps = new HashMap<String,Kind>();
		opsAndSeps.put(".", Kind.DOT);
		opsAndSeps.put(",", Kind.COMMA);
		opsAndSeps.put("?", Kind.QUESTION);
		opsAndSeps.put(":", Kind.COLON);
		opsAndSeps.put("(", Kind.LPAREN);
		opsAndSeps.put(")", Kind.RPAREN);
		opsAndSeps.put("<", Kind.LT);
		opsAndSeps.put(">", Kind.GT);
		opsAndSeps.put("[", Kind.LSQUARE);
		opsAndSeps.put("]", Kind.RSQUARE);
		opsAndSeps.put("{", Kind.LCURLY);
		opsAndSeps.put("}", Kind.RCURLY);
		opsAndSeps.put("=", Kind.ASSIGN);
		opsAndSeps.put("==", Kind.EQ);
		opsAndSeps.put("<->", Kind.EXCHANGE);
		opsAndSeps.put("<=", Kind.LE);
		opsAndSeps.put(">=", Kind.GE);
		opsAndSeps.put("!", Kind.BANG);
		opsAndSeps.put("&", Kind.BITAND);
		opsAndSeps.put("&&", Kind.AND);
		opsAndSeps.put("|", Kind.BITOR);
		opsAndSeps.put("||", Kind.OR);
		opsAndSeps.put("+", Kind.PLUS);
		opsAndSeps.put("-", Kind.MINUS);
		opsAndSeps.put("*", Kind.TIMES);
		opsAndSeps.put("**", Kind.EXP);
		opsAndSeps.put("/", Kind.DIV);
		opsAndSeps.put("%", Kind.MOD);
    }

	final String input;
	final char[] inputChars;
    int pos;
    char ch;
	int row =1;
	int column;

	int sinceLastRow=0;
	public Scanner(String input){
		//the scanner will go through the input and turn them into tokens
	this.input = input;
	inputChars = Arrays.copyOf(input.toCharArray(),input.length()+1);
	this.pos = 0; //we start at the 0 index
		current = null;
	}
	public Token getCurrent(){
		return current;
	}
	@Override
    public Token next() throws LexicalException {
		//the -1 accounts for the terminating 0 at the end of a char array
			String currentword = "";
			boolean onlydigits = true;
			boolean stringlit = false;
			boolean error = false;
			Token theToken;
			//go through input and return the tokens according to where we are position wise.

			while(pos < inputChars.length -1){
				//updates column that each token starts in

				if(currentword == ""){
					column= pos+1-sinceLastRow;
				}
				//if it's just a 0 in the beginning, then it is a num-lit by itself
				if(currentword.equals("0")){
					NumLitToken aNum = new NumLitToken(currentword,row,column, Kind.NUM_LIT);
					currentword = "";
					current = aNum;
					return aNum;
				}
				if(currentword == "" && inputChars[pos]== '"'){
					stringlit = true;
				}
				//sign of a comment coming up! Ignore content until LF or CR indicates that comment ended
				if(inputChars[pos]=='~'){
					while(inputChars[pos] != '\n'){
						pos++;
					}
					sinceLastRow = pos+1;
				}
				//looks through the ops and seps to see if the current word is matching, or if any larger one is matching
				//gotta fix this to account that there doesn't necessarily need to be white spaces around op
				if(!stringlit&&opsAndSeps.containsKey(currentword)){
					String wholeWord = currentword+inputChars[pos];
					if((wholeWord.equals("<-") || wholeWord.equals("->")) && pos< inputChars.length-2
					&& !((wholeWord+inputChars[pos+1]).equals("<->"))){
						LexicalException e = new LexicalException("exchange op not completely finished");
						throw e;
					}
						if(pos< inputChars.length-2 && opsAndSeps.containsKey(wholeWord + inputChars[pos + 1])){
							currentword+= inputChars[pos];
							currentword+= inputChars[pos+1];
							pos+=2;
							theToken = new Token(currentword,row,column,opsAndSeps.get(currentword));
						}
						else if(opsAndSeps.containsKey(wholeWord)){
							currentword+= inputChars[pos];
							pos++;
							theToken = new Token(currentword,row,column,opsAndSeps.get(currentword));
						}
						else {
							theToken = new Token(currentword, row, column, opsAndSeps.get(currentword));
						}
						current = theToken;
					return theToken;
				}
				//ignore the whitespaces
				if(Character.isWhitespace(inputChars[pos]) && currentword.length()==0){
					currentword="";
				}
				else if(((Character.isWhitespace(inputChars[pos])) || opsAndSeps.containsKey(Character.toString(inputChars[pos]))) && !stringlit){
					//if it's whitespace and currentword has content, means end of a token
					if(!currentword.equals("")) {
						int theNum;
						if (onlydigits && currentword.length() > 0) {
							//if it's only digits, then it is a numeric literal
							try {
								theNum = Integer.parseInt(currentword);
							} catch (Exception NumberFormatException) {
								LexicalException e = new LexicalException("this number is too long");
								throw e;
							}
							NumLitToken aNum = new NumLitToken(currentword, row, column, Kind.NUM_LIT);
							currentword = "";
							current = aNum;
							return aNum;
						} else if (reservedWords.containsKey(currentword)) {
							//is it a reserved word?
							theToken = new Token(currentword, row, column, reservedWords.get(currentword));
						} else if (opsAndSeps.containsKey(currentword)) {
							//double check it's not an op or sep
							theToken = new Token(currentword, row, column, opsAndSeps.get(currentword));
						} else if (error) {
							//has a non letter or digit been given to it as a non-reserved or op/seperator?
							theToken = new Token(currentword, row, column, Kind.ERROR);
							error = false;
							LexicalException e = new LexicalException("This is not a character");
							throw e;
						} else {
							//if it's nothing before, that means it is an identifier
							theToken = new Token(currentword, row, column, Kind.IDENT);
						}
						current = theToken;
						return theToken;
					}
					currentword = "";
					if(opsAndSeps.containsKey(Character.toString(inputChars[pos]))){
						currentword += inputChars[pos];
						column= pos+1-sinceLastRow;
					}
				}//the other upcoming conditions are when all the token characters haven't been put in currentword yet
				else if(currentword == "" && inputChars[pos]=='"'){
					currentword+= inputChars[pos];
					stringlit = true;
					onlydigits = false;
				}
				else if(stringlit && inputChars[pos]== '"'){
					currentword+=inputChars[pos];
					if(!(inputChars[pos-1] =='\\')){
					StringLitToken aString = new StringLitToken(currentword,row,column,Kind.STRING_LIT);
                    currentword = "";
						pos++;
						current =aString;
					return aString;
					}
				}
				else if(!stringlit && !Character.isLetter(inputChars[pos])&& !(Character.isDigit(inputChars[pos])) && inputChars[pos]!= '_'){
                    currentword+= inputChars[pos];
                    onlydigits = false;
					error = true;
				}
				else{
					if(Character.isLetter(inputChars[pos])){
						onlydigits = false;
					}
					currentword+= inputChars[pos];

				}

				if(inputChars[pos]=='\n'){
					row++;
					sinceLastRow = pos+1;
				}

                pos++;
			}
			if(currentword.length()==0){
				theToken = new Token(currentword, row, column, Kind.EOF);
				current = theToken;
				return theToken;
			}
		if(!stringlit &&opsAndSeps.containsKey(currentword)){
			theToken = new Token(currentword, row, column, opsAndSeps.get(currentword));
		}
		else if(reservedWords.containsKey(currentword)){
			theToken = new Token(currentword,row,column,reservedWords.get(currentword));
		}
		else if(onlydigits && currentword.length()>0){
			int theNum;
			try {
				theNum = Integer.parseInt(currentword);
			} catch (Exception NumberFormatException) {
				LexicalException e = new LexicalException("this number is too long");
				throw e;
			}
			NumLitToken aNum = new NumLitToken(currentword,row,column, Kind.NUM_LIT);
			currentword = "";
			current = aNum;
			return aNum;
		}
		else if(error){
			error = false;
			LexicalException e = new LexicalException("this is not a correct character");
			throw e;
			//theToken = new Token(currentword, row, column, Kind.ERROR);
		}
		else if(stringlit){
			theToken = new StringLitToken(currentword, row, column, Kind.NUM_LIT);
		}
		else{
			theToken = new Token(currentword,row,column, Kind.IDENT);
		}
			current = theToken;
			return theToken;
	}	
}
