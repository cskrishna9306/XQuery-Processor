// Generated from main/antlr/XPath.g4 by ANTLR 4.13.1
package main.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link XPathParser}.
 */
public interface XPathListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link XPathParser#eval}.
	 * @param ctx the parse tree
	 */
	void enterEval(XPathParser.EvalContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#eval}.
	 * @param ctx the parse tree
	 */
	void exitEval(XPathParser.EvalContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#absolutePath}.
	 * @param ctx the parse tree
	 */
	void enterAbsolutePath(XPathParser.AbsolutePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#absolutePath}.
	 * @param ctx the parse tree
	 */
	void exitAbsolutePath(XPathParser.AbsolutePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#relativePath}.
	 * @param ctx the parse tree
	 */
	void enterRelativePath(XPathParser.RelativePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#relativePath}.
	 * @param ctx the parse tree
	 */
	void exitRelativePath(XPathParser.RelativePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(XPathParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(XPathParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#tagName}.
	 * @param ctx the parse tree
	 */
	void enterTagName(XPathParser.TagNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#tagName}.
	 * @param ctx the parse tree
	 */
	void exitTagName(XPathParser.TagNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void enterAttributeName(XPathParser.AttributeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void exitAttributeName(XPathParser.AttributeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#fileName}.
	 * @param ctx the parse tree
	 */
	void enterFileName(XPathParser.FileNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#fileName}.
	 * @param ctx the parse tree
	 */
	void exitFileName(XPathParser.FileNameContext ctx);
}