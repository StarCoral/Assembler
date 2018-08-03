package assembler;
import java.io.*;
import java.util.*;


public class Assembler
{
	Scanner sc = new Scanner(System.in);
	String nameOfSrc,nameOfObj,inputStr;
	public static int rows = 0;		
	final static int maxline = 10000;
	public static int LTORGPosion;
	public static String[][] instruction = new String [maxline][3];
	public static Assembler  as = new Assembler();
	public static String[] Literal = new String [maxline];
	public static String LOC[] = new String[maxline];
	public static String PC[] = new String[maxline];	//Program Counter
	public static String objectCode[] = new String[maxline];
	public static int judgeSIC=0;
	public static String BASE;
	public static int isLabel = 0;
	
	/*****************主程式******************/
	public static void main(String[] args)throws IOException,NumberFormatException,NullPointerException
	{	
		as.Read();
		as.Pass1();
		as.judgeSIC();
		as.formatPC();
		as.Pass2();
		as.Write();
	}
	
	/*****************檔案讀取******************/
	void Read() throws IOException
	{
		System.out.print("Please enter the file name(name.txt): ");
		nameOfSrc = sc.next();
		
		try
		{
			FileReader fileReader = new FileReader(nameOfSrc);
			BufferedReader	bufferedReader = new BufferedReader(fileReader);
			
			while( (inputStr = bufferedReader.readLine()) != null)	//每次讀一行,直到檔案結束
			{
				int index1 = 0,index2 = 0;	//標記: 分隔符
					
				if( inputStr.indexOf(".") >= 0 )	//處理: 注解
				{
					index2 = inputStr.indexOf(".",index1);
					String temp = inputStr.substring(index2+1,inputStr.length());
					
					instruction[rows][0] = ".";
					instruction[rows][1] = temp;
					instruction[rows][2] = " ";
				}	
				else if( inputStr.equals("") )	//跳過無指令行
					continue;
				else
				{
					inputStr = inputStr.replaceAll("\\s+","|");	//取代: 空白 -> |
					/*********[0] : Label *********/
					index2 = inputStr.indexOf("|",index1);
					
					if(index2 == 0 )	//若無Symbol: 將空白讀入
						instruction[rows][0]=" ";	
					
					else
						instruction[rows][0]=inputStr.substring(index1,index2);
					
					/*********[1] : Mnemonic *********/
					index1 = index2+1;
					index2 = inputStr.indexOf("|",index1);
					
					instruction[rows][1] = inputStr.substring(index1,index2);	//每行指令皆有mnemonic,故不做判斷有無
					
					/*********[2] : Operand *********/
					index1 = index2+1;
					index2 = inputStr.indexOf("|",index1);
					
					if( index2 == inputStr.length() || index1 == inputStr.length() )	//若無operand
						instruction[rows][2] = " ";
					else	
						instruction[rows][2] = inputStr.substring(index1,index2);	
				}
				
				/*********Literal : LTORG*********/	
				if( instruction[rows][1].equals("LTORG") )
				{
					 LTORGPosion = rows;	//紀錄: LTORG所在行數
					
					 for(int i=0 ; i <rows ; i++)	//紀錄: LTORG前所使用的literal
					{
						if( instruction[i][2].indexOf("=") == 0 )
						{	
							rows++;
							instruction[rows][1] = instruction[i][2];
							instruction[rows][0] = "*";
							instruction[rows][2] = " ";
						}	
					}
				}
				
				/*********Literal : END*********/
				if(instruction[rows][1].equals("END"))
				{
					rows++;
					int count=0,k=0;
					
					for(int i=LTORGPosion ; i<rows ; i++)	//將literal存取至Literal陣列中
					{
						if(instruction[i][2].indexOf("=")==0)
						{
							count++;	//計算literal的個數
							for(int j=0 ; j<count ; j++)
							{
								if(instruction[i][2].equals(Literal[j]))	//避免重複存取
								{	
									count--;
									break;
								}	
								else
									Literal[k++] = instruction[i][2];
							}
						}
					}
					
					for(int j=0; j<count ; j++)	//將literal存放在指令列	
					{
						instruction[rows][0] = "*";
						instruction[rows][1] = Literal[j];
						instruction[rows][2] = " ";
						rows++;
					}
					break;
				}
				
				rows++;	//換下一行指令
			}
			
			bufferedReader.close();
		}
		catch(FileNotFoundException e)	//例外處理: 若無此檔案
		{
			System.out.println(nameOfSrc+" doesn't exist.");
			System.exit(0);
		}
	}
	
