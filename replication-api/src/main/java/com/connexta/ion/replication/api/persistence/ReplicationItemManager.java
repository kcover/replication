/**
 * Copyright (c) Connexta
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package com.connexta.ion.replication.api.persistence;

import com.connexta.ion.replication.api.NodeAdapter;
import com.connexta.ion.replication.api.ReplicationItem;
import com.connexta.ion.replication.api.ReplicationPersistenceException;
import com.connexta.ion.replication.api.data.Metadata;
import com.connexta.ion.replication.api.data.ReplicatorConfig;
import java.util.List;
import java.util.Optional;

public interface ReplicationItemManager {

  /**
   * If present, returns a {@link ReplicationItem} for the given {@link Metadata} identified by its.
   *
   * @param metadataId a unique metadata id
   * @param source the source {@link NodeAdapter}'s name
   * @param destination the destination {@link NodeAdapter}'s name
   * @return an optional containing the item, or an empty optional if there was an error fetching
   *     the item or it was not found.
   */
  Optional<ReplicationItem> getItem(String metadataId, String source, String destination);

  /**
   * Returns a list of {@code ReplicationItem}s associated with a {@link ReplicatorConfig}.
   *
   * @param configId unique id for the {@link ReplicatorConfig}
   * @param startIndex index to start query at
   * @param pageSize max number of results to return in a single query
   * @return list of items for the given {@link ReplicatorConfig} id
   * @throws ReplicationPersistenceException if there is an error fetching the items
   */
  List<ReplicationItem> getItemsForConfig(String configId, int startIndex, int pageSize);

  /**
   * Saves a new {@code ReplicationItem}.
   *
   * @param replicationItem the item to save.
   */
  void saveItem(ReplicationItem replicationItem);

  /**
   * Deletes all {@code ReplicationItem}s.
   *
   * @throws ReplicationPersistenceException RuntimeException thrown if delete was unsuccessful
   */
  void deleteAllItems();

  /**
   * Delete an item associated with the given {@link Metadata} id.
   *
   * @param metadataId the metadata's id
   * @param source the source {@link NodeAdapter} name
   * @param destination the destination {@link NodeAdapter} name
   */
  void deleteItem(String metadataId, String source, String destination);

  /**
   * Get the list of IDs for {@link ReplicationItem}s that failed to be transferred between the
   * source and destination {@link NodeAdapter}s.
   *
   * @param maximumFailureCount the failure count that {@link ReplicationItem#getFailureCount()}
   *     should not exceed.
   * @param source the source {@link NodeAdapter} name
   * @param destination the destination {@link NodeAdapter} name
   * @return list of string ids of items that have failed to previously transfer
   */
  List<String> getFailureList(int maximumFailureCount, String source, String destination);

  /**
   * Deletes all the items for a {@link ReplicatorConfig}.
   *
   * @param configId id of the {@link ReplicatorConfig}
   * @throws ReplicationPersistenceException if there was an error deleting the items
   */
  void deleteItemsForConfig(String configId);
}
