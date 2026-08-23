package com.kartikey.rupeeflow.UI_Screens

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartikey.rupeeflow.Cloud_Database.Constants
import com.kartikey.rupeeflow.UI_Screens.Add.TransactionModel
import com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentItem
import com.kartikey.rupeeflow.UI_Screens.Assets.InvestmentHistoryItem
import com.kartikey.rupeeflow.UI_Screens.Assets.BankAccountItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CashItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.CreditCardItem
import com.kartikey.rupeeflow.UI_Screens.Assets.Finance.FDItem
import com.kartikey.rupeeflow.UI_Screens.Home.ContriRoomModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NetworthDataPoint(
    val monthKey: String, 
    val slotIndex: Int,   
    val label: String,    
    val amount: Double
)

data class AppData(
    val userFullName: String,
    val userEmail: String,
    val userMobile: String,
    val profilePicUrl: String, 
    val userDob: String,
    val todayExpenses: Double,
    val thisMonthExpenses: Double,
    val thisYearExpenses: Double,
    val budgetLimit: Double,
    val transactionList: List<TransactionModel>,
    val investmentList: List<InvestmentItem>,
    val bankList: List<BankAccountItem>,
    val cashData: CashItem,
    val fdList: List<FDItem>,
    val ccList: List<CreditCardItem>,
    val contriRoomsList: List<ContriRoomModel>,
    val networthHistory: Map<String, List<Double>> = emptyMap()
)

object CacheManager {
    private const val PREFS_NAME = "RupeeFlow_GlobalCache"
    
    private val _appDataState = MutableStateFlow<AppData?>(null)
    val appDataState: StateFlow<AppData?> = _appDataState.asStateFlow()

    fun getProfilePicFile(context: Context): File {
        return File(context.cacheDir, "profile_pic.jpg")
    }

    fun saveCustomProfilePic(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) return false