	/*****************檔案寫出******************/
	void Write() throws IOException
	{
		System.out.print("Please enter the object program name(name.txt):");
		nameOfObj = sc.next();
		
		FileWriter fileWriter = new FileWriter(nameOfObj);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		
		bufferedWriter.write("Line\tLOC\tSource statement\t\t\tObject code");
		bufferedWriter.newLine();
		
		for(int i=0 ; i<rows ; i++)
		{
			bufferedWriter.write(Integer.toString(i));	//Line
			bufferedWriter.write("\t");
			bufferedWriter.write(LOC[i].toUpperCase()); //LOC
			bufferedWriter.write("\t");
			bufferedWriter.write(instruction[i][0]);	//Symbol
			bufferedWriter.write("\t");
			bufferedWriter.write(instruction[i][1]);	//Mnemonic
			bufferedWriter.write("\t");
			bufferedWriter.write(instruction[i][2]);	//Operand
			if(instruction[i][2].equals("BUFFER,X"))
				bufferedWriter.write("\t\t");
			else
				bufferedWriter.write("\t\t\t");
			bufferedWriter.write(objectCode[i].toUpperCase());	//OBJ
			bufferedWriter.newLine();
			
		}
		
		bufferedWriter.flush();
		bufferedWriter.close();
	}	
	
	/*****************Pass 1******************/
	void Pass1()
	{	
		for(int i=0 ; i<rows ; i++)
		{	
			/******判斷: 第一行指令是否為START******/
			if( instruction[i][1].equals("START") )	
			{
				LOC[i] = instruction[i][2];
				PC[i]  = instruction[i][2];
				LOC[i+1] = PC[i];
			}	
			
			/******判斷: 此行是否為注解******/
			if(instruction[i][0].equals("."))
			{
				PC[i] = LOC[i];
				LOC[i] = " ";
			}
			
			/******[1] assembler directive : 無Object code; 有LOC******/
			if( ( AsseDirtable.adjudge(instruction[i][1]) ) < 7 && ( AsseDirtable.adjudge(instruction[i][1]) ) > 0 )	
			{	
				objectCode[i] = " ";	//無ObjectCode
				if( ( AsseDirtable.adjudge(instruction[i][1]) ) == 2 )	//RESW
				{
					int temp = Integer.parseInt(LOC[i],16) + (Integer.parseInt(instruction[i][2])*3);
					PC[i] = Integer.toHexString(temp);
				}
				
				if( ( AsseDirtable.adjudge(instruction[i][1]) )==3 )	//RESB
				{
					int temp = Integer.parseInt(LOC[i],16) + Integer.parseInt(instruction[i][2]);
					PC[i] = Integer.toHexString(temp);
				} 
				
				if(( AsseDirtable.adjudge(instruction[i][1]) )== 4)	//EQU
				{	
					PC[i] = LOC[i];	
				}
				
				LOC[i+1] = PC[i];	
			}
			/******[2] assembler directive : 無Object code; 無LOC******/
			else if( AsseDirtable.adjudge(instruction[i][1]) > 7 )
			{	
				LOC[i+1] = LOC[i]; 
				LOC[i] = " ";	
				objectCode[i] = " ";
			}
			/******[3] 非assembler directive : 有Object code; 有LOC******/
			else
			{					
				/*************處理Literal**************/
				if( instruction[i][1].indexOf("=") == 0 )
				{
					PC[i] = Integer.toHexString(Integer.parseInt(LOC[i],16) + 3);
					LiteralPoll.insert((instruction[i][1]),LOC[i]);
				}
				/*************處理opcode**************/
				if( instruction[i][1].indexOf("+") == 0 )	//Format4
				{	
					PC[i] = Integer.toHexString(Integer.parseInt(LOC[i],16) + 4);			
				}	
				if( Optable.search(instruction[i][1]) )
				{	
					String p = Optable.position(instruction[i][1]);
					
					if(p.equals("90") || p.equals("B4") || p.equals("A0") || p.equals("9C") || p.equals("98") || p.equals("AC") 
					|| p.equals("A4")  || p.equals("A8") || p.equals("94") || p.equals("B0") || p.equals("B8"))
						PC[i] = Integer.toHexString(Integer.parseInt(LOC[i],16) + 2);	//Format2
					
					else if(p.equals("C4") || p.equals("C0") || p.equals("F4") || p.equals("C8") || 
					p.equals("F0") || p.equals("F8") || (p.equals("_") && !(instruction[i][0]).equals("EOF")))
						PC[i] = Integer.toHexString(Integer.parseInt(LOC[i],16) + 1);	//Format1
						
					else
						PC[i] = Integer.toHexString(Integer.parseInt(LOC[i],16) + 3);	//Format3
				}
				LOC[i+1] = PC[i];
			}
			/******格式化: LOC******/
			if(!LOC[i].equals(" "))
			{
				LOC[i] = String.format("%4.4s",LOC[i]);	
				LOC[i] = LOC[i].replaceAll(" ","0");
			}
			
			/******處理Symbol : Label欄位******/
			if( Symtable.search(instruction[i][0]) && !(instruction[i][0].equals(" ")) && !(instruction[i][0].equals("*")) && !(instruction[i][0].equals(".")) )
			{
				System.out.println("Error: "+instruction[i][0]+ " is existed.");
				System.exit(0);
			}
			else
			{
				Symtable.insert(instruction[i][0],LOC[i]);
			}	
			/******處理Symbol : Mnemonic欄位******/
			if(instruction[i][1].indexOf("=") ==0 && Symtable.search(instruction[i][1]))
				Symtable.insert(instruction[i][0],LOC[i]);
		}
	}
	
