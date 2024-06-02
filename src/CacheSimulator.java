import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class CacheSimulator {

    static int hits = 0;
    static int misses = 0;
    static int evictions = 0;
    static Map<Long, Integer> ram = new HashMap<>();

    public static void readTraceFile(Cache cache, String tracefile) {
        try (BufferedReader br = new BufferedReader(new FileReader(tracefile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(", ");
                char operation = parts[0].charAt(0);
                long operationAddress = Long.parseLong(parts[0].substring(2), 16);
                long size = Long.parseLong(parts[1]);
                String data = null;
                if (operation != 'L') {
                    data = parts[2];
                }
                accessCache(cache, operationAddress, size, operation, data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void accessCache(Cache cache, long operationAddress, long size, char op, String data) {
        int setIndex = (int) ((operationAddress >> cache.blockSize) & (cache.setCount - 1));
        long tag = operationAddress >> (cache.blockSize + Integer.numberOfTrailingZeros(cache.setCount));

        CacheSet set = cache.sets[setIndex];
        boolean hit = false;

        for (CacheLine line : set.lines) {
            if (line.valid && line.tag == tag) {
                hits++;
                hit = true;
                break;
            }
        }

        if (!hit) {
            misses++;
            boolean placed = false;
            for (CacheLine line : set.lines) {
                if (!line.valid) {
                    line.valid = true;
                    line.tag = tag;
                    placed = true;
                    loadFromRAM(line, operationAddress, cache.blockSize);
                    break;
                }
            }

            if (!placed) {
                evictions++;
                CacheLine line = set.lines[0];
                System.arraycopy(set.lines, 1, set.lines, 0, set.lines.length - 1);
                set.lines[set.lines.length - 1] = line;
                line.tag = tag;
                loadFromRAM(line, operationAddress, cache.blockSize);
            }
        }

        if (op == 'S' && data != null) {
            writeToRAM(operationAddress, data);
        }
    }


    private static void loadFromRAM(CacheLine line, long address, int blockSize) {
        for (int i = 0; i < blockSize; i++) {
            line.data[i] = ram.getOrDefault(address + i, 0).byteValue();
        }
    }

    private static void writeToRAM(long address, String data) {
        // Write data to RAM (as hex string)
        byte[] bytes = hexStringToByteArray(data);
        for (int i = 0; i < bytes.length; i++) {
            ram.put(address + i, (int) bytes[i]);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static void loadRAM(String ramFile) {
        try (RandomAccessFile raf = new RandomAccessFile(ramFile, "r")) {
            long address = 0;
            while (raf.getFilePointer() < raf.length()) {
                int data = raf.readInt();
                ram.put(address, data);
                address += 4;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printResults(int hits, int misses, int evictions) {
        System.out.printf("hits:%d misses:%d evictions:%d\n", hits, misses, evictions);
    }

    public static void writeCacheToFile(Cache cache) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("cache.txt"))) {
            for (int i = 0; i < cache.setCount; i++) {
                CacheSet set = cache.sets[i];
                for (CacheLine line : set.lines) {
                    writer.write(String.format("set:%d tag:%d valid:%b data:%s\n",
                            i, line.tag, line.valid, bytesToHex(line.data)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public static void main(String[] args) {
        int s = 0, E = 0, b = 0;
        String tracefile = null;
        String ramFile = "RAM.dat";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-s":
                    s = Integer.parseInt(args[++i]);
                    break;
                case "-E":
                    E = Integer.parseInt(args[++i]);
                    break;
                case "-b":
                    b = Integer.parseInt(args[++i]);
                    break;
                case "-t":
                    tracefile = args[++i];
                    break;
            }
        }

        int setCount = (int) Math.pow(2, s);
        int blockSize = (int) Math.pow(2, b);

        Cache cache = new Cache(setCount, E, blockSize);

        loadRAM(ramFile);
        readTraceFile(cache, tracefile);

        writeCacheToFile(cache);
        printResults(hits, misses, evictions);
    }
}