            val maxDim = 400
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaledBitmap = if (scale < 1) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            val file = getProfilePicFile(context)
            val out = FileOutputStream(file)
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
            out.flush()
            out.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadAndCacheProfilePic(context: Context, url: String): Boolean {
        if (url.isBlank()) return false
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val inputStream = response.body?.byteStream()
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val maxDim = 400
                    val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                    val scaledBitmap = if (scale < 1) {
                        android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else bitmap

                    val file = getProfilePicFile(context)
                    val out = FileOutputStream(file)
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                    out.flush()
                    out.close()
                    true
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getCachedData(context: Context, username: String): AppData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString("data_$username", null) ?: return null
        return try {
            val parsed = parseJsonToAppData(cachedJson)
            if (_appDataState.value == null) {
                _appDataState.value = parsed
            }
            parsed
        } catch (e: Exception) {
            null
        }
    }

    fun updateOptimisticCache(context: Context, username: String, data: AppData) {
        try {
            _appDataState.value = data

            val masterJson = JSONObject().apply {
                put("status", "success")
                put("profile", JSONObject().apply {
                    put("name", data.userFullName)
                    put("email", data.userEmail)
                    put("mobile", data.userMobile)
                    put("prfl", data.profilePicUrl)
                    put("dob", data.userDob)
                })
                put("budget_limit", data.budgetLimit)

                val expArray = JSONArray()
                data.transactionList.reversed().forEach { tx ->
                    expArray.put(JSONObject().apply {
                        put("date", tx.date)
                        put("amount", tx.amount)
                        put("category", tx.category)
                        put("detail1", tx.remark1)
                        put("detail2", tx.remark2)
                        put("mode", tx.mode)
                        put("source_type", tx.sourceType)
                        put("source_id", tx.sourceId)
                    })
                }
                put("expenses", expArray)

                put("cash", JSONObject().apply {
                    put("amount", data.cashData.amount)
                    put("last_updated", data.cashData.lastUpdated)
                })

                val banksArray = JSONArray()
                data.bankList.forEach { b ->
                    banksArray.put(JSONObject().apply {
                        put("firebase_key", b.firebaseKey) 
                        put("bank_name", b.bankName)
                        put("account_no", b.accountNo)
                        put("current_bal", b.currentBalance)
                        put("interest_rate", b.interestRate)
                        put("qtr_interest_pct", b.qtrInterestPct)
                        put("exp_qtr_int", b.expQtrInt)
                        put("accrued_qtr_int", b.accruedQtrInt)
                        put("exp_yr_int", b.expYrInt)
                        put("accrued_yr_int", b.accruedYrInt)
                        put("one_day_int", b.oneDayInt)
                    })
                }
                put("banks", banksArray)

                val fdArray = JSONArray()
                data.fdList.forEach { fd ->
                    fdArray.put(JSONObject().apply {
                        put("firebase_key", fd.firebaseKey)
                        put("bank_name", fd.bankName)
                        put("account_no", fd.accountNo)
                        put("create_date", fd.createDate)
                        put("maturity_date", fd.maturityDate)
                        put("days_to_maturity", fd.daysToMaturity)
                        put("invested_amt", fd.investedAmt)
                        put("interest_rate", fd.interestRate)
                        put("maturity_value", fd.maturityValue)
                        put("accrued_value", fd.accruedValue)
                        put("accrued_int", fd.accruedInt)
                        put("one_day_int", fd.oneDayInt)
                    })
                }
                put("fds", fdArray)

                val ccArray = JSONArray()
                data.ccList.forEach { cc ->
                    ccArray.put(JSONObject().apply {
                        put("firebase_key", cc.firebaseKey) 
                        put("issuer", cc.issuer)
                        put("card_no", cc.cardNo)
                        put("type", cc.type)
                        put("limit", cc.limit)
                        put("outstanding", cc.outstanding)
                        put("available", cc.available)
                        put("utilization", cc.utilization)
                        put("cibil_status", cc.cibilStatus)
                        put("billing_day", cc.billingDay)
                        put("due_day", cc.dueDay)
                        put("reminder_day", cc.reminderDay)
                        put("annual_fee", cc.annualFee)
                        put("joining_fee", cc.joiningFee)
                        put("last_used", cc.lastUsed)
                    })
                }
                put("credit_cards", ccArray)

                val invArray = JSONArray()
                data.investmentList.forEach { inv ->
                    invArray.put(JSONObject().apply {
                        put("asset_name", inv.assetName)
                        put("asset_type", inv.assetType)
                        put("quantity", inv.quantity)
                        put("buy_price", inv.avgBuyPrice)
                        put("current_price", inv.currentPrice)
                        put("one_day_change", inv.oneDayChangePrice)
                        
                        val histArray = JSONArray()
                        inv.history.forEach { h ->
                            histArray.put(JSONObject().apply {
                                put("date", h.date)
                                put("quantity", h.quantity)
                                put("price", h.price)
                                put("amount", h.amount)
                                put("brokerage", h.brokerage)
                            })
                        }
                        put("history", histArray)
                    })
                }
                put("investments", invArray)

                val contriArray = JSONArray()
                data.contriRoomsList.forEach { cr ->
                    contriArray.put(JSONObject().apply {
                        put("room_name", cr.roomName)
                        put("room_code", cr.roomCode)
                        put("passkey", cr.pin) 
                        put("expenses", JSONArray().apply {
                            if (cr.lastUpdated.isNotEmpty()) { 
                                put(JSONObject().apply { put("date", cr.lastUpdated) }) 
                            }
                        })
                    })
                }
                put("contri_rooms", contriArray)

                val ntworthObj = JSONObject()
                data.networthHistory.forEach { (mKey, slotList) ->
                    val arr = JSONArray()
                    slotList.forEach { arr.put(it) }
                    ntworthObj.put(mKey, arr)
                }
                put("ntworth", ntworthObj)
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("data_$username", masterJson.toString()).apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTimelineNetworth(context: Context, username: String): List<NetworthDataPoint> {
        val appData = getCachedData(context, username) ?: return emptyList()
        val timelineList = mutableListOf<NetworthDataPoint>()

        val monthNameFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val parseFormat = SimpleDateFormat("yy-MM", Locale.getDefault())

        val sortedMonths = appData.networthHistory.keys.sorted() 
        for (mKey in sortedMonths) {
            val slots = appData.networthHistory[mKey] ?: continue
            val monthDate = try { parseFormat.parse(mKey) } catch (e: Exception) { null }
            val mName = if (monthDate != null) monthNameFormat.format(monthDate) else mKey

            slots.forEachIndexed { index, amount ->
                if (amount > 0.0) {
                    val label = when (index) {
                        0 -> "01-10 $mName"
                        1 -> "11-20 $mName"
                        else -> "21-End $mName"
                    }
                    timelineList.add(NetworthDataPoint(mKey, index, label, amount))
                }
            }
        }
        return timelineList
    }

    fun syncNetworthSlot(context: Context, username: String, currentNetworth: Double) {
        if (currentNetworth <= 0.0) return
        val cached = getCachedData(context, username) ?: return
        
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthKey = SimpleDateFormat("yy-MM", Locale.getDefault()).format(cal.time)
        
        val slotIndex = when {
            day <= 10 -> 0
            day <= 20 -> 1
            else -> 2
        }

        val historyMap = cached.networthHistory.toMutableMap()
        val existingSlots = historyMap[monthKey]?.toMutableList() ?: mutableListOf(0.0, 0.0, 0.0)
        
        while (existingSlots.size < 3) {
            existingSlots.add(0.0)
        }

        existingSlots[slotIndex] = currentNetworth
        historyMap[monthKey] = existingSlots

        if (historyMap.size > 6) {
            val sortedKeys = historyMap.keys.sorted()
            val keysToRemove = sortedKeys.take(historyMap.size - 6)
            keysToRemove.forEach { historyMap.remove(it) }
        }

        val updatedData = cached.copy(networthHistory = historyMap)
        updateOptimisticCache(context, username, updatedData)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!userQuery.isEmpty) {
                    val userRef = userQuery.documents[0].reference
                    userRef.set(mapOf("ntworth" to historyMap), SetOptions.merge()).await()
                }
            } catch (e: Exception) {}
        }
    }

    fun deleteInvestment(context: Context, username: String, assetName: String) {
        val cached = getCachedData(context, username) ?: return
        val updatedInvList = cached.investmentList.filterNot { it.assetName.equals(assetName, true) }
        val updatedData = cached.copy(investmentList = updatedInvList)
        updateOptimisticCache(context, username, updatedData)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!userQuery.isEmpty) {
                    val userDoc = userQuery.documents[0]
                    val userRef = userDoc.reference
                    val investMap = (userDoc.get("invest") as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                    
                    val keyToRemove = investMap.keys.find { key ->
                        val item = investMap[key] as? Map<*, *>
                        val name = item?.get("name")?.toString()?.trim() ?: key
                        name.equals(assetName, true) || key.equals(assetName, true)
                    }
                    if (keyToRemove != null) {
                        investMap.remove(keyToRemove)
                        userRef.update("invest", investMap).await()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHistoryLot(context: Context, username: String, assetName: String, lotIndex: Int) {
        val cached = getCachedData(context, username) ?: return
        val inv = cached.investmentList.find { it.assetName.equals(assetName, true) } ?: return
        val currentHistory = inv.history.toMutableList()
        if (lotIndex !in currentHistory.indices) return
        
        currentHistory.removeAt(lotIndex)
        
        if (currentHistory.isEmpty()) {
            deleteInvestment(context, username, assetName)
            return
        }

        val newQty = currentHistory.sumOf { it.quantity }
        val newTotalCost = currentHistory.sumOf { it.quantity * it.price }
        val newAvg = if (newQty > 0) newTotalCost / newQty else 0.0

        val updatedInv = inv.copy(
            quantity = newQty,
            avgBuyPrice = newAvg,
            history = currentHistory
        )
        val updatedInvList = cached.investmentList.toMutableList()
        val invIndex = updatedInvList.indexOfFirst { it.assetName.equals(assetName, true) }
        if (invIndex != -1) {
            updatedInvList[invIndex] = updatedInv
        }

        val updatedData = cached.copy(investmentList = updatedInvList)
        updateOptimisticCache(context, username, updatedData)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!userQuery.isEmpty) {
                    val userDoc = userQuery.documents[0]
                    val userRef = userDoc.reference
                    val investMap = (userDoc.get("invest") as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                    val targetKey = investMap.keys.find { key ->
                        val item = investMap[key] as? Map<*, *>
                        val name = item?.get("name")?.toString()?.trim() ?: key
                        name.equals(assetName, true) || key.equals(assetName, true)
                    } ?: assetName

                    val newHistMap = mutableMapOf<String, Any>()
                    currentHistory.forEachIndexed { idx, h ->
                        newHistMap[idx.toString()] = mapOf(
                            "dt" to h.date,
                            "qnt" to h.quantity,
                            "prc" to h.price,
                            "amnt" to h.amount,
                            "brkrg" to h.brokerage
                        )
                    }

                    val existingAssetMap = (investMap[targetKey] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                    existingAssetMap["qnt"] = newQty
                    existingAssetMap["avg"] = newAvg
                    existingAssetMap["history"] = newHistMap
                    
                    investMap[targetKey] = existingAssetMap
                    userRef.update("invest", investMap).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editHistoryLot(context: Context, username: String, assetName: String, lotIndex: Int, updatedLot: InvestmentHistoryItem) {
        val cached = getCachedData(context, username) ?: return
        val inv = cached.investmentList.find { it.assetName.equals(assetName, true) } ?: return
        val currentHistory = inv.history.toMutableList()
        if (lotIndex !in currentHistory.indices) return
        
        currentHistory[lotIndex] = updatedLot
        
        val newQty = currentHistory.sumOf { it.quantity }
        val newTotalCost = currentHistory.sumOf { it.quantity * it.price }
        val newAvg = if (newQty > 0) newTotalCost / newQty else 0.0

        val updatedInv = inv.copy(
            quantity = newQty,
            avgBuyPrice = newAvg,
            history = currentHistory
        )
        val updatedInvList = cached.investmentList.toMutableList()
        val invIndex = updatedInvList.indexOfFirst { it.assetName.equals(assetName, true) }
        if (invIndex != -1) {
            updatedInvList[invIndex] = updatedInv
        }

        val updatedData = cached.copy(investmentList = updatedInvList)
        updateOptimisticCache(context, username, updatedData)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userQuery = db.collection("Users").whereEqualTo("username", username).get().await()
                if (!userQuery.isEmpty) {
                    val userDoc = userQuery.documents[0]
                    val userRef = userDoc.reference
                    val investMap = (userDoc.get("invest") as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                    val targetKey = investMap.keys.find { key ->
                        val item = investMap[key] as? Map<*, *>
                        val name = item?.get("name")?.toString()?.trim() ?: key
                        name.equals(assetName, true) || key.equals(assetName, true)
                    } ?: assetName

                    val newHistMap = mutableMapOf<String, Any>()
                    currentHistory.forEachIndexed { idx, h ->
                        newHistMap[idx.toString()] = mapOf(
                            "dt" to h.date,
                            "qnt" to h.quantity,
                            "prc" to h.price,
                            "amnt" to h.amount,
                            "brkrg" to h.brokerage
                        )
                    }

                    val existingAssetMap = (investMap[targetKey] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
                    existingAssetMap["qnt"] = newQty
                    existingAssetMap["avg"] = newAvg
                    existingAssetMap["history"] = newHistMap
                    
                    investMap[targetKey] = existingAssetMap
                    userRef.update("invest", investMap).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAndCacheData(context: Context, username: String, forceRefresh: Boolean = false): AppData? {
        return withContext(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                val userQuery = db.collection("Users")
                    .whereEqualTo("username", username)
                    .get()
                    .await()

                if (userQuery.isEmpty) return@withContext null

                val userDoc = userQuery.documents[0]
                val userRef = userDoc.reference

                // Write 'ver' (Double) and 'lst_opn' (Timestamp) on backend data fetch
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val rawVersionName = packageInfo.versionName ?: "1.0"
                    val appVersionDouble = rawVersionName.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 1.0
                    
                    userRef.set(
                        mapOf(
                            "ver" to appVersionDouble,
                            "lst_opn" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val profileObj = JSONObject().apply {
                    put("name", userDoc.getString("name") ?: "")
                    put("email", userDoc.getString("email") ?: "")
                    put("mobile", userDoc.getString("mobile_no_") ?: userDoc.getString("mobile") ?: "")
                    put("prfl", userDoc.getString("prfl") ?: "")
                    
                    val rawDob = userDoc.get("dob")
                    val formattedDob = when (rawDob) {
                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(rawDob.toDate())
                        is String -> rawDob
                        else -> ""
                    }
                    put("dob", formattedDob)
                }

                val budgetLimit = userDoc.getDouble("budget_limit") ?: 0.0

                val expensesDocs = userRef.collection("Expenses").get().await()
                val expensesArray = JSONArray()

                for (doc in expensesDocs) {
                    val dataMap = doc.data
                    for ((key, value) in dataMap) {
                        if (key != "000_total" && value is Map<*, *>) {
                            val expObj = JSONObject()
                            val rawDate = value["dt"]
                            val dateStr = when (rawDate) {
                                is Timestamp -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(rawDate.toDate())
                                is String -> rawDate
                                else -> ""
                            }

                            expObj.put("date", dateStr)
                            expObj.put("amount", (value["amnt"] as? Number)?.toDouble() ?: 0.0)
                            expObj.put("category", value["cat"]?.toString() ?: "")
                            expObj.put("detail1", value["det1"]?.toString() ?: "")
                            expObj.put("detail2", value["det2"]?.toString() ?: "")
                            
                            val paymentDetail = value["pay"]?.toString() ?: ""
                            val splitPayment = paymentDetail.split("|").map { it.trim() }
                            
                            expObj.put("mode", if (splitPayment.isNotEmpty()) splitPayment[0] else "")
                            expObj.put("source_type", if (splitPayment.size > 1) splitPayment[1] else "")
                            expObj.put("source_id", if (splitPayment.size > 2) splitPayment[2] else "")

                            expensesArray.put(expObj)
                        }
                    }
                }

                val financesDocs = userRef.collection("Finances").get().await()
                
                var cashObj = JSONObject().apply {
                    put("amount", 0.0)
                    put("last_updated", "")
                }
                
                val banksArray = JSONArray()
                val fdArray = JSONArray()
                val ccArray = JSONArray()
                
                val todayMillis = System.currentTimeMillis()

                for (doc in financesDocs) {
                    when (doc.id) {
                        "Bank" -> {
                            val dataMap = doc.data
                            val updateMap = mutableMapOf<String, Any>() 
                            
                            val lastUpdatedTs = dataMap["last_updated"] as? Timestamp
                            val calToday = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            
                            var yearShifted = false
                            var gapDays = 0L
                            
                            if (lastUpdatedTs != null) {
                                val calLast = Calendar.getInstance().apply {
                                    time = lastUpdatedTs.toDate()
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (calToday.get(Calendar.YEAR) > calLast.get(Calendar.YEAR)) {
                                    yearShifted = true
                                }
                                if (calToday.timeInMillis > calLast.timeInMillis) {
                                    gapDays = (calToday.timeInMillis - calLast.timeInMillis) / (1000 * 60 * 60 * 24)
                                }
                            }

                            if (gapDays > 0) {
                                updateMap["last_updated"] = FieldValue.serverTimestamp()
                            }

                            val startOfQtr = Calendar.getInstance().apply {
                                set(Calendar.MONTH, (calToday.get(Calendar.MONTH) / 3) * 3)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val daysPassedQtr = ((calToday.timeInMillis - startOfQtr.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
                            
                            val startOfYear = Calendar.getInstance().apply {
                                set(Calendar.MONTH, Calendar.JANUARY)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val daysPassedYr = ((calToday.timeInMillis - startOfYear.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1

                            val currentMonth = calToday.get(Calendar.MONTH)
                            val qtrStr = "q${(currentMonth / 3) + 1}"

                            dataMap.forEach { (key, rawData) ->
                                if (key == "cash" && rawData is Map<*, *>) {
                                    val amnt = (rawData["amnt"] as? Number)?.toDouble() ?: 0.0
                                    val lastUpd = rawData["last update"]
                                    val lastUpdStr = when (lastUpd) {
                                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(lastUpd.toDate())
                                        is String -> lastUpd
                                        else -> ""
                                    }
                                    cashObj.put("amount", amnt)
                                    cashObj.put("last_updated", lastUpdStr)
                                } 
                                else if (key != "last_updated" && key != "cash" && rawData is Map<*, *>) {
                                    val rawBank = rawData
                                    val bName = rawBank["bank"]?.toString() ?: ""
                                    val accNo = rawBank["account no."]?.toString() ?: ""
                                    val curBal = (rawBank["current bal."] as? Number)?.toDouble() ?: 0.0
                                    val rateYr = (rawBank["intrest % (yr)"] as? Number)?.toDouble() ?: 0.0
                                    val rateQtr = (rawBank["intrest % (qtr)"] as? Number)?.toDouble() ?: (rateYr / 4.0)
                                    val oneDayInt = (curBal * (rateYr / 100.0)) / 365.0

                                    val qtrAvgMap = rawBank["qtr. avg."] as? Map<*,*> ?: emptyMap<String, Any>()
                                    val yrAvgMap = rawBank["yr avg"] as? Map<*,*> ?: emptyMap<String, Any>()

                                    val currentQtrAvg = (qtrAvgMap[qtrStr] as? Number)?.toDouble() ?: curBal
                                    var curYrAvg = (yrAvgMap["cur"] as? Number)?.toDouble() ?: curBal

                                    val bankSpecificUpdates = mutableMapOf<String, Any>()

                                    if (yearShifted) {
                                        val lastYrAvg = (yrAvgMap["cur"] as? Number)?.toDouble() ?: 0.0
                                        val secondLastYrAvg = (yrAvgMap["last"] as? Number)?.toDouble() ?: 0.0
                                        
                                        bankSpecificUpdates["yr avg"] = mapOf(
                                            "2nd last" to secondLastYrAvg,
                                            "last" to lastYrAvg,
                                            "cur" to curBal
                                        )
                                        curYrAvg = curBal 
                                    }

                                    val expQtr = currentQtrAvg * (rateQtr / 100.0)
                                    val accruedQtr = expQtr * (daysPassedQtr / 90.0)
                                    val expYr = curYrAvg * (rateYr / 100.0)
                                    val accruedYr = expYr * (daysPassedYr / 365.0)

                                    if (gapDays > 0 || yearShifted) {
                                        bankSpecificUpdates["exp qtr int"] = expQtr
                                        bankSpecificUpdates["accrued qtr"] = accruedQtr
                                        bankSpecificUpdates["exp yr int"] = expYr
                                        bankSpecificUpdates["accrued yr"] = accruedYr
                                        bankSpecificUpdates["1d int"] = oneDayInt
                                    }

                                    if (bankSpecificUpdates.isNotEmpty()) {
                                        updateMap[key] = bankSpecificUpdates
                                    }

                                    val bObj = JSONObject().apply {
                                        put("firebase_key", key) 
                                        put("bank_name", bName)
                                        put("account_no", accNo)
                                        put("current_bal", curBal)
                                        put("interest_rate", rateYr)
                                        put("qtr_interest_pct", rateQtr)
                                        put("exp_qtr_int", expQtr)
                                        put("accrued_qtr_int", accruedQtr)
                                        put("exp_yr_int", expYr)
                                        put("accrued_yr_int", accruedYr)
                                        put("one_day_int", oneDayInt)
                                    }
                                    banksArray.put(bObj)
                                }
                            }
                            if (updateMap.isNotEmpty()) {
                                doc.reference.set(updateMap, SetOptions.merge()).await()
                            }
                        }

                        "CC FD" -> {
                            val dataMap = doc.data
                            val ccMap = dataMap["CC"] as? Map<*, *>
                            ccMap?.forEach { (key, rawCc) ->
                                if (rawCc is Map<*, *>) {
                                    val issuer = rawCc["issuer"]?.toString() ?: ""
                                    val cardNo = rawCc["card no."]?.toString() ?: ""
                                    val type = rawCc["type"]?.toString() ?: ""
                                    val limit = (rawCc["limit"] as? Number)?.toDouble() ?: 0.0
                                    val outstanding = (rawCc["outstanding"] as? Number)?.toDouble() ?: 0.0
                                    
                                    val avail = limit - outstanding
                                    val util = if (limit > 0) (outstanding / limit) * 100.0 else 0.0
                                    val cibilStatus = if (util <= 30.0) "Safe" else "High Risk"

                                    val ccObj = JSONObject().apply {
                                        put("firebase_key", key.toString())
                                        put("issuer", issuer)
                                        put("card_no", cardNo)
                                        put("type", type)
                                        put("limit", limit)
                                        put("outstanding", outstanding)
                                        put("available", avail)
                                        put("utilization", util)
                                        put("cibil_status", cibilStatus)
                                        put("billing_day", (rawCc["billing"] as? Number)?.toInt() ?: 0)
                                        put("due_day", (rawCc["due"] as? Number)?.toInt() ?: 0)
                                        put("reminder_day", (rawCc["rmndr"] as? Number)?.toInt() ?: 0)
                                        put("annual_fee", (rawCc["yr fee"] as? Number)?.toDouble() ?: 0.0)
                                        put("joining_fee", (rawCc["join fee"] as? Number)?.toDouble() ?: 0.0)
                                        
                                        val lastUseTs = rawCc["last use"] as? Timestamp
                                        val lastUseStr = lastUseTs?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: ""
                                        put("last_used", lastUseStr)
                                    }
                                    ccArray.put(ccObj)
                                }
                            }

                            val fdMap = dataMap["FD"] as? Map<*, *>
                            fdMap?.forEach { (key, rawFd) ->
                                if (rawFd is Map<*, *>) {
                                    val bank = rawFd["bank"]?.toString() ?: ""
                                    val accNo = rawFd["fd ac"]?.toString() ?: ""
                                    val amnt = (rawFd["amnt"] as? Number)?.toDouble() ?: 0.0
                                    val rate = (rawFd["int % yr"] as? Number)?.toDouble() ?: 0.0
                                    
                                    val createTs = rawFd["create"] as? Timestamp
                                    val maturTs = rawFd["matur"] as? Timestamp
                                    
                                    val createStr = createTs?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: ""
                                    val maturStr = maturTs?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()) } ?: ""
                                    
                                    var daysToMat = 0
                                    var matVal = amnt
                                    var accVal = amnt
                                    var accInt = 0.0
                                    var oneDayInt = 0.0
                                    
                                    if (createTs != null && maturTs != null) {
                                        val createMillis = createTs.toDate().time
                                        val maturMillis = maturTs.toDate().time
                                        
                                        val totalDays = maxOf(0L, (maturMillis - createMillis) / (1000 * 60 * 60 * 24))
                                        daysToMat = maxOf(0L, (maturMillis - todayMillis) / (1000 * 60 * 60 * 24)).toInt()
                                        val daysPassed = maxOf(0L, (minOf(todayMillis, maturMillis) - createMillis) / (1000 * 60 * 60 * 24))
                                        
                                        matVal = amnt * Math.pow(1 + (rate / 100.0), totalDays / 365.0)
                                        accVal = amnt * Math.pow(1 + (rate / 100.0), daysPassed / 365.0)
                                        accInt = accVal - amnt
                                        
                                        oneDayInt = if (todayMillis >= maturMillis || todayMillis < createMillis) 0.0 else (accVal * (rate / 100.0)) / 365.0
                                    }

                                    val fdObj = JSONObject().apply {
                                        put("firebase_key", key.toString())
                                        put("bank_name", bank)
                                        put("account_no", accNo)
                                        put("create_date", createStr)
                                        put("maturity_date", maturStr)
                                        put("days_to_maturity", daysToMat)
                                        put("invested_amt", amnt)
                                        put("interest_rate", rate)
                                        put("maturity_value", matVal)
                                        put("accrued_value", accVal)
                                        put("accrued_int", accInt)
                                        put("one_day_int", oneDayInt)
                                    }
                                    fdArray.put(fdObj)
                                }
                            }
                        }
                    }
                }

                // ⚡ FETCH LIVE MARKET QUOTES VIA ISOLATED MARKET ENGINE ⚡
                val investMap = userDoc.get("invest") as? Map<String, Any> ?: emptyMap()
                val symbolsToFetch = mutableListOf<String>()
                for ((_, value) in investMap) {
                    val itemData = value as? Map<String, Any>
                    val sym = itemData?.get("name")?.toString()?.trim() ?: ""
                    if (sym.isNotEmpty()) {
                        symbolsToFetch.add(sym)
                    }
                }
                val liveQuotesMap = MarketEngine.fetchQuotes(symbolsToFetch)

                val invArray = JSONArray()
                for ((key, value) in investMap) {
                    val itemData = value as? Map<String, Any>
                    if (itemData != null) {
                        val dbName = itemData["name"]?.toString()?.trim()?.uppercase() ?: ""
                        val buyPrice = (itemData["avg"] as? Number)?.toDouble() ?: 0.0
                        
                        val liveQuote = liveQuotesMap[dbName] ?: liveQuotesMap[dbName.replace(".NS", "").replace(".BO", "")]
                        val currentPrice = if (liveQuote != null && liveQuote.currentPrice > 0.0) liveQuote.currentPrice else buyPrice
                        val oneDayChange = liveQuote?.oneDayChangePrice ?: 0.0
                        
                        val parsedHistoryList = mutableListOf<InvestmentHistoryItem>()
                        val historyMapRaw = itemData["history"] as? Map<String, Any>
                        if (historyMapRaw != null) {
                            for ((_, hVal) in historyMapRaw) {
                                val hData = hVal as? Map<String, Any>
                                if (hData != null) {
                                    val rawDt = hData["dt"]
                                    val dtStr = when (rawDt) {
                                        is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(rawDt.toDate())
                                        is String -> rawDt
                                        else -> ""
                                    }
                                    parsedHistoryList.add(
                                        InvestmentHistoryItem(
                                            date = dtStr,
                                            quantity = (hData["qnt"] as? Number)?.toDouble() ?: 0.0,
                                            price = (hData["prc"] as? Number)?.toDouble() ?: 0.0,
                                            amount = (hData["amnt"] as? Number)?.toDouble() ?: 0.0,
                                            brokerage = (hData["brkrg"] as? Number)?.toDouble() ?: 0.0
                                        )
                                    )
                                }
                            }
                        }
                        
                        val invObj = JSONObject().apply {
                            put("asset_name", dbName) 
                            put("asset_type", itemData["type"]?.toString() ?: "Stock")
                            put("quantity", (itemData["qnt"] as? Number)?.toDouble() ?: 0.0)
                            put("buy_price", buyPrice)
                            put("current_price", currentPrice)
                            put("one_day_change", oneDayChange)
                            
                            val hArray = JSONArray()
                            parsedHistoryList.forEach { h ->
                                hArray.put(JSONObject().apply {
                                    put("date", h.date)
                                    put("quantity", h.quantity)
                                    put("price", h.price)
                                    put("amount", h.amount)
                                    put("brokerage", h.brokerage)
                                })
                            }
                            put("history", hArray)
                        }
                        invArray.put(invObj)
                    }
                }

                val contriArray = JSONArray()
                val roomsArray = userDoc.get("rooms") as? List<*> ?: emptyList<Any>()

                for (roomItem in roomsArray) {
                    val roomStr = roomItem.toString()
                    val parts = roomStr.split("_", limit = 3)
                    if (parts.size >= 3) {
                        val cObj = JSONObject().apply {
                            put("room_name", parts[2])
                            put("room_code", parts[1])
                            put("passkey", "123456")
                            put("expenses", JSONArray()) 
                        }
                        contriArray.put(cObj)
                    }
                }

                val ntworthObj = JSONObject()
                val rawNtworth = userDoc.get("ntworth") as? Map<*, *> ?: emptyMap<Any, Any>()
                rawNtworth.forEach { (k, v) ->
                    if (v is List<*>) {
                        val arr = JSONArray()
                        v.forEach { num -> arr.put((num as? Number)?.toDouble() ?: 0.0) }
                        ntworthObj.put(k.toString(), arr)
                    }
                }

                val masterJson = JSONObject().apply {
                    put("status", "success")
                    put("profile", profileObj)
                    put("budget_limit", budgetLimit)
                    put("expenses", expensesArray)
                    put("cash", cashObj)
                    put("banks", banksArray)
                    put("fds", fdArray)
                    put("credit_cards", ccArray)
                    put("investments", invArray) 
                    put("contri_rooms", contriArray)
                    put("ntworth", ntworthObj)
                }

                val responseData = masterJson.toString()

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString("data_$username", responseData).apply()

                val finalParsed = parseJsonToAppData(responseData)
                _appDataState.value = finalParsed
                return@withContext finalParsed

            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseJsonToAppData(responseData: String): AppData {
        val jsonResponse = JSONObject(responseData)
        
        val cal = Calendar.getInstance()
        val currD = cal.get(Calendar.DAY_OF_MONTH)
        val currM = cal.get(Calendar.MONTH) + 1
        val currY = cal.get(Calendar.YEAR)

        val profileObj = jsonResponse.optJSONObject("profile")
        val tempName = profileObj?.optString("name", "") ?: ""
        val tempEmail = profileObj?.optString("email", "") ?: ""
        val tempMobile = profileObj?.optString("mobile", "") ?: ""
        val tempPrfl = profileObj?.optString("prfl", "") ?: ""
        val tempDob = profileObj?.optString("dob", "") ?: ""

        val expensesArray = jsonResponse.optJSONArray("expenses")
        var tempTotal = 0.0
        var tempToday = 0.0
        var tempMonth = 0.0
        var tempYear = 0.0
        val tempHistory = mutableListOf<TransactionModel>()

        if (expensesArray != null && expensesArray.length() > 0) {
            val currDayStr = String.format(Locale.US, "%02d", currD)
            val currMonthStr = String.format(Locale.US, "%02d", currM)
            val currYearStr = currY.toString()
            
            val todayPrefixSlash = "$currDayStr/$currMonthStr/$currYearStr"
            val todayPrefixDash = "$currYearStr-$currMonthStr-$currDayStr" 
            
            for (i in 0 until expensesArray.length()) {
                val item = expensesArray.getJSONObject(i)
                val rawDate = item.optString("date", "").trim()
                val rawAmt = item.optString("amount", "0")
                val amt = rawAmt.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: item.optDouble("amount", 0.0)
                
                if (amt > 0.0) {
                    tempTotal += amt 
                    tempHistory.add(
                        TransactionModel(
                            date = rawDate, 
                            amount = amt, 
                            category = item.optString("category", "Unknown"), 
                            remark1 = item.optString("detail1", ""), 
                            remark2 = item.optString("detail2", ""), 
                            mode = item.optString("mode", ""), 
                            sourceType = item.optString("source_type", ""), 
                            sourceId = item.optString("source_id", "")
                        )
                    )
                    
                    if (rawDate.startsWith(todayPrefixSlash) || rawDate.startsWith(todayPrefixDash)) {
                        tempToday += amt
                    }

                    if (rawDate.contains(currYearStr)) {
                        tempYear += amt
                        if (rawDate.contains("-$currMonthStr-") || rawDate.contains("/$currMonthStr/") || rawDate.startsWith("$currMonthStr-") || rawDate.startsWith("$currMonthStr/")) {
                            tempMonth += amt
                        }
                    }
                }
            }
        }

        val invArray = jsonResponse.optJSONArray("investments")
        val fetchedInvList = mutableListOf<InvestmentItem>()
        if (invArray != null) {
            for (i in 0 until invArray.length()) { 
                val item = invArray.getJSONObject(i)
                
                val histArray = item.optJSONArray("history")
                val parsedHistory = mutableListOf<InvestmentHistoryItem>()
                if (histArray != null) {
                    for (j in 0 until histArray.length()) {
                        val hItem = histArray.getJSONObject(j)
                        parsedHistory.add(
                            InvestmentHistoryItem(
                                date = hItem.optString("date", ""),
                                quantity = hItem.optDouble("quantity", 0.0),
                                price = hItem.optDouble("price", 0.0),
                                amount = hItem.optDouble("amount", 0.0),
                                brokerage = hItem.optDouble("brokerage", 0.0)
                            )
                        )
                    }
                }

                fetchedInvList.add(
                    InvestmentItem(
                        assetName = item.optString("asset_name", ""),
                        assetType = item.optString("asset_type", "Stock"),
                        quantity = item.optDouble("quantity", 0.0), 
                        avgBuyPrice = item.optDouble("buy_price", 0.0),
                        currentPrice = item.optDouble("current_price", item.optDouble("buy_price", 0.0)),
                        oneDayChangePrice = item.optDouble("one_day_change", 0.0),
                        history = parsedHistory 
                    )
                ) 
            }
        }

        val banksArray = jsonResponse.optJSONArray("banks")
        val fetchedBankList = mutableListOf<BankAccountItem>()
        if (banksArray != null) {
            for (i in 0 until banksArray.length()) { 
                val item = banksArray.getJSONObject(i)
                fetchedBankList.add(
                    BankAccountItem(
                        firebaseKey = item.optString("firebase_key", ""), 
                        bankName = item.optString("bank_name", ""), 
                        accountNo = item.optString("account_no", ""), 
                        currentBalance = item.optDouble("current_bal", 0.0), 
                        interestRate = item.optDouble("interest_rate", 0.0), 
                        qtrInterestPct = item.optDouble("qtr_interest_pct", 0.0), 
                        expQtrInt = item.optDouble("exp_qtr_int", 0.0), 
                        accruedQtrInt = item.optDouble("accrued_qtr_int", 0.0), 
                        expYrInt = item.optDouble("exp_yr_int", 0.0), 
                        accruedYrInt = item.optDouble("accrued_yr_int", 0.0), 
                        oneDayInt = item.optDouble("one_day_int", 0.0)
                    )
                ) 
            }
        }

        val cashObj = jsonResponse.optJSONObject("cash")
        val fetchedCash = if (cashObj != null) { 
            CashItem(amount = cashObj.optDouble("amount", 0.0), lastUpdated = cashObj.optString("last_updated", "")) 
        } else {
            CashItem(0.0, "")
        }

        val fdArray = jsonResponse.optJSONArray("fds")
        val fetchedFDList = mutableListOf<FDItem>()
        if (fdArray != null) {
            for (i in 0 until fdArray.length()) { 
                val item = fdArray.getJSONObject(i)
                fetchedFDList.add(
                    FDItem(
                        bankName = item.optString("bank_name", ""), 
                        accountNo = item.optString("account_no", ""), 
                        createDate = item.optString("create_date", ""), 
                        maturityDate = item.optString("maturity_date", ""), 
                        daysToMaturity = item.optInt("days_to_maturity", 0), 
                        investedAmt = item.optDouble("invested_amt", 0.0), 
                        interestRate = item.optDouble("interest_rate", 0.0), 
                        maturityValue = item.optDouble("maturity_value", 0.0), 
                        accruedValue = item.optDouble("accrued_value", 0.0), 
                        accruedInt = item.optDouble("accrued_int", 0.0), 
                        oneDayInt = item.optDouble("one_day_int", 0.0)
                    )
                ) 
            }
        }

        val ccArray = jsonResponse.optJSONArray("credit_cards")
        val fetchedCCList = mutableListOf<CreditCardItem>()
        if (ccArray != null) {
            for (i in 0 until ccArray.length()) { 
                val item = ccArray.getJSONObject(i)
                fetchedCCList.add(
                    CreditCardItem(
                        issuer = item.optString("issuer", ""), 
                        cardNo = item.optString("card_no", ""), 
                        type = item.optString("type", ""), 
                        limit = item.optDouble("limit", 0.0), 
                        outstanding = item.optDouble("outstanding", 0.0), 
                        available = item.optDouble("available", 0.0), 
                        utilization = item.optDouble("utilization", 0.0), 
                        cibilStatus = item.optString("cibil_status", ""), 
                        billingDay = item.optInt("billing_day", 0), 
                        dueDay = item.optInt("due_day", 0), 
                        reminderDay = item.optInt("reminder_day", 0), 
                        annualFee = item.optDouble("annual_fee", 0.0), 
                        joiningFee = item.optDouble("joining_fee", 0.0), 
                        lastUsed = item.optString("last_used", "")
                    )
                ) 
            }
        }

        val contriArray = jsonResponse.optJSONArray("contri_rooms")
        val fetchedContriRooms = mutableListOf<ContriRoomModel>()
        if (contriArray != null) {
            for (i in 0 until contriArray.length()) {
                val item = contriArray.getJSONObject(i)
                val rName = item.optString("room_name", "")
                val rCode = item.optString("room_code", "")
                val rPin = item.optString("passkey", "123456") 
                val expArray = item.optJSONArray("expenses")
                var lastDate = ""
                if (expArray != null && expArray.length() > 0) {
                    val lastExp = expArray.getJSONObject(0)
                    lastDate = lastExp.optString("date", "")
                }
                fetchedContriRooms.add(ContriRoomModel(rName, rCode, lastDate, rPin))
            }
        }

        val fetchedBudgetLimit = jsonResponse.optDouble("budget_limit", 0.0)
        
        val ntworthMap = mutableMapOf<String, List<Double>>()
        val ntworthObj = jsonResponse.optJSONObject("ntworth")
        if (ntworthObj != null) {
            val keys = ntworthObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = ntworthObj.optJSONArray(k)
                if (arr != null) {
                    val list = mutableListOf<Double>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.optDouble(i, 0.0))
                    }
                    ntworthMap[k] = list
                }
            }
        }
        
        return AppData(
            userFullName = tempName,
            userEmail = tempEmail,
            userMobile = tempMobile,
            profilePicUrl = tempPrfl, 
            userDob = tempDob,
            todayExpenses = tempToday,
            thisMonthExpenses = tempMonth,
            thisYearExpenses = tempYear,
            budgetLimit = fetchedBudgetLimit,
            transactionList = tempHistory.reversed(),
            investmentList = fetchedInvList,
            bankList = fetchedBankList,
            cashData = fetchedCash,
            fdList = fetchedFDList,
            ccList = fetchedCCList,
            contriRoomsList = fetchedContriRooms,
            networthHistory = ntworthMap
        )
    }
}
