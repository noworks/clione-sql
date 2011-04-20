package tetz42.clione.lang.func;

import static tetz42.clione.util.ClioneUtil.*;
import tetz42.clione.exception.ParameterNotFoundException;
import tetz42.clione.lang.Instruction;
import tetz42.clione.util.ParamMap;

public class RequireParam extends AbstractParam {

	public RequireParam(String key) {
		super(key, false);
	}
	
	public RequireParam(ClioneFunction inside) {
		super(inside, false);
	}

	@Override
	protected Instruction caseParamNotExists(ParamMap paramMap,
			Instruction paramInst) {
		throw new ParameterNotFoundException("The parameter, "
				+ param.getString() + ", is required." + CRLF + resourceInfo);
	}
}
