package org.fenixedu.bennu.core.domain;

import java.util.Arrays;
import java.util.List;

import org.fenixedu.commons.StringNormalizer;

public class DefaultDisplayNameValidator implements DisplayNameValidator {
    @Override
    public boolean validate(String displayName, String fullName, UserProfile userProfile) {
        if (fullName == null) {
            return false;
        }
        List<String> fullnameParts =
                Arrays.asList(StringNormalizer.normalizeAndRemoveAccents(fullName).toLowerCase().trim().split("\\s+|-"));
        List<String> displaynameParts =
                Arrays.asList(StringNormalizer.normalizeAndRemoveAccents(displayName).toLowerCase().trim().split("\\s+|-"));
        return fullnameParts.containsAll(displaynameParts);
    }
}
