//package com.mycompany.GPExplorer;

public class Person2 extends Object {

	// enum
    public final int FROM = 0;
	public final int TO = 1;
	public final int SYS_SUM = 2;
	public final int IND_SUM = 3;
	public final int CHK_SUM = 4;
	public final int CHK_INDSUM = 5;
	public final int TRANS_AMT = 6;

    private final int eSTART = FROM;
    private final int eEND = TRANS_AMT;
    private final int eSIZE = eEND+1;

	// members
	String	m_name ;
	float[] 	m_amount = new float[eSIZE];
	boolean	m_active ;

	// methods
	private void initAmounts() {
		for (int i = eSTART; i < m_amount.length; i++) {
			m_amount[i] = 0;
		}
    }

	public Person2() {
		m_name = "" ;
		initAmounts() ;
		m_active = false ;
	}

	public Person2(String name, boolean active) {
		m_name = name ;
		initAmounts() ;
		m_active = active ;
	}

	public Person2(String name, float amount, boolean active) {
		m_name = name ;
		m_amount[eSTART] = amount ;
		m_active = active ;
	}

	public String toString() {
		String sAmts = "" ;
		for (int i = eSTART; i < m_amount.length; i++) {
			sAmts += m_amount[i] + ",";
		}
		//return "m_name: " + m_name + ", m_amount: " + sAmts + ", m_active: " + m_active ;
		return m_name + "," + sAmts + m_active ;
	}
}
