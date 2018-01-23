/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

public interface DocumentService<T> {

  /**
   * Get one document by its ID, returns null if not found.
   *
   * @param id
   * @return
   */
  T findById(String id);

  /**
   * List all documents.
   *
   * @return
   */
  List<T> findAll();

  /**
   * List all documents matching a list of IDs.
   *
   * @param ids
   * @return
   */
  List<T> findByIds(List<String> ids);

  /**
   * Get the list in a object that informs about the set of documents that was retrieved.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param studyId
   * @param query
   * @return
   */
  Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                    @Nullable String query);

  Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                    @Nullable String query, @Nullable List<String> fields);

  Documents<T> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String studyId,
                    @Nullable String query, @Nullable List<String> fields, @Nullable List<String> excludedFields);

  List<String> getSuggestionFields();

  List<String> suggest(int limit, String locale, String query);

  long getCount();

  /**
   * Whether the document service implementation shall use cache. Note that cachable document are expected to
   * be instances of {@link org.obiba.mica.spi.search.Identified}.
   * @return
   */
  default boolean useCache() {
    return false;
  }

  /**
   * Documents query result container.
   */
  class Documents<T> {
    private final int total;

    private final int from;

    private final int limit;

    private final List<T> list = Lists.newArrayList();

    public Documents(int total, int from, int limit) {
      this.total = total;
      this.from = from;
      this.limit = limit;
    }

    public List<T> getList() {
      return list;
    }

    public void add(T document) {
      if(document != null) list.add(document);
    }

    public int getTotal() {
      return total;
    }

    public int getFrom() {
      return from;
    }

    public int getLimit() {
      return limit;
    }
  }

}
