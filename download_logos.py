import os
import urllib.request
import time

# Aapke Android project ka drawable path
output_dir = "app/src/main/res/drawable"
os.makedirs(output_dir, exist_ok=True)

# 75+ Banks and Fintechs Domains Dictionary
banks = {
    # Public & Private Banks
    "ic_sbi": "sbi.co.in", "ic_hdfc": "hdfcbank.com", "ic_icici": "icicibank.com",
    "ic_axis": "axisbank.com", "ic_kotak": "kotak.com", "ic_pnb": "pnbindia.in",
    "ic_bob": "bankofbaroda.in", "ic_boi": "bankofindia.co.in", "ic_union": "unionbankofindia.co.in",
    "ic_canara": "canarabank.com", "ic_central": "centralbankofindia.co.in", "ic_indian": "indianbank.in",
    "ic_iob": "iob.in", "ic_uco": "ucobank.com", "ic_bom": "bankofmaharashtra.in",
    "ic_idbi": "idbibank.in", "ic_yes": "yesbank.in", "ic_indusind": "indusind.com",
    "ic_federal": "federalbank.co.in", "ic_idfc": "idfcfirstbank.com", "ic_bandhan": "bandhanbank.com",
    "ic_rbl": "rblbank.com", "ic_jk": "jkbank.com", "ic_karnataka": "karnatakabank.com",
    "ic_kvb": "kvb.co.in", "ic_cub": "cityunionbank.com", "ic_dhanlaxmi": "dhanbank.com",
    "ic_csb": "csb.co.in", "ic_nainital": "nainitalbank.co.in", "ic_tmb": "tmbnet.in",
    "ic_southindian": "southindianbank.com",
    
    # Small Finance & Payments Banks
    "ic_au": "aubank.in", "ic_equitas": "equitasbank.com", "ic_ujjivan": "ujjivansfb.in",
    "ic_utkarsh": "utkarsh.bank", "ic_suryoday": "suryodaybank.com", "ic_shivalik": "shivalikbank.com",
    "ic_capital": "capitalbank.co.in", "ic_jana": "janabank.com", "ic_unity": "theunitybank.com",
    "ic_esaf": "esafbank.com", "ic_nesfb": "nesfb.com", "ic_paytm": "paytm.com",
    "ic_airtel": "airtel.in", "ic_ippb": "ippbonline.com", "ic_jio": "jio.com", "ic_fino": "finobank.com",
    
    # Foreign Banks
    "ic_sc": "sc.com", "ic_citi": "citibank.co.in", "ic_hsbc": "hsbc.co.in",
    "ic_dbs": "dbs.com", "ic_barclays": "barclays.in", "ic_deutsche": "db.com",
    
    # Fintechs, Co-Branded & Credit Cards
    "ic_amazon": "amazon.in", "ic_flipkart": "flipkart.com", "ic_kiwi": "gokiwi.in",
    "ic_supermoney": "super.money", "ic_onecard": "getonecard.app", "ic_slice": "sliceit.com",
    "ic_unicards": "uni.cards", "ic_tataneu": "tatadigital.com", "ic_swiggy": "swiggy.com",
    "ic_zomato": "zomato.com", "ic_sbicard": "sbicard.com", "ic_cred": "cred.club",
    "ic_jupiter": "jupiter.money", "ic_fi": "fi.money", "ic_niyo": "goniyo.com",
    "ic_cheq": "cheq.one", "ic_bajaj": "bajajfinserv.in", "ic_rupay": "rupay.co.in",
    "ic_visa": "visa.co.in", "ic_mastercard": "mastercard.co.in", "ic_amex": "americanexpress.com"
}

req_headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}

print("Downloading HD Offline PNG Logos using Icon.Horse API (No Login Required)...\n")

for name, domain in banks.items():
    # Active & Free Icon.Horse API Endpoint
    url = f"https://icon.horse/icon/{domain}"
    file_path = f"{output_dir}/{name}.png"
    
    try:
        req = urllib.request.Request(url, headers=req_headers)
        with urllib.request.urlopen(req) as response:
            with open(file_path, "wb") as out_file:
                out_file.write(response.read())
        print(f"✅ Saved HD: {name}.png")
    except Exception as e:
        print(f"❌ Failed: {name}.png (Domain not found)")

    # Rate Limit Bypass: 2 second ka pause taaki server block na kare
    time.sleep(2)

print("\n🎉 Sabhi HD Offline Logos successfully drawable folder mein download ho gaye hain!")
