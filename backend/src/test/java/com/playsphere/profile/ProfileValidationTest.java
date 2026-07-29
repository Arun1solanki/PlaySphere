package com.playsphere.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.playsphere.profile.dto.ProfileUpsertRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ProfileValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidIndianMobileNumber() {
        var request = requestWithPhone("+919876543210");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingCountryCode() {
        var request = requestWithPhone("9876543210");
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("phoneNumber"));
    }

    private ProfileUpsertRequest requestWithPhone(String phone) {
        return new ProfileUpsertRequest(
                "Test Player", phone, "Navi Mumbai", "Vashi", "Near station", "Bio", null,
                "Football", "INTERMEDIATE", "Defender", "Weekends", null, null, true
        );
    }
}
