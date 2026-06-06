package com.example

import com.example.data.model.KhataCustomerEntity
import com.example.data.repository.KhataRepository
import com.example.data.model.KhataTransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeKhataRepository : KhataRepository {
    val customers = mutableListOf<KhataCustomerEntity>()

    override val allCustomers: Flow<List<KhataCustomerEntity>> = MutableStateFlow(emptyList())
    override val allTransactions: Flow<List<KhataTransactionEntity>> = MutableStateFlow(emptyList())

    override suspend fun getCustomersPaged(limit: Int, offset: Int): List<KhataCustomerEntity> = customers
    override suspend fun getCustomerById(id: String): KhataCustomerEntity? = customers.find { it.id == id }
    
    override suspend fun getCustomerByName(name: String): KhataCustomerEntity? = 
        customers.find { it.name.contains(name, ignoreCase = true) }

    override suspend fun searchCustomersByName(name: String): List<KhataCustomerEntity> = 
        customers.filter { it.name.contains(name, ignoreCase = true) }

    override suspend fun insertCustomer(customer: KhataCustomerEntity): Result<Unit> {
        customers.add(customer)
        return Result.success(Unit)
    }

    override suspend fun addTransaction(transaction: KhataTransactionEntity): Result<Unit> = Result.success(Unit)
    override fun getTransactionsForCustomerFlow(customerId: String): Flow<List<KhataTransactionEntity>> = MutableStateFlow(emptyList())
    override suspend fun getTransactionsForCustomerPaged(customerId: String, limit: Int, offset: Int): List<KhataTransactionEntity> = emptyList()
    override suspend fun syncPendingKhata(): Result<Unit> = Result.success(Unit)
    override suspend fun fetchKhataFromRemote(storeId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteCustomer(customerId: String): Result<Unit> {
        customers.removeAll { it.id == customerId }
        return Result.success(Unit)
    }
}

class KhataAmbiguityResolutionTest {

    @Test
    fun testDuplicateNameResolution() = runBlocking {
        val fakeRepo = FakeKhataRepository()
        
        // Setup multiple Sunita records
        fakeRepo.insertCustomer(
            KhataCustomerEntity(
                id = "1",
                storeId = "store1",
                name = "Sunita Sharma",
                runningBalance = 0.0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        fakeRepo.insertCustomer(
            KhataCustomerEntity(
                id = "2",
                storeId = "store1",
                name = "Sunita Singh",
                runningBalance = 0.0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // 1. Search for ambiguous "Sunita" name
        val ambiguousSearch = fakeRepo.searchCustomersByName("Sunita")
        assertEquals(2, ambiguousSearch.size)
        assertEquals("Sunita Sharma", ambiguousSearch[0].name)
        assertEquals("Sunita Singh", ambiguousSearch[1].name)

        // 2. Search for exact/unique "Sunita Sharma"
        val exactSearch = fakeRepo.searchCustomersByName("Sunita Sharma")
        assertEquals(1, exactSearch.size)
        assertEquals("Sunita Sharma", exactSearch[0].name)

        // 3. Search for non-existent "Vijay"
        val nonExistentSearch = fakeRepo.searchCustomersByName("Vijay")
        assertEquals(0, nonExistentSearch.size)
    }
}
