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
package com.connexta.replication.api.impl.persistence.pojo;

import java.util.Objects;
import javax.annotation.Nullable;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

/** A pojo used to load filters from persistence. */
@SolrDocument(collection = FilterPojo.COLLECTION)
public class FilterPojo extends Pojo<FilterPojo> {

  /**
   * List of possible versions:
   *
   * <ul>
   *   <li>1 - initial version.
   * </ul>
   */
  public static final int CURRENT_VERSION = 1;

  public static final String COLLECTION = "replication_filter";

  @Indexed(name = "site_id")
  @Nullable
  private String siteId;

  @Indexed(name = "filter")
  @Nullable
  private String filter;

  @Indexed(name = "name")
  @Nullable
  private String name;

  @Indexed(name = "description")
  @Nullable
  private String description;

  @Indexed(name = "suspended")
  private boolean suspended;

  /** Instantiates a default filter pojo set with the current version. */
  public FilterPojo() {
    super.setVersion(CURRENT_VERSION);
  }

  /**
   * Gets the ID of the site associated with this filter.
   *
   * @return the site id
   */
  @Nullable
  public String getSiteId() {
    return siteId;
  }

  /**
   * Sets the site id for this filter
   *
   * @param siteId the id of the site to associate with this filter.
   * @return this for chaining
   */
  public FilterPojo setSiteId(String siteId) {
    this.siteId = siteId;
    return this;
  }

  /**
   * Gets the query text for this filter.
   *
   * @return the query text for this filter
   */
  @Nullable
  public String getFilter() {
    return filter;
  }

  /**
   * Sets the query text for this filter.
   *
   * @param filter the query text to give this filter
   * @return this for chaining
   */
  public FilterPojo setFilter(String filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Gets the name for this filter.
   *
   * @return the name of this filter
   */
  @Nullable
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this filter.
   *
   * @param name the name to give this filter
   * @return this for chaining
   */
  public FilterPojo setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the description of this filter.
   *
   * @return the description of this filter
   */
  @Nullable
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of this filter.
   *
   * @param description the description to give this filter
   * @return this for chaining
   */
  public FilterPojo setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Gets the suspended status of this config.
   *
   * @return the suspended status of this config
   */
  public boolean isSuspended() {
    return suspended;
  }

  /**
   * Sets the suspended status of this config.
   *
   * @param suspended the suspended status to give this config
   * @return this for chaining
   */
  public FilterPojo setSuspended(boolean suspended) {
    this.suspended = suspended;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), siteId, filter, name, description, suspended);
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && (obj instanceof FilterPojo)) {
      final FilterPojo pojo = (FilterPojo) obj;

      return (suspended == pojo.suspended)
          && Objects.equals(siteId, pojo.siteId)
          && Objects.equals(filter, pojo.filter)
          && Objects.equals(name, pojo.name)
          && Objects.equals(description, pojo.description);
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format(
        "FilterPojo[id=%s, version=%s, siteId=%s, filter=%s, name=%s, description=%s]",
        getId(), getVersion(), siteId, filter, name, description);
  }
}
