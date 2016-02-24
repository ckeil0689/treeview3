package ColorChooser;

public enum ColorSchemeType {
	REDGREEN("Red-Green"), YELLOWBLUE("Yellow-Blue"), CUSTOM("Custom");
	
	private final String fieldName;
	
	private ColorSchemeType(final String name) {
		
		this.fieldName = name;
	}
	
	@Override
	public String toString() {
		
		return fieldName;
	}
	
	public static ColorSchemeType getMemberFromKey(final String key) {
		
		ColorSchemeType member;
		
		if("Red-Green".equals(key)) {
			member = REDGREEN;
			
		} else if("Yellow-Blue".equals(key)) {
			member = YELLOWBLUE;
			
		} else if("Custom".equals(key)) {
			member = CUSTOM;
			
		} else {
			member = REDGREEN;
		}
		
		return member;
	}
}