	/*****************Judge: SIC******************/
	void judgeSIC()
	{
		for(int i=0; i<rows ; i++)
		{
			if((instruction[i][1]).indexOf("+") == 0 || (instruction[i][1]).indexOf("=") == 0 )
				judgeSIC++;
			if(Optable.search(instruction[i][1]))
			{	
				String p = Optable.position(instruction[i][1]);
					
				if(p.equals("90") || p.equals("B4") || p.equals("A0") || p.equals("9C") || p.equals("98") 
				|| p.equals("AC") || p.equals("A8") || p.equals("94") || p.equals("B0") || p.equals("B8")
				|| p.equals("C4") || p.equals("C0") || p.equals("F4") || p.equals("C8") || p.equals("F0") 
				|| p.equals("F8") )
					judgeSIC++;	//Format2,Format1
			}
			if((AsseDirtable.adjudge(instruction[i][1]))>0)
			{
				int a = (AsseDirtable.adjudge(instruction[i][1]));
			
				if( a!=1 && a!=2 && a!=3 && a!=8 )
					judgeSIC++;	
			}
		}
	}
	
	/*****************Format: PC******************/
	void formatPC()
	{
		for(int i=0; i<rows; i++)
		{
			PC[i] = LOC[i+1];
		}
	}
	
	/*****************Pass2******************/
	void Pass2()
	{
		for(int ii=0 ; ii<rows ; ii++)
		{
			String n,i,x="0",b="0",p="0",e="0";
			isLabel = 0;
			
			/******判斷: 此行是否為注解******/
			if(instruction[ii][0].equals("."))
			{
				objectCode[ii] = " ";
				continue;
			}
			
			/******判斷: 此行是否為SIC******/
			if(judgeSIC==0)	//架構: SIC
			{
				n = "0";
				i = "0";
			}
			else	//架構: SIC/XE
			{
				n = "1";
				i = "1";
			}
			
			/********儲存: BASE*********/
			if(instruction[ii][1].equals("BASE"))
			{
				BASE = Symtable.getValue(instruction[ii][2]);
			}
			
			/********判斷: Assembler directive*********/
			if( (AsseDirtable.adjudge(instruction[ii][1]) ) > 0)	
				continue;
			
			/********Literal: mnemonic*********/
			if(instruction[ii][1].indexOf("X'") >= 0)	//X'值'
			{
				int index = instruction[ii][1].indexOf("'");	//標記: 分隔符
				
				objectCode[ii] = instruction[ii][1].substring(index+1,(instruction[ii][1]).length()-1);
				
				continue;
			}
			
			if(instruction[ii][1].indexOf("C'") >= 0)	//C'值'
			{
				int index = instruction[ii][1].indexOf("'");
				
				for(int j=index+1 ; j<(instruction[ii][1]).length() - 1 ; j++)
				{
					int temp = (int)((instruction[ii][1]).charAt(j));
					objectCode[ii] += Integer.toHexString(temp);
				}
				
				objectCode[ii] = objectCode[ii].replaceAll("null","");
				
				continue;
			}
			
			/********Byte*********/
			if(instruction[ii][1].equals("BYTE"))
			{
				int index = instruction[ii][2].indexOf("'");	//標記: 分隔符
				
				if(instruction[ii][2].indexOf("X'") >= 0)	//X'值'
				{
					objectCode[ii] = instruction[ii][2].substring(index+1,(instruction[ii][2]).length()-1);
					
					continue;
				}
				
				if(instruction[ii][2].indexOf("C'") >= 0)	//C'值'
				{
					for(int j=index+1 ; j<(instruction[ii][2]).length() - 1 ; j++)
					{
						int temp = (int)((instruction[ii][2]).charAt(j));
						
						objectCode[ii] += Integer.toHexString(temp);
					}
					objectCode[ii] = objectCode[ii].replaceAll("null","");
					continue;
				}
			}
			
			/********Word*********/
			if(instruction[ii][1].equals("WORD"))
			{
				int temp = Integer.parseInt(instruction[ii][2]);
				objectCode[ii] = Integer.toHexString(temp);
				objectCode[ii] = String.format("%6.6s",objectCode[ii]);
				objectCode[ii] = objectCode[ii].replaceAll(" ","0");
				continue;
			}
			
			/********設定: Indirect*********/
			if(instruction[ii][2].indexOf("@")==0)
			{
				n = "1";
				i = "0";
			}
			
			/********設定: Immediate*********/
			if(instruction[ii][2].indexOf("#")==0)
			{
				if(Symtable.search((instruction[ii][2]).replaceAll("#","")))
					isLabel =1;
				n = "0";
				i = "1";
			}
			
			/********設定: Index*********/
			if(instruction[ii][2].indexOf(",X")>=0)
			{
				x = "1";
			}
			
			/********Format4*********/
			if(instruction[ii][1].indexOf("+")==0)
			{
				Format4 f4 = new Format4();
				String address;
				String tempMenmonic = (instruction[ii][1]).substring(1,((instruction[ii][1]).length()));
				String tempOperand = (instruction[ii][2]).replaceAll("#","");
					   tempOperand = tempOperand.replaceAll("@","");
					   
				f4.setOp( f4.formatMnemonic( Optable.position(tempMenmonic) ));
				f4.n = n;
				f4.i = i;
				f4.x = x;
				f4.b = "0";
				f4.p = "0";
				f4.e = "1";
				
				/**判斷: immediate**/
				if(i.equals("1") && n.equals("0")) 
				{
					if(isLabel == 1) //#Label
						address = Symtable.getValue(tempOperand);
					else	//#數字
					{
						int immediate = Integer.parseInt(tempOperand);
						address = Integer.toHexString(immediate);
					}
				}
				else
					address = Symtable.getValue(tempOperand);
				
				f4.setAddress(address);
				objectCode[ii] = f4.getFormat();
			}
			
			if(Optable.search(instruction[ii][1]))
			{
				String po = Optable.position(instruction[ii][1]);
				
				/********Format1*********/
				if(po.equals("C4") || po.equals("C0") || po.equals("F4") || po.equals("C8") || po.equals("94") || po.equals("F0") || po.equals("F8") )
				{
					Format1 f1 = new Format1();
					
					f1.setOp( f1.formatMnemonic(po) );
					
					objectCode[ii] = f1.getFormat();
				}
				
				/********Format2: register*1 *********/
				else if(po.equals("B4")|| po.equals("B0") || po.equals("B8") || po.equals("A4") || po.equals("A8"))
				{
					Format2 f2 = new Format2();
					
					f2.setOp(f2.formatMnemonic(po));
					f2.r1 = Register.search(instruction[ii][2]);
					f2.r2 = "0000";
					
					objectCode[ii] = f2.getFormat();
				}
				
				/********Format2: register*2*********/
				else if(po.equals("90") || po.equals("A0") || po.equals("9C") || po.equals("98") || po.equals("AC") )
				{
					Format2 f2 = new Format2();
					
					int index = (instruction[ii][2]).indexOf(",");
					String r1 = (instruction[ii][2]).substring(0,index);
					String r2 = (instruction[ii][2]).substring(index+1,(instruction[ii][2]).length());
					f2.setOp(f2.formatMnemonic(po));
					f2.r1 = Register.search(r1);
					f2.r2 = Register.search(r2);
					
					objectCode[ii] = f2.getFormat();
				}
				
				/********Format3*********/
				else
				{
					Format3 f3 = new Format3();
					
					String sPC,sTA,sOp;
					int iPC,iTA;
					
					/****處理: Program Counter****/
					sPC= PC[ii];
					
					if(PC[ii].equals(" "))
					{	
						int temp =ii;
						while(PC[temp].equals(" "))
							sPC = PC[++temp];
					} 
					
					iPC = Integer.parseInt(sPC,16);
					
					/****處理: Operand****/
					sOp = instruction[ii][2].replaceAll("#","");
					sOp = sOp.replaceAll("@","");
					
					if(sOp.indexOf(",X") >= 0)
					{
						sOp = sOp.replaceAll(",X","");
					}
					
					/**判斷: Immediate , #數字**/
					if(i.equals("1") && n.equals("0") && isLabel == 0)
					{
						sTA = sOp;
						iTA = Integer.parseInt(sTA,16);
						f3.setDisp(iTA);
					}
					
					/**判斷: SIC**/
					else if(i.equals("0") && n.equals("0"))
					{
						sTA = Symtable.getValue(sOp);
						f3.opcode = Optable.position(instruction[ii][1]);
						
						if((f3.opcode).equals("4C")) //RSUB
							sTA="0000";
						
						objectCode[ii] = f3.opcode + sTA;
						
						continue;
					}
					
					/**判斷: SIC/XE**/
					else
					{
						/**判斷: Operand是否為空白**/
						if(sOp.equals(" "))
						{
							sTA = "0";
							iTA = 0;
						}
						
						/**判斷: Operand是否為literal**/
						else if(instruction[ii][2].indexOf("=")>=0)
						{
							sTA = LiteralPoll.getValue(sOp);
							iTA = Integer.parseInt(sTA,16);
						}
						
						else
						{
							sTA = Symtable.getValue(sOp);
							iTA = Integer.parseInt(sTA,16);	
						}
						
						/**判斷: PC-counter relative**/
						if(iTA-iPC <= 2047&& iTA-iPC >= -2048)
						{
							p = "1";
							b = "0";
							f3.setDisp((iTA-iPC));
						}
						
						/**處理: Operand為空白**/
						else if(sOp.equals(" "))
						{
							f3.setDisp(iTA);
						}
						
						/**判斷: BASE relative**/
						else
						{
							p = "0";
							b = "1";
							
							int ib = Integer.parseInt(BASE,16);
							
							f3.setDisp(iTA-ib);
						}	
					}	

					f3.setOp(f3.formatMnemonic(po));
					f3.n = n;
					f3.i = i;
					f3.x = x;
					f3.b = b;
					f3.p = p;
					f3.e = e;
					
					objectCode[ii] = f3.getFormat();
				}
			}
		}	
	}	
}

