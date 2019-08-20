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
package com.connexta.replication.api.data;

import javax.annotation.Nullable;

/** A filter for retrieving data from a specific site. */
public interface Filter extends Persistable {

  /**
   * Gets the ID of the site this filter is associated with.
   *
   * @return the id of the site associated with this filter
   */
  String getSiteId();

  /**
   * Gets the query text of this filter.
   *
   * @return the query text of this filter
   */
  String getFilter();

  /**
   * Gets the name of this filter.
   *
   * @return the name of this filter
   */
  String getName();

  /**
   * Gets the description of this filter. Can be null.
   *
   * @return the dexcription of this filter
   */
  @Nullable
  String getDescription();

  /**
   * Gets the suspended status of this filter.
   *
   * @return the suspended status of this filter
   */
  boolean isSuspended();
}
