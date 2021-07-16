/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.model

import org.dash.platform.dapi.v0.PlatformOuterClass

class GetIdentitiesByPublicKeyHashesResponse(val identities: List<ByteArray>, proof: Proof, metadata: ResponseMetadata) :
    AbstractResponse(proof, metadata) {
    constructor(response: PlatformOuterClass.GetIdentitiesByPublicKeyHashesResponse) : this(response.identitiesList.map { it.toByteArray() }, Proof(response.proof), ResponseMetadata(response.metadata))
}