class AsseDirtable
{
	public static Hashtable<String,Integer> ad = new Hashtable<String,Integer>();
		
	/*****************尋找: 是否存在******************/	
	public static boolean search(String as)
	{
		//[1] assembler directive : 無Object code; 有LOC
		ad.put("START",1);		ad.put("RESW",2);		
		ad.put("RESB",3);		ad.put("EQU",4);
		ad.put("USE",5);		ad.put("CSECT",6);
				
		//[2] assembler directive : 無Object code; 無LOC
		ad.put("END",8);		ad.put("BASE",9);
		ad.put("LTORG",10);		ad.put("EXTDEF",11);
		ad.put("EXTREF",12);
				
		if( ad.containsKey(as) )
			return true;
		else
			return false;
	}
			
	/*****************判斷: 有無LOC******************/	
	public static int adjudge(String as)
	{
		//[1] assembler directive : 無Object code; 有LOC
		ad.put("START",1);		ad.put("RESW",2);		
		ad.put("RESB",3);		ad.put("EQU",4);
		ad.put("USE",5);		ad.put("CSECT",6);
			
		//[2] assembler directive : 無Object code; 無LOC
		ad.put("END",8);		ad.put("BASE",9);
		ad.put("LTORG",10);		ad.put("EXTDEF",11);
		ad.put("EXTREF",12);
		
		if(search(as))
			return ad.get(as);
		else
			return 0;			
	}
}

