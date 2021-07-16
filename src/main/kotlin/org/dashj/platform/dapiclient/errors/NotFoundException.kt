/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.errors

import io.grpc.Status

class NotFoundException(message: String) : ResponseException(Status.NOT_FOUND.code.value(), message)
