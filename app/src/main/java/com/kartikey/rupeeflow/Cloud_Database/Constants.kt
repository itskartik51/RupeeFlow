package com.kartikey.rupeeflow.Cloud_Database

import com.kartikey.rupeeflow.R

object Constants {
    const val GOOGLE_SHEET_API_URL = "https://script.google.com/macros/s/AKfycbxq6GiNwSEDI85iWu8Zl1mMokr0TPidrIKyM5eHfnnPumnhq4Z1szCLjg-JcuTK_gr6Aw/exec"

    val IndianBanksList = listOf(
        "State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank",
        "Punjab National Bank (PNB)", "Bank of Baroda", "Bank of India", "Union Bank of India",
        "Canara Bank", "Central Bank of India", "Indian Bank", "Indian Overseas Bank", "UCO Bank",
        "Bank of Maharashtra", "IDBI Bank", "Yes Bank", "IndusInd Bank", "Federal Bank",
        "South Indian Bank", "IDFC First Bank", "Bandhan Bank", "RBL Bank", "J&K Bank",
        "Karnataka Bank", "Karur Vysya Bank", "City Union Bank", "Dhanlaxmi Bank", "CSB Bank",
        "Nainital Bank", "Tamilnad Mercantile Bank",
        
        "AU Small Finance Bank", "Equitas Small Finance Bank", "Ujjivan Small Finance Bank",
        "Utkarsh Small Finance Bank", "Suryoday Small Finance Bank", "Shivalik Small Finance Bank",
        "Capital Small Finance Bank", "Jana Small Finance Bank", "Unity Small Finance Bank",
        "ESAF Small Finance Bank", "North East Small Finance Bank",
        
        "Paytm Payments Bank", "Airtel Payments Bank", "India Post Payments Bank", 
        "Jio Payments Bank", "Fino Payments Bank",
        
        "Standard Chartered Bank", "Citi Bank", "HSBC Bank", "DBS Bank", "Barclays Bank", "Deutsche Bank",
        
        "Amazon Pay", "Flipkart", "KIWI", "Super.money", "OneCard", "Slice", "Uni Cards",
        "Tata Neu", "Swiggy", "Zomato", "SBI Card", "Cred", "Jupiter", "Fi Money", "Niyo",
        "Cheq", "Bajaj Finserv", "RuPay", "Visa", "Mastercard", "American Express", "Other Bank"
    )

    // 100% Offline 128px PNG Mapping Engine
    val BankLogoMap = mapOf(
        "State Bank of India (SBI)" to R.drawable.ic_sbi,
        "HDFC Bank" to R.drawable.ic_hdfc,
        "ICICI Bank" to R.drawable.ic_icici,
        "Axis Bank" to R.drawable.ic_axis,
        "Kotak Mahindra Bank" to R.drawable.ic_kotak,
        "Punjab National Bank (PNB)" to R.drawable.ic_pnb,
        "Bank of Baroda" to R.drawable.ic_bob,
        "Bank of India" to R.drawable.ic_boi,
        "Union Bank of India" to R.drawable.ic_union,
        "Canara Bank" to R.drawable.ic_canara,
        "Central Bank of India" to R.drawable.ic_central,
        "Indian Bank" to R.drawable.ic_indian,
        "Indian Overseas Bank" to R.drawable.ic_iob,
        "UCO Bank" to R.drawable.ic_uco,
        "Bank of Maharashtra" to R.drawable.ic_bom,
        "IDBI Bank" to R.drawable.ic_idbi,
        "Yes Bank" to R.drawable.ic_yes,
        "IndusInd Bank" to R.drawable.ic_indusind,
        "Federal Bank" to R.drawable.ic_federal,
        "IDFC First Bank" to R.drawable.ic_idfc,
        "Bandhan Bank" to R.drawable.ic_bandhan,
        "RBL Bank" to R.drawable.ic_rbl,
        "J&K Bank" to R.drawable.ic_jk,
        "Karnataka Bank" to R.drawable.ic_karnataka,
        "Karur Vysya Bank" to R.drawable.ic_kvb,
        "City Union Bank" to R.drawable.ic_cub,
        "Dhanlaxmi Bank" to R.drawable.ic_dhanlaxmi,
        "CSB Bank" to R.drawable.ic_csb,
        "Nainital Bank" to R.drawable.ic_nainital,
        "Tamilnad Mercantile Bank" to R.drawable.ic_tmb,
        "South Indian Bank" to R.drawable.ic_southindian,
        
        "AU Small Finance Bank" to R.drawable.ic_au,
        "Equitas Small Finance Bank" to R.drawable.ic_equitas,
        "Ujjivan Small Finance Bank" to R.drawable.ic_ujjivan,
        "Utkarsh Small Finance Bank" to R.drawable.ic_utkarsh,
        "Suryoday Small Finance Bank" to R.drawable.ic_suryoday,
        "Shivalik Small Finance Bank" to R.drawable.ic_shivalik,
        "Capital Small Finance Bank" to R.drawable.ic_capital,
        "Jana Small Finance Bank" to R.drawable.ic_jana,
        "Unity Small Finance Bank" to R.drawable.ic_unity,
        "ESAF Small Finance Bank" to R.drawable.ic_esaf,
        "North East Small Finance Bank" to R.drawable.ic_nesfb,
        
        "Paytm Payments Bank" to R.drawable.ic_paytm,
        "Airtel Payments Bank" to R.drawable.ic_airtel,
        "India Post Payments Bank" to R.drawable.ic_ippb,
        "Jio Payments Bank" to R.drawable.ic_jio,
        "Fino Payments Bank" to R.drawable.ic_fino,
        
        "Standard Chartered Bank" to R.drawable.ic_sc,
        "Citi Bank" to R.drawable.ic_citi,
        "HSBC Bank" to R.drawable.ic_hsbc,
        "DBS Bank" to R.drawable.ic_dbs,
        "Barclays Bank" to R.drawable.ic_barclays,
        "Deutsche Bank" to R.drawable.ic_deutsche,
        
        "Amazon Pay" to R.drawable.ic_amazon,
        "Flipkart" to R.drawable.ic_flipkart,
        "KIWI" to R.drawable.ic_kiwi,
        "Super.money" to R.drawable.ic_supermoney,
        "OneCard" to R.drawable.ic_onecard,
        "Slice" to R.drawable.ic_slice,
        "Uni Cards" to R.drawable.ic_unicards,
        "Tata Neu" to R.drawable.ic_tataneu,
        "Swiggy" to R.drawable.ic_swiggy,
        "Zomato" to R.drawable.ic_zomato,
        "SBI Card" to R.drawable.ic_sbicard,
        "Cred" to R.drawable.ic_cred,
        "Jupiter" to R.drawable.ic_jupiter,
        "Fi Money" to R.drawable.ic_fi,
        "Niyo" to R.drawable.ic_niyo,
        "Cheq" to R.drawable.ic_cheq,
        "Bajaj Finserv" to R.drawable.ic_bajaj,
        "RuPay" to R.drawable.ic_rupay,
        "Visa" to R.drawable.ic_visa,
        "Mastercard" to R.drawable.ic_mastercard,
        "American Express" to R.drawable.ic_amex
    )
}
