/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Lotus
 * Development Corporation., http://www.lotus.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xpath.functions;

//import org.w3c.dom.Node;
//import org.w3c.dom.traversal.NodeIterator;
import org.apache.xml.utils.DateTimeObj;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.apache.xpath.parser.regexp.*;
import org.apache.xalan.res.XSLMessages;

/**
 * <meta name="usage" content="advanced"/>
 * Execute the xs:matches() function.
 */
public class FuncReplace extends FunctionMultiArgs
{

  /**
   * Execute the function.  The function must return
   * a valid object.
   * @param xctxt The current execution context.
   * @return A valid XObject.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt) throws javax.xml.transform.TransformerException
  {
  	String input = m_arg0.execute(xctxt).str();
  	String pattern = m_arg1.execute(xctxt).str();
  	String replace = m_arg2.execute(xctxt).str();
  	String flags = "";
  	if (m_args != null)
  	flags = m_args[0].execute(xctxt).str(); 
  	
  	RegularExpression regex = new RegularExpression(pattern, flags);
  	String outString = "";
  	int groups = regex.getNumberOfGroups();
  	Token tokenTree = regex.getTokenTree();
  	int indexVar;
  	if (groups > 1 && (((indexVar = replace.indexOf("$")) > 0 && 
  	                  replace.charAt(indexVar - 1) != '\\') || indexVar==0))
  	{
  		
  		Token child;
  		int index = 0;
       int length = input.length();
       while (index < length)
    {
    	boolean matched = false;
    	String[] s = new String[tokenTree.size()];
    	int t=0;
  		for(int i=0; i<tokenTree.size(); i++)
  		{
  			child = tokenTree.getChild(i);
  			int[] range;
  			// Note: Not really sure this is correct.
  			// Needs review!!! See regex020, E4...
  			if (child.getType() == Token.PAREN &&
  			(i+1 < tokenTree.size()) &&
  			tokenTree.getChild(i+1).getType() == Token.CHAR)
  			{
  				regex.compileToken(tokenTree.getChild(i+1));
  			    range = regex.matchString(input, index, length);
    	       int start = range[0] > length ? length : range[0]; 
    	        regex.compileToken(child);
    	        range = regex.matchString(input, index, start);
  			}
  			else
  			{
  				regex.compileToken(child);
  				range = regex.matchString(input, index, length);
  			}  			
  			
    	int start = range[0];
    	int end = range[1];
    	if (end >= 0)
    	{
    		if (child.getType() == Token.PAREN)
    		s[t++] = input.substring(start, end);
    		outString = outString + input.substring(index, start);
    	    index = end;
    	    matched = true;
  		}
  		else
    	{
    	    s[t++] = "";
    	    outString = outString + input.substring(index);
    	    index = length;    	    
    	    break;
    	}
  		}
  		
  		if(!matched)
  		break;   		
    	
    	//int j = 0;
    	String repVars = "";
    	int start = 0;
    	while(start < replace.length())
    	{
    		indexVar = replace.indexOf("$", start);
    		if (indexVar >= 0)
    		{
    		repVars = repVars+ replace.substring(start, indexVar);
    		if (indexVar >0 && replace.charAt(indexVar-1) == '\\') 
    		{
    		repVars = repVars.replace('\\', '$');
    		start = indexVar + 1;
    		}
    		else
    		{
    			// need to account for the fact that our array starts at 0 
    		repVars = repVars + s[Integer.parseInt(String.valueOf(replace.charAt(indexVar+1))) - 1];
    		start = indexVar + 2;
    		}
    		}
    		else
    		{
    			repVars = repVars+ replace.substring(start);
    			break;
    		}
    	}
    	outString = outString + repVars; 
    }
  	}
  	else
  { 
  	//String outString = "";
  	int index = 0;
    int length = input.length();
    int i=0, j=0;    	
    while (index < length)
    {
    	int[] range = regex.matchString(input, index, length);
    	int start = range[0];
    	int end = range[1];
    	if (end >= 0)
    	{
    		outString = outString + input.substring(index, start);
    	    outString = outString + replace; 
    	    index = end;
    	}
    	else
    	{
    	   outString = outString + input.substring(index);
    	    index = length;
    	}
    }
  }
  	return new XString(outString);
    
  }
  
  /**
   * Check that the number of arguments passed to this function is correct. 
   *
   *
   * @param argNum The number of arguments that is being passed to the function.
   *
   * @throws WrongNumberArgsException
   */
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException
  {
    if (argNum < 3 || argNum > 4)
      reportWrongNumberArgs();
  }

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.
   *
   * @throws WrongNumberArgsException
   */
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
      throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("threeorfour", null));
  }
}
