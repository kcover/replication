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
package com.connexta.replication.api.impl.data;

import com.connexta.replication.api.data.Filter;
import com.connexta.replication.api.impl.persistence.pojo.FilterPojo;
import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Nullable;

/** A filter for retrieving data from a specific site. */
public class FilterImpl extends AbstractPersistable<FilterPojo> implements Filter {
  private static final String TYPE = "replication filter";

  private String siteId;

  private String filter;

  private String name;

  @Nullable private String description;

  private boolean isSuspended;

  /** Creates a default filter. */
  public FilterImpl() {
    super(FilterImpl.TYPE);
  }

  /**
   * Creates a filter from the given {@link FilterPojo}.
   *
   * @param pojo a pojo containing the values this filter should be instantiated with.
   */
  protected FilterImpl(FilterPojo pojo) {
    super(FilterImpl.TYPE);
    readFrom(pojo);
  }

  @Override
  public String getSiteId() {
    return siteId;
  }

  @Override
  public String getFilter() {
    return filter;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public boolean isSuspended() {
    return isSuspended;
  }

  @VisibleForTesting
  void setSiteId(String siteId) {
    this.siteId = siteId;
  }

  @VisibleForTesting
  void setFilter(String filter) {
    this.filter = filter;
  }

  @VisibleForTesting
  void setName(String name) {
    this.name = name;
  }

  @VisibleForTesting
  void setDescription(String description) {
    this.description = description;
  }

  @VisibleForTesting
  void setSuspended(boolean suspended) {
    this.isSuspended = suspended;
  }

  @Override
  protected FilterPojo writeTo(FilterPojo pojo) {
    super.writeTo(pojo);
    return pojo.setSiteId(siteId)
        .setFilter(filter)
        .setName(name)
        .setDescription(description)
        .setSuspended(isSuspended);
  }

  @Override
  protected void readFrom(FilterPojo pojo) {
    super.readFrom(pojo);
    setOrFailIfNullOrEmpty("siteId", pojo::getSiteId, this::setSiteId);
    setOrFailIfNullOrEmpty("filter", pojo::getFilter, this::setFilter);
    setOrFailIfNullOrEmpty("name", pojo::getName, this::setName);
    this.description = pojo.getDescription();
    this.isSuspended = pojo.isSuspended();
  }
}
