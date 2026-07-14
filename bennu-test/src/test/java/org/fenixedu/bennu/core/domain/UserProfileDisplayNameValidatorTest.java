package org.fenixedu.bennu.core.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class UserProfileDisplayNameValidatorTest {

    @Test
    public void testDefaultValidatorAcceptsValidDisplayName() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertTrue(validator.validate("João", "João Silva", null));
    }

    @Test
    public void testDefaultValidatorRejectsInvalidDisplayName() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertFalse(validator.validate("Pedro", "João Silva", null));
    }

    @Test
    public void testDefaultValidatorRejectsWhenFullNameIsNull() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertFalse(validator.validate("João", null, null));
    }

    @Test
    public void testDefaultValidatorHandlesAccents() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertTrue(validator.validate("Joao", "João Silva", null));
    }

    @Test
    public void testDefaultValidatorHandlesMultiWordDisplayName() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertTrue(validator.validate("João Silva", "João Maria Silva", null));
    }

    @Test
    public void testDefaultValidatorRejectsExtraWords() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertFalse(validator.validate("João Maria Pedro", "João Silva", null));
    }

    @Test
    public void testDefaultValidatorHandlesHyphens() {
        DefaultDisplayNameValidator validator = new DefaultDisplayNameValidator();
        assertTrue(validator.validate("Maria-José", "Maria José Silva", null));
    }

    @Test
    public void testUserProfileChangeNameWithValidDisplayName() {
        UserProfile profile = new UserProfile("João", "Silva", "João", "test@test.com", null);
        assertEquals("João", profile.getDisplayName());
    }

    @Test(expected = org.fenixedu.bennu.core.domain.exceptions.BennuCoreDomainException.class)
    public void testUserProfileChangeNameWithInvalidDisplayName() {
        new UserProfile("João", "Silva", "Pedro", "test@test.com", null);
    }

    @Test
    public void testUserProfileChangeNameWithNullDisplayName() {
        UserProfile profile = new UserProfile("João", "Silva", null, "test@test.com", null);
        assertEquals("João Silva", profile.getDisplayName());
    }

    @Test
    public void testUserProfileChangeNameUpdatesName() {
        UserProfile profile = new UserProfile("João", "Silva", "João", "test@test.com", null);
        profile.changeName("Pedro", "Santos", "Pedro");
        assertEquals("Pedro", profile.getDisplayName());
        assertEquals("Pedro Santos", profile.getFullName());
    }
}
