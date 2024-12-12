//package org.example;
import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.registry.Registry;
import java.time.Clock;
import java.util.*;
import java.nio.ByteBuffer;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static int missPenalty;
    //the following are hardcoding for the execution time for the instruction time
    static int addET=3;
    static int subET=3;
    static int mulET=4;
    static int divET=6;
    static int loadStoreET=1;


//    queue for the instruciton ready to write this will come in handy when more than one instruction finish exec
//    in the same clock cycle
    static Queue<String>readyToWrite=new LinkedList<>();
    static int instructionTableIndex=0;
    //this instruction table is a consisted of 5 columns 1st column is for instruction shortcut for e.g A1 (A1 is 1st slot in A_RS)
    //2nd column is for issue clock cycle , 3rd column is for execution time for this instruction, 4th column is for start cycle of the execution
    //5th column is for write result , //note there may be two entries (rows) in the instruction table with having same instruction shortcut because it refers to the place in the RS
    //but it is impossible that both of them don't have their values written
    static Object[][]instructionTable;

    static boolean stall=false;

    //registers is an array holding the values of the registers .the index of the elements in the aray represent the register no.
    //TODO:the register files shouldn't be of fixed length
    static int[] addressRegisters=new int[20];
    static String[] addressReservations=new String[20];


    static float[] registers=new float[20];
    //regReservation is array of the same size as the registers holding name of the tag reserving the corresponding register or "free" otherwise
    static String[] regReservations=new String[20];
