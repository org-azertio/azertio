package org.azertio.core.backend;


import org.myjtools.imconfig.Config;
import org.azertio.core.AssertionFactories;
import org.azertio.core.DataTypes;
import org.azertio.core.AzertioException;
import org.azertio.core.contributors.SetUp;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.contributors.TearDown;
import org.azertio.core.expressions.ExpressionMatcher;
import org.azertio.core.expressions.ExpressionMatcherBuilder;
import org.azertio.core.expressions.Match;
import org.azertio.core.messages.Messages;
import org.azertio.core.util.Log;
import org.azertio.core.util.Pair;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StepProviderService {

    private static final Log log = Log.of();

    private final Messages messages;
    private final StepProvider stepProvider;
    private final Map<String, StepProviderMethod> runnableMethods = new LinkedHashMap<>();
    private final List<Method> setupMethods = new ArrayList<>();
    private final List<Method> teardownMethods = new ArrayList<>();
    private final ExpressionMatcherBuilder matcherBuilder;
    private final Map<String, ExpressionMatcher> matcherCache = new ConcurrentHashMap<>();
    private final Set<String> failedExpressions = ConcurrentHashMap.newKeySet();


    public StepProviderService(
        StepProvider stepProvider,
        DataTypes dataTypes,
        AssertionFactories assertionFactories,
        Messages messages
    ) {

        this.stepProvider = stepProvider;
        this.messages = messages;
        this.matcherBuilder = new ExpressionMatcherBuilder(dataTypes, assertionFactories);

        var methods = stepProvider.getClass().getMethods();
        for (var method : methods) {
            var step = method.getAnnotation(StepExpression.class);
            addRunnableMethod(dataTypes, method, step);
            addMethod(SetUp.class, method, setupMethods);
            addMethod(TearDown.class, method, teardownMethods);
        }
    }


    public String providerLabel() {
        String name = stepProvider.getClass().getSimpleName();
        return name.endsWith("StepProvider") ? name.substring(0, name.length() - "StepProvider".length()) : name;
    }

    public List<String> stepStringsForLocale(Locale locale) {
        try {
            var localeMessages = messages.forLocale(locale);
            return runnableMethods.keySet().stream()
                .map(localeMessages::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public Optional<Pair<StepProviderMethod, Match>> matchingStep(String step, Locale locale) {
        for (var entry : runnableMethods.entrySet()) {
            String stepKey = entry.getKey();
            StepProviderMethod runnableStep = entry.getValue();
            String keyExpression = messages.forLocale(locale).get(stepKey);
            if (keyExpression == null) {
                keyExpression = stepKey;
            }
            if (failedExpressions.contains(keyExpression)) continue;
            ExpressionMatcher matcher = matcherCache.get(keyExpression);
            if (matcher == null) {
                try {
                    matcher = matcherBuilder.buildExpressionMatcher(keyExpression);
                    matcherCache.put(keyExpression, matcher);
                } catch (AzertioException e) {
                    log.warn("Cannot build matcher for step '{}': {}", keyExpression, e.getMessage());
                    failedExpressions.add(keyExpression);
                    continue;
                }
            }
            var matchingStep = matcher.matches(step, locale).map(match -> Pair.of(runnableStep, match));
            if (matchingStep.isPresent()) {
                return matchingStep;
            }
        }
        return Optional.empty();
    }


    private void addRunnableMethod(DataTypes dataTypes, Method method, StepExpression step) {
        if (step != null) {
            try {
                checkMethodNotStatic(method);
                checkMethodPublic(method);
                runnableMethods.put(step.value(), new StepProviderMethod(stepProvider, method, dataTypes));
            } catch (AzertioException e) {
                log.error(e);
            }
        }
    }


    public void setUp(Config config) {
        stepProvider.init(config);
        try  {
            for (Method setupMethod : setupMethods) {
                setupMethod.invoke(stepProvider);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AzertioException(e);
        }
    }

    public void tearDown() {
        try {
            for (Method tearDownMethod : teardownMethods) {
                tearDownMethod.invoke(stepProvider);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AzertioException(e);
        }
    }



    private void addMethod(Class<? extends Annotation> annotation, Method method, List<Method> methods) {
        if (method.isAnnotationPresent(annotation)) {
            try {
                checkMethodWithNoArguments(method);
                checkMethodNotStatic(method);
                checkMethodPublic(method);
                methods.add(method);
            } catch (AzertioException e) {
                log.error(e);
            }
        }
    }



    private void checkMethodPublic(Method method) {
        if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
            throw new AzertioException(
                "Setup method '{}.{}' must be public.",
                stepProvider.getClass().getSimpleName(),
                method.getName()
            );
        }
    }

    private void checkMethodNotStatic(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new AzertioException(
                "Setup method '{}.{}' must be static.",
                stepProvider.getClass().getSimpleName(),
                method.getName()
            );
        }
    }

    private void checkMethodWithNoArguments(Method method) {
        if (method.getParameterTypes().length > 0) {
            throw new AzertioException(
                "Setup method '{}.{}' must not have any arguments.",
                stepProvider.getClass().getSimpleName(),
                method.getName()
            );
        }
    }



}

