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
package io.gravitee.am.gateway.handler.vertx.handler.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.am.gateway.handler.jwt.JwtService;
import io.gravitee.am.gateway.handler.oauth2.client.ClientService;
import io.gravitee.am.gateway.handler.oauth2.token.TokenService;
import io.gravitee.am.gateway.handler.scim.UserService;
import io.gravitee.am.gateway.handler.vertx.handler.scim.endpoint.users.CreateUserEndpointHandler;
import io.gravitee.am.gateway.handler.vertx.handler.scim.endpoint.users.DeleteUserEndpointHandler;
import io.gravitee.am.gateway.handler.vertx.handler.scim.endpoint.users.GetUserEndpointHandler;
import io.gravitee.am.gateway.handler.vertx.handler.scim.endpoint.users.UpdateUserEndpointHandler;
import io.gravitee.am.gateway.handler.vertx.handler.scim.handler.BearerTokensParseHandler;
import io.gravitee.am.gateway.handler.vertx.handler.scim.handler.ErrorHandler;
import io.gravitee.common.http.MediaType;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SCIMRouter {

    @Autowired
    private Vertx vertx;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("scimUserService")
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ClientService clientService;

    public Router route() {
        // Create the SCIM router
        final Router router = Router.router(vertx);

        // Declare SCIM routes
        // see <a href="https://tools.ietf.org/html/rfc7644#section-3.2">3.2. SCIM Endpoints and HTTP Methods</a>

        // SCIM routes are OAuth 2.0 secured
        router.route().handler(new BearerTokensParseHandler(jwtService, tokenService, clientService));

        // Users resource
        router.get("/Users/:id").handler(GetUserEndpointHandler.create(userService, objectMapper));
        router.post("/Users").handler(CreateUserEndpointHandler.create(userService));
        router.put("/Users/:id").handler(UpdateUserEndpointHandler.create(userService));
        router.delete("/Users/:id").handler(DeleteUserEndpointHandler.create(userService));

        return router;
    }

}
