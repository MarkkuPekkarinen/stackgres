/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.ResourceScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/configmaps")
@RequestScoped
@Authenticated
public class NamespacedConfigMapResource {

  private ResourceScanner<ConfigMapDto> scanner;

  @Inject
  public NamespacedConfigMapResource(ResourceScanner<ConfigMapDto> scanner) {
    this.scanner = scanner;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = ConfigMapDto.class))) })
      })
  @CommonApiResponses
  @GET
  public List<ConfigMapDto> list(@PathParam("namespace") String namespace) {
    return scanner.findResourcesInNamespace(namespace);
  }

}
