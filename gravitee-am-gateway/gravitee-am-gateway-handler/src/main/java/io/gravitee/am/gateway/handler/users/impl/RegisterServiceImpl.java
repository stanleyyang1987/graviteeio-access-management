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
package io.gravitee.am.gateway.handler.users.impl;

import io.gravitee.am.common.email.Email;
import io.gravitee.am.common.email.EmailBuilder;
import io.gravitee.am.common.jwt.Claims;
import io.gravitee.am.common.jwt.JWT;
import io.gravitee.am.common.oidc.StandardClaims;
import io.gravitee.am.gateway.handler.email.EmailService;
import io.gravitee.am.gateway.handler.jwt.JwtBuilder;
import io.gravitee.am.gateway.handler.jwt.JwtParser;
import io.gravitee.am.gateway.handler.users.RegisterService;
import io.gravitee.am.model.Domain;
import io.gravitee.am.model.User;
import io.gravitee.am.repository.management.api.UserRepository;
import io.gravitee.am.service.authentication.crypto.password.PasswordEncoder;
import io.gravitee.am.service.exception.UserNotFoundException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RegisterServiceImpl implements RegisterService {

    @Value("${gateway.url:http://localhost:8092}")
    private String gatewayUrl;

    @Value("${jwt.secret:s3cR3t4grAv1t3310AMS1g1ingDftK3y}")
    private String signingKeySecret;

    @Value("${jwt.issuer:https://gravitee.am}")
    private String issuer;

    @Value("${jwt.expire-after:604800}")
    private Integer expireAfter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtParser jwtParser;

    @Autowired
    private JwtBuilder jwtBuilder;

    @Autowired
    private Domain domain;

    @Autowired
    private EmailService emailService;

    @Override
    public Maybe<User> verifyToken(String token) {
        return Maybe.fromCallable(() -> jwtParser.parse(token))
                .flatMap(jwt -> userRepository.findById(jwt.getSub()));
    }

    @Override
    public Completable register(User user) {
        // encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUpdatedAt(new Date());
        user.setEnabled(true);

        return userRepository.update(user).toCompletable();
    }

    @Override
    public Completable resetPassword(User user) {
        // encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUpdatedAt(new Date());

        return userRepository.update(user).toCompletable();
    }

    @Override
    public Completable forgotPassword(String email) {
        return userRepository.findByDomainAndEmail(domain.getId(), email)
                .map(users -> users.stream().filter(user -> user.isInternal()).findFirst())
                .flatMapMaybe(optionalUser -> optionalUser.isPresent() ? Maybe.just(optionalUser.get()) : Maybe.empty())
                .switchIfEmpty(Maybe.error(new UserNotFoundException(email)))
                .map(user ->
                        convert(user,
                                "Forgot Password - " + user.getFirstName() + " " + user.getLastName(),
                                EmailBuilder.EmailTemplate.RESET_PASSWORD,
                                "/users/resetPassword",
                                "resetPasswordUrl"))
                .doOnSuccess(email1 -> new Thread(() -> emailService.send(email1)).start())
                .toSingle().toCompletable();

    }

    private Email convert(User user, String title, EmailBuilder.EmailTemplate template, String redirectUri, String redirectUriName) {
        Map<String, Object> params = prepareEmail(user, redirectUri, redirectUriName);
        Email email = new EmailBuilder()
                .to(user.getEmail())
                .subject(title)
                .template(template)
                .params(params)
                .build();
        return email;
    }

    private Map<String, Object> prepareEmail(User user, String redirectUri, String redirectUriName) {
        // generate a JWT to store user's information and for security purpose
        final Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.iat, new Date().getTime() / 1000);
        claims.put(Claims.exp, new Date(System.currentTimeMillis() + expireAfter).getTime() / 1000);
        claims.put(Claims.iss, issuer);
        claims.put(Claims.sub, user.getId());
        claims.put(StandardClaims.EMAIL, user.getEmail());
        claims.put(StandardClaims.GIVEN_NAME, user.getFirstName());
        claims.put(StandardClaims.FAMILY_NAME, user.getLastName());

        String token = jwtBuilder.sign(new JWT(claims));

        String entryPoint = gatewayUrl;
        if (entryPoint != null && entryPoint.endsWith("/")) {
            entryPoint = entryPoint.substring(0, entryPoint.length() - 1);
        }

        String redirectUrl = entryPoint + "/" + user.getDomain() + redirectUri + "?token=" + token;

        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put(redirectUriName, redirectUrl);
        params.put("token", token);

        return params;
    }




}
