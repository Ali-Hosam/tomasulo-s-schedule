//package org.example;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static float[] registers=new float[20];
//    static ArrayList<Float>registers =new ArrayList<>(100);
    //clock cycles
    static int clockCycle;
    static Queue<String> instructions = new LinkedList<>();
//that is how you initialize a queue
    //each array of String is of length 5 one column for instruction , one for Issue, one for execution, one for write result
//    static ArrayList<Object[]> instructions;

    static Object[][] A_RS;
    static Object[][] M_RS;
    static Object[][]loadBuffer;
    static Object[][]storeBuffer;

//    static Queue<String> add_sub;
//    static Queue<String> mul_div;
//    static Queue<String> load_buffer;
//    static Queue<String> store_buffer;


//    static int add_sub_size=0;
//    static int mul_div_size=0;
//    static int load_buffer_size=0;
//    static int store_buffer_size=0;

    //file to read
    static File Instructions =new File("c:/Users/Ali Hoss/Desktop/DbEngine (3)/DbEngine/MP/src/instructions.txt");

    public static void scanOperations()
    {
//        registers.add((float)3.7);
//        registers.add((float)3.7);
//        registers.add((float)3.7);
//        registers.add((float)3.7);
//        registers.add((float)3.7);

        try (Scanner scanner = new Scanner(Instructions)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                System.out.println(line);
                instructions.add(line);
                System.out.println("line is added to the instruction queue");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }


    public static void EnterSizes(){
        Scanner sc =new Scanner(System.in);
        System.out.println("please enter the size of the addition/subtraction reservation station");
        A_RS=new Object[sc.nextInt()][6];
        //fillRS(A_RS);
//        add_sub_size=sc.nextInt();
        System.out.println("please enter the size of the multiplication/division reservation station");
        M_RS=new Object[sc.nextInt()][6];
        //fillRS(M_RS);
//        mul_div_size=sc.nextInt();
        System.out.println("please enter the size of the load buffer");
        loadBuffer=new Object[sc.nextInt()][2];
        //fillBuffer(loadBuffer);
//        load_buffer_size= sc.nextInt();
        System.out.println("please enter the size of the store buffer");
        storeBuffer=new Object[sc.nextInt()][3];
        //fillBuffer(storeBuffer);
//        store_buffer_size= sc.nextInt();

    }

public static void Issue()
{

    clockCycle=0;

    String instruction=instructions.peek();

    instruction.trim();
    String[]slots=instruction.split(",");
    for(String slot:slots)
    {
        slot.trim();
    }
    if(slots[0].charAt(0)=='s')
    {
//        checkBuffer();
    }

}
//    public static void Issue()
//    {
//        String inst=instruction.peek();
////        String add="add";
////        String sub="sub";
////        String mul="mul";
////        String div="div";
//        String instOp=inst.substring(0,2);
//
//        if(instOp=="ADD"||instOp=="SUB")
//        {
//            if(A_RS.length==add_sub_size)
//            {
//                //stall the instruction until some instruction is executed
//            }
//            else
//                add_sub.add(inst);
//        }
//        else if(instOp=="MUL"||instOp=="DIV")
//        {
//            if(mul_div.size()==mul_div_size)
//            {
//                //stall the instruction until some instruction is executed
//            }
//            else
//                mul_div.add(inst);
//        }
//        else if(inst.charAt(0)=='L')
//        {
//            if(load_buffer.size()==load_buffer_size)
//            {
//                //stall the instruction until some instruction is executed
//            }
//            else
//                load_buffer.add(inst);
//        }
//
//        else if(inst.charAt(0)=='S')
//        {
//            if(store_buffer.size()==store_buffer_size)
//            {
//                //stall the instruction until some instruction is executed
//            }
//            else
//                store_buffer.add(inst);
//        }
//
//    }


    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.

        EnterSizes();
        System.out.printf("Hello and welcome!");
        scanOperations();


//        for (int i = 1; i <= 5; i++) {
//            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
//            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
//            System.out.println("i = " + i);
//        }
    }
}