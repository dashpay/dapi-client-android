package org.dashevo.dapiclient

import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction
import org.bitcoinj.quorums.InstantSendLock
import org.dashevo.dpp.StateRepository
import org.dashevo.dpp.contract.DataContract
import org.dashevo.dpp.document.Document
import org.dashevo.dpp.identifier.Identifier
import org.dashevo.dpp.identity.Identity

class StateRepositoryMock : StateRepository {
    override fun fetchDataContract(id: Identifier): DataContract? {
        TODO("Not yet implemented")
    }

    override fun fetchDocuments(contractId: Identifier, type: String, where: Any): List<Document> {
        TODO("Not yet implemented")
    }

    override fun fetchIdentity(id: Identifier): Identity? {
        TODO("Not yet implemented")
    }

    override fun fetchLatestPlatformBlockHeader(): Block {
        TODO("Not yet implemented")
    }

    override fun fetchTransaction(id: String): Transaction {
        TODO("Not yet implemented")
    }

    override fun isAssetLockTransactionOutPointAlreadyUsed(outPointBuffer: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAssetLockTransactionOutPointAsUsed(outPointBuffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun removeDocument(contractId: Identifier, type: String, id: Identifier) {
        TODO("Not yet implemented")
    }

    override fun storeDataContract(dataContract: DataContract) {
        TODO("Not yet implemented")
    }

    override fun storeDocument(document: Document) {
        TODO("Not yet implemented")
    }

    override fun storeIdentity(identity: Identity) {
        TODO("Not yet implemented")
    }

    override fun storeIdentityPublicKeyHashes(identity: Identifier, publicKeyHashes: List<ByteArray>) {
        TODO("Not yet implemented")
    }

    override fun verifyInstantLock(instantLock: InstantSendLock): Boolean {
        TODO("Not yet implemented")
    }
}