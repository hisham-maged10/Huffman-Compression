package pkj;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {

        Scanner sc=new Scanner(System.in);
        System.out.println("1 - Compression");
        System.out.println("2 - deCompression");
        System.out.print("Enter : ");

        int choose= sc.nextInt();

        switch (choose){
            case 1:
                    Compressor test = new Compressor("test_huffman.txt");
                    test.compress("bla.txt");
                    double ratio = test.compressionRatio("test_huffman.txt", "bla.txt");
                    ratio = Double.parseDouble(new DecimalFormat("##.##").format(ratio));
                    System.out.println("Compresion Ratio = " + ratio + "%");
                break;
            case 2:
                Decompressor dc = new Decompressor("bla.txt");
                dc.decompress();

        }
    }

}

