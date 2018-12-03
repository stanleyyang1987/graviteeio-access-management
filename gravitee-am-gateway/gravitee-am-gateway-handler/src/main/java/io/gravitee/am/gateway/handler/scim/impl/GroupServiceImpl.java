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
package io.gravitee.am.gateway.handler.scim.impl;

import io.gravitee.am.gateway.handler.scim.GroupService;
import io.gravitee.am.gateway.handler.scim.exception.SCIMException;
import io.gravitee.am.gateway.handler.scim.model.Group;
import io.gravitee.am.gateway.handler.scim.model.Member;
import io.gravitee.am.model.Domain;
import io.gravitee.am.repository.management.api.GroupRepository;
import io.gravitee.am.service.exception.AbstractManagementException;
import io.gravitee.am.service.exception.GroupNotFoundException;
import io.gravitee.am.service.exception.TechnicalManagementException;
import io.gravitee.common.utils.UUID;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupServiceImpl implements GroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private Domain domain;

    @Override
    public Maybe<Group> get(String groupId) {
        LOGGER.debug("Find group by id : {}", groupId);
        return groupRepository.findById(groupId)
                .map(this::convert)
                .onErrorResumeNext(ex -> {
                    LOGGER.error("An error occurs while trying to find a group using its ID", groupId, ex);
                    return Maybe.error(new TechnicalManagementException(
                            String.format("An error occurs while trying to find a user using its ID: %s", groupId), ex));
                });
    }

    @Override
    public Single<Group> create(Group group) {
        LOGGER.debug("Create a new group {} for domain {}", group.getDisplayName(), domain.getName());
        io.gravitee.am.model.Group groupModel = convert(group);
        // set technical ID
        groupModel.setId(UUID.toString(UUID.random()));
        groupModel.setDomain(domain.getId());
        groupModel.setCreatedAt(new Date());
        groupModel.setUpdatedAt(groupModel.getCreatedAt());

        return groupRepository.create(groupModel)
                .map(this::convert)
                .onErrorResumeNext(ex -> {
                    LOGGER.error("An error occurs while trying to create a group", ex);
                    return Single.error(new TechnicalManagementException("An error occurs while trying to create a group", ex));

                });
    }

    @Override
    public Single<Group> update(Group group) {
        LOGGER.debug("Update a group {} for domain {}", group.getDisplayName(), domain.getName());
        return groupRepository.findById(group.getId())
                .switchIfEmpty(Maybe.error(new GroupNotFoundException(group.getId())))
                .flatMapSingle(existingGroup -> {
                    io.gravitee.am.model.Group groupToUpdate = convert(group);
                    // set immutable attribute
                    groupToUpdate.setId(existingGroup.getId());
                    groupToUpdate.setDomain(existingGroup.getDomain());
                    groupToUpdate.setCreatedAt(existingGroup.getCreatedAt());
                    groupToUpdate.setUpdatedAt(new Date());
                    return groupRepository.update(groupToUpdate);
                })
                .map(this::convert)
                .onErrorResumeNext(ex -> {
                    if (ex instanceof AbstractManagementException || ex instanceof SCIMException) {
                        return Single.error(ex);
                    } else {
                        LOGGER.error("An error occurs while trying to update a user", ex);
                        return Single.error(new TechnicalManagementException("An error occurs while trying to update a user", ex));
                    }
                });
    }

    @Override
    public Completable delete(String groupId) {
        LOGGER.debug("Delete group {}", groupId);
        return groupRepository.findById(groupId)
                .switchIfEmpty(Maybe.error(new GroupNotFoundException(groupId)))
                .flatMapCompletable(user -> groupRepository.delete(groupId))
                .onErrorResumeNext(ex -> {
                    if (ex instanceof AbstractManagementException) {
                        return Completable.error(ex);
                    } else {
                        LOGGER.error("An error occurs while trying to delete group: {}", groupId, ex);
                        return Completable.error(new TechnicalManagementException(
                                String.format("An error occurs while trying to delete group: %s", groupId), ex));
                    }
                });
    }

    private Group convert(io.gravitee.am.model.Group group) {
        Group scimGroup = new Group();
        scimGroup.setSchemas(Group.SCHEMAS);
        scimGroup.setId(group.getId());
        scimGroup.setDisplayName(group.getName());

        if (group.getMembers() != null) {
            scimGroup.setMembers(group.getMembers().stream().map(userId -> {
                Member member = new Member();
                member.setValue(userId);
                return member;
            }).collect(Collectors.toList()));
        }
        return scimGroup;
    }

    private io.gravitee.am.model.Group convert(Group scimGroup) {
        io.gravitee.am.model.Group group = new io.gravitee.am.model.Group();
        group.setId(scimGroup.getId());
        group.setName(scimGroup.getDisplayName());

        if (scimGroup.getMembers() != null) {
            group.setMembers(scimGroup.getMembers().stream().map(Member::getValue).collect(Collectors.toList()));
        }
        return group;
    }
}
