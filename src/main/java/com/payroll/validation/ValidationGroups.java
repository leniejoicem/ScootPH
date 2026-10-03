package com.payroll.validation;

public final class ValidationGroups {
    private ValidationGroups() {}
        public interface Required {}
        public interface Format {}
        @jakarta.validation.GroupSequence({ Required.class, Format.class })
        public interface Full {}
}
