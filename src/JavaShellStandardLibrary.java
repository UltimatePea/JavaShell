import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class JavaShellStandardLibrary {

	public PrintStream sysout(){
		return System.out;
	}
	
	public List<Object> arrayToList(Object[] obj){

		return Arrays.asList(obj);
	}
}
