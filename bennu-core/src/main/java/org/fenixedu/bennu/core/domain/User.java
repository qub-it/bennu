/*
 * User.java
 *
 * Copyright (c) 2013, Instituto Superior Técnico. All rights reserved.
 *
 * This file is part of bennu-core.
 *
 * bennu-core is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * bennu-core is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with bennu-core. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.bennu.core.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.fenixedu.bennu.core.domain.exceptions.BennuCoreDomainException;
import org.fenixedu.bennu.core.domain.groups.PersistentUserGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.commons.i18n.I18N;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

/**
 * The application end user.
 */
public final class User extends User_Base implements Principal {

    private static final Logger logger = LoggerFactory.getLogger(User.class);

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512";

    // The following constants may be changed without breaking existing hashes.
    private static final int SALT_BYTE_SIZE = 24;
    private static final int HASH_BYTE_SIZE = 24;
    private static final int PBKDF2_ITERATIONS = 150_000;

    private static final String USER_ANONYMOUS_NAME = "User..qub..Anonymous";

    private static Map<String, User> map = new ConcurrentHashMap<>();

    public static final String USERNAME_CHANGE_SIGNAL = "user.username.change";

    public static final Comparator<User> COMPARATOR_BY_NAME =
            Comparator.comparing(User::getDisplayName).thenComparing(User::getUsername);

    public static interface UsernameGenerator {
        public String doGenerate(UserProfile parameter);
    }

    public User(final UserProfile profile) {
        init(generateUsername(profile), profile);
    }

    public User(final String username, final UserProfile profile) {
        if (findByUsername(username) != null) {
            throw BennuCoreDomainException.duplicateUsername(username);
        }
        init(username, profile);
    }

    private void init(final String username, final UserProfile profile) {
        setBennu(Bennu.getInstance());
        setCreated(new DateTime());
        setUsername(username);
        setOriginalUsername(username);
        setProfile(profile);
        setAuthManageable(true);
    }

    @Atomic
    public void delete() {
        setBennu(null);

        for (UserLoginPeriod period : getLoginValiditySet()) {
            period.deleteWithoutRules();
        }

//        BennuGroupIndex.allDynamicGroups().map(DynamicGroup.class::cast).filter(dc -> dc.isMember(this))
//                .forEach(dc -> dc.mutator().changeGroup(dc.underlyingGroup().revoke(this)));

        getCreatedDynamicGroupSet().clear();

        Set<PersistentUserGroup> userGroupSet = new HashSet<>(getUserGroupSet());
        getUserGroupSet().clear();

        for (PersistentUserGroup group : userGroupSet) {
            if (group.getMembers().count() == 0) {
                getAnnonymousUser().addUserGroup(group);
            }
        }

        getProfile().delete();

        deleteDomainObject();
    }

    @Override
    public String getUsername() {
        //FIXME: remove when the framework enables read-only slots
        return super.getUsername();
    }

    public void changeUsername(String username) {
        User existingUser = User.findByUsername(username);
        if (existingUser != null && existingUser != this) {
            throw new IllegalArgumentException("Username already in use");
        }
        String oldUsername = getUsername();
        setUsername(username);
        UserProfile profile = getProfile();
        if (profile != null) {
            String avatarUrl = profile.getAvatarUrl();
            if (avatarUrl != null && avatarUrl.contains(oldUsername)) {
                String newAvatarURL = avatarUrl.replace(oldUsername, username);
                profile.setAvatarUrl(newAvatarURL);
            }
        }
        Signal.emit(USERNAME_CHANGE_SIGNAL, new DomainObjectEvent<User>(this));
    }

    @Override
    public String getOriginalUsername() {
        return super.getOriginalUsername();
    }

    public void changeOriginalUsername(String originalUsername) {
        User user = User.findByOriginalUsername(originalUsername);
        if (user != null && user != this) {
            throw new IllegalArgumentException("Already exists a user with original username: " + originalUsername);
        }
        if (getOriginalUsername() == null || getOriginalUsername().isEmpty()) {
            setOriginalUsername(originalUsername);
        }
    }

