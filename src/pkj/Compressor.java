package pkj;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * Compresses input file using huffman's tree
 */
public class Compressor {

    /**
     * represents a Huffman tree node that
     * is used through out the project to Compress the file using
     * Huffman's Algorithm and is a static class for ease of use
     */
    protected static class TreeNode implements Comparable<TreeNode>
    {
        protected int freq;
        protected char character;
        TreeNode left,right;

        /**
         * Initializes the TreeNode as leaf node ( null left and right nodes )
         * @param character character it represents
         * @param freq frequency of that character
         */
        protected TreeNode(char character, int freq)
        {
            this.character = character;
            this.freq = freq;
        }
        /**
         * Initializes the TreeNode as leaf node ( null left and right nodes )
         * useful for deserialization step
         * @param data String[] containing node data
         */
        protected TreeNode(String[] data)
        {
            this(data[0].length() == 0 ? '\u0000' : (char)Integer.parseInt(data[0]), Integer.valueOf(data[1]));
        }

        /**
         * Compares the two huffman nodes based on frequency of characters, used for sorting ascendingly
         * used in min binary heap to form the huffman tree
         * @param anotherTreeNode TreeNode to be compared against
         * @return int that determines if this < anotherTreeNode (-ve value) this == anotherTreeNode (0) or +ve value if this > anotherTreeNode
         */
        @Override
        public int compareTo(TreeNode anotherTreeNode)
        {
            return Integer.compare(this.freq,anotherTreeNode.freq);
        }

        /**
         * Represents the node in character=freq string form which is useful in serialization of tree
         * @return String representation of TreeNode
         */
        @Override
        public String toString()
        {
            return (int)this.character + "=" + this.freq;
        }
    }

    // immutable path of file to be compressed
    private final Path toCompress;
    // content of file including line separators
    private StringBuilder content;
    //have the remainder of last bits in file if they are not 8
    private int remainder;
    
    /**
     * String Constructor that takes file path and initializes the File to be compressed
     * @param filePath path of file to be compressed
     * @throws IOException throws a checked Exception so user remembers to handle IOExceptions
     * @throws InvalidPathException if path is invalid or doesn't exist
     * @throws NullPointerException if passed String is null
     */
    public Compressor(String filePath) throws IOException
    {
        this(Paths.get(filePath));
    }

    /**
     * Path Constructor that takes the file object that points to the file to be compressed
     * @param toCompress file to be compressed
     * @throws IOException throws a checked Exception so user remembers to handle IOExceptions
     * @throws InvalidPathException if path is invalid or doesn't exist
     */
    public Compressor(Path toCompress) throws IOException
    {
        if(!isValid(toCompress))
            throw new IllegalArgumentException("Invalid file!");
        this.toCompress = toCompress;
        this.content = new StringBuilder();

    }

    /**
     * validates the path to be of a valid file
     * @param toCompress path that points to file
     * @return boolean to check if file is valid or not
     */
    private boolean isValid(Path toCompress)
    {
        File f = toCompress.toFile();
        return f.exists() && f.isFile();
    }

