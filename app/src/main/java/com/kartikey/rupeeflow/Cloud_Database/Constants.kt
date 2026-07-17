package com.kartikey.rupeeflow.Cloud_Database

import com.kartikey.rupeeflow.R

object Constants {
    const val GOOGLE_SHEET_API_URL = "https://script.google.com/macros/s/AKfycbxq6GiNwSEDI85iWu8Zl1mMokr0TPidrIKyM5eHfnnPumnhq4Z1szCLjg-JcuTK_gr6Aw/exec"

    // Massive list of 56 Indian Banks + Wallets
    val IndianBanksList = listOf(
        "State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank",
        "Punjab National Bank (PNB)", "Bank of Baroda", "Bank of India", "Union Bank of India",
        "Canara Bank", "Central Bank of India", "Indian Bank", "Indian Overseas Bank", "UCO Bank",
        "Bank of Maharashtra", "IDBI Bank", "Yes Bank", "IndusInd Bank", "Federal Bank",
        "South Indian Bank", "IDFC First Bank", "Bandhan Bank", "RBL Bank", "AU Small Finance Bank",
        "Equitas Small Finance Bank", "Ujjivan Small Finance Bank", "Utkarsh Small Finance Bank",
        "Paytm Payments Bank", "Airtel Payments Bank", "India Post Payments Bank", "Jio Payments Bank",
        "Fino Payments Bank", "Standard Chartered Bank", "Citi Bank", "HSBC Bank", "DBS Bank",
        "Barclays Bank", "Deutsche Bank", "J&K Bank", "Karnataka Bank", "Karur Vysya Bank",
        "City Union Bank", "Dhanlaxmi Bank", "CSB Bank", "Nainital Bank", "Tamilnad Mercantile Bank",
        "Suryoday Small Finance Bank", "Shivalik Small Finance Bank", "Capital Small Finance Bank",
        "Jana Small Finance Bank", "Unity Small Finance Bank", "ESAF Small Finance Bank",
        "North East Small Finance Bank", "Saraswat Cooperative Bank", "Cosmos Cooperative Bank", "Other Bank"
    )

    // Local HD Drawable Resources Mapping
    val BankLogoMap = mapOf(
        "State Bank of India (SBI)" to R.drawable.ic_sbi,
        "HDFC Bank" to R.drawable.ic_hdfc,
        "ICICI Bank" to R.drawable.ic_icici,
        "Kotak Mahindra Bank" to R.drawable.ic_kotak,
        "Axis Bank" to R.drawable.ic_axis,
        "Punjab National Bank (PNB)" to R.drawable.ic_pnb,
        "Bank of Baroda" to R.drawable.ic_bob,
        "Bank of India" to R.drawable.ic_boi,
        "Union Bank of India" to R.drawable.ic_union,
        "Canara Bank" to R.drawable.ic_canara,
        "Utkarsh Small Finance Bank" to R.drawable.ic_utkarsh
    )
}