//    static ArrayList<Float>registers =new ArrayList<>(100);

    //clock cycles
    static int clockCycle;

    //instruction queue that the instructions are issued from
    static Queue<String> instructions = new LinkedList<>();
    //that is how you initialize a queue
    //each array of String is of length 5 one column for instruction , one for Issue, one for execution, one for write result
    //static ArrayList<Object[]> instructions;

    //reservation stations and buffers

    //static class to represent cache element as a tuple of 2 elements one for value and one for validity
    static class CacheElement{
        byte value;
        boolean validity;
        public CacheElement(){}
        public CacheElement(byte value, boolean validity){
            this.value = value;
            this.validity = validity;
        }
    }
    static Object[][] A_RS;
    static Object[][] M_RS;
    static Object[][]loadBuffer;
    static Object[][]storeBuffer;
    //the memory and cash
    static byte memory [] = new byte[1024];
    static CacheElement cache [];
    //file to read
    static File Instructions =new File("C:\\Users\\LAPTOP\\IdeaProjects\\tomasulo-s-schedule\\MP\\src\\instructions.txt");

    public static void scanOperations()
    {
        try (Scanner scanner = new Scanner(Instructions)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                System.out.println(line);
                instructions.add(line);
                System.out.println("line is added to the instruction queue");
            }
            //initialize the length of the instruction table
            instructionTable=new Object[instructions.size()][5];

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }

    //initialization method for the reservation station that make the busy bit =0 initially
    public static void fillRS(Object[][] RS)
    {
        for(int i=0;i< RS.length;i++)
        {
            //initialize the busy column with zero
            RS[i][6]=(Integer)0;
            //initialize the Q columns with zeros
//            RS[i][3]=(Integer)0;
//            RS[i][4]=(Integer)0;
        }
    }

    //initialization method for the store buffer
    public static void fillStoreBuffer(Object[][] buffer)
    {
        for(int i=0;i< buffer.length;i++)
        {
            buffer[i][2]=(Integer)0;
        }
    }

    //initialization method for the load buffer
    public static void fillLoadBuffer(Object[][] buffer)
    {
        for(int i=0;i< buffer.length;i++)
        {
            buffer[i][1]=(Integer)0;
        }
    }

    //initialization method for the registerfile by hardcoding all the registers with value 1 and the regReservation of all registers "free"
    public static void fillRegisterFile(String[]RegReservation)
    {
        for(int i =0 ; i<regReservations.length;i++)
        {
            regReservations[i]="free";
            addressReservations[i]="free";
            registers[i]=1.0f;
        }
    }

    //initialize cache with garbage values (as long as validity is false this means this cache entry is garbage) ---> it's a miss
    static void fillCache(CacheElement [] cache){
        for(CacheElement c : cache){
            c = new CacheElement();
        }
    }
    //method called when the main is run in order to enter the sizes of the reservation stations and buffers
    public static void EnterSizes(){
        Scanner sc =new Scanner(System.in);
        System.out.println("please enter the size of the addition/subtraction reservation station");
        A_RS=new Object[sc.nextInt()][7];
        fillRS(A_RS);
//        add_sub_size=sc.nextInt();
        System.out.println("please enter the size of the multiplication/division reservation station");
        M_RS=new Object[sc.nextInt()][7];
        fillRS(M_RS);
//        mul_div_size=sc.nextInt();
        System.out.println("please enter the size of the load buffer");
        loadBuffer=new Object[sc.nextInt()][4];
        fillLoadBuffer(loadBuffer);
//        load_buffer_size= sc.nextInt();
        System.out.println("please enter the size of the store buffer");
        storeBuffer=new Object[sc.nextInt()][4];
        fillStoreBuffer(storeBuffer);
//        store_buffer_size= sc.nextInt();
        System.out.println("please enter the size of the cache");
        cache = new CacheElement[sc.nextInt()];
        fillCache(cache);
    }

    public static byte[] serialize(float number) {
        // Allocate a ByteBuffer with size 4 (float is 4 bytes in IEEE 754)
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        // Put the float into the buffer
        buffer.putFloat(number);
        // Return the backing byte array
        return buffer.array();
    }

    public static byte[] serialize(double number) {
        // Allocate a ByteBuffer with size 8 (double is 8 bytes in IEEE 754)
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        // Put the double into the buffer
        buffer.putDouble(number);
        // Return the backing byte array
        return buffer.array();
    }

    // Deserialize a byte array back into a float
    public static float deserializeFloat(byte[] byteArray) {
        // Wrap the byte array into a ByteBuffer
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        // Read the float from the buffer
        return buffer.getFloat();
    }

    // Deserialize a byte array back into a double
    public static double deserializeDouble(byte[] byteArray) {
        // Wrap the byte array into a ByteBuffer
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        // Read the double from the buffer
        return buffer.getDouble();
    }

    //method for checking if there is an available place in the reservation station and returns the index of this place/slot
    // , otherwise it returns -1
    public static int checkRS(Object[][] RS)
    {
        for(int i=0;i< RS.length;i++)
        {
            if((Integer)RS[i][6]==0)
                return i;
        }
        return -1;
    }

    //after checking the RS is available we can issue the instruction to the RS
    //we want to update the register file to set the dest. reg as reserved by the corresponding RS
    //the type is type of RS it will be used to reserve the dest. reg with this type
    //availablePlace is the free index of the reservation station
    private static void issueInstruction(Object[][] aRs, int availablePlace, String[] slots,char type) {
        //a tag/shortcut for the operation that will tag the dest. register and will be put in the instruction table
        String tag = "" + type + availablePlace;
        //put the tag/(instruction symbol) (for e.g:A1) in the instruction table
        instructionTable[instructionTableIndex][0] = tag;
        //put the clock cycle the instruction was issued at
        instructionTable[instructionTableIndex][1] = clockCycle;
        System.out.println("the instruction index at the instruction table we are at right now is: " + instructionTableIndex + " and the its shortcut is: " + tag);

        //the first string of slots[] is consisted of two parts instruction operation and the dest. register so we split again
        //in order to get the dest. reg and type of the instruction seperately.
        String[] parts = slots[0].split(" ");
        String typeOfInstruction = parts[0].substring(0, 3);
        aRs[availablePlace][0] = typeOfInstruction;
        //this is the dest. reg number that will be reserved with this current RS entry for e.g:A1
        int destReg = Integer.parseInt((parts[1].substring(1, 2)));
//        if(typeOfInstruction.charAt(0)=='l')
//        {
//            //
//            slots[1].trim();
//            int address;
//
//            if(slots[1].toLowerCase().charAt(0) != 'r' ){
//                address=Integer.parseInt(String.valueOf(slots[1]));
//            }else{
//                int registerNumber=Integer.parseInt(slots[1].substring(1));
//                if(addressReservations[registerNumber].equals("free"))
//                {
//
//                }
//            }
//        }
        if (typeOfInstruction.charAt(0) == 'l') {
            slots[1].trim();
            System.out.println("trying to get the destination in load inst");
            int dest = Integer.parseInt(slots[1].substring(1));
            loadBuffer[availablePlace][2]=dest;
            loadBuffer[availablePlace][3]= dest;
        } else {

            //the following is how you get the operands of the instruciton in case of the RS
            int operand1 = Integer.parseInt(String.valueOf(slots[1].charAt(2)));
            System.out.println(slots[1].charAt(0));
            slots[2].trim();
            System.out.println(slots[1].charAt(1));
            System.out.println("operand 1 index is : " + operand1);

            int operand2 = Integer.parseInt(String.valueOf(slots[2].charAt(1)));
            System.out.println("operand 2 index is : " + operand2);



        //tag the dest. reg in the array regReservation with the correct name of the current instruction in the RS
        //regReservation is array with same length of the registers array but each index refer to the instruction reserving the corresponding
        //register of have in it "free" as a sign of that register isn't reserved
        regReservations[destReg] = tag;
//        System.out.print("reached here");
        if (regReservations[operand1].equals("free")) {
//            System.out.println("  this is free");
            //load the Vj slot with the 1st operand register content
            aRs[availablePlace][1] = registers[operand1];
            System.out.println("the 1st operand register is not being calculated by any other instruction and is loaded to Vj " + operand1);
        } else {
            aRs[availablePlace][3] = regReservations[operand1];
            System.out.println("the 1st operand register is being calculated by another register and is loaded to Qj " + operand1);
        }
        if (regReservations[operand2].equals("free")) {
            //load the VK slot with the 2nd operand register content
            aRs[availablePlace][2] = registers[operand2];
//            System.out.println("the second operand register of the instruction is R "+operand2);
            System.out.println("the 2nd operand register is not being calculated by any other instruction and is loaded to Vk " + operand2);

        } else {
//            aRs[availablePlace][4]=operand2;
            aRs[availablePlace][4] = regReservations[operand2];
            System.out.println("the 2nd operand register is being calculated by another register and is loaded to Qk " + operand2);
        }
        aRs[availablePlace][6] = 1;
//        if((Integer)aRs[availablePlace][3]==0&&(Integer)aRs[availablePlace][4]==0)

        //TODO : the Qj&Qk are initially null not equal to zero
        if (aRs[availablePlace][3] == null && aRs[availablePlace][4] == null) {

            if (typeOfInstruction.toLowerCase().contains("add")) {
                System.out.println("executing add instruction");
                executeInstruction(tag);
            }
            if (typeOfInstruction.toLowerCase().contains("sub")) {
                System.out.println("executing sub instruction");
                executeInstruction(tag);
            }
            if (typeOfInstruction.toLowerCase().contains("mul")) {
                System.out.println("executing mul instruction");
                executeInstruction(tag);
            }
            if (typeOfInstruction.toLowerCase().contains("div")) {
                System.out.println("executing div instruction");
                executeInstruction(tag);
            }

//            else if(type=='')
//            executeInstruction();
        }
        }


    }
