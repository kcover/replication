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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.connexta.replication.api.impl.persistence.pojo.FilterPojo;
import org.junit.Before;
import org.junit.Test;

public class FilterImplTest {

  private static final String SITE_ID = "site_id";

  private static final String FILTER = "filter";

  private static final String NAME = "name";

  private static final String DESCRIPTION = "description";

  private FilterImpl filter;

  @Before
  public void setup() {
    filter = new FilterImpl();
  }

  @Test
  public void gettersAndSetters() {
    filter.setSiteId(SITE_ID);
    filter.setFilter(FILTER);
    filter.setName(NAME);
    filter.setDescription(DESCRIPTION);
    filter.setSuspended(true);
    assertThat(filter.getSiteId(), is(SITE_ID));
    assertThat(filter.getFilter(), is(FILTER));
    assertThat(filter.getName(), is(NAME));
    assertThat(filter.getDescription(), is(DESCRIPTION));
    assertThat(filter.isSuspended(), is(true));
  }

  @Test
  public void readFrom() {
    FilterPojo pojo =
        new FilterPojo()
            .setId("id")
            .setVersion(1)
            .setSiteId(SITE_ID)
            .setFilter(FILTER)
            .setName(NAME)
            .setDescription(DESCRIPTION)
            .setSuspended(true);
    filter = new FilterImpl(pojo);
    assertThat(filter.getSiteId(), is(SITE_ID));
    assertThat(filter.getFilter(), is(FILTER));
    assertThat(filter.getName(), is(NAME));
    assertThat(filter.getDescription(), is(DESCRIPTION));
    assertThat(filter.isSuspended(), is(true));
  }

  @Test
  public void writeTo() {
    FilterPojo pojo = new FilterPojo();
    filter.setSiteId(SITE_ID);
    filter.setFilter(FILTER);
    filter.setName(NAME);
    filter.setDescription(DESCRIPTION);
    filter.setSuspended(true);
    filter.writeTo(pojo);
    assertThat(pojo.getSiteId(), is(SITE_ID));
    assertThat(pojo.getFilter(), is(FILTER));
    assertThat(pojo.getName(), is(NAME));
    assertThat(pojo.getDescription(), is(DESCRIPTION));
    assertThat(pojo.isSuspended(), is(true));
  }
}
