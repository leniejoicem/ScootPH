package com.payroll.validation;

public final class AccountGroups {
    private AccountGroups() {}
        public interface Required {}
        public interface Format {}
        @jakarta.validation.GroupSequence({ Required.class, Format.class })
        public interface Full {}
}
