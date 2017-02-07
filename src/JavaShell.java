import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class JavaShell {

	private static final String LOAD_CMD_NAME = "load";

	public static void main(String[] args) {
		System.out.println("Welcome to Java Shell");
		System.out.println("Use \"load [classname]\" to load a class");
		new JavaShell().interactive();
	}

	private String shellPrompt = ">>> ";

	private Class operationClass;
	private Object operatingInstance;
	private Map<String, Object> bindings = new HashMap<>();

	/**
	 * Enter the interactive shell
	 */
	private void interactive() {
		Scanner s = new Scanner(System.in);
		while (true) {
			System.out.print(shellPrompt);
			while (s.hasNextLine()) {
				String cmd = s.nextLine();
				try {
					interpreteCommand(cmd);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print(shellPrompt);
			}
		}

	}

	/**
	 * Interpretes a given command
	 * 
	 * @param cmd
	 */
	private void interpreteCommand(String cmd) {
		StringTokenizer tokenizer = new StringTokenizer(cmd);

		try {
			// try to get the action command
			String actionName = tokenizer.nextToken();

			/* Loading a class */
			if (actionName.equals(LOAD_CMD_NAME)) {
				try {
					String classname = tokenizer.nextToken();
					load(classname, tokenizer);
				} catch (NoSuchElementException e) {
					printUsage(LOAD_CMD_NAME);
				}

				/* Listing commands */

			} else if (actionName.equals("ls")) {

				try {
					ls();
				} catch (NoOperatingClassException e) {
					printDefaultPrompt();
				}
			}

			else {
				System.out.print("Unrecognized Command. ");
				printDefaultPrompt();

			}

			// catch the exception fo reading the first token
		} catch (NoSuchElementException e) {
			System.out.println("Please specify the command name");
		}

	}

	/**
	 * Prints the prompt that user should load a class
	 */
	private void printDefaultPrompt() {
		if (operationClass == null) {
			System.out.println("Use \"" + LOAD_CMD_NAME + "\" to load a class");
		} else {
			System.out
					.println("Type \"ls\" to see list of operations available");
		}
	}

	/**
	 * List a command for the operating class
	 * 
	 * @throws NoOperatingClassException
	 *             if class not loaded
	 */
	private void ls() throws NoOperatingClassException {

		if (operationClass == null)
			throw new NoOperatingClassException();
		Method[] methods = operationClass.getMethods();

		for (Method method : methods) {
			System.out.println(methodSignatureToString(method));
		}

	}

	/**
	 * Prints a string representation of the method
	 * 
	 * @param m
	 *            method to be printed
	 * @return the strin representation
	 */
	private String methodSignatureToString(Executable m) {
		StringBuilder builder = new StringBuilder();
		Class[] params = m.getParameterTypes();
		builder.append(m.getName() + " ");
		for (Class param : params) {
			builder.append("<" + param.getName() + "> ");
		}
		return builder.toString();
	}

	/**
	 * Loads a class
	 * 
	 * @param className
	 *            name of the class to be loaded
	 */
	@SuppressWarnings("unchecked")
	private void load(String className, StringTokenizer tokenizer) {

		try {
			// finds the class
			Class operationClass = Class.forName(className);
			Constructor[] cons = operationClass.getConstructors();

			// gets the string arguments
			ArrayList<String> arguments = new ArrayList<>();
			while (tokenizer.hasMoreTokens())
				arguments.add(tokenizer.nextToken());

			// iterate through constructors
			for (Constructor con : cons) {
				Class[] params = con.getParameterTypes();


				//only select the constructor with the same number of arguments
				if (params.length != arguments.size()) continue;

					// trys to construct arguments
					Object[] argumentObjs = new Object[params.length];
					for (int i = 0; i < params.length; i++) {
						String argumentStr = arguments.get(i);
						Object argumentObj;

						// first lookup the table
						if (bindings.containsKey(argumentStr)) {
							argumentObj = bindings.get(argumentStr);
						} else {
							try {
								Constructor c = params[i]
										.getConstructor(argumentStr.getClass());
								try {
									argumentObj = c.newInstance(argumentStr);
									argumentObjs[i] = argumentObj;

								} catch (InstantiationException e) {
									e.printStackTrace();
									continue;
								} catch (IllegalAccessException e) {
									e.printStackTrace();
									continue;
								} catch (IllegalArgumentException e) {
									e.printStackTrace();
									continue;
								} catch (InvocationTargetException e) {
									e.printStackTrace();
									continue;
								} // end try newInstance
							} catch (NoSuchMethodException e) {
								System.out.println(
										"Cannot Find a Constructor with Single String Argument for argument "
												+ i);
								continue;
							} catch (SecurityException e) {
								e.printStackTrace();
								continue;
							} // end try getConstructor
						} // end if bindings

					} // end for i in params

					// now we've got arguemnts ready, construct the object
					try {
						Object obj = con.newInstance(argumentObjs);
						this.operationClass = operationClass;
						this.operatingInstance = obj;
						System.out.println("Instance Constructed Successfully, ignore any instantiation errors, should there be any");
						//break the constructor for loop
						break;
					} catch (InstantiationException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						continue;
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						continue;
					} catch (InvocationTargetException e) {
						e.printStackTrace();
						continue;
					}
					

				}// end for con in constructors
			
			//for loop completed
			//no constructor found
			System.out.println("Cannot find a suitable contructor for given type");
			for(Constructor con : cons){
				System.out.println(methodSignatureToString(con));
			}

			

		} catch (ClassNotFoundException e) {
			System.out.printf("Class %s not found.\n", className);
			printUsage(LOAD_CMD_NAME);
		}
	}

	/**
	 * Prints the usage name for a command
	 * 
	 * @param commandName
	 *            command name
	 */
	private void printUsage(String commandName) {
		System.out.println("Usage : " + usage(commandName));
	}

	/**
	 * 
	 * @param cmdName
	 *            the command name
	 * @return the usage prompt for the command
	 */
	private String usage(String cmdName) {
		if (cmdName == LOAD_CMD_NAME) {
			return "load [classname]";
		}
		return null;
	}

}
