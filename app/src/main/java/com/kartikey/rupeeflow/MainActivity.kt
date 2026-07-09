function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var action = data.action;
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // 1. SIGNUP LOGIC - New Sheet Creation
    if (action === "signup") {
      var userSheet = ss.getSheetByName("Users");
      // Check if user exists
      var rows = userSheet.getDataRange().getValues();
      for (var i = 1; i < rows.length; i++) {
        if (rows[i][2] == data.username) return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "Username taken!"})).setMimeType(ContentService.MimeType.JSON);
      }
      
      // Save User to Users Sheet
      userSheet.appendRow([data.name, data.mobile, data.username, data.password]);
      
      // CREATE NEW SHEET FOR USER
      var newSheet = ss.insertSheet(data.username);
      newSheet.appendRow(["Date", "Amount", "Category", "Detail 1", "Detail 2"]);
      
      return ContentService.createTextOutput(JSON.stringify({"status": "success", "message": "Account Created!"})).setMimeType(ContentService.MimeType.JSON);
    }

    // 2. ADD EXPENSE LOGIC - Dynamic Sheet Selection
    if (action === "add_expense") {
      // Data usi sheet mein jayega jo username ke naam ki hai
      var targetSheet = ss.getSheetByName(data.username);
      if (!targetSheet) {
         // Agar galti se sheet delete ho gayi ho toh error
         return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": "User sheet not found!"})).setMimeType(ContentService.MimeType.JSON);
      }
      var timestamp = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd-MM-yyyy hh:mm a");
      targetSheet.appendRow([timestamp, data.amount, data.category, data.detail1, data.detail2]);
      return ContentService.createTextOutput(JSON.stringify({"status": "success"})).setMimeType(ContentService.MimeType.JSON);
    }
    
    // LOGIN LOGIC (Wahi purana)
    // ... (Keep the login logic same as before)
    
  } catch(error) { return ContentService.createTextOutput(JSON.stringify({"status": "error", "message": error.message})).setMimeType(ContentService.MimeType.JSON); }
}