//
//    TODO: this method might have other things to check rather than checking the instructions' execution
    public static void trackCycleChanges()
    {
        checkInstructionExecution();
    }

    // check execution of executing instructions
    // else part checks if any issued instruction can start execution
    public static void checkInstructionExecution()
    {
        //this checks the issued instructions that is entered to the instruction table
        for(int i =0;i< instructionTable.length&&instructionTable[i][0]!=null;i++)
        {
            //this if condition checks if the instruction didn't write a result , if not so then there is no point of checking it
            //because this instruction is finished already
            if(instructionTable[i][4]==null)
            {
                //if the third column has value in it and it is not null then the instruction started executing
                if(instructionTable[i][2]!=null)
                {
                    //checks if this is the clock cycle that the instruction execution is finished and can and can write in it
                    if(clockCycle==(Integer)((Integer)instructionTable[i][2]+(Integer)instructionTable[i][3]))
                    {
//                        instructionTable[i][4]=clockCycle;
                        //add this inst to a write buffer in case there is more than one inst that can write at the same clockCycle
                        readyToWrite.add((String)instructionTable[i][0]);
                        System.out.println("instruction: "+instructionTable[i][0]+" two duration: "+instructionTable[i][2]+" start exec: "+instructionTable[i][3]);
                    }


                }
                else {
                    checkIssuedInstruction();
                }
                //else this means that the instruction has been issued but didn't start execution yet
//                else
//                {
//                }
            }
//            checkIssuedInstruction();
//            checkWriteBuffer();
            String shortcut;
            if(readyToWrite.size()>0)
            {
                //tag
                shortcut = readyToWrite.remove();
                assignWriteBit(shortcut);


//                executeInstruction(shortcut);
                System.out.println(shortcut);
                System.out.println("-------------------------entered the replace");
                replace(shortcut);
            }

        }
    }
    public static void assignWriteBit(String shortcut)
    {
        for(int i=0;i<instructionTable.length;i++)
        {
            if(instructionTable[i][0].equals(shortcut))
            {
                instructionTable[i][4]=clockCycle;
            }
        }
    }
    public static void checkIssuedInstruction()
    {
        for(int i=0;i<instructionTable.length&&instructionTable[i][0]!=null;i++)
        {
            if(instructionTable[i][2]==null)
            {
                System.out.println(instructionTable[i][0]);
                String shortcut=(String)instructionTable[i][0];
                //know the slot I will search in the reservation station
                System.out.println("here is the shortcut "+shortcut.substring(1,2));
                int slot=Integer.parseInt(shortcut.substring(1,2));
                System.out.println(shortcut.charAt(0)=='A');
                if(shortcut.charAt(0)=='M')
                {
                    System.out.println("om el shortcut aho "+shortcut);
                    if (M_RS[slot][3] == null && M_RS[slot][4] == null) {
//                    String instShortcut = (String) M_RS[slot][0];
                    System.out.println("calling EX from the check issued inst.");
                    System.out.println("om el shortcut aho "+shortcut);
                    executeInstruction(shortcut);
                    }
                }

                else if(shortcut.charAt(0)=='A')
                {
                    System.out.println("om el shortcut aho "+shortcut);
                    if (A_RS[slot][3] == null && A_RS[slot][4] == null) {
//                        String instShortcut = (String) A_RS[slot][0];
                        System.out.println("calling EX from the check issued inst.");
                        System.out.println("om el shortcut aho "+shortcut);
                        executeInstruction(shortcut);
                    }
                }
            }




        }

    }
    public static void replace(String shortcut)
    {

        float value= 5.5F;
        int index=Integer.parseInt(shortcut.substring(1,2));
        switch (shortcut.charAt(0)){
            case'M':
                value=(Float)M_RS[index][5];
                break;
            case'A':
                value=(Float)A_RS[index][5];

        }
        int reservationIndex=getReservationIndex(shortcut);
        if(reservationIndex!=-1) {
            registers[reservationIndex] = value;
            regReservations[reservationIndex] = "free";
            System.out.println("freed the reserved register");
        }
        for(int i =0;i< M_RS.length;i++)
        {
            if(M_RS[i][3]!=null&&((String)(M_RS[i][3])).equals(shortcut))
            {
//                M_RS[i][3]=0;
                M_RS[i][3]=null;
                M_RS[i][1]=value;
                System.out.println("freed the RS slot");
            }
            if(M_RS[i][4]!=null&&((String)(M_RS[i][4])).equals(shortcut))
            {
//                M_RS[i][4]=0;
                M_RS[i][4]=null;
                M_RS[i][2]=value;
                System.out.println("freed the RS slot");

            }
        }
        for(int i =0;i< A_RS.length;i++)
        {
            if(A_RS[i][3]!=null&&((String)(A_RS[i][3])).equals(shortcut))
            {
//                A_RS[i][3]=0;
                A_RS[i][3]=null;
                A_RS[i][1]=value;
                System.out.println("freed the RS slot");

            }
            if(A_RS[i][4]!=null&&((String)(A_RS[i][4])).equals(shortcut))
            {
//                A_RS[i][4]=0;
                A_RS[i][4]=null;
                A_RS[i][2]=value;
                System.out.println("freed the RS slot");

            }
        }

    }
    public static int getReservationIndex(String shortcut)
    {
        int index=-1;
        for(int i=0;i< regReservations.length;i++)
        {
            if(regReservations[i].equals(shortcut)) {
                index = i;
                break;
            }
        }
        return index;
    }
