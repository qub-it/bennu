package org.fenixedu.bennu.core.domain;

import com.qubit.terra.framework.services.context.ApplicationUser;

public interface DisplayNameValidator {
    void validate(String displayName, String fullName, ApplicationUser applicationUser);
}
