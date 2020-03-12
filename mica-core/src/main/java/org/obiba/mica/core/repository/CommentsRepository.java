/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.obiba.mica.core.domain.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CommentsRepository extends MongoRepository<Comment, String> {
  List<Comment> findByClassName(String name);
  List<Comment> findByInstanceId(String id);
  List<Comment> findByResourceIdAndInstanceId(String name, String id);

  @Query("{ resourceId: ?0, instanceId: ?1, $or: [{ admin: { $exists: false } }, { admin: false }] }")
  List<Comment> findPublicCommentsByResourceIdAndInstanceId(String name, String id);

  @Query(value = "{ resourceId: ?0, instanceId: ?1, $or: [{ admin: { $exists: false } }, { admin: false }] }", count = true)
  int countPublicCommentsByResourceIdAndInstanceId(String name, String id);

  List<Comment> findByResourceIdAndInstanceIdAndAdminIsTrue(String name, String id);

  int countByResourceIdAndInstanceIdAndAdminIsTrue(String name, String id);

  @Query("{ $and: [{ _id: { $gte: ?0 } }, { resourceId: ?1, instanceId: ?2 }] }")
  List<Comment> findCommentAndNext(ObjectId commentId, String resourceId, String instanceId, Pageable pageable);
}
