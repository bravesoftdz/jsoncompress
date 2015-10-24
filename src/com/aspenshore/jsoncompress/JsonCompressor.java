import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonCompressor {

    public byte[] compress6(byte[] sourceBytes) {
        int resultLength = sourceBytes.length * 6 / 8;
        if (resultLength * 8 < sourceBytes.length * 6) {
            resultLength++;
        }
        byte[] resultBytes = new byte[resultLength];
        int offset = 0;
        for (int i = 0; i < resultBytes.length; i++) {
            resultBytes[i] = 0;
        }
        for (int i = 0; i < sourceBytes.length; i++) {
            System.err.println("i = " + i);
            byte importantBits = (byte)(0xff & sourceBytes[i]);
            if ((offset / 8) * 8 == offset) {
                // We're starting on a byte boundary
                System.err.println("We're starting on a byte boundary");
                int byteNo = offset / 8;
                System.err.println("resultBytes[" + byteNo + "] = " + toBinary(     (byte)(0xff & (importantBits << 2))       ));
                resultBytes[byteNo] = (byte)(0xff & (importantBits << 2));
            } else if (((offset - 1) / 8) * 8 == offset - 1) {
                // We're 1 position into a byte boundary
                System.err.println("We're 1 position into a byte boundary");
                int byteNo = offset / 8;
                System.err.println("resultBytes[" + byteNo + "] = " + toBinary(     (byte)(0xff & (resultBytes[byteNo] | importantBits))       ));
                resultBytes[byteNo] = (byte)(0xff & (resultBytes[byteNo] | importantBits));
            } else if (((offset - 2) / 8) * 8 == offset - 2) {
                // We're 2 positions into a byte boundary
                System.err.println("We're 2 positions into a byte boundary");
                int byteNo = offset / 8;
                System.err.println("resultBytes[" + byteNo + "] = " + toBinary(     (byte)(0xff & (resultBytes[byteNo] | importantBits))       ));
                resultBytes[byteNo] = (byte)(0xff & (resultBytes[byteNo] | importantBits));
            } else {
                // We're crossing a byte boundary
                System.err.println("We're crossing a byte boundary");
                int byteNo = offset / 8;
                int into = offset - ((offset / 8) * 8);
                System.err.println("resultBytes[" + byteNo + "] = " + toBinary(     (byte)(0xff & (resultBytes[byteNo] | (importantBits >> (into - 2))))       ));
                resultBytes[byteNo] = (byte)(0xff & (resultBytes[byteNo] | (importantBits >> (into - 2))));
                System.err.println("resultBytes[" + (byteNo + 1) + "] = " + toBinary(     (byte)(0xff & ((importantBits << (9 - into))))       ));
                resultBytes[byteNo + 1] = (byte)(0xff & ((importantBits << (9 - into))));
            }
            offset = offset + 6;
            System.err.println("");
        }
        return resultBytes;
    }

    private static String toBinary(int sourceByte) {
        return String.format("%8s", Integer.toBinaryString(sourceByte & 0xFF)).replace(' ', '0');
    }    

    public byte[] compressJson(String json) {
        int options = 0;
        String walkFormat = walkFormat(json);
        byte[] compress = compress(walkFormat.getBytes());
        byte[] bytesWithOptions = new byte[compress.length + 1];
        bytesWithOptions[0] = (byte)options;
        System.arraycopy(compress, 0, bytesWithOptions, 1, compress.length);
        return bytesWithOptions;
    }

    public String expandJson(byte[] bytesWithOptions) {
        int options = bytesWithOptions[0];
        byte[] bytes = new byte[bytesWithOptions.length - 1];
        System.arraycopy(bytesWithOptions, 1, bytes, 0, bytesWithOptions.length - 1);
        String expandedString;
        expandedString = new String(expand(bytes));
        return unwalkFormat(expandedString);
    }

    public byte[] expand(byte[] sourceBytes) {
        byte[] resultBytes = new byte[sourceBytes.length * 8 / 7];
        int offset = 0;
        for (int i = 0; i < resultBytes.length; i++) {
            if ((offset / 8) * 8 == offset) {
                // We're starting on a byte boundary
                int byteNo = offset / 8;
                resultBytes[i] = (byte)(0xff & ((0xff & sourceBytes[byteNo]) >> 1));
            }
            else if (((offset - 1) / 8) * 8 == offset - 1) {
                // We're 1 position into a byte boundary
                int byteNo = offset / 8;
                resultBytes[i] = (byte)(0xff & ((0x7f & sourceBytes[byteNo])));
            } else {
                // We're crossing a byte boundary
                int firstByteNo = offset / 8;
                int secondByteNo = firstByteNo + 1;
                int into = offset - ((offset / 8) * 8);
                byte firstByte = (byte)(0x7f & ((0xff & sourceBytes[firstByteNo]) << (into - 1)));
                byte secondByte = (byte)(0xff & ((0xff & sourceBytes[secondByteNo]) >> (9 - into)));
                resultBytes[i] = (byte)(0xff & (firstByte | secondByte));
            }
            offset = offset + 7;
        }
        if ((resultBytes.length > 0) && (resultBytes[resultBytes.length - 1] == 0)) {
            byte[] trimmed = new byte[resultBytes.length - 1];
            System.arraycopy(resultBytes, 0, trimmed, 0, trimmed.length);
            resultBytes = trimmed;
        }
        return resultBytes;
    }

    public byte[] compress(byte[] sourceBytes) {
        int resultLength = sourceBytes.length * 7 / 8;
        if (resultLength * 8 < sourceBytes.length * 7) {
            resultLength++;
        }
        byte[] resultBytes = new byte[resultLength];
        int offset = 0;
        for (int i = 0; i < resultBytes.length; i++) {
            resultBytes[i] = 0;
        }
        for (int i = 0; i < sourceBytes.length; i++) {
            byte importantBits = (byte)(0xff & sourceBytes[i]);
            if ((offset / 8) * 8 == offset) {
                // We're starting on a byte boundary
                int byteNo = offset / 8;
                resultBytes[byteNo] = (byte)(0xff & (importantBits << 1));
            }
            else if (((offset - 1) / 8) * 8 == offset - 1) {
                // We're 1 position into a byte boundary
                int byteNo = offset / 8;
                resultBytes[byteNo] = (byte)(0xff & (resultBytes[byteNo] | importantBits));
            } else {
                // We're crossing a byte boundary
                int byteNo = offset / 8;
                int into = offset - ((offset / 8) * 8);
                resultBytes[byteNo] = (byte)(0xff & (resultBytes[byteNo] | (importantBits >> (into - 1))));
                resultBytes[byteNo + 1] = (byte)(0xff & ((importantBits << (9 - into))));
            }
            offset = offset + 7;
        }
        return resultBytes;
    }
    
    public String walkFormat(String json) {
        String s = json.trim();
        StringBuilder sb = new StringBuilder();
        char[] chars = s.toCharArray();
        boolean inString = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if ((c == ' ') && !inString) {
                continue;
            }
            if ((c == '\n') && !inString) {
                continue;
            }
            if ((c == '}') && !inString) {
                sb.append('^');
                continue;
            }
            if ((c == ']') && !inString) {
                sb.append('^');
                continue;
            }
            if ((c == '{') && !inString) {
                sb.append('+');
                continue;
            }
            if ((c == '[') && !inString) {
                sb.append('*');
                continue;
            }
            if ((c == ',') && !inString) {
                sb.append('>');
                continue;
            }
            if ((c == ':') && !inString) {
                sb.append('>');
                continue;
            }
            sb.append(c);
        }
        String s1 = sb.toString();
        if (s1.startsWith("+")) {
            s1 = s1.substring(1, s1.length());
        }
        while (s1.endsWith("^")) {
            s1 = s1.substring(0, s1.length() - 1);
        }
        s1=s1.replaceAll("\\>\\+", "+");
        s1=s1.replaceAll("\\>\\*", "*");
        s1=s1.replaceAll("\\^\\>", "^");
        s1=s1.replaceAll("\\^\\+", "^");
        return s1;
    }

    String aWalk;
    int pos = 0;
    public String unwalkFormat(String walk) {
        aWalk = walk;
        if (!aWalk.startsWith("*")) {
            aWalk = "+" + aWalk + "^";
        }
        pos = 0;
        Object jsonObject = null;
        try {
            jsonObject = readWalk();
        } catch (JSONException e) {
            throw new RuntimeException("Can't parse walk", e);
        }
        return jsonObject.toString();
    }

    public Object readWalk() throws JSONException {
        char c = aWalk.charAt(pos);
        if (c == '+') {
            pos++;
            return readMap();
        }
        if (c == '*') {
            pos++;
            return readArray();
        }
        return readString();
    }

    public JSONObject readMap() throws JSONException {
        char c = aWalk.charAt(pos);
        JSONObject result = new JSONObject();
        while ((pos < aWalk.length()) && (c != '^')) {
            String key = readString();
            c = aWalk.charAt(++pos);
            if (c == '>') {
                pos++;
            }
            Object value = readWalk();
            result.put(key, value);
            if (pos == aWalk.length() - 1) {
                break;
            }
            c = aWalk.charAt(++pos);
            if (c == '>') {
                pos++;
            }
        }
        return result;
    }

    public JSONArray readArray() throws JSONException {
        char c = aWalk.charAt(pos);
        JSONArray result = new JSONArray();
        while ((pos < aWalk.length()) && (c != '^')) {
            Object value = readWalk();
            result.put(value);
            if (pos == aWalk.length() - 1) {
                break;
            }
            c = aWalk.charAt(++pos);
            if (c == '>') {
                pos++;
            }
        }
        return result;
    }

    public String readString() {
        StringBuilder sb = new StringBuilder();
        char c = aWalk.charAt(pos);
        int start = pos;
        while ((c != '^') && (c != '>') && (c != '+') && (c != '*')) {
            sb.append(c);
            if (pos == aWalk.length() - 1) {
                pos++;
                break;
            }
            c = aWalk.charAt(++pos);
        }
        pos--;
        return sb.toString();
    }
}
