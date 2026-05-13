package com.MenuBank.MenuBank.validation;

import com.MenuBank.MenuBank.user.UserRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, UserRequest> {

    @Override
    public boolean isValid(UserRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String password = value.getPassword();
        String confirmPassword = value.getConfirmPassword();

        if (password == null || password.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            return true;
        }

        return password.equals(confirmPassword);
    }
}

