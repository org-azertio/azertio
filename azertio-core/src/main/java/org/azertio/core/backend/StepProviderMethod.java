package org.azertio.core.backend;

import org.azertio.core.Assertion;
import org.azertio.core.DataType;
import org.azertio.core.DataTypes;
import org.azertio.core.AzertioException;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import org.azertio.core.util.Pair;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class StepProviderMethod {

	private StepProvider stepProvider;
	private String stepKey;
	private Method method;
	private List<Pair<String, DataType>> expectedArguments;

	private enum LastParameterType {
		REGULAR, ASSERTION, DATA_TABLE, DOCUMENT
	}
	private LastParameterType lastParameterType;



	public StepProviderMethod(StepProvider stepProvider, Method method, DataTypes dataTypes) {
		this.stepProvider = stepProvider;
		var annotation = method.getAnnotation(StepExpression.class);
		this.stepKey = annotation.value();
		this.method = method;
		this.lastParameterType = checkLastParameterType(method);
		this.expectedArguments = Pair.ofMap(checkStepArgs(dataTypes, annotation, method));
	}


	public String stepKey() {
		return stepKey;
	}


	private LastParameterType checkLastParameterType(Method method) {
		if (method.getParameterCount() == 0) {
			return LastParameterType.REGULAR;
		}
		Class<?> methodLastParameterType = method.getParameterTypes()[method.getParameterCount() - 1];
		if (methodLastParameterType == Assertion.class) {
			return LastParameterType.ASSERTION;
		} else if (methodLastParameterType == DataTable.class) {
			return LastParameterType.DATA_TABLE;
		} else if (methodLastParameterType == Document.class) {
			return LastParameterType.DOCUMENT;
		}
		return LastParameterType.REGULAR;
	}




	public void run(Map<String,Object> arguments, Object additionalData) throws Throwable {

		Object[] args = new Object[method.getParameterCount()];

		if (arguments.size() != expectedArguments.size()) {
			throw new AzertioException(
				"Step '{}' expects {} arguments, but {} were provided",
				stepKey,
				expectedArguments.size(),
				arguments.size()
			);
		}

		if (lastParameterType == LastParameterType.REGULAR && additionalData != null) {
			throw new AzertioException(
				"Step '{}' does not expect additional data, but it was provided",
				stepKey
			);
		}

		for (int i = 0; i < this.expectedArguments.size(); i++) {
			Pair<String, DataType> arg = this.expectedArguments.get(i);
			Object value = arguments.get(arg.left());
			Class<?> type = value.getClass();
			if (!method.getParameterTypes()[i].isAssignableFrom(type)) {
				throw new AzertioException(
					"Argument '{}' has type {}, but expected type is {}",
					arg.left(),
					value.getClass().getSimpleName(),
					method.getParameterTypes()[i].getSimpleName()
				);
			}
			args[i] = value;
		}

		if (lastParameterType != LastParameterType.REGULAR) {
			args[args.length - 1] = Objects.requireNonNull(
				additionalData,
				"Additional data must not be null for last parameter type: " + lastParameterType)
			;
		}

		try {
			method.invoke(stepProvider, args);
		} catch (IllegalAccessException e) {
			throw new AzertioException(e);
		} catch (InvocationTargetException e) {
			throw (e.getCause() != null) ? e.getCause() : e.getTargetException();
		}
	}


	private LinkedHashMap<String, DataType> checkStepArgs(DataTypes datatypes, StepExpression step, Method method) {
	   if (step.args().length == 0) {
			return checkImplicitStepArgs(datatypes, step, method);
		} else {
			return checkExplicitStepArgs(datatypes, step, method, step.args());
		}
	}


	private LinkedHashMap<String, DataType> checkImplicitStepArgs(DataTypes dataTypes, StepExpression step, Method method) {

		var result = new LinkedHashMap<String, DataType>();
		if (method.getParameterCount() == 0) {
			return result;
		}

		int lastParameterIndex = method.getParameterCount();
		if (lastParameterType != LastParameterType.REGULAR) {
			lastParameterIndex--;
		}

		for (int i = 0; i < lastParameterIndex; i++) {
			DataType type = dataTypes.byJavaType(method.getParameterTypes()[i]);
			if (result.containsKey(type.name())) {
				throwError(step, method, "Duplicate data type '{}'. Declare arguments explicitly.",type.name());
			}
			result.put(type.name(), type);
		}
		return result;
	}




	private LinkedHashMap<String, DataType> checkExplicitStepArgs(DataTypes dataTypes, StepExpression step, Method method, String[] args) {

		var result = new LinkedHashMap<String, DataType>();

		int lastParameterIndex = method.getParameterCount();
		if (lastParameterType == LastParameterType.ASSERTION ||
				lastParameterType == LastParameterType.DATA_TABLE ||
				lastParameterType == LastParameterType.DOCUMENT
		) {
			lastParameterIndex--;
		}

		if (step.args().length != lastParameterIndex) {
			throwError(
				step,
				method,
				"Step has {} arguments, but method expects {} parameters.",
				step.args().length,
				method.getParameterCount()
			);
		}

		for (int i = 0; i < args.length; i++) {
			String[] parts = args[i].split(":");
			if (parts.length != 2) {
				throwError(step, method, "Invalid argument format '{}'. Expected format is 'name:type'.", args[i]);
			}
			String name = parts[0];
			DataType type = dataTypes.byName(parts[1]);
			if (type.javaType() != method.getParameterTypes()[i]) {
				throwError(
					step,
					method,
					"Type mismatch for argument '{}'. Expected type: {}, but got: {}",
					name,
					type.javaType().getSimpleName(),
					method.getParameterTypes()[i].getSimpleName()
				);
			}
			result.put(name, type);
		}
		return result;
	}


	public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
		return method.isAnnotationPresent(annotationClass);
	}


	private void throwError(StepExpression step, Method method, String message, Object... args) {
		Object[] messageArgs = new Object[args.length + 3];
		messageArgs[0] = step.value();
		messageArgs[1] = stepProvider.getClass().getSimpleName();
		messageArgs[2] = method.getName();
		System.arraycopy(args, 0, messageArgs, 3, args.length);
		throw new AzertioException("Error in step '{}' ({}.{}): " + message, messageArgs);
	}
}
