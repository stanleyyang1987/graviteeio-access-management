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
package io.gravitee.am.management.service.impl;

import io.gravitee.am.common.jwt.Claims;
import io.gravitee.am.common.oidc.StandardClaims;
import io.gravitee.am.management.core.event.EmailEvent;
import io.gravitee.am.common.email.Email;
import io.gravitee.am.common.email.EmailBuilder;
import io.gravitee.am.management.service.UserService;
import io.gravitee.am.model.User;
import io.gravitee.am.model.common.Page;
import io.gravitee.am.service.model.NewUser;
import io.gravitee.am.service.model.UpdateUser;
import io.gravitee.common.event.EventManager;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("ManagementUserService")
public class UserServiceImpl implements UserService, InitializingBean {

    private Key key;

    @Value("${jwt.secret:s3cR3t4grAv1t3310AMS1g1ingDftK3y}")
    private String signingKeySecret;

    @Value("${jwt.issuer:https://gravitee.am}")
    private String issuer;

    @Value("${jwt.kid:default-gravitee-am-kid}")
    private String kid;

    @Value("${jwt.expire-after:604800}")
    private Integer expireAfter;

    @Value("${gateway.url:http://localhost:8092}")
    private String gatewayUrl;

    @Autowired
    private io.gravitee.am.service.UserService userService;

    @Autowired
    private EventManager eventManager;

    @Override
    public Single<Set<User>> findByDomain(String domain) {
        return userService.findByDomain(domain);
    }

    @Override
    public Single<Page<User>> findByDomain(String domain, int page, int size) {
        return userService.findByDomain(domain, page, size);
    }

    @Override
    public Maybe<User> findById(String id) {
        return userService.findById(id);
    }

    @Override
    public Maybe<User> loadUserByUsernameAndDomain(String domain, String username) {
        return userService.loadUserByUsernameAndDomain(domain, username);
    }

    @Override
    public Single<User> create(String domain, NewUser newUser) {
        return userService.create(domain, newUser)
                .doOnSuccess(user -> new Thread(() -> completeUserRegistration(user)).start());
    }

    @Override
    public Single<User> update(String domain, String id, UpdateUser updateUser) {
        return userService.update(domain, id, updateUser);
    }

    @Override
    public Completable delete(String userId) {
        return userService.delete(userId);
    }

    @Override
    public void afterPropertiesSet() {
        // init JWT signing key
        key = Keys.hmacShaKeyFor(signingKeySecret.getBytes());
    }

    private void completeUserRegistration(User user) {
        Map<String, Object> params = prepareUserRegistration(user);

        Email email = new EmailBuilder()
                .to(user.getEmail())
                .subject("User registration - " + user.getFirstName() + " " + user.getLastName())
                .template(EmailBuilder.EmailTemplate.USER_REGISTRATION)
                .params(params)
                .build();

        eventManager.publishEvent(EmailEvent.SEND, email);
    }

    private Map<String, Object> prepareUserRegistration(User user) {
        // generate a JWT to store user's information and for security purpose
        final Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.iss, issuer);
        claims.put(Claims.sub, user.getId());
        claims.put(StandardClaims.EMAIL, user.getEmail());
        claims.put(StandardClaims.GIVEN_NAME, user.getFirstName());
        claims.put(StandardClaims.FAMILY_NAME, user.getLastName());

        final String token = Jwts.builder()
                .signWith(key)
                .setHeaderParam(JwsHeader.KEY_ID, kid)
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireAfter))
                .compact();

        String entryPoint = gatewayUrl;
        if (entryPoint != null && entryPoint.endsWith("/")) {
            entryPoint = entryPoint.substring(0, entryPoint.length() - 1);
        }

        String registrationUrl = entryPoint + "/" + user.getDomain() + "/users/confirmRegistration?token=" + token;

        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("registrationUrl", registrationUrl);
        params.put("token", token);

        return params;
    }
}