class Optable 
{
	public static Map<String,String> op = new HashMap<>();
	
	/*****************尋找: 是否存在******************/	
	public static boolean search(String opcode)
	{
		op.put("ADD","18");		op.put("ADDF","58");	op.put("ADDR","90");
		op.put("AND","40");		op.put("CLEAR","B4");	op.put("COMP","28");
		op.put("COMPF","88");	op.put("COMPR","A0");	op.put("DIV","24");
		op.put("DIVF","64");	op.put("DIVR","9C");	op.put("FIX","C4");
		op.put("FLOAT","C0");	op.put("HIO","F4");		op.put("J","3C");
		op.put("JEQ","30");		op.put("JGT","34");		op.put("JLT","38");
		op.put("JSUB","48");	op.put("LDA","00");		op.put("LDB","68");
		op.put("LDCH","50");	op.put("LDF","70");		op.put("LDL","08");
		op.put("LDS","6C");		op.put("LDT","74");		op.put("LDX","04");
		op.put("LPS","D0");		op.put("MUL","20");		op.put("MULF","60");
		op.put("MULR","98");	op.put("NORM","C8");	op.put("OR","44");
		op.put("RD","D8");		op.put("RMO","AC");		op.put("RSUB","4C");
		op.put("SHIFTL","A4");	op.put("SHIFTR","A8");	op.put("SIO","F0");
		op.put("SSK","EC");		op.put("STA","0C");		op.put("STB","78");
		op.put("STCH","54");	op.put("STF","80");		op.put("STI","D4");
		op.put("STL","14");		op.put("STS","7C");		op.put("STSW","E8");
		op.put("STT","84");		op.put("STX","10");		op.put("SUB","1C");
		op.put("SUBF","5C");	op.put("SUBR","94");	op.put("SVC","B0");
		op.put("TD","E0");		op.put("TIO","F8");		op.put("TIX","2C");
		op.put("TIXR","B8");	op.put("WD","DC");
		
		/**Special Mnemonic**/
		op.put("WORD","___"); op.put("BYTE","_");
		
		if(op.get(opcode)!=null)
			return true;
		else
			return false;
	} 
	
