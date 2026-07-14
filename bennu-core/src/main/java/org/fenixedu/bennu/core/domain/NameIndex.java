package org.fenixedu.bennu.core.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.commons.StringNormalizer;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.FenixFramework;

class NameIndex extends NameIndex_Base {
    private static final Map<String, NameIndex> map = new ConcurrentHashMap<>();

    protected NameIndex() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected NameIndex(String keyword) {
        this();
        setKeyword(keyword);
    }

    static Stream<UserProfile> search(String query, int maxHits) {
        String[] queryParts = StringNormalizer.normalizeAndRemoveAccents(query.toLowerCase().trim()).split("\\s+");
        return Stream.of(queryParts).map(NameIndex::find).reduce(Sets::intersection).orElse(Collections.emptySet()).stream()
                .limit(maxHits);
    }

    static void updateNameIndex(UserProfile profile) {
        profile.getNameIndexSet().clear();
        Set<String> terms = collectTerms(profile);
        for (String term : terms) {
            profile.getNameIndexSet().add(create(term));
        }
    }

    private static Set<String> collectTerms(UserProfile profile) {
        Set<String> terms = new LinkedHashSet<>();
        collectTerms(profile.getFullName(), terms);
        collectTerms(profile.getDisplayName(), terms);
        return terms;
    }

    private static void collectTerms(final String name, final Set<String> terms) {
        if (StringUtils.isNotEmpty(name)) {
            String fullName = StringNormalizer.normalizeAndRemoveAccents(name.toLowerCase().trim());
            if (!fullName.isEmpty()) {
                Collections.addAll(terms, fullName.split("\\s+"));
            }
        }
    }

    private static NameIndex create(String keyword) {
        NameIndex match = map.computeIfAbsent(keyword, query -> manualFind(keyword).orElseGet(() -> new NameIndex(keyword)));
        // FIXME: the second condition is there because of bug #197 in the fenix-framework
        if (!FenixFramework.isDomainObjectValid(match) || !match.getKeyword().equals(keyword)) {
            map.remove(keyword, match);
            return create(keyword);
        }
        return match;
    }

    private static Set<UserProfile> find(String keyword) {
        NameIndex match = map.computeIfAbsent(keyword, query -> manualFind(keyword).orElse(null));
        if (match == null) {
            return Collections.emptySet();
        }
        // FIXME: the second condition is there because of bug #197 in the fenix-framework
        if (!FenixFramework.isDomainObjectValid(match) || !match.getKeyword().equals(keyword)) {
            map.remove(keyword, match);
            return find(keyword);
        }
        return match.getProfileSet();
    }

    private static Optional<NameIndex> manualFind(String keyword) {
        return Bennu.getInstance().getNameIndexSet().stream().filter(name -> name.getKeyword().equals(keyword)).findAny();
    }

    static void cleanupIndex() {
        map.clear(); // cache must be invalidated because we are deleting indexes and cache assumes otherwise
        for (NameIndex index : Bennu.getInstance().getNameIndexSet()) {
            index.delete();
        }
        for (UserProfile profile : Bennu.getInstance().getProfileSet()) {
            updateNameIndex(profile);
        }
    }

    private void delete() {
        setBennu(null);
        getProfileSet().clear();
        deleteDomainObject();
    }

    static void heatupCache() {
        Bennu.getInstance().getNameIndexSet().stream().forEach(index -> map.put(index.getKeyword(), index));
    }
}
