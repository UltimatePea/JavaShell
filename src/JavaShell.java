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
	private Object rtObj;

	/**
	 * Enter the interactive shell
	 */
	private void interactive() {
		Scanner s = new Scanner(System.in);
		while (true) {
			// prints the shell prompt
			System.out.print(shellPrompt);

			// read and execute next lines
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
			} else if (actionAvailable(actionName)) {

				executeAction(actionName, tokenizer);
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
	 * Executes a specific action
	 * 
	 * @param actionName
	 * @param tokenizer
	 */
	private void executeAction(String actionName, StringTokenizer tokenizer) {
			Method[] methods = operationClass.getMethods();

			// gets the string arguments
			ArrayList<String> arguments = argumentListFromTokenizer(tokenizer);
			Exception excp = null;
			boolean success = false;

			// iterate through methods
			for (Method mtd : methods) {

				Object[] arguemntObjs;
				try {
					arguemntObjs = argumentObjectsForExecutableWithStringParameters(
							mtd, arguments);
					// now we've got arguemnts ready, construct the object
						try {
							Object obj = mtd.invoke(operatingInstance, arguemntObjs);
							System.out.println("The return value is " + obj);
							rtObj = obj;
							success = true;
							break;
						} catch (IllegalAccessException
								| IllegalArgumentException
								| InvocationTargetException e) {
							excp = e;
						}
				} catch (UnableToConstructException e1) {
					excp = e1;
				}

			} // end for mtd in Methods

			if (success) {
				System.out.println(
						" Method executed Successfully, ignore any invokation errors, should there be any");

			} else {
				// for loop completed
				// no method executes successfully
				System.out.println(
						"Cannot find a suitable method for given type "
								+ operationClass.getName());
				System.out.println("Type \"ls\" to see a list of suitable methods");
				
				if (excp != null) {
					System.out.println(
							"Should there be any exceptions during construction, below are the messages.");
					System.out.println(excp.getMessage());
				}
			}
	}

	/**
	 * Checks if an action is available from the current operating class This
	 * method checks name only, it does not check arguments
	 * 
	 * @param actionName
	 *            the name of the action
	 * @return
	 */
	private boolean actionAvailable(String actionName) {
		for (Method m : operationClass.getMethods()) {
			if (m.getName().equals(actionName)) {
				return true;
			}
		}
		return false;
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
	 * Trys to construct an argument list from string command line input
	 * 
	 * @param exec
	 *            Constructor or Method
	 * @param arguments
	 *            the tokenized string arguments
	 * @return the constructed object
	 * @throws UnableToConstructException
	 *             if any error occurs
	 */
	private Object[] argumentObjectsForExecutableWithStringParameters(
			Executable exec, ArrayList<String> arguments)
			throws UnableToConstructException {
		Class[] paramsTypes = exec.getParameterTypes();

		// only select the constructor with the same number of arguments
		if (paramsTypes.length != arguments.size())
			throw new UnableToConstructException(
					"Type parameters count does not match");

		// trys to construct arguments
		Object[] argumentObjs = new Object[paramsTypes.length];
		for (int i = 0; i < paramsTypes.length; i++) {
			String argumentStr = arguments.get(i);
			Object argumentObj;

			// first lookup the table
			if (bindings.containsKey(argumentStr)) {
				argumentObj = bindings.get(argumentStr);
			} else {
				// then construct from single parameter-typed String
				try {
					// gets the single string constructor
					Constructor c = paramsTypes[i]
							.getConstructor(argumentStr.getClass());
					try {
						argumentObj = c.newInstance(argumentStr);
						argumentObjs[i] = argumentObj;
					} catch (InstantiationException | IllegalAccessException
							| IllegalArgumentException
							| InvocationTargetException e) {
						throw new UnableToConstructException(
								e.getClass().getName() + " :: "
										+ e.getMessage());
					}

					/* Single Sring constructor does not exist */
				} catch (NoSuchMethodException e) {
					throw new UnableToConstructException(
							"Cannot Find a Constructor with Single String Argument for argument "
									+ i);
				} catch (SecurityException e) {
					throw new UnableToConstructException(e.getMessage());
				} // end try getConstructor
			} // end if bindings

		} // end for i in params

		// no exception thrown sofar, return constructed list
		return argumentObjs;

	}

	/**
	 * Get a list of argument from a tokenizer
	 * 
	 * @param tokenizer
	 * @return
	 */
	private ArrayList<String> argumentListFromTokenizer(
			StringTokenizer tokenizer) {
		// gets the string arguments
		ArrayList<String> arguments = new ArrayList<>();
		while (tokenizer.hasMoreTokens())
			arguments.add(tokenizer.nextToken());
		return arguments;
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
			ArrayList<String> arguments = argumentListFromTokenizer(tokenizer);
			Exception excp = null;

			// iterate through constructors
			for (Constructor con : cons) {

				Object[] arguemntObjs;
				try {
					arguemntObjs = argumentObjectsForExecutableWithStringParameters(
							con, arguments);
					// now we've got arguemnts ready, construct the object
					try {
						Object obj = con.newInstance(arguemntObjs);
						this.operationClass = operationClass;
						this.operatingInstance = obj;
						// break the constructor for loop
						break;
					} catch (InstantiationException | IllegalAccessException
							| IllegalArgumentException
							| InvocationTargetException e) {
						excp = e;
					}
				} catch (UnableToConstructException e1) {
					excp = e1;
				}

			} // end for con in constructors

			if (this.operationClass != null && this.operatingInstance != null) {
				System.out.println(
						"Instance Constructed Successfully, ignore any instantiation errors, should there be any");

			} else {
				// for loop completed
				// no constructor found
				System.out.println(
						"Cannot find a suitable contructor for given type "
								+ operationClass.getName());
				System.out.println("Below are a list of suitable contructors");
				for (Constructor con : cons) {
					System.out.println(methodSignatureToString(con));

				}
				if (excp != null) {
					System.out.println(
							"Should there be any exceptions during construction, below are the messages.");
					System.out.println(excp.getMessage());
				}
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
