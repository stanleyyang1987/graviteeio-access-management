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
package io.gravitee.am.gateway.handler.vertx.handler.scim.endpoint.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.am.gateway.handler.scim.UserService;
import io.gravitee.am.service.exception.UserNotFoundException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.reactivex.ext.web.RoutingContext;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GetUserEndpointHandler implements Handler<RoutingContext> {

    private UserService userService;
    private ObjectMapper objectMapper;

    public GetUserEndpointHandler(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(RoutingContext context) {
        final String userId = context.request().getParam("id");
        userService
                .get(userId)
                .subscribe(
                        user -> context.response()
                                .putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
                                .putHeader(HttpHeaders.PRAGMA, "no-cache")
                                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                .end(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(user)),
                        error -> context.fail(error),
                        () -> context.fail(new UserNotFoundException(userId)));

    }

    public static GetUserEndpointHandler create(UserService userService, ObjectMapper objectMapper) {
        return new GetUserEndpointHandler(userService, objectMapper);
    }
}
