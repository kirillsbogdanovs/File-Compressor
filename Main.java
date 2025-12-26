import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String command;
        String sourceFile, resultFile, firstFile, secondFile;

        loop: while (true) {

            command = sc.next();

            switch (command) {
                case "comp":
                    // LZW encode start
                    System.out.println("LZW encode");
                    System.out.print("source file name: ");
                    sourceFile = sc.next();
                    System.out.print("archive name: ");
                    resultFile = sc.next();
                    compress(sourceFile, resultFile);
                    // LZW encode end
                    break;
                case "decomp":
                    // LZW decode start
                    System.out.println("LZW decode");
                    System.out.print("archive name: ");
                    sourceFile = sc.next();
                    System.out.print("file name: ");
                    resultFile = sc.next();
                    decompress(sourceFile, resultFile);
                    // LZW decode end
                    break;
                case "size":
                    System.out.print("file name: ");
                    sourceFile = sc.next();
                    filesize(sourceFile);
                    break;
                case "equal":
                    System.out.print("first file name: ");
                    firstFile = sc.next();
                    System.out.print("second file name: ");
                    secondFile = sc.next();
                    System.out.println(equal(firstFile, secondFile));
                    break;
                case "exit":
                    break loop;
            }
        }

        sc.close();
    }
    public static byte[] bitStringToByteArray(String bits) {
        BitSet bs = new BitSet(bits.length());
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') bs.set(i);
        }
        return bs.toByteArray();
    }

    public static String byteArrayToBitString(byte[] bytes, int bitLen) {
        BitSet bs = BitSet.valueOf(bytes);
        StringBuilder sb = new StringBuilder(bitLen);
        for (int i = 0; i < bitLen; i++) {
            sb.append(bs.get(i) ? '1' : '0');
        }
        return sb.toString();
    }


	public static void compress(String sourceFile, String resultFile) {
        try (FileInputStream in = new FileInputStream(sourceFile);
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(resultFile))) {

            byte[] inputBytes = in.readAllBytes();

            // 1) LZW
            List<Integer> lzwCodes = LZWDict.encoder(inputBytes);

            // 2) LZW codes -> bytes (2 bytes per code)
            byte[] lzwBytes = new byte[lzwCodes.size() * 2];
            int k = 0;
            for (int code : lzwCodes) {
                lzwBytes[k++] = (byte) ((code >> 8) & 0xFF);
                lzwBytes[k++] = (byte) (code & 0xFF);
            }

            // 3) Huffman encode LZW-bytes (use YOUR new method)
            EncodedData huff = HuffmanEncoder.encodeBytes(lzwBytes);
            String bitString = huff.getEncodedText();
            byte[] bitBytes = bitStringToByteArray(bitString);

            // 4) Write archive: table + bit length + byte length + data
            out.writeObject(huff.getCodeTable());
            out.writeInt(bitString.length());
            out.writeInt(bitBytes.length);
            out.write(bitBytes);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

	public static void decompress(String sourceFile, String resultFile) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(sourceFile));
            FileOutputStream out = new FileOutputStream(resultFile)) {

            // 1) Read archive
            Map<Character, String> codeTable = (Map<Character, String>) in.readObject();
            int bitLen = in.readInt();
            int byteLen = in.readInt();

            byte[] bitBytes = in.readNBytes(byteLen);
            String bitString = byteArrayToBitString(bitBytes, bitLen);

            // 2) Huffman decode -> LZW bytes
            byte[] lzwBytes = HuffmanEncoder.decodeToBytes(bitString, codeTable);

            // 3) LZW bytes -> codes
            if (lzwBytes.length % 2 != 0) throw new RuntimeException("Corrupted LZW byte stream");
            List<Integer> codes = new ArrayList<>(lzwBytes.length / 2);
            for (int i = 0; i < lzwBytes.length; i += 2) {
                int code = ((lzwBytes[i] & 0xFF) << 8) | (lzwBytes[i + 1] & 0xFF);
                codes.add(code);
            }

            // 4) LZW decode -> original file bytes
            byte[] original = LZWDict.decoder(codes);
            out.write(original);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

	public static void filesize(String sourceFile) {
        try {
            FileInputStream f = new FileInputStream(sourceFile);
            System.out.println("size: " + f.available());
            f.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

	public static boolean equal(String sourceFile, String resultFile) {
        try {
            FileInputStream fil1 = new FileInputStream(sourceFile);
            FileInputStream fil2 = new FileInputStream(resultFile);
            int b1, b2;
            byte[] bfr1 = new byte[1000];
            byte[] bfr2 = new byte[1000];
            do {
                b1 = fil1.read(bfr1);
                b2 = fil2.read(bfr2);
                if (b1 != b2) {
                    fil1.close();
                    fil2.close();
                    return false;
                }
                for (int i = 0; i < b1; i++) {
                    if (bfr1[i] != bfr2[i]) {
                        fil1.close();
                        fil2.close();
                        return false;
                    }

                }
            } while (!(b1 == -1 && b2 == -1));
            fil1.close();
            fil2.close();
            return true;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }
	public static void saveHuffmanTree(Node root, String fileName)  {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
            oos.writeObject(root);
            oos.close();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
	public static Node loadHuffmanTree(String fileName)  {
        try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
            return (Node) ois.readObject();
        }catch (Exception e){
            System.out.println("1");
        }
        return null;
    }
	public static String readBitFile(String fileName){
        StringBuilder bitSequence = new StringBuilder();
        try{
            FileInputStream fis = new FileInputStream(fileName);
            byte[] bytes = fis.readAllBytes();
            BitSet bitSet = BitSet.valueOf(bytes);

            for (int i = 0; i < bitSet.length(); i++) {
                char c = bitSet.get(i) ? '1' : '0';
            bitSequence.append(c);
            }
            fis.close();
        }catch (Exception e){
            System.out.println("er1");
        }
        return bitSequence.toString();
    }

	public static String fileReader(String file) {
		try {
            return Files.readString(Paths.get(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
		}
	}

	public static void fileWriterBitSet(String encodedText, String outFile) {
		BitSet bitSet = new BitSet(encodedText.length());

		for (int i = 0; i < encodedText.length(); i++) {
			char c = encodedText.charAt(i);
			if (c == '1') {
				bitSet.set(i, true);
			} else {
				bitSet.set(i, false);
			}
		}

		String fileName = outFile;

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(fileName);
			byte[] byteArray = bitSet.toByteArray();
			fileOutputStream.write(byteArray);
			System.out.println("BitSet successfully written to file.");
			fileOutputStream.close();
		} catch (IOException e) {
			System.err.println("Error writing BitSet to file: " + e.getMessage());
		}
	}
	public static void fileWriterText(String text, String resultfile){
        try{
            FileOutputStream fos = new FileOutputStream(resultfile);
            fos.write(text.getBytes());
            fos.close();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}

// Huffman coding start
class CodeTableManager {
    private static Map<String, Map<Character, String>> codeTables = new HashMap<>();

    public static void saveCodeTable(String fileName, Map<Character, String> codeTable) {
        codeTables.put(fileName, codeTable);
    }

    public static Map<Character, String> getCodeTable(String fileName) {
        return codeTables.get(fileName);
    }
}
class Node implements Comparable<Node>{
	char data;
	int frequency;
	Node left, right;

	public Node(char data, int frequency) {
		this.data = data;
		this.frequency = frequency;
		left = right = null;
	}

	public Node(int frequency, Node left, Node right) {
		this.frequency = frequency;
		this.left = left;
		this.right = right;
	}

	@Override
	public int compareTo(Node other) {
		return this.frequency - other.frequency;
	}
}

class EncodedData {
    private String encodedText;
    private Map<Character, String> codeTable;

    public EncodedData(String encodedText,Map<Character, String> codeTable) {
        this.encodedText = encodedText;
        this.codeTable = codeTable;
    }

    public String getEncodedText() {
        return encodedText;
    }

    public Map<Character, String> getCodeTable() {
        return codeTable;
    }
}

class HuffmanEncoder {
	public static Map<Character, String> generateCodes(Node root) {
		Map<Character, String> codes = new HashMap<>();
		generateCodesHelper(root, "", codes);
		return codes;
	}

	private static void generateCodesHelper(Node root, String code, Map<Character, String> codes) {
		if (root == null)
			return;

		if (root.left == null && root.right == null) {
			codes.put(root.data, code);
			return;
		}

		generateCodesHelper(root.left, code + "0", codes);
		generateCodesHelper(root.right, code + "1", codes);
	}

	public static EncodedData encode(String text) {
        try{
            Map<Character, Integer> frequencies = new HashMap<>();
            for (char c : text.toCharArray()) {
                frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
            }

            PriorityQueue<Node> pq = new PriorityQueue<>();
            for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
                pq.offer(new Node(entry.getKey(), entry.getValue()));
            }

            while (pq.size() > 1) {
                Node left = pq.poll();
                Node right = pq.poll();
                Node combined = new Node(left.frequency + right.frequency, left, right);
                pq.offer(combined);
            }

            Node root = pq.poll();
            Map<Character, String> codes = generateCodes(root);

            StringBuilder encodedText = new StringBuilder();
            for (char c : text.toCharArray()) {
                encodedText.append(codes.get(c));
            }

            return new EncodedData(encodedText.toString(), codes);

        }
        catch(Exception e){
            throw new RuntimeException("Huffman encoding failed: " + e.getMessage(), e);
        }
	}

	public static String decode(String encodedText,Map<Character, String> codeTable ) {
        try{
            StringBuilder decodedText = new StringBuilder();
            StringBuilder currentCode = new StringBuilder();

            for (char bit : encodedText.toCharArray()) {
                currentCode.append(bit);
                for (Map.Entry<Character, String> entry : codeTable.entrySet()) {
                    if (entry.getValue().equals(currentCode.toString())) {
                        decodedText.append(entry.getKey());
                        currentCode.setLength(0);
                        break;
                    }
                }
            }
            return decodedText.toString();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }
    public static EncodedData encodeBytes(byte[] data) {
        Map<Character, Integer> freq = new HashMap<>();
        for (byte b : data) {
            char c = (char) (b & 0xFF);
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }

        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (var e : freq.entrySet()) pq.offer(new Node(e.getKey(), e.getValue()));

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.offer(new Node(left.frequency + right.frequency, left, right));
        }

        Node root = pq.poll();
        Map<Character, String> codes = HuffmanEncoder.generateCodes(root);

        StringBuilder bits = new StringBuilder();
        for (byte b : data) {
            bits.append(codes.get((char) (b & 0xFF)));
        }
        return new EncodedData(bits.toString(), codes);
    }
    public static byte[] decodeToBytes(String bitString, Map<Character, String> codeTable) {
        // Reverse map: code -> symbol
        Map<String, Character> rev = new HashMap<>();
        for (var e : codeTable.entrySet()) rev.put(e.getValue(), e.getKey());

        ArrayList<Byte> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < bitString.length(); i++) {
            cur.append(bitString.charAt(i));
            Character sym = rev.get(cur.toString());
            if (sym != null) {
                out.add((byte) (sym & 0xFF));
                cur.setLength(0);
            }
        }

        byte[] res = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) res[i] = out.get(i);
        return res;
    }
}
// Huffman coding end
// LZW coding start
class LZWDict {
    public static List<Integer> encoder(byte[] dat) {
        if (dat.length == 0) {
            return new ArrayList<>();
        }
        int maxlength = 256;
        Map<List<Byte>, Integer> dict = new HashMap<>();
        for (int i = 0; i < maxlength; i++) {
            List<Byte> seq = new ArrayList<>();
            seq.add((byte) i);
            dict.put(seq, i);
        }

        List<Byte> tagseq = new ArrayList<>();
        List<Integer> encdat = new ArrayList<>();

        for (byte nextbyte : dat) {
            List<Byte> newseq = new ArrayList<>(tagseq);
            newseq.add(nextbyte);

            if (dict.containsKey(newseq)) {
                tagseq = newseq;
            } else {
                encdat.add(dict.get(tagseq));
                dict.put(newseq, dict.size());
                tagseq = new ArrayList<>(Arrays.asList(nextbyte));
            }
        }

        if (tagseq.size()> 0) {
            encdat.add(dict.get(tagseq));
        }

        return encdat;
    }

    public static byte[] decoder(List<Integer> enctext) {
        if (enctext.size() == 0) {
            return new byte[]{};
        }

        Map<Integer, List<Byte>> dictionary = new HashMap<>();
        int maxlength = 256;
        for (int i = 0; i < maxlength; i++) {
            List<Byte> seq = new ArrayList<>();
            seq.add((byte) i);
            dictionary.put(i, seq);
        }

        List<Byte> result = new ArrayList<>();
        int pagkod = enctext.get(0);
        result.addAll(dictionary.get(pagkod));

        for (int i = 1; i < enctext.size(); i++) {
            int currentCode = enctext.get(i);
            List<Byte> ieej = dictionary.getOrDefault(currentCode, new ArrayList<>(dictionary.get(pagkod)));
            if (currentCode == dictionary.size()) {
                ieej.add(dictionary.get(pagkod).get(0));
            } else if (!dictionary.containsKey(currentCode)) {
                throw new IllegalArgumentException("Bad compressed code");
            }

            List<Byte> pagkodBytes = dictionary.get(pagkod);
            byte[] jauniej = Arrays.copyOf(tobytearray(pagkodBytes), pagkodBytes.size() + 1);
            jauniej[jauniej.length - 1] = ieej.get(0);
            result.addAll(ieej);
            dictionary.put(dictionary.size(), tobytelist(jauniej));
            pagkod = currentCode;
        }
        return tobytearray(result);
    }

    private static byte[] tobytearray(List<Byte> list) {
        byte[] ret = new byte[list.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }

    private static List<Byte> tobytelist(byte[] array) {
        List<Byte> list = new ArrayList<>();
        for (byte b : array) {
            list.add(b);
        }
        return list;
    }
}
// LZW coding end