public static void executeInstruction(String instShortCut)
{
    System.out.println("the instruction ya rayyes is: "+instShortCut);
    int ET=0;
    int index=Integer.parseInt(instShortCut.substring(1,2));
    int reservationIndex=0;
    System.out.println("yastaaaaaaaaaaaaaaaaaaaaaa el instShortCut is: " + instShortCut);
    System.out.println("yastaaaaaaaaaaaaaaaaaaaaaa el index is: " + index);
    System.out.println(instShortCut.charAt(0)=='A');
    System.out.println(((String)M_RS[index][0]).toLowerCase().equals("add"));
    System.out.println(((String)M_RS[index][0]).toLowerCase());

    for(int i=0;i<regReservations.length;i++)
    {
        if(regReservations[i].equals(instShortCut)) {
            reservationIndex = i;
            break;
        }
    }
    if(instShortCut.charAt(0)=='M')
    {
        System.out.println(index);
        if(((String)M_RS[index][0]).toLowerCase().equals("mul")) {
            ET = mulET;
//            registers[reservationIndex][1]=(Float)M_RS[index][2]*(Float)M_RS[index][3];
            M_RS[index][5]=(Float)((Float)M_RS[index][1]*(Float)M_RS[index][2]);
            System.out.println("multiplying operands");
        }
    }

    else if(instShortCut.charAt(0)=='A')
    {

        if(((String)A_RS[index][0]).toLowerCase().equals("add")) {
            ET = addET;
//            registers[reservationIndex][1]=(Float)A_RS[index][2]+(Float)A_RS[index][3];
            A_RS[index][5]=(Float)((Float)A_RS[index][1]+(Float)A_RS[index][2]);

        }
    }
    else if(instShortCut.charAt(0)=='A')
    {

        if(((String)A_RS[index][0]).toLowerCase().equals("sub")) {
            ET = subET;
//            registers[reservationIndex][1]=(Float)M_RS[index][2]-(Float)M_RS[index][3];
            A_RS[index][5]=(Float)A_RS[index][1]-(Float)A_RS[index][2];

        }
    }
    else if(instShortCut.charAt(0)=='M')
    {

        if(((String)M_RS[index][0]).toLowerCase().equals("div")) {
            ET = divET;
//            registers[reservationIndex][1]=(Float)((Float)M_RS[index][2]/(Float)M_RS[index][3]);
            M_RS[index][5]=(Float)((Float)M_RS[index][1]/(Float)M_RS[index][2]);

        }
    }
//    else if(instShortCut.substring(0,1).equals("l"))
//    {
//        ET=loadStoreET;
//    }
//    else if(instShortCut.substring(0,1).equals("s"))
//    {
//        ET=loadStoreET;
//    }
    //search the corresponding entry in the instructionTable
    for(int j=0;j<instructionTable.length;j++)
    {
//        System.out.println("instruction name is: "+instructionTable[j][0]);
//        System.out.println(((String)instructionTable[j][0]).equals(instShortCut));
        if(((String)instructionTable[j][0]).equals(instShortCut)&&instructionTable[j][4]==null)
        {
//            System.out.println("entered the if condition");

            instructionTable[j][2]=ET;
            System.out.println("execution time is "+ET);
            instructionTable[j][3]=clockCycle+1;
            break;
        }

    }
//    switch (instShortCut)
//    {
//        case "mul"
//    }
}

