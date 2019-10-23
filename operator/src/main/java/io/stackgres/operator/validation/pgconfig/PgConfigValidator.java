/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.patroni.parameters.Blacklist;
import io.stackgres.operator.validation.PgConfigReview;
import io.stackgres.operator.validation.Validator;

public interface PgConfigValidator extends Validator<PgConfigReview> {

  String[] BLACKLIST_PROPERTIES = Blacklist.getBlacklistParameters().toArray(new String[0]);


}
