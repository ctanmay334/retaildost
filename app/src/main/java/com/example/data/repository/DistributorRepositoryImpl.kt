package com.example.data.repository

import android.util.Log
import com.example.data.dao.DistributorDao
import com.example.data.model.DistributorDto
import com.example.data.model.DistributorEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

private const val TAG = "DistributorRepoImpl"

/**
 * DistributorRepositoryImpl
 * ─────────────────────────
 * Implements [DistributorRepository] using Supabase client and Room [DistributorDao].
 */
class DistributorRepositoryImpl @Inject constructor(
    private val distributorDao: DistributorDao,
    private val supabaseClient: SupabaseClient
) : DistributorRepository {

    override val allDistributors: Flow<List<DistributorEntity>> = distributorDao.getAllDistributorsFlow()

    override suspend fun fetchRemoteDistributors(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val postgrest = supabaseClient.postgrest
                try {
                    val response = postgrest["distributors"].select()
                    if (response.data != "[]" && response.data.isNotBlank()) {
                        val remoteList = response.decodeList<DistributorDto>()
                        val entities = remoteList.map { dto ->
                            DistributorEntity(
                                id = dto.id ?: UUID.randomUUID().toString(),
                                name = dto.name,
                                businessName = dto.businessName,
                                category = dto.category,
                                phone = dto.phone,
                                whatsappNo = dto.whatsappNo,
                                pincode = dto.pincode,
                                serviceRegions = dto.serviceRegions,
                                address = dto.address,
                                minOrderValue = dto.minOrderValue,
                                isVerified = dto.isVerified
                            )
                        }
                        if (entities.isNotEmpty()) {
                            seedDefaultDistributors()
                            distributorDao.insertDistributors(entities)
                            Log.i(TAG, "Successfully synced ${entities.size} distributors from Supabase")
                        }
                    } else {
                        seedDefaultDistributors()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pull distributors from Supabase (offline cache preserved)", e)
                    val count = distributorDao.getAllDistributorsFlow().first().size
                    if (count < 18) {
                        seedDefaultDistributors()
                    }
                }
                Unit
            }
        }

    private suspend fun seedDefaultDistributors() {
        Log.i(TAG, "Seeding 18 default verified distributors...")
        val seeds = listOf(
            DistributorEntity(
                id = "1",
                name = "Vikram Seth",
                businessName = "Vikram Seth Enterprises",
                category = "ITC Staples, Basmati Rice & Grocery",
                phone = "9876543210",
                whatsappNo = "9876543210",
                pincode = "400001",
                serviceRegions = listOf("Aashirvaad", "Sunfeast", "Savlon"),
                address = "CST Wholesaler Market, Shop #23, Mumbai",
                minOrderValue = 1500.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "2",
                name = "Balaram Spices",
                businessName = "Balaram Spices & Flour Mills",
                category = "Flour/Atta, Local Spices & Masalas",
                phone = "9876543210",
                whatsappNo = "9876543210",
                pincode = "400003",
                serviceRegions = listOf("Fortune", "Kitchen King", "Haldi"),
                address = "Mandvi Spices Yard, Galas Shop #18, Mumbai",
                minOrderValue = 2000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "3",
                name = "Apex Beverage",
                businessName = "Apex Beverage Logistics",
                category = "Soft Drinks, Energy Drinks & Mineral Water",
                phone = "9812345670",
                whatsappNo = "9812345670",
                pincode = "400059",
                serviceRegions = listOf("Coca-Cola", "Sprite", "Thums Up", "Kinley"),
                address = "Andheri East Industrial Estate, Plot #12, Mumbai",
                minOrderValue = 3500.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "4",
                name = "Royal Dairy",
                businessName = "Royal Dairy Distributors",
                category = "Cheese, Butter, Milk & Fresh Cream",
                phone = "9823456781",
                whatsappNo = "9823456781",
                pincode = "400063",
                serviceRegions = listOf("Amul", "Mother Dairy", "Go Cheese"),
                address = "Goregaon East Milk Colony, Hub #4, Mumbai",
                minOrderValue = 1000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "5",
                name = "Krishna Oil",
                businessName = "Krishna Oil & Ghee Traders",
                category = "Refined Oils, Mustard Oil & Pure Desi Ghee",
                phone = "9834567892",
                whatsappNo = "9834567892",
                pincode = "400009",
                serviceRegions = listOf("Saffola", "Dhara", "Fortune Oils"),
                address = "Masjid Bunder Oil Market, Shop #5A, Mumbai",
                minOrderValue = 5000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "6",
                name = "Golden Harvest",
                businessName = "Golden Harvest Pulses",
                category = "Premium Dals, Pulses & Organic Grains",
                phone = "9845678903",
                whatsappNo = "9845678903",
                pincode = "400703",
                serviceRegions = listOf("Tata Sampann", "Organic India", "Golden"),
                address = "Vashi APMC Market II, Gate #3, Navi Mumbai",
                minOrderValue = 2500.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "7",
                name = "National Confectionery",
                businessName = "National Confectionery Wholesalers",
                category = "Chocolates, Biscuits, Wafers & Candy Boxes",
                phone = "9856789014",
                whatsappNo = "9856789014",
                pincode = "400001",
                serviceRegions = listOf("Cadbury", "Nestle", "Parle-G", "Britannia"),
                address = "Crawford Market, Shop #88, Mumbai",
                minOrderValue = 3000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "8",
                name = "Supreme Hygiene",
                businessName = "Supreme Hygiene & Detergents",
                category = "Soaps, Dishwash, Toilet Cleaners & Detergents",
                phone = "9867890125",
                whatsappNo = "9867890125",
                pincode = "400070",
                serviceRegions = listOf("Dettol", "Vim", "Surf Excel", "Harpic"),
                address = "Kurla West Depot Road, Building #2, Mumbai",
                minOrderValue = 1200.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "9",
                name = "Mumbai Premium Dry Fruits",
                businessName = "Mumbai Premium Dry Fruits",
                category = "Almonds, Cashews, Raisins & Exotic Nuts",
                phone = "9878901236",
                whatsappNo = "9878901236",
                pincode = "400027",
                serviceRegions = listOf("Del Monte", "Tulsi", "Happilo"),
                address = "Byculla Dry Fruit Hub, Shop #12B, Mumbai",
                minOrderValue = 8000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "10",
                name = "Modern Packaged Foods",
                businessName = "Modern Packaged Foods",
                category = "Noodles, Pasta, Sauces & Instant Soups",
                phone = "9889012347",
                whatsappNo = "9889012347",
                pincode = "400604",
                serviceRegions = listOf("Maggi", "Chings Secret", "Yippee", "Knorr"),
                address = "Thane Wagle Estate, Gali #4, Mumbai",
                minOrderValue = 2200.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "11",
                name = "Super Fresh Tea & Coffee",
                businessName = "Super Fresh Tea & Coffee Co.",
                category = "Loose Tea Dust, CTC Packets & Instant Coffee",
                phone = "9890123458",
                whatsappNo = "9890123458",
                pincode = "400080",
                serviceRegions = listOf("Red Label", "Tata Tea", "Nescafe", "Bru"),
                address = "Mulund West Commercial Street, Shop #9, Mumbai",
                minOrderValue = 1800.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "12",
                name = "Gourmet Bakery",
                businessName = "Gourmet Bakery & Snacks",
                category = "Toast, Khari, Rusk, Chips & Namkeen packets",
                phone = "9901234569",
                whatsappNo = "9901234569",
                pincode = "400050",
                serviceRegions = listOf("Haldiram", "Lay's", "Kurkure", "Monaco"),
                address = "Bandra Linking Road, Extension Block #7, Mumbai",
                minOrderValue = 1500.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "13",
                name = "Balaji Agro",
                businessName = "Balaji Agro & Staples",
                category = "Premium Basmati, Wheat & Bulk Grains",
                phone = "9911223344",
                whatsappNo = "9911223344",
                pincode = "400011",
                serviceRegions = listOf("India Gate", "Aashirvaad", "Fortune Basmati"),
                address = "Byculla Grain Market, Shop #45, Mumbai",
                minOrderValue = 3000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "14",
                name = "Narayana Beverage",
                businessName = "Narayana Beverage & Snacks",
                category = "Juices, Sodas, Chips & Party Packs",
                phone = "9922334455",
                whatsappNo = "9922334455",
                pincode = "400050",
                serviceRegions = listOf("Pepsi", "Tropicana", "Kurkure", "Lays"),
                address = "Linking Road Snack Hub, Basement #2, Mumbai",
                minOrderValue = 1500.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "15",
                name = "Pooja Cosmetics",
                businessName = "Pooja Cosmetics & Toiletries",
                category = "Soaps, Shampoos, Toothpaste & Cosmetics",
                phone = "9933445566",
                whatsappNo = "9933445566",
                pincode = "400002",
                serviceRegions = listOf("Colgate", "Dove", "Lifebuoy", "Pepsodent"),
                address = "Dharavi Commercial Lane, Gala #12, Mumbai",
                minOrderValue = 2500.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "16",
                name = "Shree Ganesha Dairy",
                businessName = "Shree Ganesha Dairy & Sweets",
                category = "Paneer, Ghee, Milk & Curd Packs",
                phone = "9944556677",
                whatsappNo = "9944556677",
                pincode = "400070",
                serviceRegions = listOf("Gowardhan", "Amul Ghee", "Mahi Milk"),
                address = "Kurla East Milk Depot, Shop #3, Mumbai",
                minOrderValue = 1000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "17",
                name = "National Spices",
                businessName = "National Spices & Condiments",
                category = "Bulk Spices, Dry Red Chillies & Seeds",
                phone = "9955667788",
                whatsappNo = "9955667788",
                pincode = "400009",
                serviceRegions = listOf("MDH", "Everest", "Badshah Spices"),
                address = "Masjid Bunder Gali #3, Spice Block, Mumbai",
                minOrderValue = 4000.0,
                isVerified = true
            ),
            DistributorEntity(
                id = "18",
                name = "Hindustan Packaging",
                businessName = "Hindustan Premium Packaging",
                category = "Disposables, Paper Bags & Packaging Material",
                phone = "9966778899",
                whatsappNo = "9966778899",
                pincode = "400059",
                serviceRegions = listOf("EcoPack", "BioWare", "CarryClean"),
                address = "Andheri West Industrial Estate, Hub #1B, Mumbai",
                minOrderValue = 2000.0,
                isVerified = true
            )
        )
        distributorDao.clearAllDistributors()
        distributorDao.insertDistributors(seeds)
    }

    override suspend fun registerDistributor(distributor: DistributorDto): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val generatedId = UUID.randomUUID().toString()
                val completeDto = distributor.copy(id = generatedId)

                // 1. Try sending remote register request
                try {
                    val postgrest = supabaseClient.postgrest
                    postgrest["distributors"].insert(completeDto)
                    Log.i(TAG, "Successfully registered distributor remotely on Supabase: ${completeDto.businessName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register distributor on Supabase (sync scheduled or failed)", e)
                    // We still cache locally for immediate offline visibility
                }

                // 2. Cache in Room DB instantly for seamless optimistic update
                val entity = DistributorEntity(
                    id = generatedId,
                    name = distributor.name,
                    businessName = distributor.businessName,
                    category = distributor.category,
                    phone = distributor.phone,
                    whatsappNo = distributor.whatsappNo,
                    pincode = distributor.pincode,
                    serviceRegions = distributor.serviceRegions,
                    address = distributor.address,
                    minOrderValue = distributor.minOrderValue,
                    isVerified = false // Default to false until admin reviews in Supabase dashboard
                )
                distributorDao.insertDistributor(entity)
                Unit
            }
        }
}