	/*****************位置:多少(16進位)******************/	
	public static String position(String opcode)
	{
		op.put("ADD","18");		op.put("ADDF","58");	op.put("ADDR","90");
		op.put("AND","40");		op.put("CLEAR","B4");	op.put("COMP","28");
		op.put("COMPF","88");	op.put("COMPR","A0");	op.put("DIV","24");
		op.put("DIVF","64");	op.put("DIVR","9C");	op.put("FIX","C4");
		op.put("FLOAT","C0");	op.put("HIO","F4");		op.put("J","3C");
		op.put("JEQ","30");		op.put("JGT","34");		op.put("JLT","38");
		op.put("JSUB","48");	op.put("LDA","00");		op.put("LDB","68");
		op.put("LDCH","50");	op.put("LDF","70");		op.put("LDL","08");
		op.put("LDS","6C");		op.put("LDT","74");		op.put("LDX","04");
		op.put("LPS","D0");		op.put("MUL","20");		op.put("MULF","60");
		op.put("MULR","98");	op.put("NORM","C8");	op.put("OR","44");
		op.put("RD","D8");		op.put("RMO","AC");		op.put("RSUB","4C");
		op.put("SHIFTL","A4");	op.put("SHIFTR","A8");	op.put("SIO","F0");
		op.put("SSK","EC");		op.put("STA","0C");		op.put("STB","78");
		op.put("STCH","54");	op.put("STF","80");		op.put("STI","D4");
		op.put("STL","14");		op.put("STS","7C");		op.put("STSW","E8");
		op.put("STT","84");		op.put("STX","10");		op.put("SUB","1C");
		op.put("SUBF","5C");	op.put("SUBR","94");	op.put("SVC","B0");
		op.put("TD","E0");		op.put("TIO","F8");		op.put("TIX","2C");
		op.put("TIXR","B8");	op.put("WD","DC");
		
		/**Special Mnemonic**/
		op.put("WORD","_"); op.put("BYTE","_");
		
		if(search(opcode))
			return op.get(opcode);
		else
			return null;
	}
}

