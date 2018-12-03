/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.gateway.handler.auth.idp.impl;

import io.gravitee.am.common.oidc.StandardClaims;
import io.gravitee.am.identityprovider.api.Authentication;
import io.gravitee.am.identityprovider.api.AuthenticationProvider;
import io.gravitee.am.identityprovider.api.DefaultUser;
import io.gravitee.am.identityprovider.api.User;
import io.gravitee.am.model.Domain;
import io.gravitee.am.repository.management.api.UserRepository;
import io.gravitee.am.service.authentication.crypto.password.PasswordEncoder;
import io.gravitee.am.service.exception.authentication.BadCredentialsException;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraviteeAuthenticationProvider.class);
    private static final String DEFAULT_IDP_ID = "gravitee-am";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Domain domain;

    @Override
    public Maybe<User> loadUserByUsername(Authentication authentication) {
        return userRepository.findByDomainAndUsernameAndSource(domain.getId(), (String) authentication.getPrincipal(), DEFAULT_IDP_ID)
                .map(user -> {
                    String presentedPassword = authentication.getCredentials().toString();
                    if (!passwordEncoder.matches(presentedPassword, user.getPassword())) {
                        LOGGER.debug("Authentication failed: password does not match stored value");
                        throw new BadCredentialsException("Bad credentials");
                    }
                    return createUser(user);
                });
    }

    @Override
    public Maybe<User> loadUserByUsername(String username) {
        return userRepository.findByDomainAndUsernameAndSource(domain.getId(), username, DEFAULT_IDP_ID).map(this::createUser);
    }

    private User createUser(io.gravitee.am.model.User modelUser) {
        DefaultUser user = new DefaultUser(modelUser.getUsername());
        user.setRoles(modelUser.getRoles());

        // set additional information
        Map<String, Object> additionalInformation = modelUser.getAdditionalInformation() == null ? new HashMap() : new HashMap(modelUser.getAdditionalInformation());
        additionalInformation.put(StandardClaims.SUB, modelUser.getId());
        if (modelUser.getFirstName() != null) {
            additionalInformation.putIfAbsent(StandardClaims.GIVEN_NAME, modelUser.getFirstName());
        }
        if (modelUser.getLastName() != null) {
            additionalInformation.putIfAbsent(StandardClaims.FAMILY_NAME, modelUser.getLastName());
        }
        if (modelUser.getEmail() != null) {
            additionalInformation.putIfAbsent(StandardClaims.EMAIL, modelUser.getEmail());
        }
        if (modelUser.getUsername() != null) {
            additionalInformation.putIfAbsent(StandardClaims.PREFERRED_USERNAME, modelUser.getUsername());
        }
        user.setAdditonalInformation(additionalInformation);
        return user;
    }
}
