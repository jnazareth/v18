//package com.mycompany.GPExplorer;

/*
-------------------
history
-------------------
v17: group implementation
v16: cleanup: created separete account, removed static declarations
v15: added category & vendor
v14: bug fix individual transaction amt
v13: added additional header, fixed sys formatting
v12: individual transaction amounts added
v11: added optional "sys" formatting
v10: "sys" account added to output
v9: csv formatting fixed (padding tabs)
v8: "sys" transaction implementation
v7: introducted individual checksum
v6: "percentage" transaction, correct implementation
v5: discard: "percentage" transaction, poor implementation
v4: "clearing" transaction implementation
v2: export to csv implementation
*/



//import Person2;

public class gp2
{
	// ----------------------------------------------------
	// showUsage
	// ----------------------------------------------------
	private void showUsage()
	{
		////System.out.println("gp2 - Purchases & Accounting breakdown");
	}

	// ----------------------------------------------------
	// main
	// ----------------------------------------------------
	public static void main (String[] args)
	throws Exception
	{
		gp2 app = new gp2();

        int parmNo;
        String fileName = "" ;


        //command line optional parameters
        if (args.length == 0 || args.length > 1) {
			app.showUsage() ;
			return;
		}

        for (parmNo = 0; parmNo < args.length; parmNo++) {
			if (args[parmNo].substring(0, 2) == "-h") {
            	app.showUsage() ;
                return;
			}
			else
                fileName = args[parmNo] ;
        }

		account myAccount = new account() ;
		myAccount.ReadAndProcessTransactions(fileName, true) ;	// read input file
        //PrintSummary2() ;

   } // end of main
} // end of class