    /**
     * Compresses the given file and outputs it in given outputName file
     * by generating character frequency mapping for characters in input
     * then building the huffman tree then using the huffman tree
     * to output mapping of chars and their huffman code
     * then serializes the huffman tree to be added to header
     * @param outputName name of file to output compressed data in
     */
    public void compress(String outputName) throws IOException
    {
        Map<Character,Integer> freqMap = generateFreqMap(); // a -> 5
        TreeNode huffmanTreeRoot = buildHuffmanTree(freqMap); // root
        Map<Character,String> codeMap = new HashMap<>(); // a -> 0100
        generateCodeMap(huffmanTreeRoot,codeMap,"");
        System.out.println(codeMap);
        String serializedTree = serialize(huffmanTreeRoot); // [5,4,7,3,10]
        System.out.println(serializedTree);
        System.out.println(serialize(deserialize(serializedTree))); // deserialize
        
        StringBuilder compressedData = substituteOfData(content, codeMap);
        StringBuilder charcompressedData = substituteOfDataChar(compressedData);
        System.out.println();
       // System.out.println(compressedData);
       // System.out.println(charcompressedData);
        writeToFile(outputName,serializedTree,charcompressedData);
    }
    
    
    /**
     * Generates character frequency map based on file given to CTOR
     * by reading input line by line and for each line frequency of chars is computed
     * @return character frequency mapping
     * @throws IOException
     */
    private Map<Character,Integer> generateFreqMap() throws IOException
    {
        Map<Character,Integer> freqMap = new HashMap<>();
        List<String> lines = Files.readAllLines(toCompress);
        lines.forEach((line) -> fillMap(line,freqMap));
        int count = lines.size();
        if(count > 1)
        {
            freqMap.put('\r',(int)count - 1);
            freqMap.put('\n',(int)count - 1);
        }
        return freqMap;
    }

    /**
     * Fills character frequency map with freq of characters for given line
     * @param s Line in the file
     * @param freqMap map to be filled
     */
    private void fillMap(String s, Map<Character,Integer> freqMap)
    {
        this.content.append(s+"\r\n");
        for(int i = 0 , n = s.length() ; i < n ; ++i)
            freqMap.put(s.charAt(i), freqMap.getOrDefault(s.charAt(i), 0) + 1);
    }

    /**
     * Builds huffman's tree using a min binary heap using the character frequency mapping
     * @param freqMap Character frequency Mapping
     * @return Root of huffman's Tree
     */
    private TreeNode buildHuffmanTree(Map<Character,Integer> freqMap)
    {
        Queue<TreeNode> minHeap = new PriorityQueue<>();
        freqMap.entrySet().forEach(entry -> {
            TreeNode node = new TreeNode(entry.getKey(),entry.getValue());
            minHeap.add(node);
        });
        TreeNode huffmanTreeRoot = null;
        TreeNode first = null, second = null, sum = null;
        while(minHeap.size() != 1)
        {
            first = minHeap.remove();
            second = minHeap.remove();
            sum = new TreeNode('\u0000',first.freq + second.freq);
            sum.left = first; sum.right = second;
            minHeap.add(sum);
            huffmanTreeRoot = sum;
        }
        return huffmanTreeRoot;
    }

    /**
     * Generates and fills Code Mapping of char and its huffman code
     * using recursive in-order dfs assigning 0 to left along the way till it reaches leaf node
     * puts char and its code then removes last 0 and pushes right if exists with 1
     * @param huffmanTreeRoot root of built huffman tree
     * @param codeMap Mapping of each character to its huffman code
     * @param huffmanTreeRoot code generated at each stack frame for left and right
     */
    private void generateCodeMap(TreeNode huffmanTreeRoot,Map<Character,String> codeMap, String code)
    {
        if(huffmanTreeRoot.left == null && huffmanTreeRoot.right == null && huffmanTreeRoot.character != '\u0000')
        {
            codeMap.put(huffmanTreeRoot.character,code);
            return;
        }
        if(huffmanTreeRoot.left != null) generateCodeMap(huffmanTreeRoot.left,codeMap,code + "0");
        if(huffmanTreeRoot.right != null) generateCodeMap(huffmanTreeRoot.right,codeMap,code + "1");

    }

