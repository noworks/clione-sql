package tetz42.clione.lang;

import static tetz42.clione.util.ClioneUtil.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tetz42.clione.exception.ClioneFormatException;
import tetz42.clione.lang.SampleOfRegexp.Unit;
import tetz42.clione.lang.func.ClioneFunction;
import tetz42.clione.lang.func.DefaultParam;
import tetz42.clione.lang.func.Extention;
import tetz42.clione.lang.func.LineCond;
import tetz42.clione.lang.func.LineParam;
import tetz42.clione.lang.func.Param;
import tetz42.clione.lang.func.Parenthesises;
import tetz42.clione.lang.func.PartCond;
import tetz42.clione.lang.func.RequireParam;
import tetz42.clione.lang.func.SQLLiteral;
import tetz42.clione.lang.func.StrLiteral;

public class ClioneFuncFactory {

	private static final Pattern delimPtn = Pattern.compile("[()'\":]");
	private static final Pattern funcPtn = Pattern
			.compile("([$@&?#%]?)(!?)([a-zA-Z0-9\\.\\-_]*)(\\s+|$))");

	public static ClioneFuncFactory get(String resourceInfo) {
		return new ClioneFuncFactory(resourceInfo);
	}

	private final String resourceInfo;

	private ClioneFuncFactory(String resourceInfo) {
		this.resourceInfo = resourceInfo;
	}

	public ClioneFunction parse(String src) {
		ClioneFunction cf = parseByDelim(src);
		for(Unparsed unparsed : Unparsed.unparsedList.get()){
			// TODO implement correctly.
			parse(unparsed.getString(), funcPtn.matcher(src));
		}
		Unparsed.unparsedList.set(null);
		return parse(src, funcPtn.matcher(src));
	}

	private ClioneFunction parseByDelim(String src) {
		Unit unit = parseByDelim(src, delimPtn.matcher(src), 0);
		if (unit.isEndParenthesis)
			throw new ClioneFormatException("Parenthesises Unmatched! src = "
					+ src);
		return unit.clioneFunc;
	}

	private Unit parseByDelim(String src, Matcher m, int begin) {
		Unit unit = new Unit();
		if (!m.find())
			return unit.clioneFunc(new Unparsed(src.substring(begin)));
		if (begin < m.start())
			unit.clioneFunc = new Unparsed(src.substring(begin, m.start()));
		Unit resultUnit;
		String delim = m.group(0);
		if (delim.equals("'"))
			resultUnit = genStr(src, m);
		else if (delim.equals("\""))
			resultUnit = genSQL(src, m);
		else if (delim.equals("("))
			resultUnit = parenthesises(src, m);
		else if (delim.equals(")"))
			resultUnit = new Unit().endPar(true);
		else // ':'
			resultUnit = new Unit().clioneFunc(new SQLLiteral(src.substring(m.end()),
					true));
		return unit.clioneFunc == null ? resultUnit : joinUnit(unit, resultUnit);
	}

	private Unit parenthesises(String src, Matcher m) {
		Unit inside = parseByDelim(src, m, m.end());
		if (!inside.isEndParenthesis)
			throw new ClioneFormatException("Parenthesises Unmatched! src = "
					+ src);
		Parenthesises par = new Parenthesises(inside.clioneFunc);
		Unit unit = parseByDelim(src, m, m.end());
		return unit.clioneFunc(par.$next(unit.clioneFunc));
	}

	private Unit genStr(String src, Matcher m) {
		return joinUnit(new Unit().clioneFunc(new StrLiteral(endQuotation(src, m, "'"))),
				parseByDelim(src, m, m.end()));
	}

	private Unit genSQL(String src, Matcher m) {
		return joinUnit(new Unit().clioneFunc(new SQLLiteral(endQuotation(src, m, "\""),
				false)), parseByDelim(src, m, m.end()));
	}

	private String endQuotation(String src, Matcher m, String quot) {
		int begin = m.end();
		while (m.find()) {
			if (quot.equals(m.group(0))) {
				if (quot.equals(nextChar(src, m.end()))) {
					m.find();
				} else {
					return src.substring(begin, m.start());
				}
			}
		}
		throw new ClioneFormatException(quot.equals("'") ? "Single" : "Double"
				+ " quotation Unmatched! data = " + src);
	}

	private Unit joinUnit(Unit unit, Unit nextUnit) {
		unit.clioneFunc.setNext(nextUnit.clioneFunc);
		return unit.endPar(nextUnit.isEndParenthesis);
	}

	private String nextChar(String src, int pos) {
		if (pos >= src.length())
			return null;
		return src.substring(pos, pos + 1);
	}
	
	private ClioneFunction parse(String src, Matcher m) {
		if (!m.find())
			throw new ClioneFormatException("Unsupported Grammer :" + src);
		ClioneFunction clione = gen(src, m);
		if (clione == null)
			return null;
		clione.setResourceInfo(resourceInfo);
		if (clione.isTerminated())
			return clione;
		clione.setNext(parse(src, m));
		return clione;
	}

	private ClioneFunction gen(String src, Matcher m) {
		if (isNotEmpty(m.group(4)))
			// '***''***'
			return new StrLiteral(m.group(4).replace("''", "'"));
		else if (isNotEmpty(m.group(7)))
			// "***""***"
			return new SQLLiteral(m.group(7).replace("\"\"", "\""), false);
		else if (isNotEmpty(m.group(17)))
			// :****$
			return new SQLLiteral(src.substring(m.end(17)).replaceAll(
					"\\\\(.)", "$1"), true);
		else
			return gen(src, m, m.group(10), m.group(11), m.group(12));
	}

	private ClioneFunction gen(String src, Matcher m, String func, String not,
			String key) {
		// System.out.println("func=" + func + ", not=" + not + ", key=" + key);
		if (isAllEmpty(func, not, key))
			return null;
		if (isAllEmpty(func, not))
			return new Param(key);
		if (isNotEmpty(func)) {
			if (func.equals("$"))
				return new LineParam(key, isNotEmpty(not));
			if (func.equals("@"))
				return new RequireParam(key);
			if (func.equals("?"))
				return new DefaultParam(key);
			if (func.equals("#"))
				return new PartCond(key, isNotEmpty(not));
			if (func.equals("&"))
				return new LineCond(key, isNotEmpty(not));
			if (func.equals("%"))
				return new Extention(key, isNotEmpty(not), src.substring(m
						.end(2)));
		}
		throw new ClioneFormatException("Unsupported Grammer :" + src);
	}

	static class Unit {
		Unit clioneFunc(ClioneFunction clioneFunc) {
			this.clioneFunc = clioneFunc;
			return this;
		}

		Unit endPar(boolean isEndParenthesis) {
			this.isEndParenthesis = isEndParenthesis;
			return this;
		}

		ClioneFunction clioneFunc = null;
		boolean isEndParenthesis = false;
	}
}