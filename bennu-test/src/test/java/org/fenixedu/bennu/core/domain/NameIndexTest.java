package org.fenixedu.bennu.core.domain;

import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.fenixframework.test.core.FenixFrameworkRunner;

@RunWith(FenixFrameworkRunner.class)
public class NameIndexTest {

    private UserProfile createProfile(String givenNames, String familyNames, String displayName) {
        return new UserProfile(givenNames, familyNames, displayName, "test@example.com", Locale.getDefault());
    }

    @Test
    public void testSearchByDisplayNameSameAsFullName() {
        UserProfile profile = createProfile("Joao", "Silva", "Joao Silva");
        assertTrue(NameIndex.search("joao", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("silva", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("joao silva", 10).collect(Collectors.toList()).contains(profile));
    }

    @Test
    public void testSearchByDisplayNameSubsetOfFullName() {
        UserProfile profile = createProfile("Pedro", "Fernandes", "Pedro");
        assertTrue(NameIndex.search("pedro", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("fernandes", 10).collect(Collectors.toList()).contains(profile));
    }

    @Test
    public void testSearchWithNullDisplayName() {
        UserProfile profile = createProfile("Ana", "Costa", null);
        assertTrue(NameIndex.search("ana", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("costa", 10).collect(Collectors.toList()).contains(profile));
    }

    @Test
    public void testSearchMultiWordQuery() {
        UserProfile profile = createProfile("Carlos", "Mendes", "Carlos Mendes");
        assertTrue(NameIndex.search("carlos mendes", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("carlos", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("mendes", 10).collect(Collectors.toList()).contains(profile));
    }

    @Test
    public void testMultipleProfilesSearchable() {
        UserProfile p1 = createProfile("Joao", "Silva", "Joao Silva");
        UserProfile p2 = createProfile("Maria", "Santos", "Maria Santos");
        assertTrue(NameIndex.search("joao", 10).collect(Collectors.toList()).contains(p1));
        assertTrue(NameIndex.search("maria", 10).collect(Collectors.toList()).contains(p2));
        assertTrue(NameIndex.search("silva", 10).collect(Collectors.toList()).contains(p1));
        assertTrue(NameIndex.search("santos", 10).collect(Collectors.toList()).contains(p2));
    }

    @Test
    public void testSearchByDisplayNameUniqueTerm() {
        UserProfile profile = createProfile("Maria", "Santos", "Maria Santos");
        profile.setDisplayName("Maria Santos Prof");
        NameIndex.updateNameIndex(profile);
        assertTrue(NameIndex.search("prof", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("maria", 10).collect(Collectors.toList()).contains(profile));
        assertTrue(NameIndex.search("santos", 10).collect(Collectors.toList()).contains(profile));
    }
}
