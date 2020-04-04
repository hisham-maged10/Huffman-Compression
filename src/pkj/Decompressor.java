package pkj;

import pkj.Compressor.TreeNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

public class Decompressor {
    
	
	// immutable path of file to be compressed
    private final Path toDecompress;
    // content of file including line separators
    private StringBuilder content;
    // name of compressed File
    private String FileName;
    // serialized tree of compressed File
    private String serializedTree;
    //code that inside the compressed file
    private StringBuilder compressedCode;
    //decompressed Text
    private StringBuilder originText;
    //remainder bits of last character
    private int remainder;
    
    /**
     * String Constructor that takes file path and initializes the File to be Decompressed
     * @param filePath path of file to be compressed
     * @throws IOException throws a checked Exception so user remembers to handle IOExceptions
     * @throws InvalidPathException if path is invalid or doesn't exist
     * @throws NullPointerException if passed String is null
     */
    public Decompressor(String filePath) throws IOException
    {
        this(Paths.get(filePath));
    }

    /**
     * Path Constructor that takes the file object that points to the file to be Decompressed
     * @param toDecompress file to be compressed
     * @throws IOException throws a checked Exception so user remembers to handle IOExceptions
     * @throws InvalidPathException if path is invalid or doesn't exist
     */

    public Decompressor(Path toDecompress) throws IOException
    {
        if(!isValid(toDecompress))
            throw new IllegalArgumentException("Invalid file!");
        this.toDecompress = toDecompress;
        this.content = new StringBuilder();
        this.compressedCode = new StringBuilder();
        this.originText = new StringBuilder();
    }

    /**
     * validates the path to be of a valid file
     * @param toDecompress path that points to file
     * @return boolean to check if file is valid or not
     */
    private boolean isValid(Path toDecompress)
    {
        File f = toDecompress.toFile();
        return f.exists() && f.isFile();
    }
    
	/**
	 * read the decompressed file and put it in stringbuilder content 
	 * @throws IOException
	 */
    private void readFile() throws IOException {
    	File f = new File(this.toDecompress.toString());
		Scanner sc = new Scanner(f);
		int index = 0;
		while(sc.hasNext()) {
			String s = sc.nextLine();
			if(index == 0)
				this.FileName = s;
			else if(index  == 1){
				this.serializedTree = s;
			}
			else if(index == 2) {
				this.remainder = Integer.parseInt(s);
			}
			else if (index > 2){
			    this.compressedCode.append(s);
			}
			s = s + "\r\n";
			this.content.append(s);
			index++;
		}
			this.content.delete(this.content.length()-2, this.content.length()-1);
		sc.close();
    }
    /**
     * start decompress the file by reading the compressed file name and serialized tree
     * and deserialized it and return the node of tree
     * @throws IOException
     */
    public void decompress() throws IOException {
    	readFile();
    	System.out.println("FileName of compressed file: " + this.FileName);
    	System.out.println("Serialized Tree : " + this.serializedTree);
    	TreeNode root = deserialize(serializedTree);
       // System.out.println(compressedCode);
        StringBuilder compressedcodebin = substituteOfBin(compressedCode);
       // System.out.println(compressedcodebin);
        originText=OriginData(root,compressedcodebin);
        //System.out.println(originText);
        writeToFile(originText);
    }
    
    private StringBuilder substituteOfBin(StringBuilder compressedCode) {
		StringBuilder compreesedcodbin = new StringBuilder();
    	for(int i = 0;i<compressedCode.length();i++) {
    		short ascii = (short) (compressedCode.charAt(i));
    		if(ascii > 127) {
    			ascii = (short) (ascii - 128);
    		}
    		String bin  = Integer.toBinaryString(ascii);
    		if(remainder != 0 && i + 1 == compressedCode.length()) {
        		while(bin.length() < remainder)
        		    bin = 0 + bin;
    		    compreesedcodbin.append(bin);
        		System.out.println(bin);
    			break;
    		}
    		while(bin.length()<7) {
    			bin = 0 + bin;
    		}
    		compreesedcodbin.append(bin);
    	}
		
    	return compreesedcodbin;
    }
    
    /**
     * Deserializes the huffman tree and returns its root
     * @param serialized serialized huffman tree
     * @return root of deserialized huffman tree
     */
    private TreeNode deserialize(String serialized)
    {
        if(serialized == null || serialized.equals("null"))
            return null;
        String[] treeData = serialized.split("_,_");
        TreeNode root = new TreeNode(treeData[0].split("="));
        TreeNode curr = root;
        Deque<TreeNode> q = new ArrayDeque<>();
        int index = 1, size = 0;
        q.add(curr);
        String left = null, right = null;
        while(!q.isEmpty() && index < treeData.length)
        {
            size = q.size();
            for(int i = 0 ; i < size ; ++i)
            {
                curr = q.remove();
                left = index < treeData.length ? treeData[index++] : "null";
                right = index < treeData.length ? treeData[index++] : "null";
                if(!left.equals("null"))
                {
                    curr.left = new TreeNode(left.split("="));
                    q.add(curr.left);
                }
                if(!right.equals("null"))
                {
                    curr.right = new TreeNode(right.split("="));
                    q.add(curr.right);
                }
            }
        }
        return root;
    }

    /**
     * Traverse over the tree to get the origin text
     * @param root TreeNode containing root of the Huffmantree
     * @param input StringBuilder containing the string of compressed file
     * return the origin Text
     */
    private StringBuilder OriginData (TreeNode root,StringBuilder input ){
        TreeNode traceNode= root;
        StringBuilder output= new StringBuilder();
        for(int i= 0 ;i< input.length();i++){
            if(input.charAt(i) == '0'){
                traceNode=traceNode.left;
                if(traceNode.left== null && traceNode.right==null){
                    output.append(traceNode.character);
                    traceNode=root;
                }
            }else{
                traceNode=traceNode.right;
                if(traceNode.left== null && traceNode.right==null){
                    output.append(traceNode.character);
                    traceNode=root;
                }
            }
        }
        return output;
    }

    /**
     * writing to the decompressed  file
     * @param originData is the string builder have the data of the file in huffman code
     * @throws IOException
     */
    public void writeToFile(StringBuilder originData) throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter("decompressed.txt"));
        writer.append(originData);
        writer.close();
    }


}