public static int checkLoadBuffer(Object[][] loadBuffer){
    for(int i=0;i< loadBuffer.length;i++)
    {
        if((Integer)loadBuffer[i][1]==0)
            return i;
    }
    return -1;
}
public static void Issue()
{

//    readyToWrite.add("ali");
//    System.out.println("the size of the buffer is: "+readyToWrite.size());

//    clockCycle=0;


//    clockCycle++;

    while (clockCycle<30/*instructions.size()>0/*||!stall*/) {
        clockCycle++;
        trackCycleChanges();

        if (clockCycle != 1) {
//            trackCycleChanges();
        }
        if (!stall&&instructions.size()>0) {
            String instruction = instructions.peek();

            instruction.trim();
            String[] slots = instruction.split(",");
//    for(String slot:slots)
//    {
//        slot.trim();
//    }

            //I am not sure about the .toLowerCase if it will work or not
            System.out.println("the operation is : " + slots[0]);
            System.out.println(slots[0].toLowerCase().substring(0, 2));
//    instructions.remove();
            System.out.println("the size of the instruction queue is : " + instructions.size());

            //check if the instruction is store
            if (slots[0].toLowerCase().charAt(0) == 's') {
//        checkBuffer();
            }
            //check if the instruction is load
            else if (slots[0].toLowerCase().charAt(0) == 'l') {
                System.out.println("the operation is load");
                int availablePlace = checkLoadBuffer(loadBuffer);
                System.out.println("the size of the load buffer is : " + loadBuffer.length);
//                System.out.println("__________________________________"+ slots.length+"__________________________");
                if (availablePlace == -1) {
                    System.out.println("no available places in the load buffer");
                    stall=true;
                }else {
//                    if(typeOfInstruction.charAt(0)=='l')
//                    {
                        //
                        slots[1].trim();
                        int address;

                        if(slots[1].toLowerCase().charAt(0) != 'r' ){
                            address=Integer.parseInt(String.valueOf(slots[1]));

                        }else {
                            int registerNumber = Integer.parseInt(slots[1].substring(1));
                            address = addressRegisters[registerNumber];
                        }
                        if(!checkStoreBufferAddress(address)) {

                                System.out.println("the issuing is stalled");
                                stall=true;
                        }

//                    }

                }
            }
            //check if the instruction is addition/subtraction
            else if (slots[0].toLowerCase().substring(0, 3).equals("add")) {
                System.out.println("the operation is add and the instruction should be issued");
                int availablePlace = checkRS(A_RS);
                System.out.println("the size of the reservations station for addition is : " + A_RS.length);
                if (availablePlace == -1) {
                    System.out.println("no available places in the add RS");
                    stall=true;

                } else {
                    instructions.remove();
                    System.out.println("reached the remove");
                    System.out.println(instructions.size());
                    issueInstruction(A_RS, availablePlace, slots, 'A');
                    instructionTableIndex++;
                }
            }
            //check if the instruction is multiplication/division
            else if (slots[0].toLowerCase().substring(0, 3).equals("mul")) {
                System.out.println("the operation is mul and the instruction should be issued");
                int availablePlace = checkRS(M_RS);
                System.out.println("the size of the reservations station for multiplication is : " + M_RS.length);
                if (availablePlace == -1) {
                    System.out.println("no available places in the mul RS");
                    stall=true;
                } else {
                    instructions.remove();
                    issueInstruction(M_RS, availablePlace, slots, 'M');
                    instructionTableIndex++;

                }
            }
//        clockCycle++;

        }
        else {
            System.out.println("the instructions issuing is stalled");
//            break;
        }
    }



}

    public static boolean checkStoreBufferAddress(int address) {
        for(int i =0;i<storeBuffer.length;i++){
            if((Integer)storeBuffer[i][3]==address)
                return false;
        }
        return true;
    }
    public static boolean checkLoadBufferAddress(int address) {
        for(int i =0;i<loadBuffer.length;i++){
            if((Integer)loadBuffer[i][2]==address)
                return false;
        }
        return true;
    }


    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.

        EnterSizes();
        System.out.printf("Hello and welcome!");
        scanOperations();
        fillRegisterFile(regReservations);
        Issue();
        for(int i =0;i<instructionTable.length;i++)
        {
            System.out.println("the instruction was issued at clock cycle: "+instructionTable[i][1]);
            System.out.println("the instruction was executed during clock cycles: "+(Integer)instructionTable[i][3]+"-"+(Integer)((Integer)instructionTable[i][3]+(Integer)instructionTable[i][2]-1));
            System.out.println("the instruction wrote result at clock cycle: "+instructionTable[i][4]+"\n");
        }


//        for (int i = 1; i <= 5; i++) {
//            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
//            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
//            System.out.println("i = " + i);
//        }
    }
}