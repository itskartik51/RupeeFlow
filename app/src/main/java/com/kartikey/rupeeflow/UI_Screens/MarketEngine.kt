package com.kartikey.rupeeflow.UI_Screens

import com.kartikey.rupeeflow.Cloud_Database.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MarketSearchResult(
    val cleanSymbol: String,
    val rawSymbol: String,
    val fullName: String,
    val livePrice: Double
)

data class MarketLiveQuote(
    val symbol: String,
    val currentPrice: Double,
    val oneDayChangePrice: Double = 0.0,
    val oneDayChangePercent: Double = 0.0
)

object MarketEngine {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    private const val YAHOO_SEARCH_URL = "https://query2.finance.yahoo.com/v1/finance/search"
    private const val YAHOO_SPARK_URL = "https://query1.finance.yahoo.com/v7/finance/spark"

    /**
     * Searches Indian market assets (Stocks, Mutual Funds, ETFs, Bonds) with 4-tier filtering,
     * deduplication (.NS preference), and live price fetching.
     */
    suspend fun searchAssets(query: String, assetType: String): List<MarketSearchResult> {
        if (query.isBlank() || assetType.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                val validQuoteTypes = when (assetType) {
                    "Mutual Fund" -> listOf("MUTUALFUND")
                    "ETF" -> listOf("ETF")
                    "Bond" -> listOf("EQUITY", "ETF")
                    else -> listOf("EQUITY")
                }

                val encodedQuery = query.trim().replace(" ", "%20")
                val searchUrl = "$YAHOO_SEARCH_URL?q=$encodedQuery&quotesCount=100&region=IN"
                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                val searchResp = httpClient.newCall(searchReq).execute()
                val searchData = searchResp.body?.string() ?: ""

                if (!searchResp.isSuccessful || searchData.isEmpty()) {
                    return@withContext emptyList()
                }

                val jsonResponse = JSONObject(searchData)
                val quotes = jsonResponse.optJSONArray("quotes") ?: return@withContext emptyList()

                val baseSymbolMap = mutableMapOf<String, String>()
                val fallbackNames = mutableMapOf<String, String>()
                val orderedBaseSymbols = mutableListOf<String>()

                for (i in 0 until quotes.length()) {
                    val quote = quotes.getJSONObject(i)
                    val sym = quote.optString("symbol", "")
                    val qType = quote.optString("quoteType", "")
                    val name = quote.optString("longname", quote.optString("shortname", ""))

                    if (sym.isNotEmpty() && (sym.endsWith(".NS") || sym.endsWith(".BO"))) {
                        if (validQuoteTypes.contains(qType)) {

                            // Filter out mutual fund codes if searching for normal stocks
                            if (assetType == "Stock" && sym.startsWith("0P")) continue

                            // Filter out ETF / Bees / Index keywords from standard stocks
                            if (assetType == "Stock") {
                                val nameUpper = name.uppercase(Locale.getDefault())
                                if (nameUpper.contains("ETF") || nameUpper.contains("BEES") ||
                                    nameUpper.contains("INDEX") || nameUpper.contains("LIQUID")) {
                                    continue
                                }
                            }

                            val baseSym = sym.replace(".NS", "").replace(".BO", "")
                            val existing = baseSymbolMap[baseSym]

                            if (existing == null) {
                                baseSymbolMap[baseSym] = sym
                                fallbackNames[sym] = name
                                orderedBaseSymbols.add(baseSym)
                            } else if (!existing.endsWith(".NS") && sym.endsWith(".NS")) {
                                baseSymbolMap[baseSym] = sym
                                fallbackNames.remove(existing)
                                fallbackNames[sym] = name
                            }
                        }
                    }
                }

                val finalSymbolsToFetch = orderedBaseSymbols.mapNotNull { baseSymbolMap[it] }.take(15)
                if (finalSymbolsToFetch.isEmpty()) return@withContext emptyList()

                // Batch fetch live prices using Yahoo Spark endpoint
                val symbolsParam = finalSymbolsToFetch.joinToString(",")
                val sparkUrl = "$YAHOO_SPARK_URL?symbols=$symbolsParam"
                val sparkReq = Request.Builder()
                    .url(sparkUrl)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                val sparkResp = httpClient.newCall(sparkReq).execute()
                val sparkData = sparkResp.body?.string() ?: ""
                val priceMap = mutableMapOf<String, Double>()

                if (sparkResp.isSuccessful && sparkData.isNotEmpty()) {
                    val sparkJson = JSONObject(sparkData)
                    val resultArr = sparkJson.optJSONObject("spark")?.optJSONArray("result")

                    if (resultArr != null) {
                        for (i in 0 until resultArr.length()) {
                            val item = resultArr.getJSONObject(i)
                            val sym = item.optString("symbol", "")
                            val meta = item.optJSONArray("response")?.optJSONObject(0)?.optJSONObject("meta")
                            val price = meta?.optDouble("regularMarketPrice", 0.0) ?: 0.0
                            priceMap[sym] = price
                        }
                    }
                }

                val parsedResults = mutableListOf<MarketSearchResult>()
                for (sym in finalSymbolsToFetch) {
                    val price = priceMap[sym] ?: 0.0
                    val name = fallbackNames[sym] ?: ""
                    val cleanSym = sym.replace(".NS", "").replace(".BO", "")

                    parsedResults.add(
                        MarketSearchResult(
                            cleanSymbol = cleanSym,
                            rawSymbol = sym,
                            fullName = name,
                            livePrice = price
                        )
                    )
                }

                parsedResults
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Batch-fetches real-time price & 1-day change for a list of portfolio symbols/tickers.
     * Returns a map keyed by both raw symbol and clean symbol.
     */
    suspend fun fetchQuotes(symbols: List<String>): Map<String, MarketLiveQuote> {
        if (symbols.isEmpty()) return emptyMap()

        return withContext(Dispatchers.IO) {
            val resultMap = mutableMapOf<String, MarketLiveQuote>()
            try {
                val formattedSymbols = symbols.map { sym ->
                    val clean = sym.trim().uppercase()
                    if (!clean.contains(".") && !clean.startsWith("0P")) {
                        "$clean.NS"
                    } else {
                        clean
                    }
                }.distinct()

                val chunks = formattedSymbols.chunked(20)

                for (chunk in chunks) {
                    val symbolsParam = chunk.joinToString(",")
                    val sparkUrl = "$YAHOO_SPARK_URL?symbols=$symbolsParam"
                    val request = Request.Builder()
                        .url(sparkUrl)
                        .header("User-Agent", USER_AGENT)
                        .get()
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseData = response.body?.string() ?: ""

                    if (response.isSuccessful && responseData.isNotEmpty()) {
                        val sparkJson = JSONObject(responseData)
                        val resultArr = sparkJson.optJSONObject("spark")?.optJSONArray("result")

                        if (resultArr != null) {
                            for (i in 0 until resultArr.length()) {
                                val item = resultArr.getJSONObject(i)
                                val returnedSym = item.optString("symbol", "").trim().uppercase()
                                val meta = item.optJSONArray("response")?.optJSONObject(0)?.optJSONObject("meta")

                                if (meta != null) {
                                    val price = meta.optDouble("regularMarketPrice", 0.0)

                                    // Direct 1D change values from Yahoo meta
                                    val hasDirectChange = meta.has("regularMarketChange")
                                    val directChange = meta.optDouble("regularMarketChange", 0.0)
                                    val directChangePercent = meta.optDouble("regularMarketChangePercent", 0.0)

                                    // Accurate previous close hierarchy
                                    val prevClose = when {
                                        meta.has("regularMarketPreviousClose") -> meta.optDouble("regularMarketPreviousClose", price)
                                        meta.has("previousClose") -> meta.optDouble("previousClose", price)
                                        else -> meta.optDouble("chartPreviousClose", price)
                                    }

                                    val changeRs = if (hasDirectChange && directChange != 0.0) {
                                        directChange
                                    } else if (prevClose > 0.0 && price > 0.0) {
                                        price - prevClose
                                    } else {
                                        0.0
                                    }

                                    val changePct = if (hasDirectChange && directChangePercent != 0.0) {
                                        directChangePercent
                                    } else if (prevClose > 0.0 && price > 0.0) {
                                        (changeRs / prevClose) * 100.0
                                    } else {
                                        0.0
                                    }

                                    val quote = MarketLiveQuote(
                                        symbol = returnedSym,
                                        currentPrice = price,
                                        oneDayChangePrice = changeRs,
                                        oneDayChangePercent = changePct
                                    )

                                    val cleanKey = returnedSym.replace(".NS", "").replace(".BO", "")
                                    resultMap[returnedSym] = quote
                                    resultMap[cleanKey] = quote
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            resultMap
        }
    }

    /**
     * Registers a new asset ticker in the Google Sheet database.
     */
    suspend fun registerTickerInSheet(sheetTicker: String, name: String, assetType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("action", "addTicker")
                    put("ticker", sheetTicker)
                    put("name", name)
                    put("type", assetType)
                }.toString()

                val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(Constants.GOOGLE_SHEET_API_URL)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