    @Override
    public DateTime getCreated() {
        //FIXME: remove when the framework enables read-only slots
        return super.getCreated();
    }

    /**
     * Ensures the existence of an open (i.e. without end date) period for this user.
     *
     * @return a {@link UserLoginPeriod} instance
     */
    public UserLoginPeriod openLoginPeriod() {
        return getLoginValiditySet().stream().filter(p -> p.getEndDate() == null).findAny()
                .orElseGet(() -> new UserLoginPeriod(this));
    }

    /**
     * Creates (if not already) a login period with the given dates for this user.
     *
     * @param start The first day of the login period (inclusive)
     * @param end The last day of the login period (inclusive)
     * @return a {@link UserLoginPeriod} instance
     */
    public UserLoginPeriod createLoginPeriod(final LocalDate start, final LocalDate end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        return getLoginValiditySet().stream().filter(p -> p.matches(start, end)).findAny()
                .orElseGet(() -> new UserLoginPeriod(this, start, end));
    }

    /**
     * Closes any not closed period setting the end day to yesterday (to effectively close, since end date is inclusive).
     */
    public void closeLoginPeriod() {
        closeLoginPeriod(LocalDate.now().minusDays(1));
    }

    /**
     * Closes any not closed period setting the end day to the given day.
     *
     * @param end the last active login day
     */
    public void closeLoginPeriod(final LocalDate end) {
        if (getLoginValiditySet().isEmpty()) {
            new UserLoginPeriod(this, getCreated().toLocalDate(), end);
        } else {
            getLoginValiditySet().stream().filter(p -> !p.isClosed()).forEach(p -> p.setEndDate(end));
        }
    }

    /**
     * Returns the expiration day for this user, that is, the last day he or she can login in the system.
     *
     * @return An optional {@link LocalDate} value that is empty when the login is open (null ended).
     */
    public Optional<LocalDate> getExpiration() {
        return getLoginValiditySet().stream().min(Comparator.naturalOrder()).map(UserLoginPeriod::getEndDate);
    }

    /**
     * Tests whether this user can login or not
     *
     * @return true if login is possible, false otherwise
     */
    public boolean isLoginExpired() {
        return getExpiration().map(p -> LocalDate.now().isAfter(p)).orElse(false);
    }

    @Override
    public String getName() {
        return getUsername();
    }

    public String getDisplayName() {
        return getProfile().getDisplayName();
    }

    public String getEmail() {
        return getProfile().getEmail();
    }

    /**
     * Generates and returns a random password for this user.
     *
     * @return a {@code String} containing the generated password
     */
    public String generatePassword() {
        final String password = UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        changePassword(password);
        return password;
    }

    /**
     * Clears local password for this user
     * 
     */
    public void clearPassword() {
        super.setPassword(null);
    }

    /**
     * Aligns user to a new username, also changes the auth manegeable to false
     * and clears local password
     * 
     * @param newUsername: the new username
     */
    public void align(String newUsername) {
        changeUsername(newUsername);
        setAuthManageable(false);
        clearPassword();
    }

    /**
     * Sets the user's password. The password is salted and hashed with a large number of iterations. Fails with {@link Error} if
     * the necessary cryptographic algorithm is not present in the environment.
     *
     * @param password the password to be set
     */
    public void changePassword(final String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_BYTE_SIZE];
            random.nextBytes(salt);

