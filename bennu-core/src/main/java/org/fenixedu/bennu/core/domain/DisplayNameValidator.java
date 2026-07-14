package org.fenixedu.bennu.core.domain;

public interface DisplayNameValidator {
    boolean validate(String displayName, String fullName, UserProfile userProfile);
}
