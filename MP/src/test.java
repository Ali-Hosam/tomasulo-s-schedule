import java.nio.ByteBuffer;

public class test {

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
    public static void main(String[] args) {
        float floatNumber = 2.98f;
        double doubleNumber = 3.555;

        // Serialize float
        byte[] floatBytes = serialize(floatNumber);
        System.out.println("Serialized float:");
        for (byte b : floatBytes) {
            System.out.printf("0x%02X ", b);
        }
        System.out.println();

        // Serialize double
        byte[] doubleBytes = serialize(doubleNumber);
        System.out.println("Serialized double:");
        for (byte b : doubleBytes) {
            System.out.printf("0x%02X ", b);
        }
        System.out.println();

        // Deserialize the numbers
        float deserializedFloat = deserializeFloat(floatBytes);
        double deserializedDouble = deserializeDouble(doubleBytes);

        // Print the results
//        System.out.println("Original float: " + floatNumber);
        System.out.println("Deserialized float: " + deserializedFloat);

//        System.out.println("Original double: " + doubleNumber);
        System.out.println("Deserialized double: " + deserializedDouble);
    }
}
