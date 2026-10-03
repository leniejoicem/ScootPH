package com.payroll.validation;

import com.payroll.validation.ValidationException;
import com.payroll.validation.ValidationGroups.Full;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.*;
import java.util.stream.Collectors;

public class ValidationService {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ValidationService() {}

    public static List<String> validate(Object object, Class<?>... groups) {
        if (object == null) return Collections.emptyList();
        Set<ConstraintViolation<Object>> violations =
                (groups == null || groups.length == 0)
                        ? validator.validate(object)
                        : validator.validate(object, groups);
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
    }

    public static List<String> validateSingleMessagePerField(Object object, Class<?>... groups) {
        if (object == null) return Collections.emptyList();

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (Class<?> group : groups) {
            Set<ConstraintViolation<Object>> violations = validator.validate(object, group);
            for (ConstraintViolation<Object> v : violations) {
                String field = v.getPropertyPath().toString();
                fieldErrors.putIfAbsent(field, v.getMessage());
            }
        }

        return new ArrayList<>(fieldErrors.values());
    }

    public static Map<String, String> fieldErrors(Object object, Class<?>... groups) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (object == null) {
            return fieldErrors;
        }
        for (Class<?> group : groups) {
            for (ConstraintViolation<Object> v : validator.validate(object, group)) {
                fieldErrors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
            }
        }
        return fieldErrors;
    }

    public static List<String> validateAllSingleMessagePerField(Class<?>[] groups, Object... objects) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        if (objects != null) {
            for (Object o : objects) {
                if (o == null) continue;
                for (Class<?> group : groups) {
                    Set<ConstraintViolation<Object>> violations = validator.validate(o, group);
                    for (ConstraintViolation<Object> v : violations) {
                        String field = v.getPropertyPath().toString();
                        fieldErrors.putIfAbsent(field, v.getMessage());
                    }
                }
            }
        }
        return new ArrayList<>(fieldErrors.values());
    }

    public static boolean isValid(Object object, Class<?>... groups) {
        if (object == null) return true;
        return validator.validate(object, groups).isEmpty();
    }

    public static String formatForDialog(List<String> errors) {
        if (errors == null || errors.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Validation errors:\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append(i + 1).append(". ").append(errors.get(i)).append('\n');
        }
        return sb.toString().trim();
    }

    public static Map<String, String> validateWithFields(Object object) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object);

        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<Object> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(fieldName, message);
        }

        return errors;
    }
    public static void validateAndThrow(Object object) throws ValidationException {
        Set<ConstraintViolation<Object>> violations = validator.validate(object);

        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());

            throw new ValidationException("Validation failed", errors);
        }
    }
}
