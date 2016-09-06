import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Collections;

public class account
{
	// ----------------------------------------------------
	// Declarations
	// ----------------------------------------------------
	private Hashtable<String, Person2> m_Persons, m_System ;
	private Hashtable<String, Hashtable<String, Person2>> m_GroupCollection ;
	private int m_numActive = 0 ;
	private float m_nTotAmount = 0, m_nSysToAmount ;
	private ArrayList<String> m_exportLines = null;
	private boolean m_bClearing = false ;
	private boolean m_bSys = false ;

	//CONSTANTS
	// Actions
	private final String ADD_ITEM = "*" ;
	private final String ENABLE_ITEM = "+" ;
	private final String DISABLE_ITEM = "-" ;

	//transaction indicators
	private final String REM = "rem" ;
	private final String ALL = "all" ;
	private final String SYS = "sys" ;
	private final String CLEARING = "$" ;
	private final String PERCENTAGE = "%" ;
	private final char 	COMMENT = '#' ;

	//separators
	private final String AMT_INDICATOR = ":" ;
	private final String ITEM_SEPARATOR = "," ;
	private final String READ_SEPARATOR = "\t" ;
	private final String TAB_SEPARATOR = "\t" ;
	//private final String TAB_SEPARATOR = "!" ;
	private final String DUMP_SEPARATOR = " : " ;

	//id markers
	private final String ID_SEPARATOR = ":" ;
	private final String SELF = ":self" ;
	private final String GROUP = ":group" ;
	private final String ID_lR = "(" ;
	private final String ID_rR = ")" ;
	private final String DEFAULT_GROUP = "default" ;

	//calculation direction
	private final int 	_FR = 0 ;
	private final int	_TO = 1 ;

	// input read positions
	private final int 	P_ITEM = 0 ;
	private final int	P_CATEGORY = 1 ;
	private final int 	P_VENDOR = 2 ;
	private final int	P_DESC = 3 ;
	private final int 	P_AMOUNT = 4 ;
	private final int	P_FROM = 5 ;
	private final int 	P_TO = 6 ;
	private final int	P_ACTION = 7 ;

	//formatting strings
	private final String lPAD = "lPad[" ;
	private final String rPAD = "]rPad" ;
	private final String lBr = "{" ;
	private final String rBr = "}" ;

	//out file strings
	private final String OUT_FILESEP = "." ;
	private final String OUT_EXTENSION = ".csv" ;
	private final String OUT_FILE = ".out" ;

	// output headers
	private final String H_TRANSACTION_AMOUNTS = "transaction amounts" ;
	private final String H_OWE = "(you owe) / owed to you" ;
	private final String H_INDIVIDUAL_TOTALS = "individual totals" ;
	private final String H_ITEM = "Item" ;
	private final String H_CATEGORY = "Category" ;
	private final String H_VENDOR = "Vendor" ;
	private final String H_DESCRIPTION = "Description" ;
	private final String H_AMOUNT = "Amount" ;
	private final String H_FROM = "From" ;
	private final String H_TO = "To" ;
	private final String H_ACTION = "Action" ;
	private final String H_CHECKSUM = "CheckSum" ;
	private final String H_INDCHECKSUM = "IndCheckSum" ;

	// ----------------------------------------------------
	// Class body
	// ----------------------------------------------------

	// ----------------------------------------------------
	// getFile
	// ----------------------------------------------------
	private File getFile(String fileName)
	throws FileNotFoundException
	{
		File aFile = new File(fileName);
		if (aFile.exists()) return aFile;
		else throw new FileNotFoundException("File  " + fileName + " does not exist.");
	}

