package tetz42.clione.lang;

import tetz42.clione.util.ParamMap;

public class ExtendedParamMap extends ParamMap {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5052097340822472319L;
	private String caller;
	private final ParamMap map;
	
	ExtendedParamMap(ParamMap map){
		this.map = map;
		this.putAll(map);
	}

	ExtendedParamMap caller(String caller) {
		this.caller = caller;
		return this;
	}

	String getCaller() {
		return caller;
	}

	ParamMap getSrc() {
		return map;
	}

}