    /**
     * Serializes the tree in level order traveral (BFS) in form parent > left > right till max level leaf nodes
     * delimiter is _,_ so if , or _ exists as a character
     * @param root root of huffman tree to be serialized
     * @return serialized tree string
     */
    private String serialize(TreeNode root)
    {
        if(root == null)
            return "null";
        Queue<TreeNode> q = new LinkedList<>();
        int size = 0;
        TreeNode curr = root;
        boolean allnulls = false;
        q.add(curr);
        StringBuilder serialzed = new StringBuilder();
        while(!q.isEmpty() && !allnulls)
        {
            size = q.size();
            allnulls = true;
            for(int i = 0 ; i < size ; ++i)
            {
                curr = q.remove();
                if(curr == null) {
                    serialzed.append("null_,_");
                    continue;
                }
                serialzed.append(curr+"_,_");
                if(curr.left != null || curr.right != null)
                    allnulls = false;

                q.add(curr.left);
                q.add(curr.right);
            }
        }
        for(int i = 0 ; i < 3 ; ++i)
            serialzed.deleteCharAt(serialzed.length()-1);
        return serialzed.toString();
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
     *substitute the data in the string content with its code 
     *substitution by done char by char and get the code from hashmap 
     * @param content string builder have all contents of file in it
     * @param codeMap map have the character and it code
     * @return string builder have the content of the file with huffman code
     */
    public StringBuilder substituteOfData(StringBuilder content,Map<Character, String> codeMap) {
    	StringBuilder substituted = new StringBuilder();
    	for(int i =0;i<content.length();++i) {
    		if(!codeMap.containsKey(content.charAt(i))) {
    			continue;
    		}
    		substituted.append(codeMap.get(content.charAt(i)));
    	}
    	return substituted;
    }
    
    /**
     * writing to the compressed file the tree and the hashmap in the header
     * and  the data with its code 
     * @param outputName is the name of the output file
     * @param serializedTree is the string of the array in bfs
     * @param compressedData is the string builder have the data of the file in huffman code
     * @throws IOException
     */
    public void writeToFile(String outputName, String serializedTree,StringBuilder compressedData) throws IOException{
    	BufferedWriter writer = new BufferedWriter(new FileWriter(outputName));
    	writer.write(this.toCompress.toString());
    	writer.append("\r\n");
    	writer.append(serializedTree);
    	writer.append("\r\n");
    	writer.append(Integer.toString(remainder));
    	writer.append("\r\n");
    	writer.append(compressedData);
    	writer.close();
    }
    
    /**
     * take every 7 bits in the binary string and change them to character to compress the file
     * @param compressedData
     * @return
     */
    private StringBuilder substituteOfDataChar(StringBuilder compressedData) {
    	StringBuilder  ascii = new StringBuilder();  
    	String comps = compressedData.toString();
    	StringBuilder sb = new StringBuilder();
 		int count = 1;
 		
    	for(int i = 0;i<comps.length();i++) {
    		if(count <= 7) {
    			sb.append(comps.charAt(i));
    		}
    		else {
    			--i;
    			short decimal = Short.parseShort(sb.toString(),2); 		//to convert 10101111 to decimal for to get the ascii
        		char subchar = (char)(decimal);
        		if(subchar == 0 || subchar == 8 || subchar == 9 || subchar == 10 || subchar == 13 ) {
        			subchar = (char) (decimal + 128);
        		}
        		ascii.append(subchar);
    			sb.setLength(0);
    			count = 0;
    		}
    		if(i + 1 == comps.length()) {
    			remainder = count % 7;
    			short decimal = Short.parseShort(sb.toString(),2); 		//to convert 10101111 to decimal for to get the ascii
        		char subchar = (char)(decimal);
        		if(subchar == 0 || subchar == 8 || subchar == 9 || subchar == 10 || subchar == 13 ) {
        			subchar = (char) (decimal + 128);
        		}
        		ascii.append(subchar);
    			sb.setLength(0);
    		}
    		count++;
    	}
    	return ascii;
    }

    public double compressionRatio(String normal, String compressed){
        File normalFile = new File(normal);
        File compressedFile = new File(compressed);

        double normalSize= (double) normalFile.length() / 1024 ;
        double compressedSize= (double) compressedFile.length() / 1024 ;

        return  (compressedSize/normalSize)*100;

    }
}
