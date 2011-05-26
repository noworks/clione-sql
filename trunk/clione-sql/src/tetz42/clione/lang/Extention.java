package tetz42.clione.lang;

import static tetz42.clione.util.ClioneUtil.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tetz42.clione.exception.ClioneFormatException;
import tetz42.clione.lang.func.ClioneFunction;
import tetz42.clione.lang.func.Parenthesises;
import tetz42.clione.util.ParamMap;

public class Extention extends ClioneFunction {

	private static final Map<String, ExtFunction> funcMap = Collections
			.synchronizedMap(new HashMap<String, ExtFunction>());

	static {
		putFunction("L", new ExtFunction() {

			@Override
			public Instruction perform() {
				Instruction inst = getFunction("ESCLIKE").perform();
				inst = getFunction("CONCAT").perform(inst);
				inst.replacement = inst.replacement + " ESCAPE '#' ";
				return inst;
			}
		});
		putFunction("ESCLIKE", new ExtFunction() {

			@Override
			protected Instruction perform(Instruction inst) {
				for (int i = 0; i < inst.params.size(); i++) {
					inst.params.set(i, escapeBySharp(inst.params.get(i)));
				}
				if (inst.next != null)
					perform(inst.next);
				return inst;
			}

		});
		putFunction("CONCAT", new ExtFunction() {

			@Override
			public Instruction perform() {
				return null;
			}
		});
		putFunction("IF", new ExtFunction() {

			@Override
			public Instruction perform() {
				return null;
			}
		});
		putFunction("IFLN", new ExtFunction() {

			@Override
			public Instruction perform() {

				return null;
			}
		});
		putFunction("DELNULL", new ExtFunction() {

			@Override
			public Instruction perform() {
				Instruction inst = getValidInstruction();
				if (inst == null)
					return new Instruction();
				List<Object> newParams = new ArrayList<Object>();
				for (Object e : inst.params) {
					if (e != null)
						newParams.add(e);
				}
				inst.params = newParams;
				return inst;
			}
		});
		putFunction("TO_SQL", new ExtFunction() {

			@Override
			public Instruction perform() {
				return null;
			}
		});
		putFunction("TO_STR", new ExtFunction() {

			@Override
			public Instruction perform() {
				return null;
			}
		});
	}

	public static ExtFunction putFunction(String keyword, ExtFunction f) {
		return funcMap.put(keyword, f);
	}

	public static ExtFunction getFunction(String keyword) {
		return funcMap.get(keyword);
	}

	protected final String func;
	protected final boolean isNegative;
	protected ClioneFunction inside;

	public Extention(String key, boolean isNegative, String literal) {
		this.isNegative = isNegative;
		this.func = key;
	}

	@Override
	public ClioneFunction inside(ClioneFunction inside) {
		if (!Parenthesises.class.isInstance(inside)) {
			super.inside(inside);
		}
		this.inside = inside;
		return this;
	}

	@Override
	public ClioneFunction getInside() {
		return this.inside;
	}

	@Override
	public String getSrc() {
		return "%" + (isNegative ? "!" : "") + func
				+ (inside == null ? "" : inside.getSrc());
	}

	@Override
	public Instruction perform(ParamMap paramMap) {
		ExtFunction extFunction = funcMap.get(this.func);
		if (extFunction == null) {
			throw new ClioneFormatException("Unknown function name '"
					+ this.func + "'\nsrc:" + getSrc() + "\nResource info:"
					+ this.resourceInfo);
		}
		// initial process
		ExtFunction.set(this, paramMap);

		Instruction instruction = extFunction.perform();
		// finally process
		ExtFunction.clear();

		return instruction;
	}
}
