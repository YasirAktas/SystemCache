import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CacheSimulator {

    static int hits = 0;
    static int misses = 0;
    static int evictions = 0;

    public static void readTraceFile(Cache cache, String tracefile) {
        try (BufferedReader br = new BufferedReader(new FileReader(tracefile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(", ");
                char operation = parts[0].charAt(0);
                long operationAddress = Long.parseLong(parts[0].substring(2),16);
                long size = Long.parseLong(parts[1]);
                String data;
                if (operation != 'L')
                    data = parts[2];
                accessCache(cache, operationAddress, size, operation);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void accessCache(Cache cache, long operationAddress, long size, char op) {
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
                    break;
                }
            }

            if (!placed) {
                evictions++;
                CacheLine line = set.lines[0]; // FIFO için ilk satırı evict et
                for (int i = 1; i < set.lines.length; i++) {
                    set.lines[i - 1] = set.lines[i];
                }
                set.lines[set.lines.length - 1] = line;
                line.tag = tag;
            }
        }
    }

    public static void printResults(int hits, int misses, int evictions) {
        System.out.printf("hits:%d misses:%d evictions:%d\n", hits, misses, evictions);
    }

    public static void main(String[] args) {
        int s = 0, E = 0, b = 0;
        String tracefile = null;

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

        int setCount = 1 << s;
        int blockSize = 1 << b;

        Cache cache = new Cache(setCount, E, blockSize);

        readTraceFile(cache, tracefile);

        printResults(hits, misses, evictions);
    }
}
