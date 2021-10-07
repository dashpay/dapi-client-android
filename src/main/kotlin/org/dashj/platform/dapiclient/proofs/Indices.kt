/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.proofs

enum class Indices(val value: Int) {
    Contracts(3),
    Documents(4),
    Identities(1),
    PublicKeyHashesToIdentityIds(2);
}
