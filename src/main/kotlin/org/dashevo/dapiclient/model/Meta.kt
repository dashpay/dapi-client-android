/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.model
//TODO: Separate into different type of metas (objectbase, etc), Check System Schema. (?)
data class Meta(
        val id: String?,
        val sig: String?,
        val height: Int?
)