class Symtable 
{
	public static Hashtable<String,String> sym = new Hashtable<String,String>();

	/*****************尋找: 是否存在******************/	
	public static boolean search(String label)
	{
		if(sym.containsKey(label))
			return true;
		else
			return false;
	}
	
	/*****************插入: 新的Lebel******************/	
	public static boolean insert(String label,String address)
	{
		if(sym.containsKey(label) || label.equals(" "))
			return false;
		else
		{
			sym.put(label,address);
			return true;
		}
	}
	
	/*****************取值: 根據pass1******************/
	public static String getValue(String label)
	{
		if(sym.containsKey(label))
			return sym.get(label);
		else
			return null;
	}
	
	/*****************印出: 根據pass1******************/
	public static void print()
	{
		for (Object key : sym.keySet()) {
			System.out.println(key + " : " + sym.get(key));}
	}
}

class Register
{
	public static Hashtable<String,String> register = new Hashtable<String,String>();

	/*****************尋找: 是否存在******************/	
	public static String search(String r)
	{
		register.put("A","0000");	register.put("X","0001");	register.put("L","0010");
		register.put("B","0011");	register.put("S","0100");	register.put("T","0101");
		register.put("F","0110");	register.put("PC","1000");	register.put("SW","1001");
		if(register.containsKey(r))
			return register.get(r);
		else
			return null;
	}	
}

class LiteralPoll 
{
	public static Hashtable<String,String> lt = new Hashtable<String,String>();

	/*****************尋找: 是否存在******************/	
	public static boolean search(String literal)
	{
		if(lt.containsKey(literal))
			return true;
		else
			return false;
	}
	
	/*****************插入: 新的Literal******************/	
	public static boolean insert(String literal,String LOC)
	{
		if(lt.containsKey(literal) || literal.equals(" "))
			return false;
		else
		{
			lt.put(literal,LOC);
			return true;
		}
	}
	
	/*****************取值: 根據pass1******************/
	public static String getValue(String literal)
	{
		if(lt.containsKey(literal))
			return lt.get(literal);
		else
			return null;
	}
	
	/*****************印出: 根據pass1******************/
	public static void print()
	{
		for (Object key : lt.keySet()) {
			System.out.println(key + " : " + lt.get(key));}
	}
}

abstract class Format
{
	protected String opcode,sformat;
	protected int opLength = getOpLength();
	protected int iformat,formatLength;
	
	/******格式化(16轉2): Mnemonic******/
	String formatMnemonic(String mnemonic)
	{
		int op10 = Integer.parseInt(mnemonic,16);	//16進位字串轉10進位整數
		mnemonic = Integer.toString(op10,2);		//10進位整數轉2進位字串
		mnemonic = String.format("%8.8s",mnemonic);
		mnemonic = mnemonic.replaceAll(" ","0");
		
		if( mnemonic.length() > opLength )
			mnemonic = mnemonic.substring(0,opLength);
		
		return mnemonic;	
	}
	
	abstract int getOpLength();
	abstract void setOp(String mnemonic);
	abstract String getFormat();
}

class Format1 extends Format
{
	/******設定: Opcode******/
	void setOp(String mnemonic)
	{
		opcode = String.format("%8.8s",mnemonic);
		opcode = opcode.replaceAll(" ","0");
	}
	
	/******設定: Opcode長度******/
	int getOpLength()
	{
		return 8;
	}
	
