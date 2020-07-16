/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import io.grpc.StatusRuntimeException

class MaxRetriesReachedException(e: StatusRuntimeException) : Exception(e)
