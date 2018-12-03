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
package io.gravitee.am.repository.mongodb.management;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.am.model.Group;
import io.gravitee.am.repository.management.api.GroupRepository;
import io.gravitee.am.repository.mongodb.common.IdGenerator;
import io.gravitee.am.repository.mongodb.management.internal.model.GroupMongo;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Titouan COMPIEGNE (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoGroupRepository extends AbstractManagementMongoRepository implements GroupRepository {

    private static final String FIELD_ID = "_id";
    private MongoCollection<GroupMongo> groupsCollection;

    @Autowired
    private IdGenerator idGenerator;

    @PostConstruct
    public void init() {
        groupsCollection = mongoOperations.getCollection("groups", GroupMongo.class);
    }

    @Override
    public Maybe<Group> findById(String group) {
        return Observable.fromPublisher(groupsCollection.find(eq(FIELD_ID, group)).first()).firstElement().map(this::convert);
    }

    @Override
    public Single<Group> create(Group item) {
        GroupMongo group = convert(item);
        group.setId(group.getId() == null ? (String) idGenerator.generate() : group.getId());
        return Single.fromPublisher(groupsCollection.insertOne(group)).flatMap(success -> findById(group.getId()).toSingle());
    }

    @Override
    public Single<Group> update(Group item) {
        GroupMongo group = convert(item);
        return Single.fromPublisher(groupsCollection.replaceOne(eq(FIELD_ID, group.getId()), group)).flatMap(success -> findById(group.getId()).toSingle());
    }

    @Override
    public Completable delete(String id) {
        return Completable.fromPublisher(groupsCollection.deleteOne(eq(FIELD_ID, id)));
    }

    private Group convert(GroupMongo groupMongo) {
        if (groupMongo == null) {
            return null;
        }
        Group group = new Group();
        group.setId(groupMongo.getId());
        group.setDomain(groupMongo.getDomain());
        group.setName(groupMongo.getName());
        group.setMembers(groupMongo.getMembers());
        group.setCreatedAt(groupMongo.getCreatedAt());
        group.setUpdatedAt(groupMongo.getUpdatedAt());
        return group;
    }

    private GroupMongo convert(Group group) {
        if (group == null) {
            return null;
        }
        GroupMongo groupMongo = new GroupMongo();
        groupMongo.setId(group.getId());
        groupMongo.setDomain(group.getDomain());
        groupMongo.setName(group.getName());
        groupMongo.setMembers(group.getMembers());
        groupMongo.setCreatedAt(group.getCreatedAt());
        groupMongo.setUpdatedAt(group.getUpdatedAt());
        return groupMongo;
    }
}
