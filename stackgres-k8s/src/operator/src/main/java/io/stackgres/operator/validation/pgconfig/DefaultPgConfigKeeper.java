/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;

@Singleton
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultPgConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresPostgresConfig, PgConfigReview>
    implements PgConfigValidator {

}
