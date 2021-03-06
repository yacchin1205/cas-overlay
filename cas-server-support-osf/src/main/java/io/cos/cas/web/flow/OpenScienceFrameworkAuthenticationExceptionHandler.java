/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.cos.cas.web.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.cos.cas.authentication.InvalidVerificationKeyException;
import io.cos.cas.authentication.LoginNotAllowedException;
import io.cos.cas.authentication.OneTimePasswordFailedLoginException;
import io.cos.cas.authentication.OneTimePasswordRequiredException;
import io.cos.cas.authentication.RemoteUserFailedLoginException;
import io.cos.cas.authentication.ShouldNotHappenException;

import org.jasig.cas.authentication.AccountDisabledException;
import org.jasig.cas.authentication.AccountPasswordMustChangeException;
import org.jasig.cas.authentication.AuthenticationException;
import org.jasig.cas.authentication.InvalidLoginLocationException;
import org.jasig.cas.authentication.InvalidLoginTimeException;
import org.jasig.cas.web.flow.AuthenticationExceptionHandler;

import org.springframework.binding.message.MessageContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;

/**
 * The OSF Authentication Exception Handler.
 *
 * @author Michael Haselton
 * @author Longze Chen
 * @since 4.1.5
 */
public class OpenScienceFrameworkAuthenticationExceptionHandler extends AuthenticationExceptionHandler {

    /** The default list of errors that are handled by this handler. */
    private static final List<Class<? extends Exception>> DEFAULT_ERROR_LIST = new ArrayList<>();

    /** A set of errors that should count against the rate limiting. */
    private static final Set<String> THROTTLE_INCREASE_SET = new HashSet<>();

    // Built-in exceptions that are not explicitly used
    static {
        DEFAULT_ERROR_LIST.add(AccountPasswordMustChangeException.class);
        DEFAULT_ERROR_LIST.add(InvalidLoginLocationException.class);
        DEFAULT_ERROR_LIST.add(InvalidLoginTimeException.class);
        DEFAULT_ERROR_LIST.add(AccountLockedException.class);
        DEFAULT_ERROR_LIST.add(CredentialExpiredException.class);
    }

    // Built-in exceptions that are used for OSF
    static {
        DEFAULT_ERROR_LIST.add(FailedLoginException.class);
        DEFAULT_ERROR_LIST.add(AccountDisabledException.class);
        DEFAULT_ERROR_LIST.add(AccountNotFoundException.class);
    }

    // Customized exceptions for OSF
    static {
        DEFAULT_ERROR_LIST.add(InvalidVerificationKeyException.class);
        DEFAULT_ERROR_LIST.add(LoginNotAllowedException.class);
        DEFAULT_ERROR_LIST.add(ShouldNotHappenException.class);
        DEFAULT_ERROR_LIST.add(RemoteUserFailedLoginException.class);
        DEFAULT_ERROR_LIST.add(OneTimePasswordFailedLoginException.class);
        DEFAULT_ERROR_LIST.add(OneTimePasswordRequiredException.class);
    }

    // Exceptions that should count against the login rate limiting
    static {
        THROTTLE_INCREASE_SET.add(FailedLoginException.class.getSimpleName());                // Wrong password
        THROTTLE_INCREASE_SET.add(AccountNotFoundException.class.getSimpleName());            // Username does not exist
        THROTTLE_INCREASE_SET.add(OneTimePasswordFailedLoginException.class.getSimpleName()); // Wrong one time password
    }

    /** Construct an instance of the handler and set the errors. */
    public OpenScienceFrameworkAuthenticationExceptionHandler() {
        super.setErrors(DEFAULT_ERROR_LIST);
    }

    /**
     * Check if the exception should count against login rate limiting.
     *
     * @param handleErrorName the simple name of the exception
     * @return true if the exception is in the THROTTLE_INCREASE_SET, false otherwise
     */
    public static Boolean countAgainstLoginRateLimiting(final String handleErrorName) {
        return THROTTLE_INCREASE_SET.contains(handleErrorName);
    }

    /**
     * The preHandle() event replaces the default handle() event.
     *
     * It first calls the default handle() to handle the exception as usual. Then it puts the specific exception name in
     * the flow context via login context so the last exception can be preserved and used later for login rate limiting.
     *
     * @param context the request context
     * @param e the authentication exception
     * @param messageContext the message context
     * @return an Event with the name of the exception
     */
    public Event preHandle(
            final RequestContext context,
            final AuthenticationException e,
            final MessageContext messageContext
    ) {
        final String handleErrorName = super.handle(e, messageContext);
        final String loginContext = (String) context.getFlowScope().get("jsonLoginContext");
        final OpenScienceFrameworkLoginHandler.OpenScienceFrameworkLoginContext osfLoginContext
                = OpenScienceFrameworkLoginHandler.OpenScienceFrameworkLoginContext.fromJson(loginContext);
        if (osfLoginContext != null) {
            osfLoginContext.setHandleErrorName(handleErrorName);
            context.getFlowScope().put("jsonLoginContext", osfLoginContext.toJson());
        }
        return new Event(this, handleErrorName);
    }
}
