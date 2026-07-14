// HELPER FUNCTION: Naye users (Signup) aur failsafe setup ke liye
function setupBankingColumns(sheet) {
  var z2Value = sheet.getRange("Z2").getValue();
  var aa2Value = sheet.getRange("AA2").getValue(); 

  if (z2Value !== "Banking Data" || aa2Value === "Banking Data") {
    var titleRange = sheet.getRange("Z2:AP2");
    titleRange.breakApart(); 
    titleRange.clearContent();
    
    sheet.getRange("Z2").setValue("Banking Data");
    titleRange.mergeAcross()
              .setHorizontalAlignment("center")
              .setFontWeight("bold")
              .setBackground("#e3f2fd"); 
    
    var headers = [
      "Bank Name", "Account No.", "Current Bal.", "Interest % (Yr)", "Qtr. Interest %", 
      "6D Bal. Blocks", "6D Avg.", "Monthly Avg.", "Qtr. Avg.", "Yearly Avg.", 
      "Exp. Qtr. Int.", "Accrued Qtr. Int.", "Exp. Yr. Int.", "Accrued Yr. Int.", "1-Day Int.",
      "", "" 
    ];
    sheet.getRange("Z3:AP3").setValues([headers])
         .setFontWeight("bold").setHorizontalAlignment("center");
  }
}

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var action = data.action;
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var userSheet = ss.getSheetByName("Users");

    // ==========================================
    // 1. SIGNUP LOGIC
    // ==========================================
    if (action === "signup") {
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if (rows[i][1] == data.mobile) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Mobile already registered!"})).setMimeType(ContentService.MimeType.JSON);
        if (rows[i][2] == data.username) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Username already taken!"})).setMimeType(ContentService.MimeType.JSON);
      }
      
      userSheet.appendRow([data.name, data.mobile, data.username, data.password, ""]);
      var newSheet = ss.insertSheet(data.username);
      
      newSheet.getRange("A1").setValue("Expenses Data");
      newSheet.getRange("A1:G1").mergeAcross().setHorizontalAlignment("center").setFontWeight("bold").setBackground("#e0f2f1");
      
      newSheet.getRange("I1").setValue("Investment Data");
      newSheet.getRange("I1:X1").mergeAcross().setHorizontalAlignment("center").setFontWeight("bold").setBackground("#fff9c4");

      var expensesHeaders = ["Date", "Amount", "Category", "Detail_1", "Detail_2", "Mode", ""];
      newSheet.getRange("A2:G2").setValues([expensesHeaders]).setFontWeight("bold");

      var invHeaders = ["Inv_Date", "Asset_Name", "Asset_Type", "Quantity", "Avg_Buy_Price", "Invested_Value", "Current_Price", "Current_Value", "1_Day_Return", "Total_Return_₹", "Total_Return_%", "Broker_Platform", "Notes", "", "", ""];
      newSheet.getRange("I2:X2").setValues([invHeaders]).setFontWeight("bold");

      setupBankingColumns(newSheet);

      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Account Created!", "username": data.username})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 2. LOGIN LOGIC
    // ==========================================
    if (action === "login") {
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if ((rows[i][1] == data.mobile || rows[i][2] == data.mobile) && rows[i][3] == data.password) {
          return ContentService.createTextOutput(JSON.stringify({
            "status": "success", 
            "message": "Login Successful!", 
            "username": rows[i][2],
            "email": rows[i][4] ? rows[i][4] : ""
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Invalid Mobile or Password!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 3. ADD EXPENSE LOGIC 
    // ==========================================
    if (action === "add_expense") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      if (targetSheet.getRange("F2").getValue() === "") targetSheet.getRange("F2").setValue("Mode").setFontWeight("bold");

      var timestamp = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd-MM-yyyy hh:mm a");
      var aValues = targetSheet.getRange("A:A").getValues();
      var nextExpenseRow = 3; 
      for (var i = aValues.length - 1; i >= 0; i--) {
        if (aValues[i][0] != "") { nextExpenseRow = i + 2; break; }
      }
      
      var expenseData = [timestamp, data.amount, data.category, data.detail1, data.detail2, data.payment_method || "", ""];
      targetSheet.getRange(nextExpenseRow, 1, 1, expenseData.length).setValues([expenseData]);
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Saved!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 4. ADD INVESTMENT LOGIC 
    // ==========================================
    if (action === "add_investment") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      var jValues = targetSheet.getRange("J:J").getValues();
      var nextRow = 3; 
      for (var k = jValues.length - 1; k >= 0; k--) {
        if (jValues[k][0] != "") { nextRow = k + 2; break; }
      }
      
      var f_invested = "=L" + nextRow + "*M" + nextRow;  
      var f_currentPrice = '=IF(OR(K'+nextRow+'="Stock", K'+nextRow+'="ETF"), GOOGLEFINANCE("NSE:"&J'+nextRow+', "price"), M'+nextRow+')'; 
      var f_currentValue = "=L" + nextRow + "*O" + nextRow;
      var f_1DayReturn = '=IF(OR(K'+nextRow+'="Stock", K'+nextRow+'="ETF"), GOOGLEFINANCE("NSE:"&J'+nextRow+', "change"), 0)';
      var f_totalReturn = "=P" + nextRow + "-N" + nextRow;
      var f_totalReturnPct = "=IF(N"+nextRow+">0, (R"+nextRow+"/N"+nextRow+")*100, 0)";

      var invData = [
        data.inv_date,                  
        data.asset_name,                
        data.asset_type,                
        data.quantity,                  
        data.buy_price,                 
        f_invested,            
        f_currentPrice,        
        f_currentValue,        
        f_1DayReturn,          
        f_totalReturn,         
        f_totalReturnPct,      
        data.broker || "",                    
        data.notes || "",
        "", "", "" 
      ];
      
      targetSheet.getRange(nextRow, 9, 1, invData.length).setValues([invData]);
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Investment Saved!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 5. ADD BANK LOGIC 
    // ==========================================
    if (action === "add_bank") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      setupBankingColumns(targetSheet);

      var zValues = targetSheet.getRange("Z:Z").getValues();
      var nextBankRow = 4; 
      var lastFilledRow = -1;
      
      for (var k = zValues.length - 1; k >= 3; k--) {
        if (zValues[k][0] != "") { 
          lastFilledRow = k + 1; 
          break; 
        }
      }
      
      if (lastFilledRow !== -1) {
        nextBankRow = lastFilledRow + 10; 
      }

      var today = new Date();
      var dayStr = Utilities.formatDate(today, "Asia/Kolkata", "d");
      var day = parseInt(dayStr, 10);
      
      var blockIndex = 0; 
      var aeOffset = 0;   
      
      if (day >= 1 && day <= 6) { blockIndex = 0; aeOffset = day - 1; }
      else if (day >= 7 && day <= 12) { blockIndex = 1; aeOffset = day - 7; }
      else if (day >= 13 && day <= 18) { blockIndex = 2; aeOffset = day - 13; }
      else if (day >= 19 && day <= 24) { blockIndex = 3; aeOffset = day - 19; }
      else if (day >= 25 && day <= 31) { blockIndex = 4; aeOffset = day - 25; }

      var numCols = 15; 
      var blockData = [];
      for (var r = 0; r < 7; r++) {  
        blockData[r] = new Array(numCols).fill(""); 
      }

      var f_qtrIntPct = "=AC" + nextBankRow + "/4"; 
      var f_1DayInt = "=(AB" + nextBankRow + "*(AC" + nextBankRow + "/100))/365"; 
      var f_MonthlyAvg = '=IFERROR(AVERAGEIF(AF' + nextBankRow + ':AF' + (nextBankRow + 4) + ', "<>0"), AB' + nextBankRow + ')'; 
      var f_QtrAvg = "=AG" + nextBankRow; 
      var f_YearlyAvg = "=AH" + nextBankRow; 
      var f_ExpQtrInt = "=IFERROR((AH" + nextBankRow + " * AD" + nextBankRow + ")/100, 0)"; 
      var f_AccruedQtrInt = '=IFERROR(AJ' + nextBankRow + ' * ((TODAY() - DATE(YEAR(TODAY()), INT((MONTH(TODAY())-1)/3)*3+1, 1) + 1) / 90), 0)';
      var f_ExpYrInt = "=IFERROR((AI" + nextBankRow + " * AC" + nextBankRow + ")/100, 0)"; 
      var f_AccruedYrInt = '=IFERROR(AL' + nextBankRow + ' * ((TODAY() - DATE(YEAR(TODAY()), 1, 1) + 1) / 365), 0)';

      blockData[0][0] = data.bank_name;        
      blockData[0][1] = data.account_no;       
      blockData[0][2] = data.current_bal;      
      blockData[0][3] = data.interest_rate;    
      blockData[0][4] = f_qtrIntPct; 
      blockData[0][7] = f_MonthlyAvg; 
      blockData[0][8] = f_QtrAvg; 
      blockData[0][9] = f_YearlyAvg; 
      blockData[0][10] = f_ExpQtrInt; 
      blockData[0][11] = f_AccruedQtrInt; 
      blockData[0][12] = f_ExpYrInt; 
      blockData[0][13] = f_AccruedYrInt;
      blockData[0][14] = f_1DayInt; 
      blockData[aeOffset][5] = data.current_bal; 
      blockData[blockIndex][6] = "=IFERROR(AVERAGE(AE" + nextBankRow + ":AE" + (nextBankRow + 6) + "), 0)"; 

      targetSheet.getRange(nextBankRow, 26, 7, numCols).setValues(blockData).setHorizontalAlignment("center");
      
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Bank Account Added!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 6. EDIT PROFILE LOGIC
    // ==========================================
    if (action === "edit_profile") {
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if (rows[i][2] === data.username) {
          if (data.new_name) userSheet.getRange(i + 1, 1).setValue(data.new_name);
          if (data.new_mobile) userSheet.getRange(i + 1, 2).setValue(data.new_mobile);
          if (data.new_username && data.new_username !== data.username) {
             var uSheet = ss.getSheetByName(data.username);
             if (uSheet) uSheet.setName(data.new_username);
             userSheet.getRange(i + 1, 3).setValue(data.new_username);
          }
          return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Profile Updated Successfully!"})).setMimeType(ContentService.MimeType.JSON);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User not found!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 7. EDIT EXPENSE LOGIC
    // ==========================================
    if (action === "edit_expense") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      var aValues = targetSheet.getRange("A:A").getValues();
      for (var i = 1; i < aValues.length; i++) {
        if (aValues[i][0] !== "") {
          var sheetDate = (aValues[i][0] instanceof Date) ? Utilities.formatDate(aValues[i][0], "Asia/Kolkata", "dd-MM-yyyy hh:mm a") : aValues[i][0].toString();
          
          if (sheetDate === data.original_date) {
            var row = i + 1;
            if (data.amount) targetSheet.getRange(row, 2).setValue(data.amount);
            if (data.category) targetSheet.getRange(row, 3).setValue(data.category);
            if (data.detail1 !== undefined) targetSheet.getRange(row, 4).setValue(data.detail1); 
            if (data.detail2 !== undefined) targetSheet.getRange(row, 5).setValue(data.detail2); 
            if (data.mode) targetSheet.getRange(row, 6).setValue(data.mode);
            
            return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Expense Updated!"})).setMimeType(ContentService.MimeType.JSON);
          }
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Expense Entry not found!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 8. EDIT BANK LOGIC
    // ==========================================
    if (action === "edit_bank") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      
      var aaValues = targetSheet.getRange("AA:AA").getValues(); 
      for (var i = 3; i < aaValues.length; i++) { 
         if (aaValues[i][0].toString() === data.original_account_no) {
            var row = i + 1;
            
            if (data.new_bank_name) targetSheet.getRange(row, 26).setValue(data.new_bank_name); 
            if (data.new_account_no) targetSheet.getRange(row, 27).setValue(data.new_account_no); 
            if (data.new_interest_rate) targetSheet.getRange(row, 29).setValue(data.new_interest_rate); 

            if (data.new_current_bal !== undefined && data.new_current_bal !== "") {
               targetSheet.getRange(row, 28).setValue(data.new_current_bal); 
               
               var today = new Date();
               var day = parseInt(Utilities.formatDate(today, "Asia/Kolkata", "d"), 10);
               var aeOffset = 0;
               
               if (day >= 1 && day <= 6) { aeOffset = day - 1; }
               else if (day >= 7 && day <= 12) { aeOffset = day - 7; }
               else if (day >= 13 && day <= 18) { aeOffset = day - 13; }
               else if (day >= 19 && day <= 24) { aeOffset = day - 19; }
               else if (day >= 25 && day <= 31) { aeOffset = day - 25; }
               
               targetSheet.getRange(row + aeOffset, 31).setValue(data.new_current_bal);
            }
            return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Bank Details Updated!"})).setMimeType(ContentService.MimeType.JSON);
         }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Bank Account not found!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 9. UNIVERSAL DELETE LOGIC
    // ==========================================
    if (action === "delete_data") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);

      var dataType = data.data_type;
      var identifier = data.identifier;

      if (dataType === "bank") {
        var aaValues = targetSheet.getRange("AA:AA").getValues();
        for (var i = 3; i < aaValues.length; i++) {
          if (aaValues[i][0].toString() === identifier) {
            targetSheet.deleteRows(i + 1, 10);
            return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Bank Deleted!"})).setMimeType(ContentService.MimeType.JSON);
          }
        }
      } 
      else if (dataType === "expense") {
        var aValues = targetSheet.getRange("A:A").getValues();
        for (var i = 1; i < aValues.length; i++) {
          if (aValues[i][0] !== "") {
            var sheetDate = (aValues[i][0] instanceof Date) ? Utilities.formatDate(aValues[i][0], "Asia/Kolkata", "dd-MM-yyyy hh:mm a") : aValues[i][0].toString();
            if (sheetDate === identifier) {
              targetSheet.deleteRow(i + 1);
              return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Expense Deleted!"})).setMimeType(ContentService.MimeType.JSON);
            }
          }
        }
      }
      else if (dataType === "investment") {
        var jValues = targetSheet.getRange("J:J").getValues();
        for (var i = 1; i < jValues.length; i++) {
          if (jValues[i][0].toString() === identifier) {
            targetSheet.deleteRow(i + 1);
            return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Investment Deleted!"})).setMimeType(ContentService.MimeType.JSON);
          }
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Data not found to delete!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // ==========================================
    // 10. GET ALL DATA (🔥 LIVE MARKET FIX HERE)
    // ==========================================
    if (action === "get_all_data") {
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) return ContentService.createTextOutput(JSON.stringify({"status": "success", "expenses": [], "investments": [], "banks": []})).setMimeType(ContentService.MimeType.JSON);
      
      setupBankingColumns(targetSheet);

      // ---> FORCE LIVE MARKET SYNC LOGIC <---
      var lastR = targetSheet.getLastRow();
      if (lastR > 2) {
        var kValues = targetSheet.getRange("K3:K" + lastR).getValues(); 
        var jValues = targetSheet.getRange("J3:J" + lastR).getValues(); 
        var formulasInjected = false;
        
        for (var idx = 0; idx < kValues.length; idx++) {
          var aType = kValues[idx][0];
          var aName = jValues[idx][0];
          
          if ((aType === "Stock" || aType === "ETF") && aName !== "") {
             var rowNum = idx + 3;
             // Zbardasti formula wapas set karo taaki freeze hat jaye
             targetSheet.getRange(rowNum, 15).setFormula('=GOOGLEFINANCE("NSE:"&J' + rowNum + ', "price")');
             targetSheet.getRange(rowNum, 17).setFormula('=GOOGLEFINANCE("NSE:"&J' + rowNum + ', "change")');
             formulasInjected = true;
          }
        }
        
        if (formulasInjected) {
           SpreadsheetApp.flush(); // Sheet ko force calculate karwao JSON bhejne se pehle!
           Utilities.sleep(1000); // 1 second ka time do taaki Google naya price fetch kar le.
        }
      }
      // ----------------------------------------

      var rows = targetSheet.getDataRange().getValues();
      var userExpenses = [];
      var userInvestments = [];
      var userBanks = [];
      
      for (var i = 2; i < rows.length; i++) {
        if(rows[i][0] != "") { 
            var dateStr = (rows[i][0] instanceof Date) ? Utilities.formatDate(rows[i][0], "Asia/Kolkata", "dd-MM-yyyy hh:mm a") : rows[i][0].toString();
            userExpenses.push({
              date: dateStr, amount: rows[i][1], category: rows[i][2],
              detail1: rows[i][3] ? rows[i][3].toString() : "", detail2: rows[i][4] ? rows[i][4].toString() : "",
              mode: rows[i][5] ? rows[i][5].toString() : ""
            });
        }
        
        if(rows[i][9] && rows[i][9] !== "") { 
            userInvestments.push({
              inv_date: rows[i][8] ? rows[i][8].toString() : "",      
              asset_name: rows[i][9].toString(),    
              asset_type: rows[i][10] ? rows[i][10].toString() : "Stock",
              quantity: parseFloat(rows[i][11]) || 0,
              buy_price: parseFloat(rows[i][12]) || 0,
              current_price: parseFloat(rows[i][14]) || 0, 
              one_day_change: parseFloat(rows[i][16]) || 0 
            });
        }

        if(i >= 3 && rows[i][25] && rows[i][25] !== "") { 
            userBanks.push({
              bank_name: rows[i][25].toString(),
              account_no: rows[i][26].toString(),
              current_bal: parseFloat(rows[i][27]) || 0,
              interest_rate: parseFloat(rows[i][28]) || 0,
              qtr_interest_pct: parseFloat(rows[i][29]) || 0,
              exp_qtr_int: parseFloat(rows[i][35]) || 0,
              accrued_qtr_int: parseFloat(rows[i][36]) || 0,
              exp_yr_int: parseFloat(rows[i][37]) || 0,
              accrued_yr_int: parseFloat(rows[i][38]) || 0,
              one_day_int: parseFloat(rows[i][39]) || 0 
            });
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "expenses": userExpenses, "investments": userInvestments, "banks": userBanks})).setMimeType(ContentService.MimeType.JSON);
    }

  } catch(error) {
    return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": error.message})).setMimeType(ContentService.MimeType.JSON);
  }
}

// ==========================================
// BACKGROUND AUTOMATION: 11:50 PM BANK SHIFT LOGIC 
// ==========================================
function processDailyBankBalances() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheets = ss.getSheets();
  
  var today = new Date();
  var timeZone = "Asia/Kolkata";
  var dayStr = Utilities.formatDate(today, timeZone, "d");
  var day = parseInt(dayStr, 10);
  
  var isLastDayOfBlock = (day === 6 || day === 12 || day === 18 || day === 24);
  
  var tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000);
  var tomorrowDay = parseInt(Utilities.formatDate(tomorrow, timeZone, "d"), 10);
  var isLastDayOfMonth = (tomorrowDay === 1);
  
  if (isLastDayOfMonth) isLastDayOfBlock = true;
  
  var blockIndex = 0;
  var aeOffset = 0;
  if (day >= 1 && day <= 6) { blockIndex = 0; aeOffset = day - 1; }
  else if (day >= 7 && day <= 12) { blockIndex = 1; aeOffset = day - 7; }
  else if (day >= 13 && day <= 18) { blockIndex = 2; aeOffset = day - 13; }
  else if (day >= 19 && day <= 24) { blockIndex = 3; aeOffset = day - 19; }
  else if (day >= 25 && day <= 31) { blockIndex = 4; aeOffset = day - 25; }
  
  for (var i = 0; i < sheets.length; i++) {
    var sheet = sheets[i];
    if (sheet.getName() !== "Users") {
      var lastRow = sheet.getLastRow();
      if(lastRow < 4) continue;
      
      var zValues = sheet.getRange("Z1:Z" + lastRow).getValues();
      var abValues = sheet.getRange("AB1:AB" + lastRow).getValues(); 
      
      for (var r = 3; r < zValues.length; r++) { 
        if (zValues[r][0] !== "") { 
          var bankRow = r + 1; 
          var currentBal = abValues[r][0];
          
          sheet.getRange(bankRow + aeOffset, 31).setValue(currentBal); 
          
          if (isLastDayOfBlock) {
             var afRange = sheet.getRange(bankRow + blockIndex, 32); 
             afRange.setValue(afRange.getValue());
             
             if (!isLastDayOfMonth) {
                var nextBlockIndex = blockIndex + 1;
                sheet.getRange(bankRow + nextBlockIndex, 32)
                     .setFormula("=IFERROR(AVERAGE(AE" + bankRow + ":AE" + (bankRow + 6) + "), 0)");
                sheet.getRange(bankRow, 31, 7, 1).clearContent();
             } else {
                sheet.getRange(bankRow, 31, 7, 1).clearContent(); 
                sheet.getRange(bankRow + 1, 32, 4, 1).clearContent(); 
                sheet.getRange(bankRow, 32).setFormula("=IFERROR(AVERAGE(AE" + bankRow + ":AE" + (bankRow + 6) + "), 0)");
             }
          }
        }
      }
    }
  }
}

// ==========================================
// BACKGROUND AUTOMATION: 3:50 PM FREEZE LOGIC
// ==========================================
function freezeMarketData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheets = ss.getSheets();
  
  for (var i = 0; i < sheets.length; i++) {
    var sheet = sheets[i];
    if (sheet.getName() !== "Users") {
      var lastRow = sheet.getLastRow();
      if (lastRow > 2) {
        var priceRange = sheet.getRange(3, 15, lastRow - 2, 1); 
        var returnRange = sheet.getRange(3, 17, lastRow - 2, 1); 
        
        priceRange.setValues(priceRange.getValues());
        returnRange.setValues(returnRange.getValues());
      }
    }
  }
}

// ==========================================
// BACKGROUND AUTOMATION: 9:00 AM RESUME LOGIC
// ==========================================
function resumeMarketData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheets = ss.getSheets();
  
  for (var i = 0; i < sheets.length; i++) {
    var sheet = sheets[i];
    if (sheet.getName() !== "Users") {
      var lastRow = sheet.getLastRow();
      if (lastRow > 2) {
        for (var row = 3; row <= lastRow; row++) {
          var assetType = sheet.getRange(row, 11).getValue(); 
          if (assetType === "Stock" || assetType === "ETF") {
            var formulaPrice = '=GOOGLEFINANCE("NSE:"&J'+row+', "price")';
            var formulaChange = '=GOOGLEFINANCE("NSE:"&J'+row+', "change")';
            
            sheet.getRange(row, 15).setFormula(formulaPrice); 
            sheet.getRange(row, 17).setFormula(formulaChange); 
          }
        }
      }
    }
  }
}
