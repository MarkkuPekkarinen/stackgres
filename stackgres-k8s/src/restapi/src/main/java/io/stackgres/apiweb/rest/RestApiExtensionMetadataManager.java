/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.net.URI;

import io.stackgres.apiweb.configuration.WebApiProperty;
import io.stackgres.apiweb.configuration.WebApiPropertyContext;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionMetadataManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class RestApiExtensionMetadataManager extends ExtensionMetadataManager {

  @Inject
  public RestApiExtensionMetadataManager(WebApiPropertyContext propertyContext,
      WebClientFactory webClientFactory) {
    super(
        webClientFactory,
        Seq.of(propertyContext.getStringArray(
            WebApiProperty.EXTENSIONS_REPOSITORY_URLS))
            .map(URI::create)
            .toList());
  }

}