	/******設定/取值: Object Code******/
	String getFormat()
	{
		iformat = Integer.parseInt(opcode,2);	//2進位字串轉10進位整數
		sformat = Integer.toHexString(iformat);	//10進位整數轉16進位字串
		sformat = String.format("%2.2s",sformat);	//format1: 8bits(2) = 2bits(16)
		sformat = sformat.replaceAll(" ","0");
		
		return sformat;
	}	
}

class Format2 extends Format
{
	protected String r1,r2;
	final int r1Length=4;
	final int r2Length=4;
	
	/******設定: Opcode******/
	void setOp(String mnemonic)
	{
		opcode = String.format("%8.8s",mnemonic);
		opcode = opcode.replaceAll(" ","0");	
	}
	
	/******設定: Opcode長度******/
	int getOpLength()
	{
		return 8;
	}
	
	/******設定/取值: Object Code******/
	String getFormat()
	{
		iformat = Integer.parseInt((opcode+r1+r2),2);
		sformat = Integer.toHexString(iformat);	//10進位整數轉16進位字串
		sformat = String.format("%4.4s",sformat);	//format1: 16bits(2) = 2bits(16)
		sformat = sformat.replaceAll(" ","0");
		
		return sformat;
	}
}

class Format3 extends Format
{
	protected String n,i,x,b,p,e,sdisp;
	
	/******設定: Opcode******/
	void setOp(String mnemonic)
	{
		opcode = String.format("%6.6s",mnemonic);
		opcode = opcode.replaceAll(" ","0");	
	}
	
	/******設定: Opcode長度******/
	int getOpLength()
	{
		return 6;
	}
	
	/******設定: Displacement******/
	void setDisp(int idisp)
	{	
		int negative =0;
		
		if(idisp < 0)
		{
			negative = 1;
		}
		
		sdisp = Integer.toBinaryString(idisp);
		
		if(negative ==1)
		{
			if( sdisp.length() > getDispLength() )
				sdisp = sdisp.substring((sdisp.length()-getDispLength()), sdisp.length());
			
			sdisp = String.format("%12.12s",sdisp);
			sdisp = sdisp.replaceAll(" ","0");
		}
		else
		{
			if( sdisp.length() > getDispLength() )
				sdisp = sdisp.substring(0,getDispLength());
			
			sdisp = String.format("%12.12s",sdisp);
			sdisp = sdisp.replaceAll(" ","0");
		}			
	}
	
	/******設定: Displacement長度******/
	int getDispLength()
	{
		return 12;
	}
	
	/******設定/取值: Object Code******/
	String getFormat()
	{
		iformat = Integer.parseInt((opcode+n+i+x+b+p+e+sdisp),2);
		sformat = Integer.toHexString(iformat);	//10進位整數轉16進位字串
		sformat = String.format("%6.6s",sformat);	//format1: 24bits(2) = 6bits(16)
		sformat = sformat.replaceAll(" ","0");
		
		return sformat;		//10進位整數轉16進位字串
	}
}

class Format4 extends Format
{
	protected String n,i,x,b,p,e,saddress;
	protected int iaddress;
	
	/******設定: Opcode******/
	void setOp(String mnemonic)
	{
		opcode = String.format("%6.6s",mnemonic);
		opcode = opcode.replaceAll(" ","0");	
	}
	
	/******設定: Opcode長度******/
	int getOpLength()
	{
		return 6;
	}
	
	/******設定: Address******/
	void setAddress(String LOC)
	{	
		iaddress =Integer.parseInt(LOC,16);
		saddress = Integer.toString(iaddress,2);
			
		if( saddress.length() > getAddressLength() )
			saddress = saddress.substring(0,getAddressLength());
			
		saddress = String.format("%20.20s",saddress);
		saddress = saddress.replaceAll(" ","0");
			
	}
	
	/******設定: Address長度******/
	int getAddressLength()
	{
		return 20;
	}
	
	/******設定/取值: Object Code******/
	String getFormat()
	{
		iformat = Integer.parseInt((opcode+n+i+x+b+p+e+saddress),2);
		sformat = Integer.toHexString(iformat);	//10進位整數轉16進位字串
		sformat = String.format("%8.8s",sformat);	//format1: 24bits(2) = 8bits(16)
		sformat = sformat.replaceAll(" ","0");
		
		return sformat;		//10進位整數轉16進位字串
	}
}
