package tetz42.clione.node;

import java.util.List;

import tetz42.clione.lang.Instruction;
import tetz42.clione.util.ParamMap;

public class SQLNode implements INode{

	public List<LineNode> nodes;
	public String resourceInfo;

	@Override
	public Instruction perform(ParamMap paramMap) {
		// TODO root should be a field.
		LineNode root = new LineNode(0);
		root.childBlocks = nodes;
		return root.perform(paramMap);
	}

	@Override
	public boolean isMultiLine() {
		if(nodes.size() > 1)
			return true;
		else if(nodes.size() == 0)
			return false;
		return nodes.get(0).isMultiLine();
	}
}
