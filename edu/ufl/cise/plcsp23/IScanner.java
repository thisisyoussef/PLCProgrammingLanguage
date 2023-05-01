
//package assignmentoneforCOP4020;
package edu.ufl.cise.plcsp23;
import java.util.Arrays;

public interface IScanner {
	/**
	 * Return an IToken and advance the internal position so that subsequent calls
	 * will return subsequent ITokens.
	 * 
	 * @return
	 * @throws LexicalException
	 */
	IToken next() throws LexicalException;
	}
