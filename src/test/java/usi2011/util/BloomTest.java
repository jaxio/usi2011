package usi2011.util;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class BloomTest {

    @Test
    public void p() {
        double falsePositiveProbability = 0.1;
        int expectedSize = 1000000;

        BloomFilter<String> bloomFilter = new BloomFilter<String>(falsePositiveProbability, expectedSize);
        String keyword = "abcdef..";

        for (int i = 0; i < 1000000; i ++) {
            bloomFilter.add(RandomStringUtils.randomAscii(40));
        }

        if (bloomFilter.contains(keyword)) {
            System.out.println("BloomFilter contains \"" + keyword + "\" with probability " + (1 - bloomFilter.expectedFalsePositiveProbability()));
        }
        for (int i = 0; i < 1000000; i ++) {
            String r = RandomStringUtils.randomAscii(40);
            if (bloomFilter.contains(r)) {
                System.out.println("BloomFilter contains \"" + r + "\" with probability " + (1 - bloomFilter.expectedFalsePositiveProbability()));
            }
//            bloomFilter.add(RandomStringUtils.randomAscii(40));
 //           System.out.println("BloomFilter contains \"" + keyword + "\" with probability " + (1 - bloomFilter.expectedFalsePositiveProbability()));
        }
        
        try {
            // Serialize to a file
            ObjectOutput out = new ObjectOutputStream(new FileOutputStream("filename.ser"));
            out.writeObject(bloomFilter);
            out.close();

            // Serialize to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
            out = new ObjectOutputStream(bos) ;
            out.writeObject(bloomFilter);
            out.close();

            // Get the bytes of the serialized object
            byte[] buf = bos.toByteArray();
            System.out.println(buf.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
