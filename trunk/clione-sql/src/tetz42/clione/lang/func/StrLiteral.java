package tetz42.clione.lang.func;

import tetz42.clione.lang.Instruction;
import tetz42.clione.util.ParamMap;

public class StrLiteral extends ClioneFunction {

	private String str;

	public StrLiteral(String str) {
		this.str = str.replaceAll("\\\\(.)", "$1");
	}

	@Override
	public Instruction perform(ParamMap paramMap) {
		Instruction instruction = getNextInstruction(paramMap);
		instruction.replacement = instruction.replacement == null ? this.str
				: this.str + instruction.replacement;
		return instruction;
	}

	@Override
	public String getSrc() {
		return "'" + str + "'";
	}
}