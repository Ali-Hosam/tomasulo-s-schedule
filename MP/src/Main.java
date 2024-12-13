//package org.example;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.nio.ByteBuffer;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static int missPenalty = 4;
    //the following are hard coding for the execution time for the instruction time
    static int addET = 3;
    static int subET = 3;
    static int mulET = 4;
    static int divET = 6;
    static int loadET = 1;
    static int storeET = 1;
    static int loadStoreET = 1;
    static int compulsoryAddress = -1;
    static String loadOperation;


    //    queue for the instruciton ready to write this will come in handy when more than one instruction finish exec
//    in the same clock cycle
    static Queue<String> readyToWrite = new LinkedList<>();
    static int instructionTableIndex = 0;
    //this instruction table is a consisted of 5 columns 1st column is for instruction shortcut for e.g A1 (A1 is 1st slot in A_RS)
    //2nd column is for issue clock cycle , 3rd column is for execution time for this instruction, 4th column is for start cycle of the execution
    //5th column is for write result , //note there may be two entries (rows) in the instruction table with having same instruction shortcut because it refers to the place in the RS
    //but it is impossible that both of them don't have their values written
    static Object[][] instructionTable;

    static boolean stall = false;

    //registers is an array holding the values of the registers .the index of the elements in the aray represent the register no.
    //TODO:the register files shouldn't be of fixed length
    static int[] addressRegisters = new int[20];
    static String[] addressReservations = new String[20];


    static float[] registers = new float[20];
    //regReservation is array of the same size as the registers holding name of the tag reserving the corresponding register or "free" otherwise
    static String[] regReservations = new String[20];
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
    static class CacheElement {
        byte value;
        boolean validity;

        public CacheElement() {
        }

        public CacheElement(byte value, boolean validity) {
            this.value = value;
            this.validity = validity;
        }
        public String toString(){
            return "value: " + this.value + ", validity: " + this.validity;
        }
    }

    static Object[][] A_RS;
    static Object[][] M_RS;
    static Object[][] loadBuffer;
    static Object[][] storeBuffer;
    //the memory and cash
    static byte memory[] = new byte[1024];
    static CacheElement cache[];
    //file to read
    static File Instructions = new File("C:\\Users\\LAPTOP\\IdeaProjects\\tomasulo-s-schedule\\MP\\src\\instructions.txt");

    public static void scanOperations() {

        try (Scanner scanner = new Scanner(Instructions)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                System.out.println(line);
                instructions.add(line);
                System.out.println("line is added to the instruction queue");
            }
            //initialize the length of the instruction table
            instructionTable = new Object[instructions.size()][5];
            System.out.println("instruction table" + instructionTable);

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }

    //initialization method for the reservation station that make the busy bit =0 initially
    public static void initRS(Object[][] RS) {
        for (int i = 0; i < RS.length; i++) {
            //initialize the busy column with zero
            RS[i][6] = (Integer) 0;
            //initialize the Q columns with zeros
//            RS[i][3]=(Integer)0;
//            RS[i][4]=(Integer)0;
        }
    }

    //initialization method for the store buffer
    public static void initStoreBuffer(Object[][] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i][1] = (Integer) 0;
        }
    }

    //initialization method for the load buffer
    public static void initLoadBuffer(Object[][] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i][1] = (Integer) 0;
        }
    }

    //initialization method for the registerfile by hardcoding all the registers with value 1 and the regReservation of all registers "free"
    public static void initRegisterFile(String[] RegReservation) {
        for (int i = 0; i < regReservations.length; i++) {
            regReservations[i] = "free";
//            addressReservations[i] = "free";
            registers[i] = 1.0f;
        }
    }

    //initialize cache with garbage values (as long as validity is false this means this cache entry is garbage) ---> it's a miss
    static void fillCache(CacheElement[] cache) {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = new CacheElement();
        }
    }

    //initialize a 1024-byte sized memory
    static void initMemory(){
        for(int i = 0; i < memory.length; i++){
            memory[i] = (byte)(i% 128);
            //this line to keep track for memory values we add % to ensure that no value is greater than 127
//            System.out.println("memory[i]: " + memory[i]);
        }
    }

    //method called when the main is run in order to enter the sizes of the reservation stations and buffers
    public static void init() {
        Scanner sc = new Scanner(System.in);
        System.out.println("please enter the size of the addition/subtraction reservation station");
        A_RS = new Object[sc.nextInt()][7];
        initRS(A_RS);
//        add_sub_size=sc.nextInt();
        System.out.println("please enter the size of the multiplication/division reservation station");
        M_RS = new Object[sc.nextInt()][7];
        initRS(M_RS);
//        mul_div_size=sc.nextInt();
        System.out.println("please enter the size of the load buffer");
        //edited the size of the buffers
        loadBuffer = new Object[sc.nextInt()][5];
        initLoadBuffer(loadBuffer);
//        load_buffer_size= sc.nextInt();
        System.out.println("please enter the size of the store buffer");
        storeBuffer = new Object[sc.nextInt()][5];
        initStoreBuffer(storeBuffer);
//        store_buffer_size= sc.nextInt();
        System.out.println("please enter the size of the cache");
        cache = new CacheElement[sc.nextInt()];
        fillCache(cache);
        //initialize memory
        initMemory();
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
    public static int checkRS(Object[][] RS) {
        for (int i = 0; i < RS.length; i++) {
            if ((Integer) RS[i][6] == 0)
                return i;
        }
        return -1;
    }

    //after checking the RS is available we can issue the instruction to the RS
    //we want to update the register file to set the dest. reg as reserved by the corresponding RS
    //the type is type of RS it will be used to reserve the dest. reg with this type
    //availablePlace is the free index of the reservation station
    private static void issueInstruction(Object[][] aRs, int availablePlace, String[] slots, char type) {
        //a tag/shortcut for the operation that will tag the dest. register and will be put in the instruction table
        String tag = "" + type + availablePlace;
        //put the tag/(instruction symbol) (for e.g:A1) in the instruction table
        instructionTable[instructionTableIndex][0] = tag;
        if(tag.contains("L"))
            System.out.println("the load tag is Put");
        //put the clock cycle the instruction was issued at
        instructionTable[instructionTableIndex][1] = clockCycle;
        System.out.println("the instruction index at the instruction table we are at right now is: " + instructionTableIndex + " and the its shortcut is: " + tag);

        //the first string of slots[] is consisted of two parts instruction operation and the dest. register so we split again
        //in order to get the dest. reg and type of the instruction seperately.
        String[] parts = slots[0].split(" ");
        loadOperation = parts[0];
        String typeOfInstruction = parts[0].substring(0, 3);
        aRs[availablePlace][0] = typeOfInstruction;
        //this is the dest. reg number that will be reserved with this current RS entry for e.g:A1
        int destReg = Integer.parseInt((parts[1].substring(1)));

        //this is for the load instruction
        if (typeOfInstruction.toLowerCase().charAt(0) == 'l') {
//            slots[1].trim();
            System.out.println("trying to get the destination in load inst");
//            int dest = Integer.parseInt(slots[1].substring(1));
            //reserve destination register in the register file

            System.out.println("reserved the register to load at");

            // the effective address
//            int address = Integer.parseInt(parts[1].substring(1));
            int address = Integer.parseInt(slots[1].trim());

//            loadBuffer[availablePlace][2] = dest;
            loadBuffer[availablePlace][3] = address;
            System.out.println("the loadBuffer's address is: "+loadBuffer[availablePlace][3]);
            //put the address you want to load from in the load Buffer
            //as DR. Milad said the address will always be a number
//            loadBuffer[availablePlace][2] = address;
            //check the address in the cache
            if(!checkAddress(address)) {
                //applying the compulsory penalty
                compulsoryAddress = address;
                System.out.println("________________________ cache miss ___________ penalty applied");
//                loadBlock(address);
            }
//            if(regReservations[destReg].equals("free")) {

                regReservations[destReg]=tag;

                loadBuffer[availablePlace][2] = registers[destReg];
                System.out.println("executing load");

                //reserve the register for the load
//                regReservations[destReg]=tag;
                executeInstruction(tag);

//            }
            loadBuffer[availablePlace][1]=1;

//            else loadBuffer[availablePlace][4]=regReservations[destReg];

        } else if(typeOfInstruction.charAt(0) == 's'){
            slots[1].trim();
            System.out.println("trying to get the destination in store inst");
            //TODO: the destination register mayn't be starting from the index 1
//            int dest = Integer.parseInt(slots[1].substring(1));

            System.out.println("reserved the store register");

            //calculating the effective address
            int address = Integer.parseInt(slots[1].trim());
            storeBuffer[availablePlace][3] = address;
            if(regReservations[destReg].equals("free")) {
                regReservations[destReg]=tag;
                System.out.println("the store register is free");
                storeBuffer[availablePlace][2] = registers[destReg];
                //reserve the reg for the store

                executeInstruction(tag);
            }
            else {
                storeBuffer[availablePlace][4]=regReservations[destReg];
                System.out.println("documented the tag reserving the register");
            }

//            executeInstruction(tag);
            storeBuffer[availablePlace][1]=1;
        }else {

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

    //check if the address is in the cache
    //TODO
    private static boolean checkAddress(int address) {
        //check for compulsory miss
        return cache[address%(cache.length)].validity;
    }

    //
//    TODO: this method might have other things to check rather than checking the instructions' execution
    public static void trackCycleChanges() {
        checkInstructionExecution();
    }

    // check execution of executing instructions
    // else part checks if any issued instruction can start execution
    public static void checkInstructionExecution() {
        //this checks the issued instructions that is entered to the instruction table
        for (int i = 0; i < instructionTable.length && instructionTable[i][0] != null; i++) {
            //this if condition checks if the instruction didn't write a result , if not so then there is no point of checking it
            //because this instruction is finished already
            if (instructionTable[i][4] == null) {
                //if the third column has value in it and it is not null then the instruction started executing
                if (instructionTable[i][3] != null) {
                    //checks if this is the clock cycle that the instruction execution is finished and can and can write in it
                    if (clockCycle == (Integer) ((Integer) instructionTable[i][2] + (Integer) instructionTable[i][3])) {
//                        instructionTable[i][4]=clockCycle;
                        //add this inst to a write buffer in case there is more than one inst that can write at the same clockCycle
                        readyToWrite.add((String) instructionTable[i][0]);
                        System.out.println("instruction: " + instructionTable[i][0] + " the duration: " + instructionTable[i][2] + " start exec: " + instructionTable[i][3]);
                    }


                } else {
                    checkIssuedInstruction();
                }

            }

        }
        String tag;
        if (readyToWrite.size() > 0) {
            //tag
            tag = readyToWrite.remove();
            writeResult(tag);


//                executeInstruction(tag);
            System.out.println(tag);
            System.out.println("-------------------------entered the replace");
            replace(tag);
            checkIssuedInstruction();
        }
    }

    public static void writeResult(String tag) {
        for (int i = 0; i < instructionTable.length&&instructionTable[i][0]!=null; i++) {
            if (instructionTable[i][0].equals(tag)) {
                instructionTable[i][4] = clockCycle;
            }
        }
    }

    public static void checkIssuedInstruction() {
        for (int i = 0; i < instructionTable.length && instructionTable[i][0] != null; i++) {
            //check if this instruction started execution
            if (instructionTable[i][2] == null) {
                System.out.println(instructionTable[i][0]);
                String tag = (String) instructionTable[i][0];
                //know the index I will search in the reservation station
                System.out.println("here is the tag " + tag.substring(1));
                int index = Integer.parseInt(tag.substring(1));
//                System.out.println(tag.charAt(0) == 'A');
                if (tag.charAt(0) == 'M') {
                    System.out.println("om el tag aho " + tag);
                    //check if the Qj and Qk are empty
                    if (M_RS[index][3] == null && M_RS[index][4] == null) {
//                    String instShortcut = (String) M_RS[index][0];
                        System.out.println("calling EX from the check issued inst.");
                        System.out.println("om el tag aho " + tag);
                        executeInstruction(tag);
                    }
                } else if (tag.charAt(0) == 'A') {
                    System.out.println("om el tag aho " + tag);
                    //check if the Qj and Qk are empty
                    if (A_RS[index][3] == null && A_RS[index][4] == null) {
//                        String instShortcut = (String) A_RS[index][0];
                        System.out.println("calling EX from the check issued inst.");
                        System.out.println("om el tag aho " + tag);
                        executeInstruction(tag);
                    }
                }
//                else if(tag.toLowerCase().charAt(0) == 'l'){
//                    String value= ((String)loadBuffer[index][4]);
//                    if (!value.toLowerCase().contains("a")||!value.toLowerCase().contains("m"))
//                    {
//                        System.out.println();
//                        executeInstruction(tag);
//                    }
//                }

            }


        }

    }

    public static void replace(String tag) {

        float value=0.0F ;
        int index = Integer.parseInt(tag.substring(1));
        switch (tag.charAt(0)) {
            case 'M':
                value = (Float) M_RS[index][5];
                M_RS[index][6]=0;
                break;
            case 'A':
                value = (Float) A_RS[index][5];
                A_RS[index][6]=0;
                break;
            case 'L':
//                System.out.println("got the value of the load: "+value);
                value = (Byte)loadBuffer[index][2];
                loadBuffer[index][1]=0;
                System.out.println("got the value of the load: "+value);
                break;
//                break;
//            case 'S':
//                value = (Float) storeBuffer[index][2];

        }
        //get the index(number) of register that the instruction was reserving at the register filr
        int reservationIndex = getReservationIndex(tag);
        //returning -1 means that the register was reserved later by another inst. rather than the one is being replaced now
        if (reservationIndex != -1) {
            registers[reservationIndex] = value;
            regReservations[reservationIndex] = "free";
            System.out.println("freed the reserved register");
        }
        for (int i = 0; i < M_RS.length; i++) {
            if (M_RS[i][3] != null && ((String) (M_RS[i][3])).equals(tag)) {
//                M_RS[i][3]=0;
                M_RS[i][3] = null;
                M_RS[i][1] = value;
                System.out.println("freed the RS slot");
            }
            if (M_RS[i][4] != null && ((String) (M_RS[i][4])).equals(tag)) {
//                M_RS[i][4]=0;
                M_RS[i][4] = null;
                M_RS[i][2] = value;
                System.out.println("freed the RS slot");

            }

        }
        for (int i = 0; i < A_RS.length; i++) {
            if (A_RS[i][3] != null && ((String) (A_RS[i][3])).equals(tag)) {
//                A_RS[i][3]=0;
                A_RS[i][3] = null;
                A_RS[i][1] = value;
                System.out.println("freed the RS slot");

            }
            if (A_RS[i][4] != null && ((String) (A_RS[i][4])).equals(tag)) {
//                A_RS[i][4]=0;
                A_RS[i][4] = null;
                A_RS[i][2] = value;
                System.out.println("freed the RS slot");

            }
        }
        //TODO,TODO
//        for(int i =0;i<loadBuffer.length;i++){
//            if (loadBuffer[i][4] != null && ((String) (loadBuffer[i][4])).equals(tag)) {
//                loadBuffer[i][2]=value;
//            }
//        }
        //TODO
        for(int i =0;i<storeBuffer.length;i++){
            if (storeBuffer[i][4] != null && ((String) (storeBuffer[i][4])).equals(tag)) {
                storeBuffer[i][2]=value;
            }
        }



    }

    public static int getReservationIndex(String shortcut) {
        int index = -1;
        for (int i = 0; i < regReservations.length; i++) {
            if (regReservations[i].equals(shortcut)) {
                index = i;
                break;
            }
        }
        return index;
    }

    static void missCallBack(int compulsoryAddress, char indicator){
        if (indicator == 'w')
            for (int i = 0; i < 4; i++) {
                int effectiveAddress = compulsoryAddress + i;
                cache[effectiveAddress%cache.length].value = memory[effectiveAddress];
                cache[effectiveAddress%cache.length].validity = true;
                System.out.print(effectiveAddress%cache.length + ", ");
            }
        else if (indicator == 'd') {
            for (int i = 0; i < 8; i++) {
                int effectiveAddress = compulsoryAddress + i;
                cache[effectiveAddress%cache.length].value = memory[effectiveAddress];
                cache[effectiveAddress%cache.length].validity = true;
                System.out.print(effectiveAddress%cache.length + ", ");
            }
        }
        System.out.println();
    }

    public static void executeInstruction(String tag) {
        System.out.println("the instruction ya rayyes is: " + tag);
        int ET = 0;
        int index = Integer.parseInt(tag.substring(1, 2));
        int reservationIndex = 0;
        System.out.println("yastaaaaaaaaaaaaaaaaaaaaaa el tag is: " + tag);
        System.out.println("yastaaaaaaaaaaaaaaaaaaaaaa el index is: " + index);
//        System.out.println(tag.charAt(0) == 'A');
//        System.out.println(((String) M_RS[index][0]).toLowerCase().equals("add"));
//        System.out.println(((String) M_RS[index][0]).toLowerCase());

//        for (int i = 0; i < regReservations.length; i++) {
//            if (regReservations[i].equals(tag)) {
//                reservationIndex = i;
//                break;
//            }
//        }
        if (tag.charAt(0) == 'M') {
//            System.out.println(index);
            if (((String) M_RS[index][0]).toLowerCase().contains("mul")) {
                ET = mulET;
//            registers[reservationIndex][1]=(Float)M_RS[index][2]*(Float)M_RS[index][3];
                M_RS[index][5] = (Float) ((Float) M_RS[index][1] * (Float) M_RS[index][2]);
                System.out.println("multiplying operands");
                System.out.println((Float)M_RS[index][5]);
            }
            else if (((String) M_RS[index][0]).toLowerCase().contains("div")) {
                ET = divET;
//            registers[reservationIndex][1]=(Float)((Float)M_RS[index][2]/(Float)M_RS[index][3]);
                M_RS[index][5] = (Float) ((Float) M_RS[index][1] / (Float) M_RS[index][2]);
                System.out.println("dividing operands");

            }
        } else if (tag.charAt(0) == 'A') {

            if (((String) A_RS[index][0]).toLowerCase().contains("add")) {
                ET = addET;
//            registers[reservationIndex][1]=(Float)A_RS[index][2]+(Float)A_RS[index][3];
                A_RS[index][5] = (Float) ((Float) A_RS[index][1] + (Float) A_RS[index][2]);
                System.out.println("adding operands");
                System.out.println((Float)A_RS[index][5]);

            }
            else if (((String) A_RS[index][0]).toLowerCase().contains("sub")) {
                ET = subET;
//            registers[reservationIndex][1]=(Float)M_RS[index][2]-(Float)M_RS[index][3];
                A_RS[index][5] = (Float) A_RS[index][1] - (Float) A_RS[index][2];
                System.out.println("subtracting operands");

            }
        }
        else if (tag.toLowerCase().charAt(0)=='l'/*((String) loadBuffer[index][0]).toLowerCase().contains("l")*/){
//            System.out.println("found the load at clock cycle "+clockCycle);
            if(compulsoryAddress != -1){
                ET = missPenalty;
//                String arr [] = loadOperation.split(" ");
                //to indicate wether it's a word "w" or double word "d"
                char indicator = loadOperation.toLowerCase().charAt(loadOperation.length()-1);
                System.out.println("miss indicator is: " + (char)indicator + " and compulsory address is: " + compulsoryAddress );
                missCallBack(compulsoryAddress, indicator);
                compulsoryAddress = -1;
            }
            else
                ET = loadET;

            //loading the value in the dest. register
            //put the value from the memory into the value placeholder in the load buffer
            //TODO:after constructing the cache the loadBuffer[index][2] should have the value in the address 100 from the cache
            loadBuffer[index][2] = cache[(Integer)loadBuffer[index][3]%cache.length].value;
            System.out.println("the value to be put in the inst table: "+cache[(Integer)loadBuffer[index][3]%cache.length]);
            System.out.println("b7bak ya 7ot " + cache[(Integer)loadBuffer[index][3]%cache.length]+" the et is "+ET);
//            System.out.println("value in the corresponding placeholder in loadBuffer: "+loadBuffer[index][2]);

        }
        else if(((String) A_RS[index][0]).toLowerCase().contains("s")){
            ET=loadET;
//            System.out.println("el execution ");
            //TODO:after constructing the cache thevalue of storeBuffer[index][2]should be stored in
            // the address (storeBuffer[index][3]) in the cache
            /*cache[storeBuffer[index][3]]*/storeBuffer[index][3] =101;//storeBuffer[index][2];
//            ET=storeET;
//            //loading the value in the dest. register
//            loadBuffer[index][2] =registers[(Integer)loadBuffer[index][3]];
        }

        //search the corresponding entry in the instructionTable
        for (int j = 0; j < instructionTable.length&&instructionTable[j][0]!=null; j++) {
//            check the corresponding entry in the instructionTable
            if (((String) instructionTable[j][0]).equals(tag) && instructionTable[j][4] == null) {
//            System.out.println("entered the if condition");

//                the execution time needed by the instruction
                instructionTable[j][2] = ET;
                System.out.println("execution time of: "+tag+" is " + ET);
//                the start cycle of the execution
                instructionTable[j][3] = clockCycle + 1;
                System.out.println("the clockCycle now is  "+clockCycle);
                break;
            }

        }

    }

    public static int checkLoadBuffer(Object[][] loadBuffer) {
        for (int i = 0; i < loadBuffer.length; i++) {
            if ((Integer) loadBuffer[i][1] == 0)
                return i;
        }
        return -1;
    }
    public static int checkStoreBuffer(Object[][] storeBuffer) {
        for (int i = 0; i < storeBuffer.length; i++) {
            if ((Integer) storeBuffer[i][1] == 0)
                return i;
        }
        return -1;
    }

    public static void Issue() {



        while (clockCycle < 30/*instructions.size()>0/*||!stall*/) {
            clockCycle++;
            trackCycleChanges();


            if (!stall && instructions.size() > 0) {
                String instruction = instructions.peek();

                instruction.trim();
//                System.out.println(instruction);
                String[] slots = instruction.split(",");


                //I am not sure about the .toLowerCase if it will work or not
                System.out.println("the operation is : " + slots[0]);
                System.out.println(slots[0].toLowerCase().substring(0, 3));
//    instructions.remove();
                System.out.println("the size of the instruction queue is : " + instructions.size());

                //check if the instruction is store
                if (slots[0].toLowerCase().charAt(0) == 's') {
                    System.out.println("the operation is store");
                    int availablePlace = checkStoreBuffer(storeBuffer);
                    System.out.println("the size of the store buffer is : " + storeBuffer.length);
//                System.out.println(""+ slots.length+"");
                    if (availablePlace == -1) {
                        System.out.println("no available places in the store buffer");
                        stall = true;
                    }
                    else {
//                    if(typeOfInstruction.charAt(0)=='l')
//                    {
                        //
                        slots[1].trim();
                        int address;
                        System.out.println("I have the address");
//                        int registerNumber = Integer.parseInt(slots[1].substring(1));
                        //TODO
                        address = 100;//addressRegisters[registerNumber];

                        if (!checkBufferAddress(loadBuffer,address)||!checkBufferAddress(storeBuffer,address)) {

                            System.out.println("the issuing is stalled");
                            stall = true;
                        }
                        else {
                            instructions.remove();
                            System.out.println("issued the inst store at: "+clockCycle);
                            issueInstruction(storeBuffer,availablePlace,slots,'S');
                        }


//                    }

                    }
                }
                //check if the instruction is load
                else if (slots[0].toLowerCase().charAt(0) == 'l') {
                    System.out.println("the operation is load");
                    int availablePlace = checkLoadBuffer(loadBuffer);
                    System.out.println("the size of the load buffer is : " + loadBuffer.length);
//                System.out.println(""+ slots.length+"");
                    if (availablePlace == -1) {
                        System.out.println("no available places in the load buffer");
                        stall = true;
                    } else {
//                    if(typeOfInstruction.charAt(0)=='l')
//                    {
                        //
                        slots[1].trim();
                        int address;
                        System.out.println("I have the address");
//                        int registerNumber = Integer.parseInt(slots[1].substring(1));
                        //TODO
//                        address = 100;//addressRegisters[registerNumber];
                        address = Integer.parseInt(slots[1].trim());
                        System.out.println("address slot " + address);
                        if (!checkBufferAddress(storeBuffer,address)) {
                            System.out.println("the issuing is stalled");
                            stall = true;
                        }
                        else {

                            instructions.remove();
                            System.out.println("issued the inst load at: "+clockCycle);
                            issueInstruction(loadBuffer,availablePlace,slots,'L');
                            instructionTableIndex++;
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
                        stall = true;

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
                        stall = true;
                    } else {
                        instructions.remove();
                        issueInstruction(M_RS, availablePlace, slots, 'M');
                        instructionTableIndex++;

                    }
                }
//        clockCycle++;

            } else {
                System.out.println("the instructions issuing is stalled");
//            break;
            }
        }


    }
    //TODO
    public static boolean checkBufferAddress(Object[][]buffer,int address) {
        for (int i = 0; i < buffer.length; i++) {
            if((Integer)buffer[i][1]!=0){
                if ((Integer) buffer[i][3] == address) {
                    return false;
                }
            }
        }
        return true;
    }




    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.

        init();

        scanOperations();
        initRegisterFile(regReservations);
        Issue();
        for (CacheElement c: cache){
            System.out.println(c);
        }
        for (int i = 0; i < instructionTable.length; i++) {
            System.out.println("the instruction: "+instructionTable[i][0]);
            System.out.println("the instruction was issued at clock cycle: " + instructionTable[i][1]);
            System.out.println("the instruction was executed during clock cycles: " + (Integer) instructionTable[i][3] + "-" + (Integer) ((Integer) instructionTable[i][3] + (Integer) instructionTable[i][2] - 1));
            System.out.println("the instruction wrote result at clock cycle: " + instructionTable[i][4] + "\n");
            //edited
            System.out.println(cache[4].validity + " "/*+ cache[5].validity*/);
        }
    }
}