	// ----------------------------------------------------
	// numActive
	// ----------------------------------------------------
	private int numActive()
	{
		Iterator<String> iter = m_Persons.keySet().iterator();
		int i=0;
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			if (aPer.m_active == true) i++ ;
		}
		return i ;
	}

	// ----------------------------------------------------
	// dumpPersons
	// ----------------------------------------------------
	private void dumpPersons()
	{
		////System.out.println("dumpPersons") ;
		Iterator<String> iter = m_Persons.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			////System.out.println(aPer.m_name + ":" + aPer.m_active) ;
		}
	}

	// ----------------------------------------------------
	// dumpAll
	// ----------------------------------------------------
	private void dumpAll()
	{
		////System.out.println("dumpAll") ;
		Iterator<String> iter = m_Persons.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			////System.out.println(aPer.m_name + DUMP_SEPARATOR + aPer.m_active + DUMP_SEPARATOR + aPer.m_amount[aPer.SYS_SUM] + DUMP_SEPARATOR + aPer.m_amount[aPer.IND_SUM]) ;
		}
	}

	// ----------------------------------------------------
	// percentageToAmounts
	// ----------------------------------------------------
	private String percentageToAmounts(float amt, String in, String action)
	{
		if (in.indexOf(PERCENTAGE) == -1) return in;

		String sOut = "" ;
		String sIn[] = in.split(ITEM_SEPARATOR) ;

		String eachName = "" ;
		float eachPer = 0 ;
		for (int i = 0; i < sIn.length; i++) {
			eachName = "";	eachPer = 0 ;
			String sEach[] = sIn[i].split(AMT_INDICATOR) ;
			for (int k = 0; k < sEach.length; k++) {
				int pLoc = -1 ;
				if ((pLoc = sEach[k].indexOf(PERCENTAGE)) == -1) {
					try {
						eachPer = Float.parseFloat(sEach[k]) ;
						sOut += AMT_INDICATOR + sEach[k] ;
					} catch (NumberFormatException e) {
						eachName = sEach[k].trim() ;
						if (sOut == "") sOut += sEach[k] ;
						else sOut += ITEM_SEPARATOR + sEach[k] ;
					}
				} else {
					eachPer = Float.parseFloat(sEach[k].substring(0, pLoc)) ;
					float xAmt = amt * eachPer / 100 ;
					sOut += AMT_INDICATOR + String.valueOf(xAmt) ;
				}
			}
		}
		////System.out.println("percentageToAmounts: amt = " + amt + ", sIn = " + in + ", sOut = " +  sOut);
		return sOut ;
	}

	// ----------------------------------------------------
	// doFromTo
	// ----------------------------------------------------
	private void doFromTo(int idx, float amt, String in)
	{
		float amtRem = 0, amtAll = 0 ;
		int numRem = 0, numAll = numActive() ;

		String sIn[] = in.split(ITEM_SEPARATOR) ;

		int numRest = 0 ;
		for (int i = 0; i < sIn.length; i++) {
			if (sIn[i].indexOf(ALL) != -1) continue ;
			if (sIn[i].indexOf(REM) != -1) continue ;
			if (sIn[i].indexOf(SYS) != -1) continue ;
			numRest++ ;
		}
		numRem = numRest ;

		String sNonRem = "" ;
		if (sIn.length == 1) {
			amtRem = amtAll = amt ;
			///*if (idx == _TO) */ ////System.out.println("START::" + "amtRem:" + amtRem + ",amtAll:" + amtAll + ",numAll:" + numAll + ",numRem:" + numRem + ",numRest:" + numRest) ;
		}
		else {
			for (int j = 0; j < sIn.length; j++) {
				if (sIn[j].indexOf(REM) != -1) continue ;
				if (sIn[j].indexOf(ALL) != -1) continue ;
				if (sIn[j].indexOf(SYS) != -1) continue ;

				String sEach[] = sIn[j].split(AMT_INDICATOR) ;
				for (int k = 0; k < sEach.length; k++) {
					try {
						float xAmt = Float.parseFloat(sEach[k]) ;
						amtRem += xAmt ;
					} catch (NumberFormatException e) {
						sNonRem += ITEM_SEPARATOR + sEach[k] ;
					}
				}
			}
			amtAll = amt ;
		}
		//if (idx == _TO) System.out.println("START::" + "amtRem:" + amtRem + ",amtAll:" + amtAll + ",sNonRem:" + sNonRem) ;

		String eachName = "" ;
		float eachAmt = 0 ;
		for (int j = 0; j < sIn.length; j++) {
			eachName = "";	eachAmt = 0 ;
			String sEach[] = sIn[j].split(AMT_INDICATOR) ;
			for (int k = 0; k < sEach.length; k++) {
				try {
					eachAmt = Float.parseFloat(sEach[k]) ;
				} catch (NumberFormatException e) {
					eachName = sEach[k].trim() ;
					//if (idx == _TO) System.out.println("eachName::" + eachName) ;
				}
			}

			if ((eachName.compareToIgnoreCase(ALL) == 0) && (in.indexOf(ALL) != -1)) {
				float tAmt =  0;
				if (eachAmt != 0) tAmt = eachAmt ;
				else tAmt = amtAll ;
				float iAmt = tAmt/numAll ;
				//System.out.println(ALL + "::" + "eachAmt:" + eachAmt + ",amtAll:" + amtAll + ",numAll:" + numAll + ",tAmt:" + tAmt + ",iAmt:" + iAmt) ;

				Iterator<String> iter = m_Persons.keySet().iterator();
				while(iter.hasNext()){
					Person2 aPer = m_Persons.get(iter.next()) ;
					if (aPer.m_active != true) continue ;
					aPer.m_amount[idx] += iAmt ;
					if ((idx == _TO) && (m_bClearing == false)) {
						aPer.m_amount[aPer.TRANS_AMT] += iAmt ;
						aPer.m_amount[aPer.IND_SUM] += iAmt ;
					}
					m_Persons.put(aPer.m_name, aPer) ;
				}
			} else if ((eachName.compareToIgnoreCase(REM) == 0) && (in.indexOf(REM) != -1)) {
				float tAmt =  0;
				if (eachAmt != 0) tAmt = eachAmt ;
				else tAmt = amtAll-amtRem ;
				float rAmt = tAmt/(numAll-numRem) ;
				////System.out.println(REM + "::" + "eachAmt:" + eachAmt + ",amtAll:" + amtAll + ",amtRem:" + amtRem + ",numAll:" + numAll + ",numRem:" + numRem + ",tAmt:" + tAmt + ",rAmt:" + rAmt) ;

				Iterator<String> iter = m_Persons.keySet().iterator();
				while(iter.hasNext()){
					Person2 aPer = m_Persons.get(iter.next()) ;
					if (aPer.m_active != true) continue ;
					if (sNonRem.indexOf(aPer.m_name) != -1) continue ;
					aPer.m_amount[idx] += rAmt ;
					if ((idx == _TO) && (m_bClearing == false)) {
						aPer.m_amount[aPer.TRANS_AMT] += rAmt ;
						aPer.m_amount[aPer.IND_SUM] += rAmt ;
					}
					m_Persons.put(aPer.m_name, aPer) ;
				}
			} else if ((eachName.compareToIgnoreCase(SYS) == 0) && (in.indexOf(SYS) != -1)) {
				m_bSys = true ;
				float sAmt =  0;
				if (eachAmt != 0) sAmt = eachAmt ;
				else sAmt = amtAll;
				float rAmt = sAmt;
				////System.out.println(SYS + "::" + "eachAmt:" + eachAmt + ",amtAll:" + amtAll + ",amtRem:" + amtRem + ",numAll:" + numAll + ",numRem:" + numRem + ",sAmt:" + sAmt + ",rAmt:" + rAmt) ;

				Iterator<String> iter = m_System.keySet().iterator();
				while(iter.hasNext()){
					Person2 aPer = m_System.get(iter.next()) ;
					if (aPer.m_active != true) continue ;
					aPer.m_amount[idx] += rAmt ;
					m_System.put(aPer.m_name, aPer) ;
				}
			} else {
				float pAmt = 0 ;
				if ((sIn[j].indexOf(AMT_INDICATOR) != -1)) pAmt = eachAmt ;		// amount specified
				else pAmt = amtAll/numRest ;
				//if (idx == _TO) System.out.println("else pAmt: " + pAmt + ",eachAmt: " + eachAmt + ",numRest: " + numRest) ;

				try {
					Person2 aPer = m_Persons.get(eachName) ;
					aPer.m_amount[idx] += pAmt ;
					if ((idx == _TO) && (m_bClearing == false)) {
						aPer.m_amount[aPer.TRANS_AMT] += pAmt ;
						aPer.m_amount[aPer.IND_SUM] += pAmt ;
					}
					m_Persons.put(aPer.m_name, aPer) ;
				} catch (NullPointerException e) {
					////System.out.println("NullPointerException: " +  e.getMessage());
				}
			}
		}
	}


	// ----------------------------------------------------
	// 	initFromTo
	// ----------------------------------------------------
	private void initFromTo()
	{
		////System.out.println("initFromTo") ;
		Iterator<String> iter = m_Persons.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			aPer.m_amount[aPer.FROM] = aPer.m_amount[aPer.TO] = aPer.m_amount[aPer.TRANS_AMT]  = aPer.m_amount[aPer.CHK_SUM] = aPer.m_amount[aPer.CHK_INDSUM] = 0 ;
			m_Persons.put(aPer.m_name, aPer) ;
		}

		iter = m_System.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_System.get(iter.next()) ;
			aPer.m_amount[aPer.FROM] = aPer.m_amount[aPer.TO] = aPer.m_amount[aPer.TRANS_AMT] = aPer.m_amount[aPer.CHK_SUM] = aPer.m_amount[aPer.CHK_INDSUM] = 0 ;
			m_System.put(aPer.m_name, aPer) ;
		}
	}

	// ----------------------------------------------------
	// 	initPersons
	// ----------------------------------------------------
	private void initPersons()
	{
		m_Persons = new Hashtable<String, Person2>() ;
		m_nTotAmount = 0;		m_nSysToAmount = 0 ;

		m_System = new Hashtable<String, Person2>() ;
		Person2 aPerson = new Person2(SYS, true) ;
		m_System.put(SYS, aPerson) ;
		m_bSys = false ;
	}

	// ----------------------------------------------------
	// 	sumFromTo
	// ----------------------------------------------------
	private float sumFromTo(float amt, String action)
	{
		// sys account
		float sysAmount = 0 ;
		Iterator<String> iter = m_System.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_System.get(iter.next()) ;
			if (aPer.m_active == true) aPer.m_amount[aPer.SYS_SUM] += (aPer.m_amount[aPer.FROM] + ((-1)*aPer.m_amount[aPer.TO])) ;
			sysAmount = aPer.m_amount[aPer.SYS_SUM] ;		m_nSysToAmount += aPer.m_amount[aPer.TO] ;
			m_System.put(aPer.m_name, aPer) ;
		}

		// person account
		float nCheckSum = 0, nCheckIndSum = 0 ;
		iter = m_Persons.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			if (aPer.m_active == true) aPer.m_amount[aPer.SYS_SUM] += (aPer.m_amount[aPer.FROM] + ((-1)*aPer.m_amount[aPer.TO])) ;
			aPer.m_amount[aPer.FROM] = aPer.m_amount[aPer.TO] = 0 ;

			nCheckSum += aPer.m_amount[aPer.SYS_SUM] ;
			if (!action.endsWith(CLEARING)) nCheckIndSum += aPer.m_amount[aPer.IND_SUM] ;
		}
		iter = m_Persons.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			if (aPer.m_active == true) aPer.m_amount[aPer.CHK_SUM] = (nCheckSum + sysAmount /* adjust for sys account*/);
			m_Persons.put(aPer.m_name, aPer) ;
		}

		// individual checksum
		m_nTotAmount += amt ;
		iter = m_Persons.keySet().iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			if (!action.endsWith(CLEARING)) aPer.m_amount[aPer.CHK_INDSUM] = ((m_nTotAmount - m_nSysToAmount) /* this is adjusted for sys account */ - nCheckIndSum) ;
			m_Persons.put(aPer.m_name, aPer) ;
		}

		return nCheckSum ;
	}

	// ----------------------------------------------------
	// 	PrintSummary
	// ----------------------------------------------------
	private void PrintSummary()
	{
		float cs = 0 ;
		Set<String> set= m_Persons.keySet();
		Iterator<String> iter = set.iterator();
		////System.out.println("---------------------------------");
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			////System.out.println(aPer.m_name + "\t\t" + aPer.m_amount[aPer.SYS_SUM]);
			cs = aPer.m_amount[aPer.CHK_SUM] ;
		}
		////System.out.println("CheckSum" + "\t\t" + Math.round(cs));
		////System.out.println("---------------------------------");
	}

	private void PrintSummary2()
	{
		try {
			String	sTabs = TAB_SEPARATOR ;
			String	sPersons = "", sAmounts = "", sIndAmounts = "" ;

			// sys account
			Iterator<String> iter = m_System.keySet().iterator();
			while(iter.hasNext()){
				Person2 aPer = m_System.get(iter.next()) ;
				sPersons += sTabs + aPer.m_name ;
				sAmounts += sTabs + aPer.m_amount[aPer.SYS_SUM] ;
			}

			float cs = 0, indcs = 0 ;
			iter = m_Persons.keySet().iterator();
			////System.out.println("-------------- PrintSummary2 -------------------");
			while(iter.hasNext()){
				Person2 aPer = m_Persons.get(iter.next()) ;
				sPersons += sTabs + aPer.m_name ;
				sAmounts += sTabs + aPer.m_amount[aPer.SYS_SUM] ;
				sIndAmounts += sTabs + aPer.m_amount[aPer.IND_SUM] ;

				cs = aPer.m_amount[aPer.CHK_SUM] ;
				indcs = aPer.m_amount[aPer.CHK_INDSUM] ;
			}
			sPersons += sTabs + "CheckSum" ;
			sAmounts += sTabs + roundAmount(cs) ;

			////System.out.println("PrintSummary2::" + sPersons + sTabs + sAmounts + sTabs + sIndAmounts + sTabs + indcs);
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	private String roundAmount(float f)
	{
		try {
			int decimalPlace = 2 ;
			BigDecimal bd = new BigDecimal(f);
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
			return bd.toString() ;
		} catch (NumberFormatException e) {
			return e.getMessage() ;
		}
	}


	ArrayList<String> ArraySummary()
	{
		ArrayList<String> summary = null;
		summary = new ArrayList<String>() ;

		String	sSep = " : " ;
		String	sPersons = "", sAmounts = "", sIndAmounts = "" ;
		float cs = 0, indcs = 0 ;

		/*
		Set<String> set= m_Persons.keySet();
		Iterator<String> iter = set.iterator();
		while(iter.hasNext()){
			Person2 aPer = m_Persons.get(iter.next()) ;
			sPersons = aPer.m_name ;
			sAmounts = roundAmount(aPer.m_amount[aPer.SYS_SUM]) ;
			sIndAmounts = roundAmount(aPer.m_amount[aPer.IND_SUM]) ;
			cs = aPer.m_amount[aPer.CHK_SUM] ;
			summary.add(sPersons + sSep + sAmounts + " (" + sIndAmounts + ")") ;
		}
		summary.add("CheckSum"  + sSep + roundAmount(cs)); //Math.round(cs) ;
		return summary ;
		*/

		Iterator<String> iter ;
		if (m_bSys == true) {
			iter = m_System.keySet().iterator();
			while(iter.hasNext()){
				Person2 aPer = m_System.get(iter.next()) ;
				if (aPer.m_active == true) summary.add(aPer.m_name + sSep + aPer.m_amount[aPer.SYS_SUM]) ;
			}
		}

		/* sort persons, :get the iterator & sort it */
		List<String> mapKeys = new ArrayList<String>(m_Persons.keySet());
		Collections.sort(mapKeys);
		iter = mapKeys.iterator();
		while (iter.hasNext()) {
			Person2 aPer = m_Persons.get(iter.next()) ;
			sPersons = aPer.m_name ;
			sAmounts = roundAmount(aPer.m_amount[aPer.SYS_SUM]) ;
			sIndAmounts = roundAmount(aPer.m_amount[aPer.IND_SUM]) ;
			//System.out.println("sPersons:: " + sPersons + ", sAmounts::" + sAmounts + ", sIndAmounts:: " + sIndAmounts);
			cs = aPer.m_amount[aPer.CHK_SUM] ;
			indcs = aPer.m_amount[aPer.CHK_INDSUM] ;
			summary.add(sPersons + sSep + sAmounts + " (" + sIndAmounts + ")") ;
		}
		if ((cs != 0) || (indcs != 0)) summary.add("CheckSum"  + sSep + roundAmount(cs) + " (" +  roundAmount(indcs) + ")") ;
		return summary ;
	}

	// ----------------------------------------------------
	// ProcessTransaction
	// ----------------------------------------------------
	private void ProcessTransaction(String item, String desc, String amt, String from, String to, String action, String def)
	{
		try {
			m_bClearing = false ;

			doAction(action) ;
			dumpCollection() ;

			// process Action
			if (action.length() != 0) {
				StringTokenizer st = new StringTokenizer(action, ITEM_SEPARATOR);
				String sActs = "", sAct = "";
				while (st.hasMoreTokens()) {
					sActs = st.nextToken() ;
					if (sActs.endsWith(ADD_ITEM)) {	// add
						sAct = sActs.substring(0, sActs.indexOf('*')).trim() ;
						if (m_Persons.containsKey(sAct)) {
							// Error: already exists
						} else {
							Person2 aPerson = new Person2(sAct.trim(), true) ;
							m_Persons.put(sAct, aPerson) ;
						}
					} else if (sActs.endsWith(ENABLE_ITEM) || sActs.endsWith(DISABLE_ITEM)) {
						sAct = sActs.substring(0, sActs.length()-1).trim() ;
						if (m_Persons.containsKey(sAct)) {
							Person2 aPer  = m_Persons.get(sAct) ;
							aPer.m_active = !aPer.m_active ;	// toggle, the correct way to do this would be to check if exists & enable/disable
							m_Persons.put(sAct, aPer) ;
						} else {
							// Error: does not exist
						}
					} else if (sActs.endsWith(CLEARING)) {	// pay between individuals
						m_bClearing = true ;
					}
				}
				//dumpPersons() ;
			} // valid action

			float xAmt = 0 ;
			try {
				xAmt = Float.parseFloat(amt) ;
			} catch (NumberFormatException e) {
			}
			initFromTo() ;

			String aFrom = percentageToAmounts(xAmt, from, action) ;
			String aTo = percentageToAmounts(xAmt, to, action) ;

			doFromTo(_FR, xAmt, aFrom) ;
			doFromTo(_TO, xAmt, aTo) ;
			sumFromTo(xAmt, action) ;

			PrintSummary2() ;
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}


	// ----------------------------------------------------
	// prepareToExport
	// ----------------------------------------------------
	private void prepareToExport(String item, String category, String vendor, String desc, String amt, String from, String to, String action, String def)
	{
		String	sTabs = TAB_SEPARATOR ;
		String aLine ;
		aLine = item + TAB_SEPARATOR + category + TAB_SEPARATOR + vendor + TAB_SEPARATOR + desc + TAB_SEPARATOR + amt + TAB_SEPARATOR + from + TAB_SEPARATOR + to + TAB_SEPARATOR + action ;
		////System.out.println("aLine = " + aLine);

		Iterator<String> iter ;
		// sys account
		String	sSysAmt = "" ;
		if (m_bSys == true) {
			iter = m_System.keySet().iterator();
			while(iter.hasNext()) {
				Person2 aPer = m_System.get(iter.next()) ;
				sSysAmt += lBr + aPer.m_name + AMT_INDICATOR + roundAmount(aPer.m_amount[aPer.SYS_SUM]) + rBr ;
			}
			sSysAmt = lPAD + sSysAmt + rPAD + TAB_SEPARATOR;
		}
		//sSysAmt = lPAD + sSysAmt + rPAD  + TAB_SEPARATOR ;
		////System.out.println("sSysAmt = " + sSysAmt);

		// person account
		String	sPerAmt = "", sIndAmt = "", sTransAmt = "" ;
		float cs = 0, indcs = 0 ;
		/* sort persons, :get the iterator & sort it */
		List<String> mapKeys = new ArrayList<String>(m_Persons.keySet());
		Collections.sort(mapKeys);
	    iter = mapKeys.iterator();
		while (iter.hasNext()) {
			Person2 aPer = m_Persons.get(iter.next()) ;
			sTransAmt += ITEM_SEPARATOR + lBr + aPer.m_name + AMT_INDICATOR + roundAmount(aPer.m_amount[aPer.TRANS_AMT]) + rBr ;
			sPerAmt += ITEM_SEPARATOR + lBr + aPer.m_name + AMT_INDICATOR + roundAmount(aPer.m_amount[aPer.SYS_SUM]) + rBr ;
			sIndAmt+= ITEM_SEPARATOR + lBr + aPer.m_name + AMT_INDICATOR + roundAmount(aPer.m_amount[aPer.IND_SUM]) + rBr ;
			cs = aPer.m_amount[aPer.CHK_SUM] ;
			indcs = aPer.m_amount[aPer.CHK_INDSUM] ;
		}

		sTransAmt = lPAD + sTransAmt.substring(ITEM_SEPARATOR.length(), sTransAmt.length()) + rPAD ;
		sPerAmt = lPAD + sPerAmt.substring(ITEM_SEPARATOR.length(), sPerAmt.length()) + rPAD ;
		sIndAmt = lPAD + sIndAmt.substring(ITEM_SEPARATOR.length(), sIndAmt.length()) + rPAD ;

		aLine += TAB_SEPARATOR + sTransAmt + TAB_SEPARATOR + sSysAmt /*+ TAB_SEPARATOR*/ + sPerAmt + roundAmount(cs)+ TAB_SEPARATOR + sIndAmt + roundAmount(indcs) ;

		if (m_exportLines == null) {
			m_exportLines = new ArrayList<String>() ;
			m_exportLines.add(aLine) ;
		}
		else
			m_exportLines.add(aLine) ;
	}

	private String padAmountString(String sNames[], String embededPers, String unpaddedLine)
	{
		try {
			//System.out.println("sNames: " + sNames + ", embededPers: " + embededPers + ", unpaddedLine: " + unpaddedLine);

			for (int i = 0; i < sNames.length; i++) {
				int fPos = embededPers.indexOf(sNames[i]) ;
				//System.out.println("sNames[i]: " + sNames[i]);
				//System.out.println("fPos: " + fPos);
				if (fPos == -1) /* not found */ {
					unpaddedLine = unpaddedLine.replaceFirst(sNames[i], "") ;
					//System.out.println("unpaddedLine: " + unpaddedLine);
				} else {		/* found */
					int tPos = embededPers.indexOf(rBr, fPos) ;
					//System.out.println("tPos: " + tPos);
					String sAmt = embededPers.substring(fPos + sNames[i].length() + AMT_INDICATOR.length(), tPos) ;
					//System.out.println("sAmt: " + sAmt);
					unpaddedLine = unpaddedLine.replaceFirst(sNames[i], sAmt) ;
					//System.out.println("unpaddedLine: " + unpaddedLine);
				}
			}
			return unpaddedLine ;
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
			return unpaddedLine ;
		}
	}

	private String makeOutFilename(String fileName)
	{
		String outFilename = "" ;
		int fileExt = fileName.lastIndexOf(OUT_FILESEP) ;
		if (fileExt == -1) // not found
			outFilename += OUT_EXTENSION ;
		else
			outFilename = fileName.substring(0, fileExt) + OUT_FILE + OUT_EXTENSION ;
			//outFilename = fileName.substring(0, fileExt) + OUT_FILE + fileName.substring(fileExt, fileName.length()) ;

		return outFilename ;
	}

	private String padHeader(int nTabs)
	{
		String xHeader = "" ;
		while (nTabs > 0) {
				xHeader += TAB_SEPARATOR ;
				nTabs-- ;
		}
		return xHeader ;
	}

	private void exportToCSV(String fileName)
	{
		String outFilename = makeOutFilename(fileName) ;
		////System.out.println("outFilename: " + outFilename);

		try {
			// Create file
			FileWriter fstream = new FileWriter(outFilename);
			BufferedWriter out = new BufferedWriter(fstream);

			// begin Header {
			String	sTabs = "" ; //TAB_SEPARATOR ;
			String	sPersons = "" ;

			/* sort persons, :get the iterator & sort it */
			List<String> mapKeys = new ArrayList<String>(m_Persons.keySet());
			Collections.sort(mapKeys);
			Iterator<String> iter = mapKeys.iterator();
			while (iter.hasNext()) {
				Person2 aPer = m_Persons.get(iter.next()) ;
				sPersons += sTabs + aPer.m_name ;
				sTabs = TAB_SEPARATOR ;
			}

			String sHeader0, sHeader01, sHeader02, sHeader03, sHeader ;
			sHeader01 = H_TRANSACTION_AMOUNTS + padHeader(m_Persons.size()) ;
			if (m_bSys == true)
				sHeader02 = H_OWE + padHeader(m_Persons.size() + 1 + 1 /*CheckSum*/) ;
			else
				sHeader02 = H_OWE + padHeader(m_Persons.size() + 1 /*CheckSum*/) ;
			sHeader03 = H_INDIVIDUAL_TOTALS + padHeader(m_Persons.size() - 1 + 1 /*IndCheckSum*/) ;
			sHeader0 = padHeader(2 /* v15 */ + 6) + sHeader01 + sHeader02 + sHeader03 ;
			sHeader = H_ITEM + TAB_SEPARATOR + H_CATEGORY + TAB_SEPARATOR + H_VENDOR + TAB_SEPARATOR + H_DESCRIPTION + TAB_SEPARATOR + H_AMOUNT + TAB_SEPARATOR + H_FROM + TAB_SEPARATOR + H_TO + TAB_SEPARATOR + H_ACTION ;

			if (m_bSys == true)
				sHeader += TAB_SEPARATOR  + sPersons + TAB_SEPARATOR + SYS  + TAB_SEPARATOR + sPersons + TAB_SEPARATOR + H_CHECKSUM + TAB_SEPARATOR + sPersons + TAB_SEPARATOR + H_INDCHECKSUM ;
			else
				sHeader += TAB_SEPARATOR /*SYS + TAB_SEPARATOR*/ + sPersons + TAB_SEPARATOR + sPersons + TAB_SEPARATOR + H_CHECKSUM + TAB_SEPARATOR + sPersons + TAB_SEPARATOR + H_INDCHECKSUM ;
			out.write(sHeader0);	out.newLine();
			out.write(sHeader);		out.newLine();
			// } end Header

			sPersons += TAB_SEPARATOR ;
			//System.out.println("sPersons: " + sPersons);
			for (String aLine : m_exportLines) {
				//String unpaddedLine = sPersons.substring(TAB_SEPARATOR.length(), sPersons.length()) ;
				String unpaddedLine = sPersons.substring(0, sPersons.length()) ;
				//System.out.println("unpaddedLine: " + unpaddedLine);

				String sNames[] = unpaddedLine.split(TAB_SEPARATOR) ;

				//System.out.println("aLine = " + aLine);
				//out.write(aLine);		out.newLine();
				String debugLine ;

				// trans amount
				int fx = aLine.indexOf(lPAD) + lPAD.length() ;
				int tx = aLine.indexOf(rPAD, fx) ;
				String embededPersonsx = aLine.substring(fx, tx).trim() ;
				String sNewLinex = padAmountString(sNames, embededPersonsx, unpaddedLine) ;
				debugLine = "fx = " + fx + ", tx = " + tx + ", embededPersonsx = " + embededPersonsx + ", sNewLinex = [" + sNewLinex + "]" ;
				//out.write(debugLine);		out.newLine();

				// sys amount
				int f0 = 0, t0 = 0 ;
				String sNewLine0 = "" ;
				if (m_bSys == true) {
					f0 = aLine.indexOf(lPAD, tx + rPAD.length()) + lPAD.length() ;
					t0 = aLine.indexOf(rPAD, f0) ;
					String embededPersons0 = aLine.substring(f0, t0).trim() ;
					String[] sysAC = new String[] { SYS };
					sNewLine0 = padAmountString(sysAC, embededPersons0, SYS) ;
					debugLine = "f0 = " + f0 + ", t0 = " + t0 + ", embededPersons0 = " + embededPersons0 + ", sNewLine0 = [" + sNewLine0 + "]" ;
					//out.write(debugLine);		out.newLine();
				} else {
					f0 = fx ;
					t0 = tx ;
				}

				// person amounts
				int f1 = aLine.indexOf(lPAD, t0 + rPAD.length()) + lPAD.length() ;
				int t1 = aLine.indexOf(rPAD, f1) ;
				String embededPersons1 = aLine.substring(f1, t1).trim() ;
				String sNewLine1 = padAmountString(sNames, embededPersons1, unpaddedLine) ;
				debugLine = "f1 = " + f1 + ", t1 = " + t1 + ", embededPersons1 = " + embededPersons1 + ", sNewLine1 = [" + sNewLine1 + "]" ;
				//System.out.println("debugLine: " + debugLine);
				//out.write(debugLine);		out.newLine();

				// individual amounts
				int f2 = aLine.indexOf(lPAD, t1 + rPAD.length()) + lPAD.length() ;
				int t2 = aLine.indexOf(rPAD, f2) ;
				String embededPersons2 = aLine.substring(f2, t2).trim() ;
				String sNewLine2 = padAmountString(sNames, embededPersons2, unpaddedLine) ;
				debugLine = "f2 = " + f2 + ", t2 = " + t2 + ", embededPersons2 = " + embededPersons2 + ", sNewLine2 = [" + sNewLine2 + "]" ;
				//out.write(debugLine);		out.newLine();
				//System.out.println("debugLine: " + debugLine);

				//checksum
				int f11 = t1 + rPAD.length() ;
				int t11 = aLine.indexOf(lPAD, f11) ;
				debugLine = "f11 = " + f11 + ", t11 = " + t11;
				String checkSum = aLine.substring(f11, t11).trim() ;
				//System.out.println("checkAum: " + debugLine + ":" + checkSum);

				//indchecksum
				int f21 = t1 + rPAD.length() ;
				int t21 = aLine.indexOf(lPAD, f21) ;
				debugLine = "f21 = " + f21 + ", t21 = " + t21;
				String indcheckSum = aLine.substring(f21, t21).trim() ;
				//System.out.println("indcheckSum: " + debugLine + ":" + indcheckSum);

				////System.out.println("here.0");
				////System.out.println("substring(0, fx - lPAD.length()," + fx + ", " + lPAD.length() + aLine.substring(0, fx - lPAD.length()));
				//int i1 = tx + rPAD.length() ;
				//int i2 = f0 - lPAD.length() ;
				////System.out.println("aLine.substring(tx + rPAD.length(), f0 - lPAD.length()," + i1 + "," + i2 /*+ ", " + aLine.substring(tx + rPAD.length(), f0 - lPAD.length())*/);

				String sToFile = "" ;
				String outLine = "" ;
				/*
				out.write("0: newLines:");		out.newLine();
				out.write(sNewLinex);		out.newLine();
				if (m_bSys == true) {out.write(sNewLine0);		out.newLine();}
				out.write(sNewLine1);		out.newLine();
				out.write(sNewLine2);		out.newLine();
				*/

				outLine += aLine.substring(0, fx - lPAD.length()) + sNewLinex ;
				if (m_bSys == true) outLine += sNewLine0  + TAB_SEPARATOR ;
				outLine += sNewLine1 + checkSum + TAB_SEPARATOR;
				outLine += sNewLine2 + indcheckSum ;
				sToFile = outLine ;

				/*
				//sToFile += aLine.substring(0, fx - lPAD.length()) + sNewLinex + aLine.substring(tx + rPAD.length(), f0 - lPAD.length()) ;
				sToFile += aLine.substring(0, fx - lPAD.length()) + sNewLinex + aLine.substring(f0 - lPAD.length(), tx + rPAD.length()) ;
				debugLine = aLine.substring(0, fx - lPAD.length()) ;
				out.write("1:" + debugLine);		out.newLine();
				int x = f0 - lPAD.length() ; int y = tx + rPAD.length() ;
				out.write("1.1:" + x + "," + y);		out.newLine();
				debugLine = aLine.substring(f0 - lPAD.length(), tx + rPAD.length()) ;
				out.write("2:" + debugLine);		out.newLine();
				if (m_bSys == true) { sToFile += sNewLine0 + aLine.substring(t0 + rPAD.length(), f1 - lPAD.length()) ;
					////System.out.println("sToFile1 = [" + sToFile + "]");
					//out.write(sToFile);		out.newLine();
				}

				////System.out.println("here.1");
				sToFile += sNewLine1 + aLine.substring(t1 + rPAD.length(), f2 - lPAD.length()) ;
				////System.out.println("sToFile2 = [" + sToFile + "]");
				out.write(sToFile);		out.newLine();

				sToFile += sNewLine2 + aLine.substring(t2 + rPAD.length(), aLine.length()) ;
				////System.out.println("sToFile3 = [" + sToFile + "]");
				//out.write(sToFile);		out.newLine();
				*/

				out.write(sToFile);		out.newLine();
			}
			out.close();		//Close the output stream
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}


	// dump collection
	private void dumpCollection()
	{
		System.out.println("--------------------------------------");
		Enumeration<String> keysGroup = m_GroupCollection.keys();
		while(keysGroup.hasMoreElements()){
			String groupName = keysGroup.nextElement();
			Hashtable<String, Person2> aGroup = m_GroupCollection.get(groupName) ;
			System.out.println("group: " + groupName);

			Enumeration<String> keysPeople = aGroup.keys();
			while(keysPeople.hasMoreElements()){
				/* two step get
				String key = keysPeople.nextElement();
				Person2 person = (Person2)aGroup.get(key);*/
				// single step, get
				Person2 person = aGroup.get(keysPeople.nextElement());
				System.out.println("person: " + person.m_name + ":" + person.m_active);
				//System.out.println("Value of "+key+" is: "+aGroup.get(key));
			}
		}
		System.out.println("--------------------------------------");
	}


	// getPersons
	Hashtable<String, Person2> getPersons(String sGrpName)
	{
		// find group
		try {
			Hashtable<String, Person2> persons = m_GroupCollection.get(sGrpName) ;
			if (persons != null) {
			} else {
				// not found, error !
			}
			return persons ;
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
			return null ;
		}
	}

	// Find_CreateGroup
	Hashtable<String, Person2> Find_CreateGroup(String sGrpName)
	{
		// find group
		try {
			Hashtable<String, Person2> aGrp = m_GroupCollection.get(sGrpName) ;
			if (aGrp == null) {
				aGrp = new Hashtable<String, Person2>() ;
				m_GroupCollection.put(sGrpName, aGrp) ;
			} else {
				// found, do nothing
			}
			return aGrp ;
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
			return null ;
		}
	}

	// getAction: get specific action
	String getAction(int lR, String sA)
	{
		String sAct = "" ;
		// get action
		int idS = 0 ;
		if ( ((idS = sA.indexOf(ID_SEPARATOR)) != -1) )
			sAct = sA.substring(lR+1, idS).trim() ;
		else
			System.err.println("Action not specified: " + sA);

		return sAct ;
	}

	// doAction: process input action
	// name1 (*/+/-:self), name2 (*/+/-:self): add/enable/disable individuals
	// group1 (*/+/-:group): add/enable/disable group
	private void doAction(String action)
	{
		//System.out.println("action: " + action);

		boolean bGroup = false, bInd = false ;
		ArrayList<String> grpActions = null, indActions = null ;

		// process Action
		if (action.length() != 0) {
			StringTokenizer st = new StringTokenizer(action, ITEM_SEPARATOR);
			String sActs = "", sIndAct = ADD_ITEM, sGrpAct = ADD_ITEM;
			while (st.hasMoreTokens()) {
				sActs = st.nextToken() ;

				int lR = 0, rR = 0 ;
				String aName = "", aGroup = DEFAULT_GROUP ;
				if ( ((lR = sActs.indexOf(ID_lR)) != -1) && ((rR = sActs.indexOf(ID_rR)) != -1) ) {	// valid construct
						// get name: self or group
						if ( (bInd = sActs.contains(SELF)) ) {
							aName = sActs.substring(0, lR).trim() ;
							sIndAct = getAction(lR, sActs) ;
						}
						else if ( (bGroup = sActs.contains(GROUP)) ) {
							aGroup = sActs.substring(0, lR).trim() ;
							sGrpAct = getAction(lR, sActs) ;
						}
						else
							; //System.err.println("Individual or Group not specified: " + action);
				}

				if (bGroup) {
					if (grpActions == null) {
						grpActions = new ArrayList<String>() ;
						grpActions.add(aGroup + ID_SEPARATOR + sGrpAct) ;
					} else
						grpActions.add(aGroup + ID_SEPARATOR + sGrpAct) ;
				}

				if (bInd) {
					if (indActions == null) {
						indActions = new ArrayList<String>() ;
						indActions.add(aName + ID_SEPARATOR + sIndAct) ;
					} else
						indActions.add(aName + ID_SEPARATOR + sIndAct) ;
				}
			} // while

			// Create Collections
			if (m_GroupCollection == null) m_GroupCollection = new Hashtable<String, Hashtable<String, Person2>>() ;

			String sGrpName = DEFAULT_GROUP ;
			if (grpActions != null) {
				for (String aAction : grpActions) {
					int idS = -1 ;
					if ( ((idS = aAction.indexOf(ID_SEPARATOR)) != -1) ) {
						sGrpName = aAction.substring(0, idS).trim() ;
						sGrpAct = aAction.substring(idS+1, aAction.length()).trim() ;
						Find_CreateGroup(sGrpName) ;
					}
				}
			}

			if (indActions != null) {
				for (String aAction : indActions) {
					int idS = -1 ;
					if ( ((idS = aAction.indexOf(ID_SEPARATOR)) != -1) ) {
						String sIndName = aAction.substring(0, idS).trim() ;
						sIndAct = aAction.substring(idS+1, aAction.length()).trim() ;

						try {
							Hashtable<String, Person2> aGrp = Find_CreateGroup(sGrpName) ;

							Person2 aPn = aGrp.get(sIndName);
							if (aPn != null) { // found, flip enable/disable
								//System.out.println("SEARCH: " + sIndName + " ,FOUND: " + aPn.m_name + ":" + aPn.m_active + ": flip active");
								if (sIndAct.compareToIgnoreCase(DISABLE_ITEM) == 0) {
									aPn.m_active = false ;
								} else if (sIndAct.compareToIgnoreCase(ENABLE_ITEM) == 0) {
									aPn.m_active = true ;
								}
								aGrp.put(sIndName, aPn) ;
							} else { // not found, add
								//System.out.println("SEARCH: " + sIndName + ": NOT found, add");
								if (sIndAct.compareToIgnoreCase(ADD_ITEM) == 0) {
									Person2 aPerson = new Person2(sIndName.trim(), true) ;
									aGrp.put(sIndName, aPerson) ;
								}
							}
						} catch (Exception e){
							System.err.println("Error:doAction " + e.getMessage());
						}
					}
				}
			}
		} // action
	}


	// ----------------------------------------------------
	// ReadAndProcessTransactions
	// ----------------------------------------------------
	public void ReadAndProcessTransactions(String fileName, boolean bExport)
	{
        // open logfile
        FileReader fileReader = null;
		try {
			fileReader = new FileReader(getFile(fileName));
			//read file
			BufferedReader buffReader = new BufferedReader(fileReader);
			String sLine = "";

			initPersons() ;
			if (bExport) m_exportLines = null ;

			try {
				while ((sLine = buffReader.readLine()) != null) {
					// stream the input, one line at a time
					StringTokenizer st = new StringTokenizer(sLine, READ_SEPARATOR);
					int pos = 0 ;
					String item="", category="", vendor="", desc="",amt="", from="", to="", action="", def="" ;
					while (st.hasMoreTokens()) {
						switch (pos) {
							case P_ITEM:
								item = st.nextToken() ;
								break ;
							case P_CATEGORY:
								category = st.nextToken() ;
								break ;
							case P_VENDOR:
								vendor = st.nextToken() ;
								break ;
							case P_DESC:
								desc = st.nextToken() ;
								break ;
							case P_AMOUNT:
								amt = st.nextToken() ;
								break ;
							case P_FROM:
								from = st.nextToken() ;
								break ;
							case P_TO:
								to = st.nextToken() ;
								break ;
							case P_ACTION:
								action = st.nextToken() ;
								break ;
							default:
								def = def + st.nextToken() ;
						}
						pos++ ;
					}
					if (sLine.length() == 0) continue ;
					if (item.charAt(0) == COMMENT) continue ; // comment, skip

					ProcessTransaction(item, desc, amt, from, to, action, def) ;
					prepareToExport(item, category, vendor, desc, amt, from, to, action, def) ;
				} // end of while
				buffReader.close() ;
				////System.out.println("map: " + m_Transactions.toString()); // dump HashMap

				if (bExport) exportToCSV(fileName) ;
			} catch (IOException e) {
				////System.out.println("There was a problem reading:" + fileName);
			}
		} catch (FileNotFoundException e) {
			////System.out.println("Could not locate a file: " + e.getMessage());
		}
	}

} // end of class
