/*
 * Authenticate.java
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
package org.fenixedu.bennu.core.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.fenixedu.bennu.core.domain.AuthenticationContext;
import org.fenixedu.bennu.core.domain.AuthenticationContext.AuthenticationMethodEvent;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.exceptions.AuthorizationException;
import org.fenixedu.bennu.core.i18n.I18NFilter;
import org.fenixedu.commons.i18n.I18N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class Authenticate {
    private static final Logger logger = LoggerFactory.getLogger(Authenticate.class);

    @Deprecated
    private static final String LOGGED_USER_ATTRIBUTE = "LOGGED_USER_ATTRIBUTE";

    private static final String LOGGED_USER_AUTHENTICATION_CONTEXT_ATTRIBUTE = "LOGGED_USER_AUTHENTICATION_CONTEXT_ATTRIBUTE";

    private static final InheritableThreadLocal<AuthenticationContext> loggedUserContext = new InheritableThreadLocal<>();

    private static final Collection<UserAuthenticationListener> userAuthenticationListeners = new ConcurrentLinkedQueue<>();

    private static final Collection<MockListener> MOCK_LISTENERS = new ArrayList<>();

    public static interface MockListener extends Serializable {

        public void mocking(User originalUser, User mockedUser);

        public void unmocking();
    }

    /**
     * Logs in the given User, associating it with the browser session for the current user.
     * 
     * @param request
     *            The request that triggered the login
     * @param response
     *            The response associated with the request that triggered the login
     * @param user
     *            The user to log in
     * @param authenticationMethod
     *            The authentication method used to login the user
     * @return
     *         The logged in user
     * @throws AuthorizationException
     *             If the provided user is {@code null} or if the user has its login expired
     */
    public static User login(final HttpServletRequest request, final HttpServletResponse response, final User user,
            final String authenticationMethod) {
        if (user == null || user.isLoginExpired()) {
            throw AuthorizationException.authenticationFailed();
        }

        AuthenticationContext authenticationContext = loggedUserContext.get();
        if (authenticationContext == null) {
            authenticationContext = new AuthenticationContext(user, authenticationMethod);
            loggedUserContext.set(authenticationContext);

            storeInSession(request, authenticationContext);
            // Locale from user preference
            Locale preferredLocale = user.getProfile().getPreferredLocale();
            // Locale from cookie
            Locale localeInRequest = I18N.getLocale();
            //
            // Whenever there's a locale change the locale cookie is changed. That means that
            // the cookie is change when:
            // 
            // 1. The user is logged in and changes the prefered locale
            // 2. The user is in the login page and changes the locale
            //
            // If there's a mismatch between the prefered locale and the locale in the cookie
            // it basically means that the user changed the locale in the login page, hence,
            // we'll force it into the preferred locale.
            //
            // 2 January 2024 - Paulo Abrantes
            if (localeInRequest != null && preferredLocale != localeInRequest) {
                preferredLocale = localeInRequest;
                setUserLocale(user, localeInRequest);
            }
            I18NFilter.updateLocale(preferredLocale, request, response);

            fireLoginListeners(request, response, authenticationContext);
            logger.debug("Logged in user: " + user.getUsername());
        } else if (authenticationContext.getUser() != user) {
            throw AuthorizationException.authenticationFailed();
        } else {
            authenticationContext.addAuthenticationMethodEvent(authenticationMethod);
        }

        return user;
    }

    @Atomic(mode = TxMode.WRITE)
    private static void setUserLocale(User user, Locale locale) {
        user.getProfile().setPreferredLocale(locale);
    }

    /**
     * Invalidates the current session, logging out the current user.
     * 
     * Calling this method will cause subsequent calls to {{@link #getUser()} to return {@code null}.
     * 
     * @param request
     *            The request that triggered the logout
     * @param response
     *            The response associated with the requires that triggered the logout
     */
    public static void logout(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpSession session = request.getSession(false);
        if (session != null) {
            final AuthenticationContext authenticationContext =
                    (AuthenticationContext) session.getAttribute(LOGGED_USER_AUTHENTICATION_CONTEXT_ATTRIBUTE);
            if (authenticationContext != null) {
                final User user = authenticationContext.getUser();
                if (user != null && FenixFramework.isDomainObjectValid(user)) {
                    fireLogoutListeners(request, response, authenticationContext);
                }
            }
            session.invalidate();
        }
        clear();
    }

    public static void mock(final User user, final String authenticationMethod) {
        User originalUser = getUser();
        final AuthenticationContext authenticationContext = loggedUserContext.get();
        if (authenticationContext == null || authenticationContext.getUser() != user) {
            loggedUserContext.set(new AuthenticationContext(user, authenticationMethod));
        } else {
            authenticationContext.addAuthenticationMethodEvent(authenticationMethod);
        }
        MOCK_LISTENERS.forEach(l -> l.mocking(originalUser, user));
    }

    public static void unmock() {
        MOCK_LISTENERS.forEach(l -> l.unmocking());
        clear();
    }

    public static User getUser() {
        final AuthenticationContext context = loggedUserContext.get();
        return context == null ? null : context.getUser();
    }

    public static boolean isLogged() {
        final AuthenticationContext context = loggedUserContext.get();
        return context != null && context.getUser() != null;
    }

    public static AuthenticationContext getAuthenticationContext() {
        return loggedUserContext.get();
    }

    private static void storeInSession(final HttpServletRequest request, final AuthenticationContext authenticationContext) {
        final HttpSession session = request.getSession();
        session.setAttribute(LOGGED_USER_ATTRIBUTE, authenticationContext.getUser());
        session.setAttribute(LOGGED_USER_AUTHENTICATION_CONTEXT_ATTRIBUTE, authenticationContext);
    }

    static void updateFromSession(final HttpSession session) {
        if (session != null) {
            final AuthenticationContext authenticationContext =
                    (AuthenticationContext) session.getAttribute(LOGGED_USER_AUTHENTICATION_CONTEXT_ATTRIBUTE);
            if (authenticationContext != null) {
                final User user = authenticationContext.getUser();
                if (user != null && FenixFramework.isDomainObjectValid(user)) {
                    loggedUserContext.set(authenticationContext);
                } else {
                    clear();
                }
            }
        }
    }

    static void clear() {
        final AuthenticationContext context = loggedUserContext.get();
        if (context != null) {
            final AuthenticationMethodEvent[] events = context.getAuthenticationMethodEvents();
            final int length = events.length;
            if (length > 0) {
                context.removeAuthenticationMethodEvent(events[length - 1]);
            }
            loggedUserContext.set(null);
        }
    }

    public static void addUserAuthenticationListener(final UserAuthenticationListener listener) {
        userAuthenticationListeners.add(listener);
    }

    public static void removeUserAuthenticationListener(final UserAuthenticationListener listener) {
        userAuthenticationListeners.remove(listener);
    }

    private static void fireLoginListeners(final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationContext authenticationContext) {
        for (final UserAuthenticationListener listener : userAuthenticationListeners) {
            listener.onLogin(request, response, authenticationContext);
        }
    }

    private static void fireLogoutListeners(final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationContext authenticationContext) {
        for (final UserAuthenticationListener listener : userAuthenticationListeners) {
            listener.onLogout(request, response, authenticationContext);
        }
    }

    public static void addMockListener(MockListener mockListener) {
        MOCK_LISTENERS.add(mockListener);
    }

}
