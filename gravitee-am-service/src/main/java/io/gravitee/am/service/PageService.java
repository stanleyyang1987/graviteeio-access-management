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
package io.gravitee.am.service;

import io.gravitee.am.model.Page;
import io.gravitee.am.service.model.NewPage;
import io.gravitee.am.service.model.UpdatePage;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PageService {

    Maybe<Page> findByDomainAndTypeAndTemplate(String domain, String type, String template);

    Maybe<Page> findByDomainAndClientAndTypeAndTemplate(String domain, String client, String type, String template);

    Single<Page> create(String domain, NewPage page);

    Single<Page> update(String domain, String id, UpdatePage page);

    Completable delete(String pageId);

}