            byte[] hash = pbkdf2(PBKDF2_ALGORITHM, password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
            setPassword(PBKDF2_ALGORITHM + ":" + PBKDF2_ITERATIONS + ":" + BaseEncoding.base64().encode(salt) + ":"
                    + BaseEncoding.base64().encode(hash));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new Error("Please provide proper cryptographic algorithm implementation");
        }
    }

    /**
     * Verifies that the given password matches the user's password. Fails with {@link Error} if the necessary cryptographic
     * algorithm is not present in the environment.
     *
     * @param password the password to verify
     * @return true if matches, false otherwise
     */
    public boolean matchesPassword(final String password) {
        if (getPassword() == null) {
            return false;
        }
        if (!getPassword().contains(":")) {
            final String hash = Hashing.sha512().hashString(getSalt() + password, Charsets.UTF_8).toString();
            return hash.equals(getPassword());
        } else {
            try {
                String[] params = getPassword().split(":");
                String algorithm = params[0];
                int iterations = Integer.parseInt(params[1]);
                byte[] salt = BaseEncoding.base64().decode(params[2]);
                byte[] hash = BaseEncoding.base64().decode(params[3]);
                byte[] testHash = pbkdf2(algorithm, password.toCharArray(), salt, iterations, hash.length);
                return MessageDigest.isEqual(hash, testHash);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new Error("Please provide proper cryptographic algorithm implementation");
            }
        }
    }

    public Group groupOf() {
        return Group.users(this);
    }

    /**
     * Computes the PBKDF2 hash of a password.
     *
     * @param algorithm the algorithm name
     * @param password the password to hash.
     * @param salt the salt
     * @param iterations the iteration count (slowness factor)
     * @param bytes the length of the hash to compute in bytes
     * @return the PBDKF2 hash of the password
     */
    private static byte[] pbkdf2(final String algorithm, final char[] password, final byte[] salt, final int iterations,
            final int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
        return skf.generateSecret(spec).getEncoded();
    }

    public static User findByUsername(final String username) {
        if (username == null) {
            return null;
        }
        User match = (match = map.get(username)) == null ? manualFind(username) : match;
        if (match == null) {
            return null;
        }
        // FIXME: the second condition is there because of bug #197 in the fenix-framework
        if (!FenixFramework.isDomainObjectValid(match) || !match.getUsername().equals(username)) {
            map.remove(username, match);
            return findByUsername(username);
        }
        return match;
    }

    private static User findByOriginalUsername(String originalUsername) {
        if (originalUsername == null) {
            return null;
        }
        for (final User user : Bennu.getInstance().getUserSet()) {
            if (originalUsername.equals(user.getOriginalUsername())) {
                return user;
            }
        }
        return null;
    }

    private static User manualFind(final String username) {
        for (final User user : Bennu.getInstance().getUserSet()) {
            cacheUser(user);
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    private static void cacheUser(final User user) {
        map.putIfAbsent(user.getUsername(), user);
    }

    public static void setUsernameGenerator(final UsernameGenerator generator) {
        usernameGenerator = generator;
    }

    public boolean isAuthManageable() {
        return Boolean.TRUE.equals(getAuthManageable());
    }

    public void changeAuthManageable(Boolean authManageable) {
        setAuthManageable(authManageable);
    }

    private static UsernameGenerator usernameGenerator = new UsernameGenerator() {
        private final AtomicInteger currentId = new AtomicInteger(0);

        @Override
        public String doGenerate(final UserProfile profile) {
            return "bennu" + currentId.getAndIncrement();
        }
    };

    private static String generateUsername(final UserProfile profile) {
        while (true) {
            String username = usernameGenerator.doGenerate(profile);
            if (User.findByUsername(username) == null) {
                logger.debug("Generated username {} for {}", username, profile);
                return username;
            }
        }
    }

    @Atomic
    public static User getAnnonymousUser() {
        User result = User.findByUsername(USER_ANONYMOUS_NAME);
        if (result == null) {
            result = new User(USER_ANONYMOUS_NAME, new UserProfile(USER_ANONYMOUS_NAME, USER_ANONYMOUS_NAME, USER_ANONYMOUS_NAME,
                    "anonymous@bennuUser.pt", I18N.getLocale()));
        }
        return result;
    }

}
